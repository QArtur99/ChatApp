package com.artf.chatapp.utils

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artf.chatapp.R
import com.artf.chatapp.data.model.Chat
import com.artf.chatapp.data.model.Message
import com.artf.chatapp.data.model.User
import com.artf.chatapp.utils.extension.saveTo
import com.artf.chatapp.utils.extension.toDp
import com.artf.chatapp.view.chatRoom.MsgAdapter
import com.artf.chatapp.view.chatRooms.ChatListAdapter
import com.artf.chatapp.view.searchUser.SearchAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@BindingAdapter("msgList")
fun bindMsgRecyclerView(recyclerView: RecyclerView, data: List<Message>?) {
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

    if (imgUrl == "loading") {
        imgView.setImageResource(R.drawable.loading_animation)
        return
    }

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
fun bindingTextHourTime(textView: TextView, timestamp: Any?) {
    if (timestamp is Timestamp) {
        val df = SimpleDateFormat("HH:mm", Locale.getDefault())
        textView.text = df.format(timestamp.toDate())
    }
}

@BindingAdapter("textOrGoneName", "groupChat")
fun bindingTextOrGoneUsername(textView: TextView, text: String?, isGroupChat: Boolean?) {
    val groupChat = isGroupChat ?: false

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

@BindingAdapter("lastMsg")
fun bindingLastMsg(textView: TextView, message: Message?) {
    if (message == null) return
    message.audioUrl?.let { textView.text = "\uD83C\uDFA4 Record" }
    message.photoUrl?.let { textView.text = "\uD83D\uDCF7 Photo" }
    message.text?.let { textView.text = it }
}

@BindingAdapter("goneIfNull")
fun bindingGoneIfNull(view: View, text: String?) {
    view.visibility = if (text == null) View.GONE else View.VISIBLE
}

@BindingAdapter("goneIfNotNull")
fun bindingGoneIfNotNull(view: View, text: String?) {
    view.visibility = if (text == null) View.VISIBLE else View.GONE
}

@BindingAdapter("audioTime")
fun bindingAudioTime(textView: TextView, audioFile: Long?) {
    audioFile?.let { Utility.setAudioTimeMmSs(textView, audioFile) }
}

@BindingAdapter("fakeAudioProgress")
fun bindingFakeAudioProgress(view: View, msg: Message?) {
    if (msg == null) return
    CoroutineScope(Dispatchers.Main).launch {
        try {
            view.visibility = View.VISIBLE
            getAudio(msg)
            msg.audioDownloaded = true
            view.visibility = View.GONE
        } catch (e: Exception) {
            view.visibility = View.GONE
        }
    }
}

@BindingAdapter("onlineTint")
fun bindingOnlineTint(view: ImageView, isOnline: Boolean?) {
    isOnline?.let {
        view.setColorFilter(
            if (isOnline) ContextCompat.getColor(view.context, R.color.onlineOn)
            else ContextCompat.getColor(view.context, R.color.onlineOff)
        )
    }
}

@BindingAdapter("readIconTint")
fun bindingReadIconTint(view: ImageView, timestamp: Any?) {
    view.setColorFilter(
        if (timestamp != null) ContextCompat.getColor(view.context, R.color.readOn)
        else ContextCompat.getColor(view.context, R.color.readOff)
    )
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
        val newHeight = if (customHeight > parentHeight) parentHeight else customHeight
        linearLayout.layoutParams.height = if (it.isEmpty()) 90.toDp() else newHeight
    }
}

suspend fun getAudio(msg: Message) {
    withContext(Dispatchers.IO) { msg.audioFile?.let { msg.audioUrl?.saveTo(it) } }
}