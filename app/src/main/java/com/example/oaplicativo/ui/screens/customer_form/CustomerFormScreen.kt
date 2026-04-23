package com.example.oaplicativo.ui.screens.customer_form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.ui.components.BooleanOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    customerId: String? = null,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CustomerFormViewModel = viewModel()
) {
    val customer = remember(customerId) { viewModel.getCustomer(customerId) }

    var nome by remember(customer) { mutableStateOf(customer?.name ?: "") }
    var matricula by remember(customer) { mutableStateOf(customer?.registrationNumber ?: "") }
    var email by remember(customer) { mutableStateOf(customer?.email ?: "") }
    var telefoneFixo by remember(customer) { mutableStateOf(customer?.landline ?: "") }
    var celular by remember(customer) { mutableStateOf(customer?.cellPhone ?: "") }
    var isCaixaPadrao by remember(customer) { mutableStateOf(customer?.isStandardMeasurementBox ?: false) }
    var isLacresPadronizados by remember(customer) { mutableStateOf(customer?.isStandardizedSeals ?: false) }
    var isHdAcessivel by remember(customer) { mutableStateOf(customer?.isHdAccessible ?: false) }
    var isVeranista by remember(customer) { mutableStateOf(customer?.isVacationer ?: false) }

    val emailRegex = """^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[a-z]{2,}$""".toRegex()

    var showErrors by remember { mutableStateOf(value = false) }

    val isNomeValid = nome.length in 3..100
    val isEmailValid = (email.length in 5..254) && emailRegex.matches(email.trim())
    val isMatriculaValid = matricula.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (customer == null) "Cadastrar Cliente" else "Atualizar Cliente") },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = nome,
                onValueChange = { if (it.length <= 100) nome = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth(),
                isError = showErrors && !isNomeValid,
                supportingText = {
                    if (showErrors && !isNomeValid) {
                        Text("O nome deve ter entre 3 e 100 caracteres")
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = matricula,
                onValueChange = { if (it.all { char -> char.isDigit() }) matricula = it },
                label = { Text("Matrícula") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = showErrors && !isMatriculaValid
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { if (it.length <= 254) email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = showErrors && !isEmailValid,
                supportingText = {
                    if (showErrors && !isEmailValid) {
                        Text("E-mail inválido (mínimo 5 caracteres)")
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = telefoneFixo,
                onValueChange = { if (it.all { char -> char.isDigit() }) telefoneFixo = it },
                label = { Text("Telefone Fixo") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = celular,
                onValueChange = { if (it.all { char -> char.isDigit() }) celular = it },
                label = { Text("Celular") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))

            BooleanOption(label = "Caixa de medição é padrão?", checked = isCaixaPadrao) { isCaixaPadrao = it }
            BooleanOption(label = "Lacres padronizados?", checked = isLacresPadronizados) { isLacresPadronizados = it }
            BooleanOption(label = "HD acessível?", checked = isHdAcessivel) { isHdAcessivel = it }
            BooleanOption(label = "Veranista?", checked = isVeranista) { isVeranista = it }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (isNomeValid && isEmailValid && isMatriculaValid) {
                        viewModel.saveCustomer(
                            Customer(
                                id = customer?.id ?: "",
                                name = nome,
                                registrationNumber = matricula,
                                email = email.trim().lowercase(),
                                landline = telefoneFixo,
                                cellPhone = celular,
                                isStandardMeasurementBox = isCaixaPadrao,
                                isStandardizedSeals = isLacresPadronizados,
                                isHdAccessible = isHdAcessivel,
                                isVacationer = isVeranista
                            )
                        )
                        onSaveSuccess()
                    } else {
                        showErrors = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar")
            }
            TextButton(
                onClick = { onBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Voltar")
            }
        }
    }
}