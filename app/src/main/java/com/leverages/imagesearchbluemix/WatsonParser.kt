package com.leverages.imagesearchbluemix

import com.beust.klaxon.*;

/**
 * Created by takeo.kusama on 2017/07/26.
 */
object WatsonParser{
    var klaxonParser:Parser = Parser()
    fun vrResponseParse(strJson:String,category: String = "tea"):String?{
        var json:JsonObject = klaxonParser.parse(StringBuilder(strJson)) as JsonObject
        var images = json.array<JsonObject>("images")

        var classifiers = images?.firstOrNull {
            (it.array<JsonObject>("classifiers") as JsonArray).any {
                it.string("name") == category
            }
        }?.array<JsonObject>("classifiers")
        var selectedClassifier = classifiers?.maxBy<JsonObject,Double> {
            var _class = it.array<JsonObject>("classes")?.maxBy<JsonObject,Double> nest@ {
                return@nest if(!it.isEmpty()) it.double("score")!! else .0
            }
            _class?.double("score")!!
        }
        var selectedClass = selectedClassifier?.array<JsonObject>("classes")?.maxBy<JsonObject,Double> {
            return@maxBy it.double("score")!!
        }?.string("class")
        return selectedClass
    }
}