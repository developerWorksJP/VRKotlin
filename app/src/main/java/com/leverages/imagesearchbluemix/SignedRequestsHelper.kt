package com.leverages.imagesearchbluemix

/**
 * Created by takeo.kusama on 2017/07/19.
 */

import java.io.UnsupportedEncodingException

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.SortedMap
import java.util.TimeZone
import java.util.TreeMap

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Base64

class SignedRequestsHelper() {
    lateinit var associateTag :String
    lateinit var endpoint:String
    lateinit var awsAccessKeyId:String
    lateinit var awsSecretKey:String
    private var secretKeySpec: SecretKeySpec? = null
    private var mac: Mac? = null

    fun sign(params: MutableMap<String, String>): String {
        val REQUEST_METHOD = "GET"
        val REQUEST_URI = "/onca/xml"

        params.put("AWSAccessKeyId", awsAccessKeyId)
        params.put("Timestamp", timestamp())

        val sortedParamMap = TreeMap(params)
        val canonicalQS = canonicalize(sortedParamMap)
        val toSign = REQUEST_METHOD + "\n" + endpoint + "\n" + REQUEST_URI + "\n" + canonicalQS
        val hmac = hmac(toSign)
        val sig = percentEncodeRfc3986(hmac)
        val url = "http://" + endpoint + REQUEST_URI + "?" +
                canonicalQS + "&Signature=" + sig

        return url
    }

    private fun hmac(stringToSign: String): String {
        var signature: String? = null
        val data: ByteArray
        val rawHmac: ByteArray
        try {
            data = stringToSign.toByteArray(charset(UTF8_CHARSET))
            rawHmac = mac!!.doFinal(data)
            val encoder = Base64()
            signature = String(encoder.encode(rawHmac))
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(UTF8_CHARSET + " is unsupported!", e)
        }

        return signature
    }

    private fun timestamp(): String {
        var timestamp: String? = null
        val cal = Calendar.getInstance()
        val dfm = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        dfm.timeZone = TimeZone.getTimeZone("GMT")
        timestamp = dfm.format(cal.time)
        return timestamp
    }

    private fun canonicalize(sortedParamMap: SortedMap<String, String>): String {
        if (sortedParamMap.isEmpty()) {
            return ""
        }

        val buffer = StringBuffer()
        val iter = sortedParamMap.entries.iterator()

        while (iter.hasNext()) {
            val kvpair = iter.next()
            buffer.append(percentEncodeRfc3986(kvpair.key))
            buffer.append("=")
            buffer.append(percentEncodeRfc3986(kvpair.value))
            if (iter.hasNext()) {
                buffer.append("&")
            }
        }
        val cannoical = buffer.toString()
        return cannoical
    }

    private fun percentEncodeRfc3986(s: String): String {
        var out: String
        try {
            out = URLEncoder.encode(s, UTF8_CHARSET)
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~")
        } catch (e: UnsupportedEncodingException) {
            out = s
        }
        return out
    }

    companion object {
        private val UTF8_CHARSET = "UTF-8"
        private val HMAC_SHA256_ALGORITHM = "HmacSHA256"
        fun getInstance(endpoint:String,awsAccessKeyId:String,awsSecretKey:String,associateTag:String): SignedRequestsHelper
        {
            if (null == endpoint || endpoint.length == 0)
            { throw IllegalArgumentException("endpoint is null or empty"); }
            if (null == awsAccessKeyId || awsAccessKeyId.length == 0)
            { throw IllegalArgumentException("awsAccessKeyId is null or empty"); }
            if (null == awsSecretKey || awsSecretKey.length == 0)
            { throw IllegalArgumentException("awsSecretKey is null or empty"); }

            var instance: SignedRequestsHelper  = SignedRequestsHelper();
            instance.endpoint = endpoint.toLowerCase();
            instance.awsAccessKeyId = awsAccessKeyId;
            instance.awsSecretKey = awsSecretKey;
            instance.associateTag = associateTag;

            var secretyKeyBytes = instance.awsSecretKey.toByteArray(charset(UTF8_CHARSET));
            instance.secretKeySpec = SecretKeySpec(secretyKeyBytes, HMAC_SHA256_ALGORITHM);
            instance.mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            instance.mac!!.init(instance.secretKeySpec);

            return instance;
        }
    }

}