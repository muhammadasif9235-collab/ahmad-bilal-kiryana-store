package com.example.auth

import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.UUID
import kotlinx.coroutines.tasks.await

data class UserInstallRecord(
    val id: String = "",
    val type: String = "INSTALL", // "INSTALL" یا "SIGN_UP"
    val storeName: String = "",
    val phone: String = "",
    val email: String = "",
    val uid: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val timestampMs: Long = 0L
)

data class InstallSummaryReport(
    val totalInstalls: Long = 0L,
    val totalSignUps: Long = 0L,
    val records: List<UserInstallRecord> = emptyList()
)

object UserInstallTracker {

    private const val TAG = "UserInstallTracker"
    private const val COLLECTION_USERS = "registered_users"
    private const val COLLECTION_INSTALLS = "app_installs"
    private const val COLLECTION_STATS = "app_stats"
    private const val DOC_SUMMARY = "summary"

    fun recordSignUp(storeName: String = "", phone: String = "", installId: String = "") {
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        val cleanStore = storeName.trim()
        val cleanPhone = phone.trim()
        val uniqueInstallKey = installId.trim().ifBlank { UUID.randomUUID().toString() }
        val entryType = if (user != null) "SIGN_UP" else "INSTALL"

        if (user != null) {
            val userRecord = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "storeName" to cleanStore,
                "phone" to cleanPhone,
                "installId" to uniqueInstallKey,
                "signedUpAt" to FieldValue.serverTimestamp(),
                "lastSeenAt" to FieldValue.serverTimestamp()
            )

            db.collection(COLLECTION_USERS)
                .document(user.uid)
                .set(userRecord, SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed saving registered_users", e)
                }
        }

        val installRecord = hashMapOf(
            "installId" to uniqueInstallKey,
            "type" to entryType,
            "uid" to (user?.uid ?: ""),
            "email" to (user?.email ?: ""),
            "storeName" to cleanStore,
            "phone" to cleanPhone,
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            "androidVersion" to (Build.VERSION.RELEASE ?: ""),
            "installedAt" to FieldValue.serverTimestamp(),
            "lastSeenAt" to FieldValue.serverTimestamp()
        )

        db.collection(COLLECTION_INSTALLS)
            .document(uniqueInstallKey)
            .set(installRecord)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed saving app_installs", e)
            }

        val statsUpdate = hashMapOf<String, Any>(
            "totalInstalls" to FieldValue.increment(1),
            "lastStoreName" to cleanStore,
            "lastInstallAt" to FieldValue.serverTimestamp()
        )
        if (user != null) {
            statsUpdate["totalSignUps"] = FieldValue.increment(1)
        }

        db.collection(COLLECTION_STATS)
            .document(DOC_SUMMARY)
            .set(statsUpdate, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed updating app_stats/summary", e)
            }
    }

    fun touchLastSeen(installId: String = "") {
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            db.collection(COLLECTION_USERS)
                .document(user.uid)
                .set(
                    hashMapOf("lastSeenAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                )
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed updating user lastSeenAt", e)
                }
        }

        if (installId.isNotBlank()) {
            db.collection(COLLECTION_INSTALLS)
                .document(installId.trim())
                .set(
                    hashMapOf("lastSeenAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                )
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed updating install lastSeenAt", e)
                }
        }
    }

    suspend fun fetchAllRecords(): Result<InstallSummaryReport> = runCatching {
        val db = FirebaseFirestore.getInstance()

        val summarySnap = db.collection(COLLECTION_STATS)
            .document(DOC_SUMMARY)
            .get()
            .await()

        val installsSnap = db.collection(COLLECTION_INSTALLS)
            .orderBy("installedAt", Query.Direction.DESCENDING)
            .limit(200)
            .get()
            .await()

        val usersSnap = db.collection(COLLECTION_USERS)
            .limit(200)
            .get()
            .await()

        val installList = installsSnap.documents.map { doc ->
            val ts = doc.getTimestamp("installedAt")?.toDate()?.time
                ?: doc.getTimestamp("lastSeenAt")?.toDate()?.time
                ?: 0L
            UserInstallRecord(
                id = doc.id,
                type = doc.getString("type").orEmpty().ifBlank { "INSTALL" },
                storeName = doc.getString("storeName").orEmpty(),
                phone = doc.getString("phone").orEmpty(),
                email = doc.getString("email").orEmpty(),
                uid = doc.getString("uid").orEmpty(),
                deviceModel = doc.getString("deviceModel").orEmpty(),
                androidVersion = doc.getString("androidVersion").orEmpty(),
                timestampMs = ts
            )
        }

        val knownUids = installList.mapNotNull { it.uid.takeIf { id -> id.isNotBlank() } }.toSet()
        val extraSignUpList = usersSnap.documents.mapNotNull { doc ->
            val uid = doc.getString("uid").orEmpty().ifBlank { doc.id }
            if (uid in knownUids) null
            else {
                val ts = doc.getTimestamp("signedUpAt")?.toDate()?.time
                    ?: doc.getTimestamp("lastSeenAt")?.toDate()?.time
                    ?: 0L
                UserInstallRecord(
                    id = doc.id,
                    type = "SIGN_UP",
                    storeName = doc.getString("storeName").orEmpty(),
                    phone = doc.getString("phone").orEmpty(),
                    email = doc.getString("email").orEmpty(),
                    uid = uid,
                    timestampMs = ts
                )
            }
        }

        val combined = (installList + extraSignUpList).sortedByDescending { it.timestampMs }
        val totalInstalls = summarySnap.getLong("totalInstalls") ?: combined.size.toLong()
        val totalSignUps = summarySnap.getLong("totalSignUps")
            ?: combined.count { it.type == "SIGN_UP" || it.email.isNotBlank() }.toLong()

        InstallSummaryReport(
            totalInstalls = totalInstalls,
            totalSignUps = totalSignUps,
            records = combined
        )
    }
}
