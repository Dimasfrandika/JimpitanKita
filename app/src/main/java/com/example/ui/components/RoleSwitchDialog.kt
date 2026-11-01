package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.House
import com.example.data.model.RtConfig
import com.example.data.model.UserRole

@Composable
fun RoleSwitchDialog(
    currentRole: UserRole,
    activeHouseId: String,
    houses: List<House>,
    config: RtConfig,
    isBendaharaUnlocked: Boolean,
    isPetugasUnlocked: Boolean,
    onDismiss: () -> Unit,
    onSelectRole: (role: UserRole, houseId: String?) -> Unit,
    onVerifyBendahara: (user: String, pass: String) -> Boolean,
    onVerifyPetugas: (user: String, pass: String) -> Boolean
) {
    var selectedRole by remember { mutableStateOf(currentRole) }
    var selectedHouseId by remember { mutableStateOf(activeHouseId) }

    // Bendahara Password Prompt states
    var bendaharaUsername by remember { mutableStateOf("") }
    var bendaharaPassword by remember { mutableStateOf("") }
    var bendaharaPasswordVisible by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isVerifyingBendahara by remember { mutableStateOf(false) }

    // Petugas Ronda Password Prompt states
    var petugasUsername by remember { mutableStateOf("") }
    var petugasPassword by remember { mutableStateOf("") }
    var petugasPasswordVisible by remember { mutableStateOf(false) }
    var petugasAuthError by remember { mutableStateOf<String?>(null) }
    var isVerifyingPetugas by remember { mutableStateOf(false) }

    // Dialog Verifikasi Bendahara
    if (isVerifyingBendahara) {
        AlertDialog(
            onDismissRequest = { isVerifyingBendahara = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Autentikasi Keamanan Pengurus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Untuk mencegah sabotase dan akses tanpa izin ke pembukuan kas RT, masukkan Username & Password Bendahara:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bendaharaUsername,
                        onValueChange = {
                            bendaharaUsername = it
                            authError = null
                        },
                        label = { Text("Username Bendahara") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("treasurer_username_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = bendaharaPassword,
                        onValueChange = {
                            bendaharaPassword = it
                            authError = null
                        },
                        label = { Text("Password Bendahara") },
                        trailingIcon = {
                            IconButton(onClick = { bendaharaPasswordVisible = !bendaharaPasswordVisible }) {
                                Icon(
                                    imageVector = if (bendaharaPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (bendaharaPasswordVisible) "Sembunyikan Sandi" else "Lihat Sandi"
                                )
                            }
                        },
                        visualTransformation = if (bendaharaPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("treasurer_password_input")
                    )

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = authError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onVerifyBendahara(bendaharaUsername, bendaharaPassword)
                        if (success) {
                            onSelectRole(UserRole.BENDAHARA, null)
                            onDismiss()
                        } else {
                            authError = "Username atau Password Bendahara salah!"
                        }
                    },
                    modifier = Modifier.testTag("submit_treasurer_auth_button")
                ) {
                    Text("Masuk Sebagai Bendahara")
                }
            },
            dismissButton = {
                TextButton(onClick = { isVerifyingBendahara = false }) {
                    Text("Batal")
                }
            }
        )
        return
    }

    // Dialog Verifikasi Petugas Ronda
    if (isVerifyingPetugas) {
        AlertDialog(
            onDismissRequest = { isVerifyingPetugas = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Autentikasi Petugas Ronda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Akses penarikan jimpitan lapangan hanya untuk petugas ronda resmi. Masukkan Username & Password Petugas:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = petugasUsername,
                        onValueChange = {
                            petugasUsername = it
                            petugasAuthError = null
                        },
                        label = { Text("Username Petugas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("officer_username_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = petugasPassword,
                        onValueChange = {
                            petugasPassword = it
                            petugasAuthError = null
                        },
                        label = { Text("Password Petugas") },
                        trailingIcon = {
                            IconButton(onClick = { petugasPasswordVisible = !petugasPasswordVisible }) {
                                Icon(
                                    imageVector = if (petugasPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (petugasPasswordVisible) "Sembunyikan Sandi" else "Lihat Sandi"
                                )
                            }
                        },
                        visualTransformation = if (petugasPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("officer_password_input")
                    )

                    if (petugasAuthError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = petugasAuthError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onVerifyPetugas(petugasUsername, petugasPassword)
                        if (success) {
                            onSelectRole(UserRole.PETUGAS, null)
                            onDismiss()
                        } else {
                            petugasAuthError = "Username atau Password Petugas Ronda salah!"
                        }
                    },
                    modifier = Modifier.testTag("submit_officer_auth_button")
                ) {
                    Text("Masuk Sebagai Petugas")
                }
            },
            dismissButton = {
                TextButton(onClick = { isVerifyingPetugas = false }) {
                    Text("Batal")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pilih Peran Pengguna",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Aplikasi JimpitanKita memiliki 3 hak akses berbeda sesuai kebutuhan warga dan pengurus RT:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Role Options
                UserRole.values().forEach { role ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedRole = role }
                            .testTag("role_option_${role.name}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedRole == role)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (role) {
                                    UserRole.WARGA -> Icons.Default.Home
                                    UserRole.PETUGAS -> Icons.Default.Security
                                    UserRole.BENDAHARA -> Icons.Default.ManageAccounts
                                },
                                contentDescription = null,
                                tint = if (selectedRole == role)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = role.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedRole == role)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    if (role == UserRole.BENDAHARA || role == UserRole.PETUGAS) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Perlindungan Password",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = when (role) {
                                        UserRole.WARGA -> "Lihat QR rumah, bayar iuran, ajukan bantuan warga sakit"
                                        UserRole.PETUGAS -> "Scan QR rumah, catat jimpitan harian & bulanan (Terproteksi Sandi)"
                                        UserRole.BENDAHARA -> "Kelola kas RT, catat pemasukan & pengeluaran, unduh PDF (Terproteksi Sandi)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedRole == role)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedRole == role) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Fitur Pencarian Nama Warga
                if (selectedRole == UserRole.WARGA && houses.isNotEmpty()) {
                    var citizenSearchQuery by remember { mutableStateOf("") }
                    val matchingHouses = remember(houses, citizenSearchQuery) {
                        if (citizenSearchQuery.isBlank()) houses
                        else {
                            val q = citizenSearchQuery.trim().lowercase()
                            houses.filter {
                                it.headOfFamily.lowercase().contains(q) ||
                                it.houseNumber.lowercase().contains(q) ||
                                it.block.lowercase().contains(q) ||
                                it.id.lowercase().contains(q) ||
                                it.spouseOrSecondary.lowercase().contains(q)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pencarian Nama Warga:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${matchingHouses.size} dari ${houses.size} warga",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = citizenSearchQuery,
                        onValueChange = { citizenSearchQuery = it },
                        placeholder = { Text("Ketik nama kepala keluarga / no rumah...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari Warga",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (citizenSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { citizenSearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus Pencarian"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("citizen_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (matchingHouses.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Nama warga tidak ditemukan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                                .testTag("searched_houses_list")
                        ) {
                            items(matchingHouses.distinctBy { it.id }, key = { "role_house_${it.id}" }) { house ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedHouseId == house.id)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else
                                        Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable { selectedHouseId = house.id }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedHouseId == house.id,
                                            onClick = { selectedHouseId = house.id }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${house.block} ${house.houseNumber} - ${house.headOfFamily}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (selectedHouseId == house.id) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = "ID: ${house.id}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (selectedHouseId == house.id) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "Dipilih",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedRole == UserRole.BENDAHARA && !isBendaharaUnlocked) {
                        isVerifyingBendahara = true
                    } else if (selectedRole == UserRole.PETUGAS && !isPetugasUnlocked) {
                        isVerifyingPetugas = true
                    } else {
                        onSelectRole(selectedRole, if (selectedRole == UserRole.WARGA) selectedHouseId else null)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_role_switch_button")
            ) {
                Text(
                    if ((selectedRole == UserRole.BENDAHARA && !isBendaharaUnlocked) || 
                        (selectedRole == UserRole.PETUGAS && !isPetugasUnlocked)) 
                        "Verifikasi Sandi" 
                    else 
                        "Terapkan Peran"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
