package com.artf.chatapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artf.chatapp.databinding.ItemSearchBinding
import com.artf.chatapp.model.Message

class SearchAdapter(private val clickListener: OnClickListener) : ListAdapter<Message,
        RecyclerView.ViewHolder>(GridViewDiffCallback) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val product = getItem(position)
        (holder as MsgViewHolder).bind(clickListener, product)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
        return MsgViewHolder(ItemSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    class MsgViewHolder constructor(val binding: ItemSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener: OnClickListener, item: Message) {
            binding.message = item
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