package com.bios.serverack.data.repository

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bios.serverack.BiosApplication
import com.bios.serverack.R
import com.bios.serverack.data.model.Signup
import com.bios.serverack.data.model.User
import com.bios.serverack.data.remote.ServiceBuilder.retrofitService

class Repository {
    private val networkService = retrofitService
    private val application: Application = BiosApplication.instance

    private val sharedPref: SharedPreferences by lazy {
        application.getSharedPreferences(
            application.getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )
    }


    suspend fun doLoginService(user: User): String {
        Log.i("TAG", "doLoginService: ")
        return networkService.doLoginAsync(user)
    }

    suspend fun doSignUpService(signup: Signup): String {
        return networkService.doSignUp(signup)
    }

    suspend fun getFilesFromServer(): String? {
        return getJWTToken()?.let { networkService.getAllFiles(it) }
    }


    fun saveJWTToken(token: String) {
        sharedPref.edit().putString(application.getString(R.string.jwt_token), token).apply()
    }

    fun getJWTToken() =
        sharedPref.getString(application.getString(R.string.jwt_token), "")

    suspend fun deleteFile(filename: String): String {
        return networkService.deleteFile(getJWTToken()!!, filename)
    }


}