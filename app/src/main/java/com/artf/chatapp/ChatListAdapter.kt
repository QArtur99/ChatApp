package com.artf.chatapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artf.chatapp.databinding.ItemChatBinding
import com.artf.chatapp.model.Chat

class ChatListAdapter(private val fragment: Fragment, private val clickListener: OnClickListener) : ListAdapter<Chat,
        RecyclerView.ViewHolder>(GridViewDiffCallback) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val product = getItem(position)
        (holder as MsgViewHolder).bind(fragment, clickListener, product)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
        return MsgViewHolder(ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    class MsgViewHolder constructor(val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(fragment: Fragment, clickListener: OnClickListener, item: Chat) {
            binding.lifecycleOwner = fragment
            binding.chat = item
            binding.clickListener = clickListener
            binding.executePendingBindings()
        }
    }

    companion object GridViewDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem.chatId == newItem.chatId
        }

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
            return oldItem == newItem
        }
    }

    class OnClickListener(val clickListener: (productId: Chat) -> Unit) {
        fun onClick(v: View, product: Chat) = clickListener(product)
    }
}