package io.fallcare.data


import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.fallcare.util.appTimeStamp
import io.fallcare.util.logger


object FireStoreRepository {

    private const val devicesCollection = "devices"
    private const val tag = "FireStoreRepository"

    fun saveFallData(
        androidID: String,
        entity: FallEntity,
    ) {

        FirebaseFirestore.getInstance()
            .collection(devicesCollection)
            .document(androidID)
            .collection(appTimeStamp.toString())
            .add( entity)
            .addOnFailureListener { e ->
                logger(tag, "Error guardando el token: ${e.message}")
            }
    }
}
