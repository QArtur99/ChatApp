package com.artf.chatapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.artf.chatapp.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val RC_SIGN_IN = 1
    val ANONYMOUS = "anonymous"
    val DEFAULT_MSG_LENGTH_LIMIT = 1000
    val FRIENDLY_MSG_LENGTH_KEY = "friendly_msg_length"


    private val RC_PHOTO_PICKER = 2
    private val TAG = "MainActivity"

    private lateinit var mMessageAdapter: MessageAdapter
    private var mUsername: String? = null

    private val firebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val firebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    private val usersReference by lazy { firebaseDatabase.reference.child("users") }
    private val databaseReference by lazy { firebaseDatabase.reference.child("messages") }
    private val storageReference by lazy { firebaseStorage.reference.child("chat_photos") }

    private var childEventListener: ChildEventListener? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null


    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        mUsername = ANONYMOUS

        FirebaseApp.initializeApp(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        // Initialize message ListView and its adapter
        val friendlyMessages = ArrayList<FriendlyMessage>()
        mMessageAdapter = MessageAdapter(this, R.layout.item_message, friendlyMessages)
        binding.messageListView.adapter = mMessageAdapter

        // Initialize progress bar
        progressBar.visibility = ProgressBar.INVISIBLE

        // ImagePickerButton shows an image picker to upload a image for a message
        photoPickerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER)
        }

        // Enable Send button when there's text to send
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                sendButton.isEnabled = charSequence.toString().trim { it <= ' ' }.isNotEmpty()
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        messageEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT))

        // Send button sends a message and clears the EditText
        sendButton.setOnClickListener {
            val friendlyMessage = FriendlyMessage(messageEditText.text.toString(), mUsername!!, null)
            databaseReference.push().setValue(friendlyMessage)
            messageEditText.setText("")
        }

        //val intent = Intent(this, LoginActivity::class.java)
        //startActivityForResult(intent, 5)
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                val userName = if (user.displayName.isNullOrEmpty()) user.phoneNumber else user.displayName
                onSignedInInitialize(user.uid, userName)
            } else {
                // User is signed out
                onSignedOutCleanup()

                val providers =
                    mutableListOf(
                        AuthUI.IdpConfig.EmailBuilder().build(),
                        AuthUI.IdpConfig.FacebookBuilder().build(),
                        AuthUI.IdpConfig.GoogleBuilder().build(),
                        AuthUI.IdpConfig.PhoneBuilder().build()
//                        AuthUI.IdpConfig.TwitterBuilder().build()
                    )

                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(providers)
                        .setLogo(R.mipmap.ic_launcher)
                        .build(), RC_SIGN_IN
                )
            }
        }

        fetchConfig()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data!!.data
            val photoRef = storageReference.child(selectedImageUri!!.lastPathSegment!!)
            photoRef.putFile(selectedImageUri)
                .addOnSuccessListener(this) { taskSnapshot ->
                    val urlTask = taskSnapshot.storage.downloadUrl
                    urlTask.addOnSuccessListener { uri ->
                        val friendlyMessage = FriendlyMessage(null, mUsername!!, uri.toString())
                        databaseReference.push().setValue(friendlyMessage)
                    }
                }
        }
    }


    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener!!)
    }

    override fun onPause() {
        super.onPause()
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener!!)
        }
        mMessageAdapter.clear()
        detachDatabaseReadListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                AuthUI.getInstance().signOut(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun onSignedInInitialize(userId:String, username: String?) {
        mUsername = username
        attachDatabaseReadListener()
    }

    private fun onSignedOutCleanup() {
        mUsername = ANONYMOUS
        mMessageAdapter.clear()
        detachDatabaseReadListener()
    }

    private fun attachDatabaseReadListener() {
        if (childEventListener == null) {
            childEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    val friendlyMessage = dataSnapshot.getValue(FriendlyMessage::class.java)
                    mMessageAdapter.add(friendlyMessage)
                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                    val friendlyMessage = dataSnapshot.getValue(FriendlyMessage::class.java)
                    mMessageAdapter.remove(mMessageAdapter.getItem(mMessageAdapter.count - 1))
                    mMessageAdapter.add(friendlyMessage)
                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}

                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}

                override fun onCancelled(databaseError: DatabaseError) {}
            }
            databaseReference.addChildEventListener(childEventListener!!)
        }
    }

    private fun detachDatabaseReadListener() {
        if (childEventListener != null) {
            databaseReference.removeEventListener(childEventListener!!)
            childEventListener = null
        }
    }

    private fun fetchConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap[FRIENDLY_MSG_LENGTH_KEY] = DEFAULT_MSG_LENGTH_LIMIT
        firebaseRemoteConfig.setDefaults(defaultConfigMap)

        firebaseRemoteConfig.fetchAndActivate()
            .addOnSuccessListener {
                firebaseRemoteConfig.activate()
                applyRetrievedLengthLimit()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error fetching config", e)
                applyRetrievedLengthLimit()
            }
    }

    private fun applyRetrievedLengthLimit() {
        val friendlyMsgLength = firebaseRemoteConfig.getLong(FRIENDLY_MSG_LENGTH_KEY)
        messageEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(friendlyMsgLength.toInt()))
        Log.d(TAG, "$FRIENDLY_MSG_LENGTH_KEY = $friendlyMsgLength")
    }
}
