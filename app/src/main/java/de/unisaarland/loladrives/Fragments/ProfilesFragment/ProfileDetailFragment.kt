package de.unisaarland.loladrives.Fragments.ProfilesFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import de.unisaarland.loladrives.Constants
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import de.unisaarland.loladrives.profiles.ProfileCommandListAadapter
import de.unisaarland.loladrives.profiles.ProfileParser
import de.unisaarland.loladrives.profiles.RDECommand
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_profile_detail.*
import pcdfEvent.events.obdEvents.OBDCommand
import java.io.File

/**
 * A simple [Fragment] subclass.
 */
class ProfileDetailFragment : Fragment() {
    private val parser = ProfileParser()
    lateinit var path: File
    private lateinit var letDirectory: File
    private lateinit var activity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_detail, container, false)
    }

    override fun onStart() {
        activity = requireActivity() as MainActivity
        activity.title_textview.text = getString(R.string.edit_profile)
        activity.backButton.setImageResource(R.drawable.back_arrow_icon)

        path = requireActivity().getExternalFilesDir(null)!!
        letDirectory = File(path, "profiles")

        profileNameEditText.setText((requireActivity() as MainActivity).editedProfile.first)
        setupCommands()
        super.onStart()
    }

    override fun onResume() {
        path = requireActivity().getExternalFilesDir(null)!!
        letDirectory = File(path, "profiles")

        setupCommands()
        super.onResume()
    }

    private fun setupCommands() {
        val commandNames = mutableListOf<String>()
        OBDCommand.values().subtract(Constants.NOT_TRACKABLE_EVENTS).forEach {
            commandNames.add(it.toString())
        }
        val selectedCommands = mutableMapOf<OBDCommand, Int>()
        (requireActivity() as MainActivity).editedProfile.second.forEach {
            selectedCommands[it.command] = it.update_frequency
        }
        val adapter = ProfileCommandListAadapter(
            requireActivity(),
            commandNames.toTypedArray(),
            selectedCommands,
            this
        )

        commandSelectListView.adapter = adapter
    }

    fun onBackPressed() {
        if (profileNameEditText.text.toString() == "") {
            val toast = Toast.makeText(
                requireActivity().applicationContext,
                "Please enter a profile name",
                Toast.LENGTH_LONG
            )
            toast.show()
        } else {
            val adapter = commandSelectListView.adapter as ProfileCommandListAadapter
            saveEditedProfile(profileNameEditText.text.toString(), adapter.checkedEvents)
        }
    }

    private fun saveEditedProfile(name: String, commands: MutableMap<OBDCommand, Int>) {
        val commandList = mutableListOf<RDECommand>()
        commands.forEach { commandList.add(RDECommand(it.key, it.value)) }

        // delete old profile
        val oldProfile =
            File(letDirectory, (requireActivity() as MainActivity).editedProfile.first + ".json")

        if (oldProfile.exists()) {
            oldProfile.delete()
        }

        // Create new and parse edited profile
        val profileString = parser.generateFromArray(commandList.toTypedArray())
        val newProfile = File(letDirectory, "$name.json")
        newProfile.appendText(profileString)

        activity.editedProfile = Pair(name, commandList.toTypedArray())

        if (!activity.tracking) {
            activity.selectedProfile = activity.editedProfile
        }
    }
}
