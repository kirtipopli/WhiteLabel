package com.app.whitelabel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.app.whitelabel.helpers.FacebookHelper
import com.app.whitelabel.helpers.GoogleSignInHelper
import com.app.whitelabel.model.ProfileDetails
import com.facebook.CallbackManager
import com.facebook.FacebookSdk
import com.facebook.GraphResponse
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONException


class LoginScreenActivity : AppCompatActivity(), GoogleSignInHelper.OnGoogleSignInListener,
    FacebookHelper.OnFbSignInListener {

    private var userName: String = ""
    private var isLoggedIn: Boolean = false
    private var mFacebookCallbackManager: CallbackManager? = null
    private val TAG: String = "LoginScreen"
    private var googleSignInHelper: GoogleSignInHelper? = null
    private var fbConnectHelper: FacebookHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FacebookSdk.sdkInitialize(applicationContext);
        mFacebookCallbackManager = CallbackManager.Factory.create()

        setContentView(R.layout.activity_login)

        googleSignInHelper = GoogleSignInHelper(this, this)
        googleSignInHelper?.connect()
        btn_googleLogin.setOnClickListener {
            isLoggedIn = false
            googleSignInHelper?.signIn()
        }

        fbConnectHelper = FacebookHelper(this, this)
        btn_facebookLogin.setOnClickListener {
            isLoggedIn = false
            fbConnectHelper?.connect()
        }
    }


    private fun navigateToMainActivity(profileDetails: ProfileDetails) {
        if (isLoggedIn) {
            startActivity(Intent(this, MapsActivity::class.java).putExtra("data", profileDetails))
            finish()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        fbConnectHelper?.onActivityResult(requestCode, resultCode, data)
        googleSignInHelper?.onActivityResult(requestCode, resultCode, data)

    }

    override fun onStart() {
        super.onStart()
        googleSignInHelper?.onStart()
    }

    override fun OnGSignInSuccess(googleSignInAccount: GoogleSignInAccount?) {
        if (googleSignInAccount != null) {
            userName = googleSignInAccount.givenName + googleSignInAccount.familyName
            val profileDetails = ProfileDetails()
            profileDetails.name = userName
            isLoggedIn = true
            navigateToMainActivity(profileDetails)
        }
    }

    override fun OnGSignInError(error: String?) {
        Log.e(TAG, "Google :$error");
    }

    override fun OnFbSignInComplete(graphResponse: GraphResponse?, error: String?) {
        if (error == null) {
            try {
                val jsonObject = graphResponse!!.jsonObject
                userName = jsonObject.getString("name")
                val profileDetails = ProfileDetails()
                profileDetails.name = userName
                isLoggedIn = true
                navigateToMainActivity(profileDetails)
            } catch (e: JSONException) {
                Log.i(TAG, e.message)
            }
        }
    }
}