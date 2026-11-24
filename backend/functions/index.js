const crypto = require("crypto");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

function utcDateKey(date = new Date()) {
  return date.toISOString().slice(0, 10); // YYYY-MM-DD in UTC
}

function hashSecurityCode(rawCode) {
  return crypto
    .createHash("sha256")
    .update(String(rawCode).trim().toUpperCase())
    .digest("hex");
}

async function assertAdmin(auth) {
  if (!auth || !auth.uid) {
    throw new HttpsError("unauthenticated", "Authentication is required.");
  }
  const userSnap = await db.collection("users").doc(auth.uid).get();
  if (!userSnap.exists || userSnap.data().role !== "ADMIN" || userSnap.data().status !== "ACTIVE") {
    throw new HttpsError("permission-denied", "Administrator privileges are required.");
  }
}

/**
 * Ensures newly registered Firebase Auth users receive a consumer-only Firestore profile
 * and can never self-assign the ADMIN role.
 */
exports.initializeUserProfile = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in before initializing your profile.");
  }
  const uid = request.auth.uid;
  const fullName = String(request.data?.fullName || "").trim();
  const email = String(request.auth.token.email || request.data?.email || "").trim().toLowerCase();

  if (fullName.length < 2 || fullName.length > 120) {
    throw new HttpsError("invalid-argument", "Full name must be between 2 and 120 characters.");
  }

  const userRef = db.collection("users").doc(uid);
  await db.runTransaction(async (tx) => {
    const existing = await tx.get(userRef);
    const now = admin.firestore.FieldValue.serverTimestamp();
    if (!existing.exists) {
      tx.set(userRef, {
        uid,
        fullName,
        email,
        role: "CONSUMER",
        status: "ACTIVE",
        emailVerified: Boolean(request.auth.token.email_verified),
        createdAt: now,
        lastActiveAt: now,
      });
    } else {
      tx.update(userRef, { fullName, lastActiveAt: now });
    }
  });

  return { ok: true, role: "CONSUMER" };
});

/**
 * Atomically verifies a manufacturer scratch-off security code inside a Firestore transaction.
 * First redemption returns REAL; subsequent redemptions return SUSPICIOUS; flagged lots return FAKE.
 */
exports.verifySecurityCode = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in to verify a product security code.");
  }
  const uid = request.auth.uid;
  const rawCode = String(request.data?.code || "").trim().toUpperCase();
  if (!/^[A-Z0-9-]{6,24}$/.test(rawCode)) {
    throw new HttpsError("invalid-argument", "Security code must be 6-24 alphanumeric characters.");
  }

  const codeHash = hashSecurityCode(rawCode);
  const codeRef = db.collection("productCodes").doc(codeHash);

  const outcome = await db.runTransaction(async (tx) => {
    const codeSnap = await tx.get(codeRef);
    const now = admin.firestore.FieldValue.serverTimestamp();

    if (!codeSnap.exists) {
      return {
        status: "UNVERIFIED",
        reason: "No trusted manufacturer or regulatory record matched this security code.",
      };
    }

    const data = codeSnap.data();
    let status = "REAL";
    let reason = "Security code verified for the first time.";

    if (data.authenticityFlag === "COUNTERFEIT") {
      status = "FAKE";
      reason = "Security code matches a revoked or known counterfeit lot.";
    } else if ((data.verificationCount || 0) >= 1) {
      status = "SUSPICIOUS";
      reason = `Warning: security code was already verified ${data.verificationCount} time(s).`;
    }

    tx.update(codeRef, {
      verificationCount: admin.firestore.FieldValue.increment(1),
      firstVerifiedAt: data.firstVerifiedAt || now,
      firstVerifiedByUid: data.firstVerifiedByUid || uid,
      lastCheckedAt: now,
    });

    const auditRef = db.collection("securityCodeVerifications").doc();
    tx.set(auditRef, {
      verificationId: auditRef.id,
      codeHash,
      uid,
      outcome: status,
      checkedAt: now,
    });

    return {
      status,
      reason,
      productId: data.productId,
      batchId: data.batchId || null,
    };
  });

  return outcome;
});

/**
 * Records UTC-dated daily scan counters whenever a new scan is written.
 */
exports.onScanCreated = onDocumentCreated("scans/{scanId}", async (event) => {
  const scan = event.data?.data();
  if (!scan) return;

  const dateUtc = utcDateKey();
  const analyticsRef = db.collection("dailyAnalytics").doc(dateUtc);
  const statusFieldMap = {
    REAL: "realCount",
    SUSPICIOUS: "suspiciousCount",
    FAKE: "fakeCount",
    UNVERIFIED: "unverifiedCount",
  };
  const statusField = statusFieldMap[scan.status] || "unverifiedCount";

  await analyticsRef.set(
    {
      dateUtc,
      totalScans: admin.firestore.FieldValue.increment(1),
      [statusField]: admin.firestore.FieldValue.increment(1),
      [`methods.${scan.method || "OTHER"}`]: admin.firestore.FieldValue.increment(1),
      [`categories.${scan.category || "OTHER"}`]: admin.firestore.FieldValue.increment(1),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true }
  );
});

/**
 * Maintains UTC-dated unique DAU counts when userActivity/${uid}_${yyyyMMdd} is created.
 */
exports.onUserActivityWritten = onDocumentWritten("userActivity/{activityKey}", async (event) => {
  const beforeExists = event.data?.before?.exists;
  const afterData = event.data?.after?.data();
  if (beforeExists || !afterData?.activityDateUtc) return;

  await db.collection("dailyAnalytics").doc(afterData.activityDateUtc).set(
    {
      dateUtc: afterData.activityDateUtc,
      dau: admin.firestore.FieldValue.increment(1),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true }
  );
});

/**
 * Scheduled daily UTC reconciliation for MAU and exact user counts.
 */
exports.recomputeDailyRollup = onSchedule("every 60 minutes", async () => {
  const now = new Date();
  const todayUtc = utcDateKey(now);
  const thirtyDaysAgo = new Date(now.getTime() - 29 * 86400000);
  const thirtyDaysKey = utcDateKey(thirtyDaysAgo);

  const [usersSnap, dauSnap, mauSnap] = await Promise.all([
    db.collection("users").count().get(),
    db.collection("userActivity").where("activityDateUtc", "==", todayUtc).count().get(),
    db.collection("userActivity").where("activityDateUtc", ">=", thirtyDaysKey).get(),
  ]);

  const distinctMauUsers = new Set();
  mauSnap.forEach((doc) => {
    const uid = doc.data().uid;
    if (uid) distinctMauUsers.add(uid);
  });

  await db.collection("dailyAnalytics").doc(todayUtc).set(
    {
      dateUtc: todayUtc,
      totalUsers: usersSnap.data().count,
      dau: dauSnap.data().count,
      mau: distinctMauUsers.size,
      reconciledAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true }
  );
});

/**
 * Admin-only callable function to suspend or reactivate a consumer account.
 */
exports.setAccountStatus = onCall(async (request) => {
  await assertAdmin(request.auth);
  const targetUid = String(request.data?.targetUid || "");
  const status = String(request.data?.status || "");
  if (!targetUid || !["ACTIVE", "SUSPENDED"].includes(status)) {
    throw new HttpsError("invalid-argument", "Invalid targetUid or status.");
  }
  if (targetUid === request.auth.uid) {
    throw new HttpsError("failed-precondition", "Administrators cannot suspend their own account.");
  }

  await db.collection("users").doc(targetUid).update({ status });
  await admin.auth().updateUser(targetUid, { disabled: status === "SUSPENDED" });
  return { ok: true, targetUid, status };
});
