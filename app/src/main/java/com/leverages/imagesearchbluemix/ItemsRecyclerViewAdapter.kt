package com.leverages.imagesearchbluemix

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.support.v7.widget.RecyclerView
import com.squareup.picasso.Picasso

/**
 * Created by takeo.kusama on 2017/07/19.
 */

class ItemsRecyclerViewAdapter(private val mValues: List<Item>, private val mListener: ItemViewInterface?) : RecyclerView.Adapter<ItemsRecyclerViewAdapter.ViewHolder>(){

    interface ItemViewInterface {
        fun onAdaptorInteraction(item:Item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_view, parent, false) as View
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mItem = mValues[position]
        holder.mTitle.text = mValues[position].title
        holder.mLink = mValues[position].link

        Picasso.with(holder.mImageView!!.context).load(holder?.mItem?.image).into(holder.mImageView)
        holder.mView.setOnClickListener {
            if (null != mListener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                mListener!!.onAdaptorInteraction(holder.mItem as Item)
            }
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        var mTitle: TextView = mView.findViewById<TextView>(R.id.item_name)
        var mItem: Item? = null
        var mImageView: ImageView? =null
        var mLink: String? = null

        init {
            mImageView = mView.findViewById<ImageView>(R.id.item_image)
        }

    }
}
