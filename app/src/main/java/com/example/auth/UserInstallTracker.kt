package com.example.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object UserInstallTracker {

    fun recordSignUp(storeName: String = "", phone: String = "") {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val record = hashMapOf(
            "uid" to user.uid,
            "email" to (user.email ?: ""),
            "storeName" to storeName.trim(),
            "phone" to phone.trim(),
            "signedUpAt" to FieldValue.serverTimestamp(),
            "lastSeenAt" to FieldValue.serverTimestamp()
        )

        db.collection("registered_users")
            .document(user.uid)
            .set(record, SetOptions.merge())
    }

    fun touchLastSeen() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("registered_users")
            .document(user.uid)
            .set(
                hashMapOf("lastSeenAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            )
    }
}
