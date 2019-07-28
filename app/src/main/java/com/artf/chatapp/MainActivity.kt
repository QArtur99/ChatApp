package com.artf.chatapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.artf.chatapp.databinding.ActivityMainBinding
import com.artf.chatapp.model.Message
import com.artf.chatapp.utils.afterTextChanged
import com.firebase.ui.auth.AuthUI
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var mMessageAdapter: MessageAdapter
    private lateinit var binding: ActivityMainBinding
    private val firebaseHandler by lazy { FirebaseHandler(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.firebaseHandler = firebaseHandler

        // Initialize message ListView and its adapter
        val friendlyMessages = ArrayList<Message>()
        mMessageAdapter = MessageAdapter(this, R.layout.item_message, friendlyMessages)
        binding.messageListView.adapter = mMessageAdapter
        firebaseHandler.msgData.observe(this, Observer { msgList ->
            mMessageAdapter.clear()
            mMessageAdapter.addAll(msgList)
        })

        // Initialize progress bar
        progressBar.visibility = ProgressBar.INVISIBLE

        // ImagePickerButton shows an image picker to upload a image for a message
        photoPickerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(
                Intent.createChooser(intent, "Complete action using"),
                FirebaseHandler.RC_PHOTO_PICKER
            )
        }

        binding.messageEditText.afterTextChanged { text ->
            sendButton.isEnabled = text.isNotEmpty()
        }

        sendButton.setOnClickListener {
            firebaseHandler.pushMsg(binding.messageEditText.text.toString(), null)
            binding.messageEditText.setText("")
        }

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        firebaseHandler.onActivityResult(requestCode, resultCode, data)
    }


    override fun onResume() {
        super.onResume()
        firebaseHandler.onResume()
    }

    override fun onPause() {
        super.onPause()
        firebaseHandler.onPause()
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
            R.id.change_username -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
