package com.leverages.imagesearchbluemix

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Created by takeo.kusama on 2017/07/19.
 */

class ItemsFragment : Fragment(),ItemsRecyclerViewAdapter.ItemViewInterface{
    override fun onAdaptorInteraction(item: Item) {
        (activity as OnFragmentInteractionListener).onListItemClick(this,item)
    }

    private var mColumnCount = 1
    var item_list :List<Item>?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater!!.inflate(R.layout.fragment_items, container, false)
        val rview = root.findViewById<RecyclerView>(R.id.item_list) as RecyclerView
        (activity as OnFragmentInteractionListener).onListFragmentInit(this,rview)
        return root
    }

    fun setDataToRecyclerView(rview: RecyclerView) {
        if (rview is RecyclerView) {
            val context = rview.context
            if (mColumnCount <= 1) {
                rview.layoutManager = LinearLayoutManager(context)
            } else {
                rview.layoutManager = GridLayoutManager(context, mColumnCount)
            }
            rview.adapter = ItemsRecyclerViewAdapter(item_list!!,this)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    interface OnFragmentInteractionListener {
        fun onListFragmentInit(fragment:ItemsFragment,rview: RecyclerView)
        fun onListItemClick(fragment:ItemsFragment,item:Item)
    }

    companion object {

        private val ARG_COLUMN_COUNT = "column-count"

        fun newInstance(columnCount: Int): ItemsFragment {
            val fragment = ItemsFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)

            fragment.arguments = args
            return fragment
        }
    }
}
