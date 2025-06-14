package com.example.fastnote

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import com.example.fastnote.db.ThoughtEntity

class ThoughtsListAdapter(
    private var items: List<ThoughtEntity>
) : RecyclerView.Adapter<ThoughtsListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thought: TextView = itemView.findViewById(R.id.thought)
        val expiration: TextView = itemView.findViewById(R.id.expiration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.thought_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val element = items[position]
        holder.thought.text = element.content
        holder.expiration.text = element.createdAt.toString()
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ThoughtEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}