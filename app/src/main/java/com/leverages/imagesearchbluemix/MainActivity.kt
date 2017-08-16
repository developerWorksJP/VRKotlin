package com.leverages.imagesearchbluemix

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.widget.Toast
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getAs

class MainActivity : Activity(),
        CameraFragment.CameraFragmentInterface,
        ItemsFragment.OnFragmentInteractionListener
{
    override fun onListItemClick(fragment: ItemsFragment, item: Item) {
        var uri = Uri.parse(item.link);
        var i = Intent(Intent.ACTION_VIEW,uri);
        startActivity(i)
    }

    override fun onListFragmentInit(fragment:ItemsFragment,rview: RecyclerView) {
        if(fragment.arguments.containsKey("query")){
            var query = fragment.arguments.getString("query")
            val requestUrl = AmazonApi.search(listOf(query))
            requestUrl?.httpGet()?.responseString { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        Toast.makeText(this@MainActivity,"検索結果が見つかりませんでした",Toast.LENGTH_LONG).show()
                        Log.d("Error",result.component2().toString());
                    }
                    is Result.Success -> {
                        fragment.item_list = Item.ItemXmlParser.getParseItems(result.getAs<String>() as String)
                        fragment.setDataToRecyclerView(rview)
                    }
                }
            }
        }
    }

    override fun onCameraFragmentInteraction(fragment: CameraFragment) {
        this.runOnUiThread({
            var itemsFragment = ItemsFragment()
            var args = Bundle()
            args.putString("query",fragment.resultQuery)
            itemsFragment.arguments = args
            fragmentManager.beginTransaction()
                    .replace(R.id.Container,itemsFragment,"camera")
                    .addToBackStack("camera")
                    .commit()
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fragmentManager.beginTransaction()
                .replace(R.id.Container,CameraFragment(),"container")
                .addToBackStack("container")
                .commit();
    }
}
