package com.example.data.local

import com.example.data.model.AccountStatus
import com.example.data.model.ProductCategory
import com.example.data.model.UserRole

object SampleCatalogSeeder {
    const val TRUSTED_ADMIN_UID = "admin_truecheck_root"
    const val TRUSTED_ADMIN_EMAIL = "admin@truecheck.app"

    suspend fun seedIfNeeded(
        userDao: UserDao,
        productCatalogDao: ProductCatalogDao,
        securityCodeDao: SecurityCodeDao
    ) {
        // Seed trusted initial administrator account if absent (non-consumer-assignable).
        if (userDao.getById(TRUSTED_ADMIN_UID) == null && userDao.getByEmail(TRUSTED_ADMIN_EMAIL) == null) {
            val salt = PasswordHasher.generateSaltHex()
            val hash = PasswordHasher.hashPassword("TrueCheckAdmin#2026", salt)
            val now = System.currentTimeMillis()
            userDao.insert(
                UserEntity(
                    id = TRUSTED_ADMIN_UID,
                    fullName = "TRUECHECK Administrator",
                    email = TRUSTED_ADMIN_EMAIL,
                    passwordHash = hash,
                    passwordSalt = salt,
                    role = UserRole.ADMIN.name,
                    status = AccountStatus.ACTIVE.name,
                    emailVerified = true,
                    createdAtUtc = now,
                    lastActiveAtUtc = now
                )
            )
        }

        if (productCatalogDao.countProducts() == 0) {
            productCatalogDao.insertAll(
                listOf(
                    ProductEntity(
                        id = "demo_prod_1",
                        barcode = "8901234567890",
                        qrIdentifier = "TC-VERIFY-AMOX-2026",
                        productName = "Amoxil Capsules 500mg",
                        brand = "MedSafe Pharma",
                        category = ProductCategory.MEDICINES.name,
                        manufacturer = "MedSafe Laboratories Ltd",
                        registrationNumber = "NAFDAC-DEMO-A4-0192",
                        batchNumber = "MS-2604A",
                        expiryDate = "2028-06",
                        registryStatus = "REAL",
                        sourceLabel = "TRUECHECK Demonstration Registry (Sample Data)",
                        safetyNote = "Batch active and verified in demonstration catalog."
                    ),
                    ProductEntity(
                        id = "demo_prod_2",
                        barcode = "5012345678900",
                        qrIdentifier = "TC-VERIFY-GLOW-991",
                        productName = "HydraPure Vitamin C Serum",
                        brand = "BotaniCare",
                        category = ProductCategory.BEAUTY_PERSONAL_CARE.name,
                        manufacturer = "BotaniCare Cosmetics",
                        registrationNumber = "NAFDAC-DEMO-C2-4410",
                        batchNumber = "BC-8812",
                        expiryDate = "2027-11",
                        registryStatus = "REAL",
                        sourceLabel = "TRUECHECK Demonstration Registry (Sample Data)",
                        safetyNote = "Authorized formulation registered in demonstration catalog."
                    ),
                    ProductEntity(
                        id = "demo_prod_3",
                        barcode = "6151100043219",
                        qrIdentifier = "TC-SUSP-MILK-404",
                        productName = "NutriGrow Infant Formula 400g",
                        brand = "NutriGrow",
                        category = ProductCategory.FOOD_BEVERAGES.name,
                        manufacturer = "NutriGrow Foods",
                        registrationNumber = "NAFDAC-DEMO-F1-2104",
                        batchNumber = "NG-RECALL-09",
                        expiryDate = "2025-12",
                        registryStatus = "SUSPICIOUS",
                        sourceLabel = "TRUECHECK Demonstration Registry (Sample Data)",
                        safetyNote = "Batch mismatch or expired packaging window flagged for inspection."
                    ),
                    ProductEntity(
                        id = "demo_prod_4",
                        barcode = "0001112223334",
                        qrIdentifier = "TC-FAKE-COUNTERFEIT-00",
                        productName = "Coartem Antimalarial Pack (Flagged)",
                        brand = "Unverified Copy",
                        category = ProductCategory.MEDICINES.name,
                        manufacturer = "Unknown / Revoked Lot",
                        registrationNumber = "REVOKED-LOT-882",
                        batchNumber = "CF-000X",
                        expiryDate = "2024-01",
                        registryStatus = "FAKE",
                        sourceLabel = "TRUECHECK Demonstration Safety Alert (Sample Data)",
                        safetyNote = "Identifier matches known counterfeit / recalled lot in demonstration database."
                    )
                )
            )
        }

        if (securityCodeDao.countCodes() == 0) {
            securityCodeDao.insertAll(
                listOf(
                    SecurityCodeEntity(
                        code = "TC88429104",
                        productId = "demo_prod_1",
                        productName = "Amoxil Capsules 500mg",
                        brand = "MedSafe Pharma",
                        category = ProductCategory.MEDICINES.name,
                        manufacturer = "MedSafe Laboratories Ltd",
                        registrationNumber = "NAFDAC-DEMO-A4-0192",
                        batchNumber = "MS-2604A",
                        expiryDate = "2028-06",
                        authenticityFlag = "GENUINE",
                        verificationCount = 0,
                        firstVerifiedAtUtc = null,
                        firstVerifiedByUserId = null,
                        sourceLabel = "TRUECHECK Demonstration MAS Registry (Sample Data)"
                    ),
                    SecurityCodeEntity(
                        code = "MAS40912877",
                        productId = "demo_prod_2",
                        productName = "HydraPure Vitamin C Serum",
                        brand = "BotaniCare",
                        category = ProductCategory.BEAUTY_PERSONAL_CARE.name,
                        manufacturer = "BotaniCare Cosmetics",
                        registrationNumber = "NAFDAC-DEMO-C2-4410",
                        batchNumber = "BC-8812",
                        expiryDate = "2027-11",
                        authenticityFlag = "GENUINE",
                        verificationCount = 0,
                        firstVerifiedAtUtc = null,
                        firstVerifiedByUserId = null,
                        sourceLabel = "TRUECHECK Demonstration MAS Registry (Sample Data)"
                    ),
                    SecurityCodeEntity(
                        code = "FAKE99001122",
                        productId = "demo_prod_4",
                        productName = "Coartem Antimalarial Pack (Flagged)",
                        brand = "Unverified Copy",
                        category = ProductCategory.MEDICINES.name,
                        manufacturer = "Unknown / Revoked Lot",
                        registrationNumber = "REVOKED-LOT-882",
                        batchNumber = "CF-000X",
                        expiryDate = "2024-01",
                        authenticityFlag = "COUNTERFEIT",
                        verificationCount = 3,
                        firstVerifiedAtUtc = System.currentTimeMillis() - 86_400_000L,
                        firstVerifiedByUserId = "system_flag",
                        sourceLabel = "TRUECHECK Demonstration Safety Alert (Sample Data)"
                    )
                )
            )
        }
    }
}
