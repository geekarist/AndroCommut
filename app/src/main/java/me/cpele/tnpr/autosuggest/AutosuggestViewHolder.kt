package me.cpele.tnpr.autosuggest

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import me.cpele.tnpr.R

class AutosuggestViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(item: PlaceUiModel?) {
        val labelTextView = itemView.findViewById<TextView>(R.id.autosuggest_result_text)
        labelTextView.text = item?.label
    }
}
