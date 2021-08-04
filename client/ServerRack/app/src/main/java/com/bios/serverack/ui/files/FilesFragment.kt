package com.bios.serverack.ui.files

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bios.serverack.R
import com.bios.serverack.data.model.Message
import com.bios.serverack.databinding.FragmentFilesBinding
import com.google.android.material.snackbar.Snackbar


class FilesFragment : Fragment() {

    private val fileViewModel: FilesViewModel by viewModels()
    lateinit var filesBinding: FragmentFilesBinding
    private lateinit var filesAdapter: FilesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        filesBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_files, container, false)
        fileViewModel.getFilesDataFromServer()

        fileViewModel.networkHandler.observe(viewLifecycleOwner, {
            if (!it) {
                filesBinding.filesProgressBar.visibility = View.VISIBLE
            } else {
                filesBinding.filesProgressBar.visibility = View.GONE
            }
        })

        fileViewModel.messageHandler.observe(viewLifecycleOwner, {
            it?.let { it1 ->
                if (it1.isNotEmpty()) {
                    showSnackBar(it1)
                } else {
                    showSnackBar("Loading data from server")
                }
            }
        })

        fileViewModel.messageData.observe(viewLifecycleOwner, {
            filesAdapter =
                FilesAdapter(FilesAdapter.OnClickListener { message: Message, view: View ->
                    if (view.id == R.id.deleteButton) {
                        fileViewModel.deleteFile(message)
                    } else {
                        fileViewModel.downloadFile(message)
                    }
                })
            filesBinding.filesList.apply {
                adapter = filesAdapter
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(activity)
            }
            filesAdapter.submitList(it)


        })

        filesBinding.uploadFiles.setOnClickListener {
            this.findNavController()
                .navigate(FilesFragmentDirections.actionFilesFragmentToUploadFragment())
        }
        return filesBinding.root
    }


    fun showSnackBar(msg: String, len: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(filesBinding.root, msg, len)
            .show()
    }

}