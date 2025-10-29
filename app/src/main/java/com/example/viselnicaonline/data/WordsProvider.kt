package com.example.viselnicaonline.data

import android.content.Context
import org.json.JSONObject
import javax.inject.Inject

class WordsProvider @Inject constructor(
    private val context: Context
) {
    private val words: List<String> by lazy {
        val json = context.assets.open("words.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(json).getJSONArray("words")
        List(arr.length()) { arr.getString(it) }
    }

    fun getRandomWord(): String = words.random()
}