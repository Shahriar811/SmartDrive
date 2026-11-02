package com.shuvo.SmartDrive.data.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.shuvo.SmartDrive.data.model.LinkedDriveAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages OAuth 2.0 authentication and fresh Bearer token retrieval for up to 5
 * distinct Google Drive accounts.
 *
 * Utilizes native Google Play Services Auth with zero user-side API keys or client secrets.
 */
class MultiDriveOAuthManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // In-memory cache of valid OAuth Bearer tokens keyed by account email
    private val tokenCache = ConcurrentHashMap<String, String>()

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                Scope(DRIVE_SCOPE),
                Scope(DRIVE_FILE_SCOPE)
            )
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    /**
     * Intent to launch the native Android Google Account Picker bottom sheet.
     * Uses Android's native AccountManager to display all Google accounts on device
     * without triggering Developer Error 10 from GoogleSignInClient scope mismatches.
     */
    fun getAccountPickerIntent(): Intent {
        return try {
            AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
        } catch (e: Exception) {
            googleSignInClient.signOut()
            googleSignInClient.signInIntent
        }
    }

    /**
     * Extracts account info from the Account Picker result intent.
     * Checks native AccountManager result first, then falls back to GoogleSignIn if applicable.
     */
    fun parseAccountFromIntent(intent: Intent?): Result<LinkedDriveAccount> {
        if (intent == null) {
            return Result.failure(Exception("No account selected"))
        }

        // 1. First check if returned by native AccountManager
        val accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!accountName.isNullOrBlank()) {
            val rawName = accountName.substringBefore('@').replace('.', ' ')
            val formattedName = rawName.split(' ')
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }.ifBlank { accountName.substringBefore('@') }

            val linked = LinkedDriveAccount(
                accountEmail = accountName,
                displayName = formattedName,
                avatarUrl = "",
                totalQuotaBytes = 15L * 1024L * 1024L * 1024L,
                usedQuotaBytes = 0L,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            return Result.success(linked)
        }

        // 2. Fallback check for GoogleSignIn result
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = account.email ?: return Result.failure(Exception("Account has no email"))
            val displayName = account.displayName ?: email.substringBefore('@')
            val avatarUrl = account.photoUrl?.toString() ?: ""

            val linked = LinkedDriveAccount(
                accountEmail = email,
                displayName = displayName,
                avatarUrl = avatarUrl,
                totalQuotaBytes = 15L * 1024L * 1024L * 1024L,
                usedQuotaBytes = 0L,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            Result.success(linked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtains a fresh OAuth 2.0 Bearer access token for the given account email
     * without opening any browser or asking user for keys.
     */
    suspend fun getAccessToken(accountEmail: String): Result<String> = withContext(Dispatchers.IO) {
        val cached = tokenCache[accountEmail]
        if (!cached.isNullOrBlank()) {
            return@withContext Result.success(cached)
        }

        try {
            val googleAccount = Account(accountEmail, "com.google")
            val scopeString = "oauth2:$DRIVE_SCOPE"
            val token = GoogleAuthUtil.getToken(context, googleAccount, scopeString)
            if (!token.isNullOrBlank()) {
                tokenCache[accountEmail] = token
                Result.success(token)
            } else {
                Result.failure(Exception("Failed to acquire access token for $accountEmail"))
            }
        } catch (e: UserRecoverableAuthException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Invalidates a cached token (e.g. on 401 Unauthorized or expiry)
     * so that the next request fetches a fresh token.
     */
    suspend fun invalidateToken(accountEmail: String) = withContext(Dispatchers.IO) {
        val token = tokenCache.remove(accountEmail)
        if (token != null) {
            try {
                GoogleAuthUtil.clearToken(context, token)
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Persists the list of linked drive accounts locally.
     */
    fun saveLinkedAccounts(accounts: List<LinkedDriveAccount>) {
        val jsonArray = JSONArray()
        accounts.take(MAX_SLOTS).forEach { account ->
            val obj = JSONObject().apply {
                put("accountEmail", account.accountEmail)
                put("displayName", account.displayName)
                put("avatarUrl", account.avatarUrl)
                put("totalQuotaBytes", account.totalQuotaBytes)
                put("usedQuotaBytes", account.usedQuotaBytes)
                put("lastSyncTimestamp", account.lastSyncTimestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_LINKED_ACCOUNTS, jsonArray.toString()).apply()
    }

    /**
     * Retrieves stored linked accounts from local persistent storage.
     */
    fun getStoredLinkedAccounts(): List<LinkedDriveAccount> {
        val jsonString = prefs.getString(KEY_LINKED_ACCOUNTS, null) ?: return emptyList()
        val list = mutableListOf<LinkedDriveAccount>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    LinkedDriveAccount(
                        accountEmail = obj.optString("accountEmail"),
                        displayName = obj.optString("displayName"),
                        avatarUrl = obj.optString("avatarUrl"),
                        totalQuotaBytes = obj.optLong("totalQuotaBytes", 15L * 1024L * 1024L * 1024L),
                        usedQuotaBytes = obj.optLong("usedQuotaBytes", 0L),
                        lastSyncTimestamp = obj.optLong("lastSyncTimestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (ignored: Exception) {
        }
        return list
    }

    /**
     * Removes an account from linked slots.
     */
    fun removeLinkedAccount(accountEmail: String) {
        val current = getStoredLinkedAccounts().toMutableList()
        current.removeAll { it.accountEmail.equals(accountEmail, ignoreCase = true) }
        saveLinkedAccounts(current)
        tokenCache.remove(accountEmail)
    }

    companion object {
        const val MAX_SLOTS = 5
        const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive"
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val PREFS_NAME = "smartdrive_multi_account_prefs"
        private const val KEY_LINKED_ACCOUNTS = "linked_drive_accounts"
    }
}

