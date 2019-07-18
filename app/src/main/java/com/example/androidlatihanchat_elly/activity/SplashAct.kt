package com.example.androidlatihanchat_elly.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.ProgressBar
import com.example.androidlatihanchat_elly.R
import com.example.androidlatihanchat_elly.data.SettingApi
import com.example.androidlatihanchat_elly.data.Tools
import com.example.androidlatihanchat_elly.utilities.Const
import com.example.androidlatihanchat_elly.utilities.Const.Companion.NODE_ID
import com.example.androidlatihanchat_elly.utilities.Const.Companion.NODE_NAME
import com.example.androidlatihanchat_elly.utilities.Const.Companion.NODE_PHOTO
import com.example.androidlatihanchat_elly.utilities.CustomToast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*

class SplashAct : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener,
View.OnClickListener {

    private var RC_SIGN_IN = 100
    private var signInButton: SignInButton? = null
    private var loginProgress: ProgressBar? = null

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mFirebaseAuth: FirebaseAuth? = null
    internal lateinit var ref: DatabaseReference
    internal lateinit var set: SettingApi

    internal lateinit var customToast: CustomToast
    val USER_CHILD = "users"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
//        bindLogo()

        customToast = CustomToast(this)

        //assign fields
        signInButton = findViewById(R.id.sign_in_button) as SignInButton
        loginProgress = findViewById(R.id.login_progress) as ProgressBar

        //set klick listen
        signInButton!!.setOnClickListener(this)

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

            //initialize FireBaseAuth
            mFirebaseAuth = FirebaseAuth.getInstance()
            set = SettingApi(this)

            if (intent.getStringExtra("mode") != null) {
                if (intent.getStringExtra("mode") == "logout") {
                    mGoogleApiClient!!.connect()
                    mGoogleApiClient!!.registerConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                        override fun onConnected(bundle: Bundle?) {
                            mFirebaseAuth!!.signOut()
                            Auth.GoogleSignInApi.signOut(mGoogleApiClient)
                            set.deleteAllSettings()
                        }

                        override fun onConnectionSuspended(i: Int) {

                        }
                    })
                }
            }
            if (!mGoogleApiClient!!.isConnecting()) {
                if (!set.readSetting(Const.PREF_MY_ID).equals("na")) {
                    signInButton!!.setVisibility(View.GONE)
                    val handler = Handler()
                    handler.postDelayed({
                        startActivity(Intent(this@SplashAct, MainActivity::class.java))
                        finish()
                    }, 3000)
                }
            }
            // for system bar in lollipop
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Tools.systemBarLolipop(this)
            }
        }
    private fun bindLogo(){
        //start animating the image
        val splash = findViewById(R.id.splash) as ImageView
        val animation1 = AlphaAnimation(0.2f, 1.0f)
        animation1.duration = 700
        val animation2 = AlphaAnimation(1.0f, 0.2f)
        animation2.duration = 700
        //animation1 animationListener
        animation1.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                //start animation2 when animation1 ends(continue)
                splash.startAnimation(animation2)

            }
            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationStart(animation: Animation?) {}
        })

        //animation2 AnimationListener
        animation2.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(arg0: Animation) {
                // start animation1 when animation2 ends (repeat)
                splash.startAnimation(animation1)
            }

            override fun onAnimationRepeat(arg0: Animation) {}

            override fun onAnimationStart(arg0: Animation) {}
        })

        splash.startAnimation(animation1)
    }

    private fun signIn(){
        val signIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signIntent, RC_SIGN_IN)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        customToast.showError("Google Play Services Error")
    }

    override fun onClick(p0: View?) {
        when (p0!!.getId()) {
            R.id.sign_in_button -> signIn()
            else -> return
        }
    }

    private fun firebaseAuthWithGoogle (acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mFirebaseAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                // If sign in fails, display a message to the user. If sign in succeeds
                // the auth state listener will be notified and logic to handle the
                // signed in user can be handled in the listener.
                if (!task.isSuccessful) {
                    customToast.showError(getString(R.string.error_authetication_failed))
                } else {
                    ref = FirebaseDatabase.getInstance().getReference(USER_CHILD)
                    ref.addListenerForSingleValueEvent(object  : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val usrNm = acct.displayName
                            val usrId = acct.id
                            val usrDp = acct.photoUrl!!.toString()

                            set.addUpdateSetting(Const.PREF_MY_ID, usrId!!)
                            set.addUpdateSetting(Const.PREF_MY_NAME, usrNm!!)
                            set.addUpdateSetting(Const.PREF_MY_DP, usrDp)

                            if (!snapshot.hasChild(usrId)) {
                                ref.child("$usrId/$NODE_NAME").setValue(usrNm)
                                ref.child("$usrId/$NODE_PHOTO").setValue(usrDp)
                                ref.child("$usrId/$NODE_ID").setValue(usrId)
                            }

                        }

                            override fun onCancelled(databaseError: DatabaseError) {}
                    })

                    startActivity(Intent(this@SplashAct, MainActivity::class.java))
                    finish()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                signInButton!!.setVisibility(View.GONE)
                loginProgress!!.setVisibility(View.VISIBLE)
                // Google Sign In was successful, authenticate with Firebase
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
                customToast.showError(getString(R.string.error_login_failed))
            }
        }
    }
}