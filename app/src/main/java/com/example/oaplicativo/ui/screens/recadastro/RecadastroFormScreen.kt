package com.example.oaplicativo.ui.screens.recadastro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.ui.components.AppFormTextField
import com.example.oaplicativo.ui.components.LoadingActionButton
import com.example.oaplicativo.ui.components.SpinnerOption
import com.example.oaplicativo.ui.screens.recadastro.viewmodel.RecadastroViewModel
import com.example.oaplicativo.util.CepVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecadastroFormScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    viewModel: RecadastroViewModel = viewModel()
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Novo Recadastro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp, 
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding() 
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    LoadingActionButton(
                        text = "Salvar Recadastro",
                        onClick = {
                            // QUALIDADE AUTOMÁTICA: O valor é calculado aqui antes de salvar
                            val quality = viewModel.calculateDataQuality()
                            println("Qualidade do Cadastro: $quality")
                            onSave()
                        },
                        isLoading = false 
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // GRUPO: IDENTIFICAÇÃO E LOCALIZAÇÃO
            item {
                FormCard("Identificação e Localização") {
                    AppFormTextField(value = viewModel.matricula, onValueChange = { viewModel.matricula = it }, label = "Matrícula", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    AppFormTextField(value = viewModel.lote, onValueChange = { viewModel.lote = it }, label = "Lote")
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Coordenadas GPS", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = if (viewModel.latitude != null) "Lat: ${viewModel.latitude}, Long: ${viewModel.longitude}" else "Localização não capturada",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { /* Lógica GPS */ },
                                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Capturar Localização")
                            }
                        }
                    }
                }
            }

            // GRUPO: DADOS PESSOAIS
            item {
                FormCard("Dados Pessoais") {
                    AppFormTextField(value = viewModel.nomeCompleto, onValueChange = { viewModel.nomeCompleto = it }, label = "Nome Completo")
                    AppFormTextField(value = viewModel.cpfCnpj, onValueChange = { viewModel.cpfCnpj = it }, label = "CPF/CNPJ", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    AppFormTextField(value = viewModel.nomeMae, onValueChange = { viewModel.nomeMae = it }, label = "Nome da Mãe")
                    
                    AppFormTextField(
                        value = viewModel.dataNascimento, 
                        onValueChange = { viewModel.dataNascimento = it }, 
                        label = "Data de Nascimento (DD/MM/AAAA)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    SpinnerOption(
                        label = "Sexo",
                        options = listOf("Masculino", "Feminino", "Outro"),
                        selectedOption = viewModel.sexo,
                        onOptionSelected = { viewModel.sexo = it }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = viewModel.apresentouDoc, onCheckedChange = { viewModel.apresentouDoc = it })
                        Spacer(Modifier.width(12.dp))
                        Text("Apresentou documento?")
                    }
                    
                    if (viewModel.apresentouDoc) {
                        AppFormTextField(value = viewModel.qualDoc, onValueChange = { viewModel.qualDoc = it }, label = "Qual documento apresentado?")
                    }
                }
            }

            // GRUPO: VÍNCULO E CONTATO
            item {
                FormCard("Vínculo e Contato") {
                    Row(Modifier.fillMaxWidth()) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = viewModel.isProprietario, onCheckedChange = { viewModel.isProprietario = it })
                            Text("Proprietário")
                        }
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = viewModel.isMorador, onCheckedChange = { viewModel.isMorador = it })
                            Text("Morador")
                        }
                    }
                    
                    AppFormTextField(value = viewModel.email, onValueChange = { viewModel.email = it }, label = "E-mail", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    AppFormTextField(value = viewModel.telefone, onValueChange = { viewModel.telefone = it }, label = "Telefone", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { AppFormTextField(value = viewModel.celular1, onValueChange = { viewModel.celular1 = it }, label = "Celular 1", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) }
                        Box(Modifier.weight(1f)) { AppFormTextField(value = viewModel.celular2, onValueChange = { viewModel.celular2 = it }, label = "Celular 2", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { AppFormTextField(value = viewModel.celular3, onValueChange = { viewModel.celular3 = it }, label = "Celular 3", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) }
                        Box(Modifier.weight(1f)) { AppFormTextField(value = viewModel.celular4, onValueChange = { viewModel.celular4 = it }, label = "Celular 4", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) }
                    }
                }
            }

            // GRUPO: ENDEREÇO
            item {
                FormCard("Endereço") {
                    AppFormTextField(
                        value = viewModel.cep, 
                        onValueChange = { viewModel.onCepChange(it) }, 
                        label = "CEP", 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = CepVisualTransformation(),
                        error = if (viewModel.cepError) "CEP não encontrado" else null,
                        trailingIcon = if (viewModel.isCepLoading) {
                            { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp) }
                        } else null
                    )
                    
                    AppFormTextField(value = viewModel.logradouro, onValueChange = { viewModel.logradouro = it }, label = "Logradouro")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { AppFormTextField(value = viewModel.numero, onValueChange = { viewModel.numero = it }, label = "Número") }
                        Box(Modifier.weight(2f)) { AppFormTextField(value = viewModel.complemento, onValueChange = { viewModel.complemento = it }, label = "Complemento") }
                    }
                    AppFormTextField(value = viewModel.bairro, onValueChange = { viewModel.bairro = it }, label = "Bairro")
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(2f)) { AppFormTextField(value = viewModel.cidade, onValueChange = { viewModel.cidade = it }, label = "Cidade") }
                        Box(Modifier.weight(1f)) { AppFormTextField(value = viewModel.uf, onValueChange = { viewModel.uf = it }, label = "UF") }
                    }
                }
            }

            // GRUPO: IMÓVEL E CONSUMO
            item {
                FormCard("Características do Imóvel") {
                    AppFormTextField(value = viewModel.numeroMoradores, onValueChange = { viewModel.numeroMoradores = it }, label = "Nº de Moradores", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    
                    SpinnerOption(label = "Pavimento Rua", options = listOf("Asfalto", "Paralelepípedo", "Terra", "Outro"), selectedOption = viewModel.pavimentoRua, onOptionSelected = { viewModel.pavimentoRua = it })
                    SpinnerOption(label = "Pavimento Calçada", options = listOf("Concreto", "Piso", "Terra", "Outro"), selectedOption = viewModel.pavimentoCalcada, onOptionSelected = { viewModel.pavimentoCalcada = it })
                    
                    SpinnerOption(label = "Fonte Abastecimento", options = listOf("Rede Pública", "Poço", "Caminhão Pipa"), selectedOption = viewModel.fonteAbastecimento, onOptionSelected = { viewModel.fonteAbastecimento = it })
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { SpinnerOption(label = "CATEGORIA 1", options = listOf("Residencial", "Comercial"), selectedOption = viewModel.categoria1, onOptionSelected = { viewModel.categoria1 = it }) }
                        Box(Modifier.weight(1f)) { SpinnerOption(label = "CATEGORIA 2", options = listOf("Industrial", "Pública"), selectedOption = viewModel.categoria2, onOptionSelected = { viewModel.categoria2 = it }) }
                    }
                }
            }

            // GRUPO: HIDROMETRIA
            item {
                FormCard("Hidrometria") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = viewModel.possuiHidrometro, onCheckedChange = { viewModel.possuiHidrometro = it })
                        Spacer(Modifier.width(12.dp))
                        Text("Possui Hidrômetro?")
                    }
                    
                    if (viewModel.possuiHidrometro) {
                        AppFormTextField(value = viewModel.numeroHidrometro, onValueChange = { viewModel.numeroHidrometro = it }, label = "Nº Hidrômetro")
                    }
                    
                    SpinnerOption(
                        label = "Local Instalação", 
                        options = listOf("CAIXA PADRÃO", "INTERNO", "CAVALETE EXTERNO"), 
                        selectedOption = viewModel.localInstalacao, 
                        onOptionSelected = { viewModel.localInstalacao = it }
                    )
                    
                    SpinnerOption(
                        label = "Acessibilidade/Não Padronizada", 
                        options = listOf("Facil Acesso", "Dificil Acesso"), 
                        selectedOption = viewModel.acessibilidade, 
                        onOptionSelected = { viewModel.acessibilidade = it }
                    )
                    
                    // CAMPO QUALIDADE REMOVIDO DA INTERFACE
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { AppFormTextField(value = viewModel.economias, onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.economias = it }, label = "ECONOMIAS", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }
                    }
                }
            }

            // GRUPO: FINALIZAÇÃO
            item {
                FormCard("Finalização") {
                    AppFormTextField(value = viewModel.observacao, onValueChange = { viewModel.observacao = it }, label = "Observação", maxLength = 500)
                    
                    Button(onClick = { /* Abrir Câmera */ }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Anexar Fotos")
                    }
                    
                    AppFormTextField(value = viewModel.nomePrestadorInformacoes, onValueChange = { viewModel.nomePrestadorInformacoes = it }, label = "Nome Prestador de Informações")
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun FormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}
