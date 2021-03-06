package com.bios.serverack.ui.upload

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter.formatShortFileSize
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bios.serverack.BuildConfig
import com.bios.serverack.R
import com.bios.serverack.Utils
import com.bios.serverack.data.repository.Repository
import com.bios.serverack.databinding.FragmentUploadBinding
import com.bios.serverack.ui.files.FilesFragmentDirections
import com.bios.serverack.ui.signup.SignupFragmentDirections
import com.google.android.material.snackbar.Snackbar
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import java.io.File
import java.lang.Exception
import java.util.*


class UploadFragment : Fragment() {
    val uploadViewModel: UploadViewModel by viewModels()
    lateinit var uploadBinding: FragmentUploadBinding
    lateinit var uploadFile: Uri
    var fileExtensions = listOf("txt", "pdf", "png", "jpg", "jpeg", "gif")

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


    private var resultContracts =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val selectedFile: Uri? = it.data?.data
                val extension = Utils.getPath(requireActivity(), it.data!!.data!!)
                if (extension != null) {
                    val file = File(extension)
                    val filesize = (file.length() / 1024)
                    Log.i("File", filesize.toString())
                    if (file.extension in fileExtensions) {
                        if (filesize < 97) {
                            uploadFile = selectedFile!!
                            uploadBinding.fileName.text = file.name
                        } else {
                            showSnackBar("Too Large File Choose a file less than 100 KB")
                        }
                    } else {
                        showSnackBar("File Extension Not Supported")
                    }
                } else {
                    showSnackBar("Invalid File Or Extension")
                }
            }
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
            if (Utils.isNetworkAvailable(requireActivity())) {
                if (this::uploadFile.isInitialized) {
                    showSnackBar("Starting Upload")
                    uploadBinding.fileUploadProgress.visibility = View.VISIBLE
                    MultipartUploadRequest(
                        requireActivity(),
                        serverUrl = Repository().getServerEndPoint() + "upload"
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
                                    if (uploadInfo.successfullyUploadedFiles >= 1) {
                                        showSnackBar("Upload Completed")
                                    }
                                    uploadBinding.fileUploadProgress.visibility = View.GONE
                                }

                                override fun onCompletedWhileNotObserving() {

                                }

                                override fun onError(
                                    context: Context,
                                    uploadInfo: UploadInfo,
                                    exception: Throwable
                                ) {
                                    uploadBinding.fileUploadProgress.visibility = View.GONE
                                    if (exception.message?.contains("Upload error")!!) {
                                        showSnackBar("Something is wrong, if you are admin, you can upload the server yaml file.")
                                        uploadViewModel.checkUserIsAdmin()
                                    }
                                }

                                override fun onProgress(context: Context, uploadInfo: UploadInfo) {
                                    Log.i("TAG", "onProgress:${uploadInfo.progressPercent} ")
                                }

                                override fun onSuccess(
                                    context: Context,
                                    uploadInfo: UploadInfo,
                                    serverResponse: ServerResponse
                                ) {
                                    uploadBinding.fileUploadProgress.visibility = View.GONE
                                    showSnackBar(serverResponse.bodyString)
                                }

                            })
                } else {
                    showSnackBar("Please select a file to upload")
                }
            } else {
                showSnackBar("Please connect to the internet")
            }
        }
        uploadViewModel.isAdminLiveData.observe(viewLifecycleOwner, {
            if (it) {
                showSnackBar("You are a admin")
                Handler(Looper.getMainLooper()).postDelayed({
                    openYamlFragment()
                }, 3000)
            } else {
                showSnackBar("You are not a admin")
            }

        })
    }

    private fun openYamlFragment() {
        this.findNavController()
            .navigate(UploadFragmentDirections.actionUploadFragmentToYamlFragment())
    }

    private fun checkStoragePermissions() {
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