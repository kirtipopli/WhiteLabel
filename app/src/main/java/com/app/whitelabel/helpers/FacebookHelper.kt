package com.app.whitelabel.helpers

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult


class FacebookHelper {
    private val permissions: Collection<String> =
        listOf("public_profile ")
    private var callbackManager: CallbackManager? = null
    private var loginManager: LoginManager? = null
    private var activity: Activity? = null
    private var fbSignInListener: OnFbSignInListener? = null

    /**
     * Interface to listen the Facebook login
     */
    interface OnFbSignInListener {
        fun OnFbSignInComplete(
            graphResponse: GraphResponse?,
            error: String?
        )
    }

    constructor(activity: Activity?, fbSignInListener: OnFbSignInListener?) {
        this.activity = activity
        this.fbSignInListener = fbSignInListener
    }

    fun connect() {
        callbackManager = CallbackManager.Factory.create()
        loginManager = LoginManager.getInstance()
        if (activity != null) loginManager?.logInWithReadPermissions(
            activity,
            permissions
        )
        loginManager?.registerCallback(callbackManager,
            object : FacebookCallback<LoginResult?> {
                override fun onSuccess(loginResult: LoginResult?) {
                    if (loginResult != null) {
                        callGraphAPI(loginResult.accessToken)
                    }
                }

                override fun onCancel() {
                    fbSignInListener!!.OnFbSignInComplete(null, "User cancelled.")
                }

                override fun onError(exception: FacebookException) {
                    if (exception is FacebookAuthorizationException) {
                        if (AccessToken.getCurrentAccessToken() != null) {
                            LoginManager.getInstance().logOut()
                        }
                    }
                    fbSignInListener!!.OnFbSignInComplete(null, exception.message)
                }
            })
    }

    private fun callGraphAPI(accessToken: AccessToken) {
        val request = GraphRequest.newMeRequest(
            accessToken
        ) { `object`, response -> fbSignInListener!!.OnFbSignInComplete(response, null) }
        val parameters = Bundle()
        //Explicitly we need to specify the fields to get values else some values will be null.
        parameters.putString("fields", "name")
        request.parameters = parameters
        request.executeAsync()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (callbackManager != null) callbackManager!!.onActivityResult(
            requestCode,
            resultCode,
            data
        )
    }

}