package com.artf.chatapp.utils

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artf.chatapp.MsgAdapter
import com.artf.chatapp.R
import com.artf.chatapp.model.Message
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("listData")
fun bindMoviesRecyclerView(recyclerView: RecyclerView, data: List<Message>) {
    val adapter = recyclerView.adapter as MsgAdapter
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

@BindingAdapter("hourTime")
fun bindingTextHourTime(textView: TextView, timestamp: Long) {
    val df = SimpleDateFormat("HH:mm", Locale.getDefault())
    textView.text = df.format(Date(timestamp))
}