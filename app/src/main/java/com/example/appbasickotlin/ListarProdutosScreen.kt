@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.appbasickotlin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListarProdutosScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val produtos = remember { mutableStateListOf<Produto>() }
    var produtoEmEdicao by remember { mutableStateOf<Produto?>(null) }
    var nome by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var docIdEmEdicao by remember { mutableStateOf<String?>(null) }

    // Listener para Firestore
    DisposableEffect(userId) {
        var registration: ListenerRegistration? = null
        if (userId != null) {
            registration = FirebaseFirestore.getInstance()
                .collection("produtos")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, _ ->
                    produtos.clear()
                    snapshot?.documents?.forEach { doc ->
                        val nome = doc.getString("nome") ?: ""
                        val quantidade = doc.getLong("quantidade")?.toInt() ?: 0
                        val descricao = doc.getString("descricao") ?: ""
                        produtos.add(Produto(doc.id, nome, quantidade, descricao))
                    }
                }
        }
        onDispose { registration?.remove() }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produtos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        modifier = Modifier.background(brush = gradient)
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .background(brush = gradient)
        ) {
            items(produtos, key = { it.id }) { produto ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = produto.nome,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Quantidade: ${produto.quantidade}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (produto.descricao.isNotBlank()) {
                                Text(
                                    text = produto.descricao,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        IconButton(onClick = {
                            produtoEmEdicao = produto
                            nome = produto.nome
                            quantidade = produto.quantidade.toString()
                            descricao = produto.descricao
                            docIdEmEdicao = produto.id
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = {
                            FirebaseFirestore.getInstance().collection("produtos")
                                .document(produto.id)
                                .delete()
                                .addOnFailureListener {
                                    Toast.makeText(context, "Erro ao excluir", Toast.LENGTH_SHORT).show()
                                }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                        }
                    }
                }
            }
        }
    }

    if (produtoEmEdicao != null) {
        AlertDialog(
            onDismissRequest = { produtoEmEdicao = null },
            title = { Text("Editar Produto") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Produto") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantidade,
                        onValueChange = { quantidade = it },
                        label = { Text("Quantidade") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = descricao,
                        onValueChange = { descricao = it },
                        label = { Text("Descrição") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val docId = docIdEmEdicao
                    if (docId != null) {
                        val produtoAtualizado = hashMapOf(
                            "nome" to nome,
                            "quantidade" to quantidade.toIntOrNull(),
                            "descricao" to descricao,
                            "userId" to userId
                        )
                        FirebaseFirestore.getInstance().collection("produtos")
                            .document(docId)
                            .set(produtoAtualizado)
                            .addOnSuccessListener {
                                produtoEmEdicao = null
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Erro ao atualizar", Toast.LENGTH_SHORT).show()
                            }
                    }
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { produtoEmEdicao = null }) { Text("Cancelar") }
            }
        )
    }
}

data class Produto(
    val id: String = "",
    val nome: String,
    val quantidade: Int,
    val descricao: String
)
