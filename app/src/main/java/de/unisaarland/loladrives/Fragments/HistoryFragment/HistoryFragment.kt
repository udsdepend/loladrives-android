package de.unisaarland.loladrives.Fragments.historyFragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_history.*
import kotlinx.android.synthetic.main.privacy_dialog.view.*
import java.io.File

class HistoryFragment : Fragment() {

    // Track Directory
    private lateinit var letDirectory: File

    // List of done tracks in track directory
    private var files: MutableList<File> = mutableListOf()
    private var fileNames: MutableList<String> = mutableListOf()

    private lateinit var activity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        activity = requireActivity() as MainActivity
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onStart() {
        activity.title_textview.text = getString(R.string.history)
        activity.backButton.setImageResource(R.drawable.back_arrow_icon)

        refreshList()

/*        if (!activity.donatingAllowed) {
            val pd = PrivacyDialog()
            //pd.show(activity.supportFragmentManager, "")
        }*/

        super.onStart()
    }

    private fun refreshList() {
        val path = requireActivity().getExternalFilesDir(null)
        letDirectory = File(path, "pcdfdata")
        letDirectory.mkdirs()

        files = mutableListOf()

        val filesArray = letDirectory.listFiles()

        filesArray?.forEach { directory ->
            if (directory.isDirectory) {
                directory.listFiles()?.forEach {
                    if (it.name.contains("ppcdf")) {
                        files.add(it)
                    }
                }
            }
        }
        files.sortDescending()

        fileNames = mutableListOf<String>()
        files.forEach {
            val displayName = it.name
                .replace(".ppcdf", "")
                .replace("_", "   ")
            fileNames.add(displayName)
        }

        if (fileNames.isEmpty()) {
            fileNames.add("No records available.")
        }

        val adapter =
            ArrayAdapter<String>(requireActivity(), android.R.layout.simple_list_item_1, fileNames)

        tripListView.adapter = adapter
        initOnClick()
    }

    private fun initOnClick() {
        // OnClick Listeners for the items to get into detail view
        tripListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (files.isNotEmpty()) {
                activity.openDetailView(files[position], false)
            }
        }

        // LongClick Listeners to delete the trips
        tripListView.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { _, _, position, _ ->
                if (files.isNotEmpty()) {
                    val deleteDialog = DeleteDialog(files[position], this)
                    deleteDialog.show(requireActivity().supportFragmentManager, "")
                    true
                } else {
                    true
                }
            }
    }

    class PrivacyDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val dialog = activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.create()
            } ?: throw IllegalStateException("Activity can not be null")

            val dialogView = layoutInflater.inflate(R.layout.privacy_dialog, null)
            dialogView.privacySwitch.setOnClickListener {
                (activity as MainActivity).changeDonatingAllowed(dialogView.privacySwitch.isChecked)
                dialog.dismiss()
            }

            dialog.setView(dialogView)

            return dialog
        }
    }

    class DeleteDialog(val file: File, val fragment: HistoryFragment) : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val fileName = file.nameWithoutExtension
            return activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle("Delete Trip")
                    .setPositiveButton("Delete") { _, _ ->
                        deleteFile(file)
                    }
                    .setNegativeButton("Cancel") { _, _ -> ; }
                    .setMessage("You really want to delete the saved Trip $fileName ? ")
                builder.create()
            } ?: throw IllegalStateException("Activity can not be null")
        }

        private fun deleteFile(file: File) {
            file.delete()
            val directory = File(fragment.letDirectory, file.nameWithoutExtension)
            if (directory.exists() && directory.isDirectory && directory.listFiles()
                    ?.isEmpty() == true
            ) {
                directory.delete()
            }

            fragment.refreshList()
            val sharedPref =
                fragment.activity.getSharedPreferences("uploaded", Context.MODE_PRIVATE)
            val alreadyUploaded = mutableSetOf<String>()
            alreadyUploaded.addAll(sharedPref.getStringSet("uploaded_trips", mutableSetOf())!!)
            alreadyUploaded.remove(file.name)
            sharedPref.edit().putStringSet("uploaded_trips", alreadyUploaded).apply()
        }
    }
}
