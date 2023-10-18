package de.unisaarland.loladrives.Fragments.license

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_license.*

class LicenseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        (requireActivity() as MainActivity).toolBarBar.visibility = View.INVISIBLE
        return inflater.inflate(R.layout.fragment_license, container, false)
    }

    override fun onStart() {
        val activity = requireActivity() as MainActivity
        licenseWebView.loadUrl("file:///android_res/raw/disclaimer.html")

        acceptButton.setOnClickListener {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean("license", true).apply()

            if (activity.donatingAllowed) {
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, activity.homeFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
            } else {
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, InitialPrivacyFragment())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
            }
        }

        super.onStart()
    }
}
