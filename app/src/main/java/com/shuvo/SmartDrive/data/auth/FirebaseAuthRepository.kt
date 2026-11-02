package com.shuvo.SmartDrive.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.shuvo.SmartDrive.data.model.LinkedDriveAccount
import com.shuvo.SmartDrive.data.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles master application user authentication using Firebase Auth and
 * persists user profiles and linked account metadata to Cloud Firestore.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Flow tracking current Firebase Auth user state.
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Flow listening to the user's Firestore profile and linked drive accounts.
     */
    fun userProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val docRef = firestore.collection(USERS_COLLECTION).document(uid)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Authentication returned empty user")
            ensureUserProfileExists(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Failed to create user account")

            // Create new Firestore profile
            val profile = UserProfile(
                uid = user.uid,
                name = name.ifBlank { email.substringBefore('@') },
                email = email,
                createdAt = System.currentTimeMillis(),
                linkedDriveAccounts = emptyList()
            )
            firestore.collection(USERS_COLLECTION).document(user.uid).set(profile).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google authentication failed")
            ensureUserProfileExists(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun ensureUserProfileExists(user: FirebaseUser) {
        val docRef = firestore.collection(USERS_COLLECTION).document(user.uid)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val profile = UserProfile(
                uid = user.uid,
                name = user.displayName ?: user.email?.substringBefore('@') ?: "User",
                email = user.email ?: "",
                createdAt = System.currentTimeMillis(),
                linkedDriveAccounts = emptyList()
            )
            docRef.set(profile).await()
        }
    }

    suspend fun updateLinkedAccounts(uid: String, accounts: List<LinkedDriveAccount>): Result<Unit> {
        return try {
            val docRef = firestore.collection(USERS_COLLECTION).document(uid)
            val updates = mapOf(
                "linkedDriveAccounts" to accounts.map { it.toMap() }
            )
            docRef.set(updates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUserProfile(uid: String): Result<UserProfile> {
        return try {
            val docRef = firestore.collection(USERS_COLLECTION).document(uid)
            val snapshot = docRef.get().await()
            val profile = snapshot.toObject(UserProfile::class.java)
                ?: UserProfile(uid = uid)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    companion object {
        const val USERS_COLLECTION = "users"
        const val FILES_COLLECTION = "files"
    }
}

