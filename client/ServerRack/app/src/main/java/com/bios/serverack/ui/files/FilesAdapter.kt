package com.bios.serverack.ui.files


import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bios.serverack.data.model.Message
import com.bios.serverack.databinding.FileItemsBinding

class FilesAdapter : ListAdapter<Message, FileViewHolder>(FileItemCallBack()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        Log.i("TAG", "bind:${item.filename} ")
    }
}

class FileViewHolder(val binding: FileItemsBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: Message) = with(binding) {
        binding.message = item
        Log.i("TAG", "bind:${item.filename} ")
        binding.executePendingBindings()
    }

    companion object {
        fun from(parent: ViewGroup): FileViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = FileItemsBinding.inflate(layoutInflater, parent, false)
            return FileViewHolder(binding)
        }
    }
}


class FileItemCallBack : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.id == oldItem.id
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem == newItem
    }


}