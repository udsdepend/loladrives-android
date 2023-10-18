package de.unisaarland.loladrives.Fragments.ProfilesFragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import de.unisaarland.loladrives.profiles.ProfileListAdapter
import de.unisaarland.loladrives.profiles.ProfileParser
import de.unisaarland.loladrives.profiles.RDECommand
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_profiles.*
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * A simple [Fragment] subclass.
 */
class ProfilesFragment : Fragment() {
    lateinit var path: File
    private lateinit var letDirectory: File
    private lateinit var activity: MainActivity

    var profiles = mutableMapOf<String, Array<RDECommand>>()

    // validates and parses profiles
    private val parser = ProfileParser()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profiles, container, false)
    }

    override fun onStart() {
        activity = requireActivity() as MainActivity
        activity.title_textview.text = getString(R.string.profiles)
        activity.backButton.setImageResource(R.drawable.back_arrow_icon)

        path = requireActivity().getExternalFilesDir(null)!!
        letDirectory = File(path, "profiles")

        setupProfilesList()

        addProfileButton.setOnClickListener {
            // Create new Empty editable profile and change to detail view
            activity.editedProfile = Pair("", mutableListOf<RDECommand>().toTypedArray())

            activity.supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                (requireActivity() as MainActivity).profileDetialFragment
            ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        }

        super.onStart()
    }

    private fun setupProfilesList() {
        profiles = mutableMapOf()

        val files = letDirectory.listFiles()

        /**
         * validate each file in the profile directory against the profile schema to check wether it is a valid
         * profile, then add it to the available profile map
         */
        files?.forEach {
            val profileString = it.readText()
                .replace("\r", "")
                .replace("\n", "")
            try {
                val profile = parser.parseProfile(profileString)
                if (profile != null && it.name.endsWith(".json")) {
                    profiles[it.name] = profile
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        profileList.adapter = ProfileListAdapter(
            requireActivity(),
            profiles.keys.toTypedArray(),
            activity.selectedProfile.first,
            this
        )

        profileList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            if (activity.tracking) {
                if (activity.rdeOnGoing) {
                    Toast.makeText(
                        context,
                        "You can not change your Profile while a RDE Drive is ongoing",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val dialog = activity.let {
                        AlertDialog.Builder(it).setMessage(
                            "You are trying to change your Monitoring Profile while a " +
                                    "track is ongoing. \nThis will stop your current track and start a new one with the " +
                                    "selected profile."
                        )
                            .setTitle("Track ongoing")
                            .setNegativeButton("Cancel") { _, _ -> }
                            .setPositiveButton("Change Profile") { _, _ ->
                                runBlocking {
                                    activity.stopTracking()
                                }
                                changeProfile(position)
                                runBlocking {
                                    activity.startTracking(false)
                                }
                            }.create()
                    }
                    dialog.show()
                }
            } else {
                changeProfile(position)
            }
        }
    }

    private fun changeProfile(position: Int) {
        val selectedName = profiles.keys.toTypedArray()[position]
        activity.selectedProfile = Pair(selectedName.replace(".json", ""), profiles[selectedName]!!)
        println("Setting selected profile:" + selectedName + " " + profiles[selectedName]!!.joinToString())
        // Remember selected Profile
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
        sharedPref.edit().putString("profile", selectedName).apply()

        // TODO: move this into adapter
        val adapter = ProfileListAdapter(
            requireActivity(),
            profiles.keys.toTypedArray(),
            activity.selectedProfile.first,
            this
        )
        profileList.adapter = adapter
    }
}
