package com.example.oaplicativo.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.oaplicativo.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.call.body
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String, // Changelog
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0L
)

data class AppUpdateInfo(
    val version_name: String,
    val apk_url: String,
    val changelog: String,
    val fileSize: Long = 0L
)

class UpdateManager(private val context: Context) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    // CONFIGURADO COM SEU REPOSITÓRIO REAL
    private val GITHUB_OWNER = "italloskull"
    private val GITHUB_REPO = "AplicativoLeiturista"
    private val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    suspend fun checkForUpdates(): AppUpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("UpdateManager", "Verificando atualizações em: $GITHUB_API_URL")
                val responseString: String = client.get(GITHUB_API_URL).body()
                Log.d("UpdateManager", "Resposta do GitHub: $responseString")
                val release = json.decodeFromString<GitHubRelease>(responseString)
                
                val latestVersion = release.tagName.removePrefix("v")
                val currentVersion = BuildConfig.VERSION_NAME
                
                Log.d("UpdateManager", "Versão atual: $currentVersion | Versão no GitHub: $latestVersion")

                if (isNewerVersion(latestVersion, currentVersion)) {
                    Log.d("UpdateManager", "Nova versão detectada!")
                    val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                    if (apkAsset != null) {
                        Log.d("UpdateManager", "APK encontrado: ${apkAsset.downloadUrl} (${apkAsset.size} bytes)")
                        AppUpdateInfo(
                            version_name = latestVersion,
                            apk_url = apkAsset.downloadUrl,
                            changelog = release.body,
                            fileSize = apkAsset.size
                        )
                    } else {
                        Log.e("UpdateManager", "Nenhum arquivo .apk encontrado na release do GitHub!")
                        null
                    }
                } else {
                    Log.d("UpdateManager", "O app já está na versão mais recente.")
                    null
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Erro ao verificar atualizações", e)
                null
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.trim().toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.trim().toIntOrNull() }
        
        val maxLength = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val latestPart = latestParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }

    suspend fun downloadAndInstallApk(apkUrl: String, onProgress: (Float) -> Unit) {
        withContext(Dispatchers.IO) {
            val storageDir = context.externalCacheDir ?: context.cacheDir
            val file = File(storageDir, "update.apk")
            
            var totalBytes = -1L
            var bytesRead = 0L
            var attempt = 0
            val maxAttempts = 8 // SÊNIOR RESILIENCE: Mais tentativas para o campo

            // LOOP DE INSISTÊNCIA (Retries)
            while (attempt < maxAttempts) {
                try {
                    Log.d("debugs", "🚀 [DOWNLOAD] Tentativa ${attempt + 1} de $maxAttempts...")
                    
                    // Se o arquivo existe e já baixamos algo, tentamos o 'Resume' (se o servidor suportar)
                    val isResuming = file.exists() && bytesRead > 0
                    
                    client.prepareGet(apkUrl) {
                        if (isResuming) {
                            header("Range", "bytes=$bytesRead-")
                        }
                    }.execute { response ->
                        if (response.status.value !in 200..299 && response.status.value != 206) {
                            throw Exception("HTTP Erro: ${response.status}")
                        }

                        if (totalBytes == -1L) {
                            totalBytes = (response.contentLength() ?: 0L) + bytesRead
                        }
                        
                        val channel = response.bodyAsChannel()
                        
                        // Append mode se estivermos resumindo
                        FileOutputStream(file, isResuming).use { output ->
                            val buffer = ByteArray(8 * 1024) // Buffer menor para mais estabilidade
                            while (!channel.isClosedForRead) {
                                val read = channel.readAvailable(buffer)
                                if (read == -1) break
                                if (read > 0) {
                                    output.write(buffer, 0, read)
                                    bytesRead += read
                                    
                                    if (totalBytes > 0) {
                                        val progress = bytesRead.toFloat() / totalBytes.toFloat()
                                        // Update UI mais frequente em baixa velocidade
                                        withContext(Dispatchers.Main) {
                                            onProgress(progress.coerceIn(0f, 0.99f))
                                        }
                                    }
                                }
                            }
                            output.flush()
                        }
                    }

                    // Se chegou aqui sem exceção, o download terminou (ou o canal fechou)
                    if (bytesRead >= totalBytes && totalBytes > 0) {
                        Log.d("debugs", "✅ [DOWNLOAD] Sucesso absoluto! ${file.length()} bytes.")
                        break 
                    } else {
                        throw Exception("Download incompleto: $bytesRead de $totalBytes")
                    }

                } catch (e: Exception) {
                    attempt++
                    Log.w("debugs", "⚠️ [DOWNLOAD] Falha na tentativa $attempt: ${e.message}. Reconectando em 2s...")
                    if (attempt >= maxAttempts) {
                        Log.e("debugs", "❌ [DOWNLOAD] Limite de tentativas esgotado.")
                        return@withContext
                    }
                    kotlinx.coroutines.delay(2000) // Espera antes de insistir de novo
                }
            }

            if (file.exists() && file.length() > 1024) {
                withContext(Dispatchers.Main) {
                    onProgress(1f)
                    installApk(file)
                }
            }
        }
    }

    private fun installApk(file: File) {
        try {
            Log.d("debugs", "📦 [INSTALL] Iniciando instalador nativo.")
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("debugs", "❌ [INSTALL] Erro: ${e.message}")
        }
    }
}
