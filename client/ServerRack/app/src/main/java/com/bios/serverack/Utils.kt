package com.bios.serverack

import com.bios.serverack.data.model.Message
import org.json.JSONArray
import org.json.JSONObject

object Utils {


    fun filesDataConverter(data: String): ArrayList<Message> {
        val fileListArray = JSONObject(data).getJSONArray("message")
        val arrayList = ArrayList<Message>()
        for (i in 0 until fileListArray.length()) {
            val filename = fileListArray.getJSONObject(i).getString("filename");
            val size = fileListArray.getJSONObject(i).getInt("size");
            arrayList.add(Message(i,filename, size))
        }
        return arrayList
    }
}

