package de.unisaarland.loladrives.Fragments.license

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_license.acceptButton
import kotlinx.android.synthetic.main.initial_privacy.*

class InitialPrivacyFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        (requireActivity() as MainActivity).toolBarBar.visibility = View.INVISIBLE
        return inflater.inflate(R.layout.initial_privacy, container, false)
    }

    override fun onStart() {
        val activity = requireActivity() as MainActivity
        initialPrivacyWebView.settings.javaScriptEnabled = true
        initialPrivacyWebView.loadUrl("file:///android_res/raw/privacy.html")

        acceptButton.setOnClickListener {
            activity.changeDonatingAllowed(true)
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, activity.homeFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        }

        rejectButton.setOnClickListener {
            activity.changeDonatingAllowed(false)
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, activity.homeFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        }

        super.onStart()
    }
}
