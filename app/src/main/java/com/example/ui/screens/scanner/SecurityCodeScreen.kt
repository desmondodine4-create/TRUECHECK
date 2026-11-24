package com.example.ui.screens.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.auth.trueCheckFieldColors
import com.example.ui.theme.DeepTeal
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedSoft
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LightMintBackground
import com.example.ui.theme.MainText
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SoftMint

@Composable
fun SecurityCodeScreen(
    scannerViewModel: ScannerViewModel,
    onBack: () -> Unit,
    onSubmitCode: (String) -> Unit
) {
    val state by scannerViewModel.securityCodeState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = PureWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 560.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("security_code_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainText)
                    }
                    Text("Security Code Verification", style = MaterialTheme.typography.titleLarge, color = MainText)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Enter Security Code",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MainText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter the unique authentication code printed or revealed on your product packaging.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = state.code,
                    onValueChange = { scannerViewModel.updateSecurityCode(it) },
                    label = { Text("Authentication Code") },
                    placeholder = { Text("e.g. TC88429104") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = "Security code") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    colors = trueCheckFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("security_code_input")
                )

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRedSoft),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.error!!,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { scannerViewModel.validateAndSubmitSecurityCode(onSubmitCode) },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepTeal, contentColor = PureWhite),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("verify_product_button")
                ) {
                    Text(
                        text = "VERIFY PRODUCT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = LightMintBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = DeepTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "NAFDAC MAS & Provider Status",
                                style = MaterialTheme.typography.titleSmall,
                                color = MainText
                            )
                        }
                        Text(
                            text = "Live NAFDAC Mobile Authentication Service (MAS) lookups require authorized gateway credentials. Unconfigured codes return Unable to Verify rather than false confirmation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                        Surface(
                            color = SoftMint,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Demonstration Sample PINs:\n• TC88429104 (First scan = Real, repeat = Suspicious)\n• MAS40912877 (Sample Cosmetics)\n• FAKE99001122 (Flagged Counterfeit)",
                                style = MaterialTheme.typography.bodySmall,
                                color = DeepTeal,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
