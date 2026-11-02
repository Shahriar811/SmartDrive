package com.shuvo.SmartDrive.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Master User Profile stored in Cloud Firestore under `users/{userId}`.
 */
data class UserProfile(
    @get:PropertyName("uid")
    @set:PropertyName("uid")
    var uid: String = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("linkedDriveAccounts")
    @set:PropertyName("linkedDriveAccounts")
    var linkedDriveAccounts: List<LinkedDriveAccount> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "createdAt" to createdAt,
            "linkedDriveAccounts" to linkedDriveAccounts.map { it.toMap() }
        )
    }
}

