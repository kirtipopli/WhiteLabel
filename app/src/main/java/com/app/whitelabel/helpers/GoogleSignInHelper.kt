package com.app.whitelabel.helpers

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class GoogleSignInHelper(
    // Activity instance
    private val activity: Activity,
    /**
     * Google sign in Listener
     */
    private val onGoogleSignInListener: OnGoogleSignInListener?
) {
    // GoogleSignInClient
    private var googleSignInClient: GoogleSignInClient? = null

    // [START declare_auth]
    private var mAuth: FirebaseAuth? = null

    /**
     * Connect to google
     */
    fun connect() {
        //Mention the GoogleSignInOptions to get the user profile and email.
        // Instantiate Google SignIn Client.
        googleSignInClient = GoogleSignIn.getClient(
            activity,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        )

        mAuth = FirebaseAuth.getInstance()
    }

    /**
     * Call this method in your onStart().If user have already signed in it will provide result directly.
     */
    fun onStart() {
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account != null && onGoogleSignInListener != null) {
            onGoogleSignInListener.OnGSignInSuccess(account)
        }

        val currentUser = mAuth!!.currentUser
    }

    /**
     * To Init the sign in process.
     */
    fun signIn() {
        val signInIntent = googleSignInClient!!.signInIntent
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    /**
     * To signOut from the application.
     */
    fun signOut() {
        if (googleSignInClient != null) {
            googleSignInClient!!.signOut()
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = completedTask.getResult(ApiException::class.java)
            // Signed in successfully
            onGoogleSignInListener?.OnGSignInSuccess(account)
        } catch (e: ApiException) {
            onGoogleSignInListener?.OnGSignInError(
                GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)
            )
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN && resultCode == Activity.RESULT_OK) {
            // The Task returned from this call is always completed, no need to attach a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
//            val account = task.getResult(ApiException::class.java)!!
//            Log.d("Google", "firebaseAuthWithGoogle:" + account.id)
//            firebaseAuthWithGoogle(account.idToken!!)

            handleSignInResult(task)
        } else {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth?.signInWithCredential(credential)?.addOnCompleteListener(
            activity
        ) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d("GoogleSign", "signInWithCredential:success")
                val user = mAuth!!.currentUser

            } else {
                // If sign in fails, display a message to the user.
                Log.w("GoogleSign", "signInWithCredential:failure", task.exception)

            }
        }
    }

    /**
     * Interface to listen the Google sign in
     */
    interface OnGoogleSignInListener {
        fun OnGSignInSuccess(googleSignInAccount: GoogleSignInAccount?)
        fun OnGSignInError(error: String?)
    }

    companion object {
        const val RC_SIGN_IN = 1008
    }

}