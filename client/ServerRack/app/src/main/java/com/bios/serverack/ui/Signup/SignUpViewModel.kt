package com.bios.serverack.ui.Signup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bios.serverack.data.model.Signup
import com.bios.serverack.data.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception

class SignUpViewModel : ViewModel() {
    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(viewModelJob + Dispatchers.Main)
    private val repository = Repository()
    val doSignUp = MutableLiveData<String>()


    fun doSignUp(username: String, password: String, email: String) {
        viewModelScope.launch {
            val signup = try {
                repository.doSignUpService(Signup(username, email, password))
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
            doSignUp.value = JSONObject(signup).getString("message")
        }
    }
}