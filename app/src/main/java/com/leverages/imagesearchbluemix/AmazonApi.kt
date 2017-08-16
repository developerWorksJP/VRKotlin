package com.leverages.imagesearchbluemix

/**
 * Created by takeo.kusama on 2017/07/19.
 */

import java.util.HashMap

object AmazonApi {
    /*
     * Use the end-point according to the region you are interested in.
     */
    private val ENDPOINT = "webservices.amazon.co.jp"
    fun search(q: List<String>,contentType:String = "xml",searchIndex:String = "All"):String? {

        /*
         * Set up the signed requests helper.
         */
        val helper: SignedRequestsHelper
        var requestUrl: String? = null
        try {
            helper = SignedRequestsHelper.getInstance(ENDPOINT, AWS_ACCESS_KEY_ID, AWS_SECRET_KEY,ASSOCIATE_TAG)
            val params = HashMap<String, String>()
            params.put("Service", "AWSECommerceService")
            params.put("Operation", "ItemSearch")
            params.put("AWSAccessKeyId",AWS_ACCESS_KEY_ID)
            params.put("AssociateTag", ASSOCIATE_TAG)
            params.put("SearchIndex", searchIndex)
            params.put("ResponseGroup", "Images,ItemAttributes")
            params.put("Keywords", q.joinToString(" "))
            if(contentType == "html") {
                params.put("ContentType","text/html")
            }
            requestUrl = helper.sign(params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return requestUrl
    }
}