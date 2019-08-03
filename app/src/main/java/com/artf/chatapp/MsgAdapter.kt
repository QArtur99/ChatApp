package com.artf.chatapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artf.chatapp.databinding.ItemMessageLeftBinding
import com.artf.chatapp.databinding.ItemMessageLeftImgBinding
import com.artf.chatapp.databinding.ItemMessageRightBinding
import com.artf.chatapp.databinding.ItemMessageRightImgBinding
import com.artf.chatapp.model.Message

class MsgAdapter(
    private val clickListener: OnClickListener
) : ListAdapter<Message, RecyclerView.ViewHolder>(GridViewDiffCallback) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        (holder as MsgViewHolder<*>).bind(clickListener, msg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_message_left -> MsgViewHolder(
                ItemMessageLeftBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            R.layout.item_message_right -> MsgViewHolder(
                ItemMessageRightBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            R.layout.item_message_left_img -> MsgViewHolder(
                ItemMessageLeftImgBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            R.layout.item_message_right_img -> MsgViewHolder(
                ItemMessageRightImgBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)!!
        return if (item.isOwner!!) {
            if (item.photoUrl == null) R.layout.item_message_right else R.layout.item_message_right_img
        } else {
            if (item.photoUrl == null) R.layout.item_message_left else R.layout.item_message_left_img
        }
    }

    class MsgViewHolder<T : ViewDataBinding> constructor(val binding: T) : RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: OnClickListener, item: Message) {
            when (binding) {
                is ItemMessageLeftBinding -> {
                    binding.message = item
                }
                is ItemMessageRightBinding -> {
                    binding.message = item
                }
                is ItemMessageLeftImgBinding -> {
                    binding.message = item
                }
                is ItemMessageRightImgBinding -> {
                    binding.message = item
                }
            }

            //binding.clickListener = clickListener
            binding.executePendingBindings()
        }
    }

    companion object GridViewDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    class OnClickListener(val clickListener: (productId: Message) -> Unit) {
        fun onClick(product: Message) = clickListener(product)
    }
}