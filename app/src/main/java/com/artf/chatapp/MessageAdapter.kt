package com.artf.chatapp

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.artf.chatapp.model.Message
import com.bumptech.glide.Glide

class MessageAdapter(context: Context, resource: Int, objects: List<Message>) :
    ArrayAdapter<Message>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = (context as Activity).layoutInflater.inflate(R.layout.item_message, parent, false)
        }

        val photoImageView = convertView!!.findViewById(R.id.photoImageView) as ImageView
        val messageTextView = convertView.findViewById(R.id.messageTextView) as TextView
        val authorTextView = convertView.findViewById(R.id.nameTextView) as TextView

        val message = getItem(position)

        val isPhoto = message!!.photoUrl != null
        if (isPhoto) {
            messageTextView.visibility = View.GONE
            photoImageView.visibility = View.VISIBLE
            Glide.with(photoImageView.context)
                .load(message.photoUrl)
                .into(photoImageView)
        } else {
            messageTextView.visibility = View.VISIBLE
            photoImageView.visibility = View.GONE
            messageTextView.text = message.text
        }
        authorTextView.text = message.name

        return convertView
    }
}