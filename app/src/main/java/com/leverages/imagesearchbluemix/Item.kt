package com.leverages.imagesearchbluemix

/**
 * Created by takeo.kusama on 2017/07/19.
 */


import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.StringReader

class Item{
    var title:String
    var link:String
    var image:String

    constructor(title:String,link:String,image:String ) {
        this.title = title
        this.link = link
        this.image = image
    }

    object ItemXmlParser {

        fun getParseItems(xml:String): List<Item>? {
                var parser = Xml.newPullParser()
                parser.setInput(StringReader(xml));
                parser.nextTag();
                return readFeed(parser);
        }

        fun getParseItems(xmlBytes: InputStream): List<Item>? {
            try {
                var parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(xmlBytes, null);
                parser.nextTag();
                return readFeed(parser);
            } finally {
                xmlBytes.close();
            }
            return null
        }

        fun readFeed(parser:XmlPullParser) :MutableList<Item>{
            var entries =  mutableListOf<Item>()
            searchTag(parser,"Items")
            parser.require(XmlPullParser.START_TAG, ns, "Items")
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue;
                }
                var name = parser?.name
                // Starts by looking for the entry tag
                if (name.equals("Item")) {
                    entries.add(readItem(parser));
                } else {
                    skip(parser);
                }
            }
            return entries;
        }

        private val ns: String? = null

        fun readItem(parser: XmlPullParser): Item {
            parser.require(XmlPullParser.START_TAG, ns, "Item")
            var title: String? = null
            var image: String? = null
            var link: String? = null
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                val name = parser.name
                if (name == "ItemAttributes") {
                    title = readTitle(parser)
                } else if (name == "MediumImage") {
                    image = readImageUrl(parser)
                } else if (name == "DetailPageURL") {
                    link = readLink(parser)
                } else {
                    skip(parser)
                }
            }
            return Item(title!!, link!!, image!!)
        }

        @Throws(IOException::class, XmlPullParserException::class)
        private fun readTitle(parser: XmlPullParser): String {
            var depth = searchTag(parser,"Title")
            parser.require(XmlPullParser.START_TAG, ns, "Title")
            val title = readText(parser)
            while(parser.next() != XmlPullParser.END_TAG || parser?.name != "ItemAttributes"){

            }
            parser.require(XmlPullParser.END_TAG, ns, "ItemAttributes")
            return title
        }

        @Throws(IOException::class, XmlPullParserException::class)
        private fun readLink(parser: XmlPullParser): String {
            parser.require(XmlPullParser.START_TAG, ns, "DetailPageURL")
            var link = readText(parser)
            parser.require(XmlPullParser.END_TAG, ns, "DetailPageURL")
            return link
        }

        @Throws(IOException::class, XmlPullParserException::class)
        private fun readImageUrl(parser: XmlPullParser): String {
            var depth = searchTag(parser,"URL")
            parser.require(XmlPullParser.START_TAG, ns, "URL")
            val imageUrl = readText(parser)
            while(parser.next() != XmlPullParser.END_TAG || parser?.name != "MediumImage"){

            }
            parser.require(XmlPullParser.END_TAG, ns, "MediumImage")
            return imageUrl
        }

        // For the tags title and imageUrl, extracts their text values.
        @Throws(IOException::class, XmlPullParserException::class)
        private fun readText(parser: XmlPullParser): String {
            var result = ""
            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.text
                parser.nextTag()
            }
            return result
        }


        @Throws(XmlPullParserException::class, IOException::class)
        private fun searchTag(parser: XmlPullParser,name:String) :Int{
            if (parser.eventType != XmlPullParser.START_TAG) {
                throw IllegalStateException()
            }
            var depth = 1
            while (depth != 0) {
                if(parser?.name == name) return depth
                when (parser.next()) {
                    XmlPullParser.END_TAG -> depth--
                    XmlPullParser.START_TAG -> depth++
                }
            }
            return depth
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun skip(parser: XmlPullParser) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                throw IllegalStateException()
            }
            var depth = 1
            while (depth != 0) {
                when (parser.next()) {
                    XmlPullParser.END_TAG -> depth--
                    XmlPullParser.START_TAG -> depth++
                }
            }
        }

    }

}

