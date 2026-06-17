package com.somewhere.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.somewhere.app.ui.theme.SomewhereColors

@Composable
fun LegalDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            // Glassmorphism Container
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SomewhereColors.GlassBackground)
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = SomewhereColors.TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val blocks = content.split("\n\n")
                        for (block in blocks) {
                            when {
                                block.startsWith("## ") -> {
                                    Text(
                                        text = block.removePrefix("## "),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 20.sp
                                        ),
                                        color = SomewhereColors.TextPrimary,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                    )
                                }
                                block.startsWith("### ") -> {
                                    Text(
                                        text = block.removePrefix("### "),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 18.sp
                                        ),
                                        color = SomewhereColors.TextPrimary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                else -> {
                                    val lines = block.split("\n")
                                    for (line in lines) {
                                        if (line.startsWith("* ") || line.startsWith("✓ ") || line.startsWith("✗ ")) {
                                            Row(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
                                                Text(
                                                    text = line.take(1) + " ",
                                                    color = SomewhereColors.GlowAccent,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = line.drop(2),
                                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                                    color = SomewhereColors.TextSecondary
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = line,
                                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                                color = SomewhereColors.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    SomewhereButton(
                        text = "Back",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
