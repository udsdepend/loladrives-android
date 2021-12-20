package de.unisaarland.loladrives.Fragments.license

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.work.WorkManager
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.privacy_dialog.*

class PrivacyFragment : Fragment() {

    private lateinit var activity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        activity = requireActivity() as MainActivity
        return inflater.inflate(R.layout.privacy_dialog, container, false)
    }

    override fun onStart() {
        activity.title_textview.text = getString(R.string.privacy)
        activity.backButton.setImageResource(R.drawable.back_arrow_icon)

        privacyWebView.settings.javaScriptEnabled = true
        privacyWebView.loadUrl("file:///android_res/raw/privacy.html")
        privacySwitch.setOnClickListener {
            activity.changeDonatingAllowed(privacySwitch.isChecked)
            if (activity.donatingAllowed) {
                WorkManager.getInstance(activity).cancelAllWorkByTag("upload")
                activity.initUploadWorker()
            }
        }
        privacySwitch.isChecked = activity.donatingAllowed

        super.onStart()
    }
}
