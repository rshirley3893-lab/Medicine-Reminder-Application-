package com.example.ui.stock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.StockTransactionEntity
import com.example.ui.components.SectionHeader
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusOnTrack
import com.example.ui.theme.StatusPending
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAlertsScreen(
    medications: List<MedicationEntity>,
    onRefillStock: (Long, Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    var refillTargetMed by remember { mutableStateOf<MedicationEntity?>(null) }
    var customRefillQty by remember { mutableStateOf("30") }

    val lowStockList = remember(medications) {
        medications.filter { it.stockQuantity <= it.lowStockThreshold }
    }

    val adequateStockList = remember(medications) {
        medications.filter { it.stockQuantity > it.lowStockThreshold }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock & Inventory Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lowStockList.isNotEmpty()) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (lowStockList.isNotEmpty()) Color(0xFFE65100).copy(alpha = 0.2f) else StatusOnTrack.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (lowStockList.isNotEmpty()) Icons.Default.Warning else Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = if (lowStockList.isNotEmpty()) Color(0xFFE65100) else StatusOnTrack,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (lowStockList.isNotEmpty()) "${lowStockList.size} Medicine(s) Require Refill" else "All Medications In Stock",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (lowStockList.isNotEmpty()) Color(0xFFE65100) else StatusOnTrack
                            )
                            Text(
                                text = if (lowStockList.isNotEmpty()) "Quantities are at or below safety warning thresholds." else "Sufficient inventory recorded for upcoming daily schedules.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Low Stock Section
            if (lowStockList.isNotEmpty()) {
                item {
                    SectionHeader(title = "⚠️ Depleted & Low Stock (${lowStockList.size})")
                }

                items(lowStockList, key = { it.id }) { med ->
                    StockMedicationCard(
                        med = med,
                        isLowStock = true,
                        onQuickRefill = { qty -> onRefillStock(med.id, qty) },
                        onCustomRefill = { refillTargetMed = med }
                    )
                }
            }

            // Adequate Stock Section
            item {
                SectionHeader(title = "Adequate Inventory (${adequateStockList.size})")
            }

            if (adequateStockList.isEmpty() && lowStockList.isEmpty()) {
                item {
                    Text(
                        text = "No active medicines in database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(adequateStockList, key = { it.id }) { med ->
                    StockMedicationCard(
                        med = med,
                        isLowStock = false,
                        onQuickRefill = { qty -> onRefillStock(med.id, qty) },
                        onCustomRefill = { refillTargetMed = med }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Custom Refill Dialog
    if (refillTargetMed != null) {
        val target = refillTargetMed!!
        AlertDialog(
            onDismissRequest = { refillTargetMed = null },
            title = { Text("Refill Stock: ${target.name}") },
            text = {
                Column {
                    Text(
                        text = "Current Stock: ${target.stockQuantity.toInt()} ${target.doseUnit.symbol}s\nThreshold: ${target.lowStockThreshold.toInt()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customRefillQty,
                        onValueChange = { customRefillQty = it },
                        label = { Text("Add Units (${target.doseUnit.symbol}s)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = customRefillQty.toDoubleOrNull() ?: 30.0
                        onRefillStock(target.id, qty)
                        refillTargetMed = null
                    }
                ) {
                    Text("Add to Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { refillTargetMed = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StockMedicationCard(
    med: MedicationEntity,
    isLowStock: Boolean,
    onQuickRefill: (Double) -> Unit,
    onCustomRefill: () -> Unit
) {
    val progress = (med.stockQuantity / (med.lowStockThreshold * 3.0)).coerceIn(0.0, 1.0).toFloat()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = med.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${med.strength} • ${med.form.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isLowStock) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = "${med.stockQuantity.toInt()} ${med.doseUnit.symbol}s left",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) StatusMissed else StatusOnTrack,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stock Bar
            LinearProgressIndicator(
                progress = { progress },
                color = if (isLowStock) StatusMissed else StatusOnTrack,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Refill Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { onQuickRefill(10.0) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text("+10", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = { onQuickRefill(30.0) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text("+30", fontSize = 12.sp)
                }

                Button(
                    onClick = onCustomRefill,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom Refill", fontSize = 12.sp)
                }
            }
        }
    }
}
