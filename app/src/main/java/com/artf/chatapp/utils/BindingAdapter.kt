package com.artf.chatapp.utils

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.artf.chatapp.utils.extension.toDp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("msgList")
fun bindMsgRecyclerView(recyclerView: RecyclerView, data: List<Message>) {
    val adapter = recyclerView.adapter as MsgAdapter
    adapter.submitList(data) { recyclerView.layoutManager?.scrollToPosition(adapter.itemCount - 1) }
    adapter.notifyDataSetChanged()
}

@BindingAdapter("userSearchList")
fun bindUserSearchRecyclerView(recyclerView: RecyclerView, data: List<User>?) {
    val adapter = recyclerView.adapter as SearchAdapter
    adapter.submitList(data)
    adapter.notifyDataSetChanged()
}

@BindingAdapter("userChatList")
fun bindUserChatRecyclerView(recyclerView: RecyclerView, data: List<Chat>?) {
    val adapter = recyclerView.adapter as ChatListAdapter
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

@BindingAdapter("textOrGoneName", "groupChat")
fun bindingTextOrGoneUsername(textView: TextView, text: String?, isGroupChat: Boolean?) {
    val groupChat  = isGroupChat ?: false

    if (text == null || groupChat.not()) {
        textView.visibility = View.GONE
    } else {
        textView.visibility = View.VISIBLE
        textView.text = text
    }
}

@BindingAdapter("textOrGone")
fun bindingTextOrGone(textView: TextView, text: String?) {
    if (text == null) {
        textView.visibility = View.GONE
    } else {
        textView.visibility = View.VISIBLE
        textView.text = text
    }
}

/**
 * When @RecyclerView is in parent layout_height="wrap_content" it blinks on data change.
 * Setting layout_height programmatically prevent blinking onDataChange.
 */
@BindingAdapter("preventSearchBlinking")
fun bindPreventSearchBlinking(linearLayout: LinearLayout, data: List<User>?) {
    data?.let {
        val parentHeight = (linearLayout.parent as LinearLayout).height
        val customHeight = it.size * 74.toDp() + 16.toDp()
        val newHeight = if(customHeight > parentHeight) parentHeight else customHeight
        linearLayout.layoutParams.height = if (it.isEmpty()) 90.toDp() else newHeight
    }
}