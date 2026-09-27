package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(
        val user: FirebaseUser,
        val isAnonymous: Boolean,
        val displayName: String,
        val email: String,
        val photoUrl: String?
    ) : AuthState()
    data class Unauthenticated(val message: String? = null) : AuthState()
}

class FirebaseAuthManager(private val context: Context) {
    private val tag = "FirebaseAuthManager"
    private var auth: FirebaseAuth? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        try {
            auth = FirebaseAuth.getInstance()
            updateCurrentUser(auth?.currentUser)
            auth?.addAuthStateListener { firebaseAuth ->
                updateCurrentUser(firebaseAuth.currentUser)
            }
        } catch (e: Exception) {
            Log.w(tag, "Firebase Auth initialization notice: ${e.message}")
            _authState.value = AuthState.Unauthenticated("Firebase not initialized yet")
        }
    }

    private fun updateCurrentUser(user: FirebaseUser?) {
        if (user != null) {
            _authState.value = AuthState.Authenticated(
                user = user,
                isAnonymous = user.isAnonymous,
                displayName = user.displayName ?: if (user.isAnonymous) "Guest User" else (user.email?.substringBefore("@") ?: "User"),
                email = user.email ?: if (user.isAnonymous) "guest@offline.local" else "",
                photoUrl = user.photoUrl?.toString()
            )
        } else {
            _authState.value = AuthState.Unauthenticated()
        }
    }

    fun getCurrentUserId(): String? {
        return auth?.currentUser?.uid
    }

    fun isUserSignedIn(): Boolean {
        return auth?.currentUser != null
    }

    suspend fun signInWithGoogle(webClientId: String? = null): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized"))
        return try {
            val credentialManager = CredentialManager.create(context)
            
            // Generate a random nonce for security
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            // Use provided web client ID, default_web_client_id from google-services.json, or fallback
            val defaultWebClientIdRes = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val resolvedDefaultClientId = if (defaultWebClientIdRes != 0) {
                try { context.getString(defaultWebClientIdRes) } catch (e: Exception) { null }
            } else null

            val clientId = when {
                !webClientId.isNullOrBlank() -> webClientId
                !resolvedDefaultClientId.isNullOrBlank() -> resolvedDefaultClientId
                else -> "658507097712.apps.googleusercontent.com"
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw Exception("Google sign-in user is null")
                updateCurrentUser(user)
                Result.success(user)
            } else {
                throw Exception("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Sign-in cancelled"))
        } catch (e: Exception) {
            Log.w(tag, "Google Sign-in exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized"))
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user ?: throw Exception("User is null after anonymous sign-in")
            updateCurrentUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.w(tag, "Anonymous sign-in error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized"))
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: throw Exception("User is null")
            updateCurrentUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Firebase Auth not initialized"))
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: throw Exception("User is null")
            updateCurrentUser(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
            updateCurrentUser(null)
        } catch (e: Exception) {
            Log.e(tag, "Error signing out", e)
        }
    }
}
