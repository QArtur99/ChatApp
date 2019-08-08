package com.artf.chatapp.utils

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artf.chatapp.ChatListAdapter
import com.artf.chatapp.MsgAdapter
import com.artf.chatapp.R
import com.artf.chatapp.SearchAdapter
import com.artf.chatapp.model.Chat
import com.artf.chatapp.model.Message
import com.artf.chatapp.model.User
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("msgList")
fun bindMsgRecyclerView(recyclerView: RecyclerView, data: List<Message>) {
    val adapter= recyclerView.adapter as MsgAdapter
    adapter.submitList(data)
    adapter.notifyDataSetChanged()
    recyclerView.layoutManager!!.scrollToPosition(recyclerView.adapter!!.itemCount-1)
}

@BindingAdapter("userSearchList")
fun bindUserSearchRecyclerView(recyclerView: RecyclerView, data: List<User>?) {
    val adapter= recyclerView.adapter as SearchAdapter
    adapter.submitList(data)
    adapter.notifyDataSetChanged()
}

@BindingAdapter("userChatList")
fun bindUserChatRecyclerView(recyclerView: RecyclerView, data: List<Chat>?) {
    val adapter= recyclerView.adapter as ChatListAdapter
    adapter.submitList(data)
    adapter.notifyDataSetChanged()
}

@BindingAdapter("imageUrl")
fun bindImage(imgView: ImageView, imgUrl: String?) {

    if (imgUrl.isNullOrEmpty()) {
        imgView.visibility = View.GONE
        return
    } else imgView.visibility = View.VISIBLE

    Glide.with(imgView.context)
        .load(imgUrl)
        .apply(
            RequestOptions()
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_broken_image)
        )
        .into(imgView)
}

@BindingAdapter("profileImgUrl")
fun bindProfileImg(imgView: ImageView, imgUrl: String?) {

    if (imgUrl.isNullOrEmpty()) {
        imgView.setImageResource(R.drawable.ic_account_circle_black_24dp)
        return
    }

    Glide.with(imgView.context)
        .load(imgUrl)
        .apply(
            RequestOptions()
                .placeholder(R.drawable.loading_animation)
                .error(R.drawable.ic_account_circle_black_24dp)
                .circleCrop()
        )
        .into(imgView)
}

@BindingAdapter("hourTime")
fun bindingTextHourTime(textView: TextView, timestamp: Long) {
    val df = SimpleDateFormat("HH:mm", Locale.getDefault())
    textView.text = df.format(Date(timestamp))
}

@BindingAdapter("textOrGone")
fun bindingTextOrGone(textView: TextView, text: String?) {
    if(text == null){
        textView.visibility = View.GONE
    }else{
        textView.visibility = View.VISIBLE
        textView.text = text
    }
}