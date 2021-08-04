package com.bios.serverack.ui.upload

import androidx.lifecycle.ViewModel
import com.bios.serverack.data.repository.Repository

class UploadViewModel : ViewModel() {
    private val repository = Repository()

    fun getToken() = repository.getJWTToken()
}