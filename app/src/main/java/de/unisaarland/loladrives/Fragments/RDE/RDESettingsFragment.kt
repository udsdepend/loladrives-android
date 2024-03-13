package de.unisaarland.loladrives.Fragments.RDE

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_r_d_e_settings.*

/**
 * A simple [Fragment] subclass.
 */
class RDESettingsFragment : Fragment() {
    private var distance = 83

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_r_d_e_settings, container, false)
    }

    override fun onStart() {
        super.onStart()
        val activity = requireActivity() as MainActivity
        activity.title_textview.text = getString(R.string.rde_settings)
        activity.backButton.setImageResource(R.drawable.back_arrow_icon)
        textViewDistance.text = "$distance km"

        startImageButton.setOnClickListener {
            activity.checkConnection()
            if (!activity.bluetoothConnected) {
                val toast = Toast.makeText(
                    context,
                    "No Bluetooth OBD-Device Connected",
                    Toast.LENGTH_LONG
                )
                toast.show()
            } else {
                if (!activity.tracking) {
                    activity.rdeFragment.distance = distance.toDouble()

                    activity.supportFragmentManager.beginTransaction().replace(
                        R.id.frame_layout,
                        activity.rdeFragment
                    ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
                } else {
                    val toast = Toast.makeText(
                        context,
                        "Live Monitoring is already ongoing",
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                }
            }
        }

        distanceSeekBar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {
                distance = seekParams.progress
                textViewDistance.text = "$distance km"
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}
            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {}
        }

        textViewRemember.setOnClickListener {
            val helpDialog = MainActivity.InfoDialog("help.html", null)
            helpDialog.show(activity.supportFragmentManager, "")
        }
    }
}
