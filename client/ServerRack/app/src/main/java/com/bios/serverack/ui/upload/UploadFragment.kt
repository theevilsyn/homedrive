package com.bios.serverack.ui.upload

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bios.serverack.BuildConfig
import com.bios.serverack.R
import com.bios.serverack.databinding.FragmentUploadBinding
import com.google.android.material.snackbar.Snackbar
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest


class UploadFragment : Fragment() {
    val uploadViewModel: UploadViewModel by viewModels()
    lateinit var uploadBinding: FragmentUploadBinding
    lateinit var uploadFile: Uri
    private val checkPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            var count = 0
            for ((key, value) in it) {
                if (value) {
                    count++
                }
            }
            if (count >= 2)
                launchFilePicker()
        }


    var resultContracts =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val selectedFile: Uri? = it.data?.data
//                Log.i("TAG", "${RealPathUtil.getRealPath(requireActivity(), selectedFile!!)} ")
                uploadFile = it.data!!.data!!
                Log.i("TAG", "$uploadFile ")

//                val file = File(RealPathUtil.getRealPath(requireActivity(), selectedFile!!))


            }
        }

    companion object {
        const val pickFileRequestCode = 42
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        uploadBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_upload, container, false
        )
        return uploadBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uploadBinding.selectfileButton.setOnClickListener {
            checkStoragePermissions()
        }

        uploadBinding.startUpload.setOnClickListener {
            if (this::uploadFile.isInitialized) {
                MultipartUploadRequest(
                    requireActivity(),
                    serverUrl = BuildConfig.API_POINT + "upload"
                )
                    .setMethod("POST")
                    .addHeader("x-access-token", uploadViewModel.getToken()!!)
                    .addFileToUpload(
                        filePath = uploadFile.toString(),
                        parameterName = "file"
                    ).subscribe(
                        context = requireContext(),
                        lifecycleOwner = viewLifecycleOwner,
                        delegate = object : RequestObserverDelegate {
                            override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
                                showSnackBar("Upload Completed")

                            }

                            override fun onCompletedWhileNotObserving() {
                                TODO("Not yet implemented")
                            }

                            override fun onError(
                                context: Context,
                                uploadInfo: UploadInfo,
                                exception: Throwable
                            ) {
                                showSnackBar("Upload Failed")

                            }

                            override fun onProgress(context: Context, uploadInfo: UploadInfo) {
                                Log.i("TAG", "onProgress:${uploadInfo.progressPercent} ")
                            }

                            override fun onSuccess(
                                context: Context,
                                uploadInfo: UploadInfo,
                                serverResponse: ServerResponse
                            ) {
                                showSnackBar(serverResponse.bodyString)
                            }

                        })
            } else {
                showSnackBar("Please select a file to upload")
            }
        }
    }


    fun checkStoragePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        checkPermission.launch(permissions)

    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*";
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        resultContracts.launch(intent)
    }

    fun showSnackBar(msg: String, len: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(uploadBinding.root, msg, len)
            .show()
    }
}