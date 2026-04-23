package com.example.oaplicativo.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.oaplicativo.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
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
    @SerialName("browser_download_url") val downloadUrl: String
)

data class AppUpdateInfo(
    val version_name: String,
    val apk_url: String,
    val changelog: String
)

class UpdateManager(private val context: Context) {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    // COLOQUE SEU USUÁRIO E NOME DO REPOSITÓRIO AQUI
    private val GITHUB_OWNER = "seu-usuario"
    private val GITHUB_REPO = "seu-repositorio"
    private val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    suspend fun checkForUpdates(): AppUpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val responseString: String = client.get(GITHUB_API_URL).body()
                val release = json.decodeFromString<GitHubRelease>(responseString)
                
                // GitHub costuma usar "v1.0.0", removemos o 'v' para comparar
                val latestVersion = release.tagName.removePrefix("v")
                val currentVersion = BuildConfig.VERSION_NAME

                if (isNewerVersion(latestVersion, currentVersion)) {
                    val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                    if (apkAsset != null) {
                        AppUpdateInfo(
                            version_name = latestVersion,
                            apk_url = apkAsset.downloadUrl,
                            changelog = release.body
                        )
                    } else null
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        // Comparação simples de string, ou você pode quebrar em números
        return latest != current
    }

    suspend fun downloadAndInstallApk(apkUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                val response = client.get(apkUrl)
                val bytes = response.bodyAsBytes()
                
                val file = File(context.cacheDir, "update.apk")
                FileOutputStream(file).use { it.write(bytes) }
                
                installApk(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun installApk(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context, 
            "${context.packageName}.fileprovider", 
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
