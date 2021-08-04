package com.bios.serverack.ui.login

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bios.serverack.data.model.User
import com.bios.serverack.data.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception

class LoginViewModel : ViewModel() {
    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(viewModelJob + Dispatchers.Main)
    private val repository = Repository()
    val doLogin = MutableLiveData<Boolean>(false)


    fun doLogin(userName: String, password: String) {
        Log.i("LoginData", "doLogin:methodcalled")

        viewModelScope.launch {
            val loginInfo = try {
                repository.doLoginService(User(userName, password))
            } catch (e: Exception) {
                Log.i("LoginData", "doLogin:${e.printStackTrace()}")
                ""
            }
            if (loginInfo.isNotEmpty()) {
                repository.saveJWTToken(JSONObject(loginInfo).getString("token"))
                Log.i("LoginData", "doLogin:${JSONObject(loginInfo).getString("token")}")
                doLogin.value = true
            }
        }

    }

}