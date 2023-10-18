package de.unisaarland.loladrives.profiles

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.FragmentTransaction
import de.unisaarland.loladrives.Fragments.ProfilesFragment.ProfilesFragment
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.profiles_list_layout2.view.*

class ProfileListAdapter(
    private val context: Activity,
    private val profileName: Array<String>,
    private val checked: String,
    private val fragment: ProfilesFragment
) : ArrayAdapter<String>(
    context,
    R.layout.profiles_list_layout2,
    profileName
) {

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.profiles_list_layout2, null, true)
        val name = profileName[position].replace(".json", "")
        rowView.profileName.text = name

        rowView.editProfileButton.setOnClickListener {
            (fragment.requireActivity() as MainActivity).editedProfile =
                Pair(name, fragment.profiles["$name.json"]!!)
            fragment.requireActivity().supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                (fragment.requireActivity() as MainActivity).profileDetialFragment
            ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        }

        rowView.profileRadioButton.isChecked = checked == name

        return rowView
    }
}
