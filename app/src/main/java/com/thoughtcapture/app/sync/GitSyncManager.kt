package com.thoughtcapture.app.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.thoughtcapture.app.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.transport.RemoteRefUpdate
import java.io.File

class GitSyncManager(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    private val repoDirInternal: File
        get() = File(context.filesDir, "thought_repo")

    suspend fun cloneIfNeeded(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!prefs.isConfigured) {
                return@withContext Result.failure(IllegalStateException("未配置仓库信息"))
            }
            if (File(repoDirInternal, ".git").exists()) {
                return@withContext Result.success(Unit)
            }
            Git.cloneRepository()
                .setURI(prefs.repoUrl)
                .setDirectory(repoDirInternal)
                .setCredentialsProvider(createCredential())
                .setBranch(prefs.repoBranch)
                .call()
                .close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pull(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val git = Git.open(repoDirInternal)
            val stashRef = git.stashCreate().call()
            git.pull()
                .setCredentialsProvider(createCredential())
                .setRebase(true)
                .call()
            if (stashRef != null) {
                git.stashDrop().setStashRef(0).call()
            }
            git.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun push(commitMessage: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val git = Git.open(repoDirInternal)
            git.add().addFilepattern(".").call()
            git.commit()
                .setMessage(commitMessage)
                .setAuthor("ThoughtCapture", "app@thoughtcapture.local")
                .call()
            val result = git.push()
                .setCredentialsProvider(createCredential())
                .call()
            git.close()

            val update = result.firstOrNull()?.remoteUpdates?.firstOrNull()
            if (update != null && update.status == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                return@withContext Result.failure(Exception("Push 被拒绝，需先 pull"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushWithRetry(commitMessage: String, maxRetries: Int = 3): Result<Unit> {
        var lastError: Throwable? = null
        for (i in 0..<maxRetries) {
            pull()
            val result = push(commitMessage)
            if (result.isSuccess) return result
            lastError = result.exceptionOrNull()
            delay(1000L * (1 shl i))
        }
        return Result.failure(lastError ?: Exception("未知错误"))
    }

    fun getRepoDir(): File = repoDirInternal
    fun getMediaDir(): File = File(repoDirInternal, "media").also { if (!it.exists()) it.mkdirs() }

    private fun createCredential(): UsernamePasswordCredentialsProvider {
        return UsernamePasswordCredentialsProvider(prefs.githubPat ?: "", "")
    }

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun canSync(): Boolean {
        if (!isNetworkAvailable()) return false
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true
        return prefs.syncOnMobileData
    }
}
