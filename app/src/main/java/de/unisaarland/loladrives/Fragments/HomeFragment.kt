package de.unisaarland.loladrives.Fragments

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import de.unisaarland.loladrives.R.layout
import de.unisaarland.loladrives.Sinks.RDEHomeUpdater
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * The inital Fragment
 */
class HomeFragment : Fragment() {
    private lateinit var rdeTimeUpdater: RDEHomeUpdater

    companion object {
        lateinit var mActivity: MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mActivity = (requireActivity() as MainActivity)

        // check if bluetooth is enabled
        if (!mActivity.mBluetoothAdapter.isEnabled) {
            // if not, intent to enable it
            val enableBleutoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBleutoothIntent, 1)
        }
        return inflater.inflate(layout.fragment_home, container, false)
    }

    @ExperimentalCoroutinesApi
    override fun onStart() {
        super.onStart()
        mActivity.title_textview.text = getString(R.string.titleText)
        mActivity.checkConnection()
        initMenuButtons()
    }

    @ExperimentalCoroutinesApi
    override fun onResume() {
        mActivity.checkConnection()
        if ((requireActivity() as MainActivity).rdeOnGoing) {
            startUiUpdating()
        }
        super.onResume()
    }

    override fun onPause() {
        mActivity.checkConnection()
        stopUiUpdating()
        super.onPause()
    }

    override fun onStop() {
        mActivity.checkConnection()
        stopUiUpdating()
        super.onStop()
    }

    @SuppressLint("ResourceAsColor")
    fun showBluetoothDialog() {
        (requireActivity() as MainActivity).showBluetoothDialog()
    }

    @ExperimentalCoroutinesApi
    private fun initMenuButtons() {
        rdeImageButton.setOnClickListener {
            mActivity.openRDEFragment()
        }

        monitoringImageButton.setOnClickListener {
            if (mActivity.bluetoothConnected) {
                mActivity.supportFragmentManager.beginTransaction().replace(
                    R.id.frame_layout,
                    mActivity.trackingFragment
                ).setTransition(
                    FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                ).commit()
            } else {
                mActivity.targetFragment = mActivity.trackingFragment
                showBluetoothDialog()
            }
        }

        profilesImageButton.setOnClickListener {
            mActivity.supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                mActivity.profilesFragment
            ).setTransition(
                FragmentTransaction.TRANSIT_FRAGMENT_OPEN
            ).commit()
        }

        historyImageButton.setOnClickListener {
            mActivity.supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                mActivity.historyFragment
            ).setTransition(
                FragmentTransaction.TRANSIT_FRAGMENT_OPEN
            ).commit()
        }

        privacyImageButton.setOnClickListener {
            mActivity.supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                mActivity.privacyFragment
            ).setTransition(
                FragmentTransaction.TRANSIT_FRAGMENT_OPEN
            ).commit()
        }

        helpImageButton.setOnClickListener {
            val htmlFragment = mActivity.simpleHTMLContentFragment
            htmlFragment.url = "file:///android_res/raw/help.html"
            htmlFragment.title = "Help"
            mActivity.supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                htmlFragment
            ).setTransition(
                FragmentTransaction.TRANSIT_FRAGMENT_OPEN
            ).commit()
        }

        textViewButtonAck.setOnClickListener {
            val htmlFragment = mActivity.simpleHTMLContentFragment
            htmlFragment.url = "file:///android_res/raw/acknowledgements.html"
            htmlFragment.title = "Acknowledgements"
            mActivity.supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                htmlFragment
            ).setTransition(
                FragmentTransaction.TRANSIT_FRAGMENT_OPEN
            ).commit()
        }

        textViewButtonImpressum.setOnClickListener {
            val htmlFragment = mActivity.simpleHTMLContentFragment
            htmlFragment.url = "file:///android_res/raw/impressum.html"
            htmlFragment.title = "Impressum"
            mActivity.supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                htmlFragment
            ).setTransition(
                FragmentTransaction.TRANSIT_FRAGMENT_OPEN
            ).commit()
            /* Car Overview Fragment
            val carFrag = CarsFragment()
            mActivity.supportFragmentManager.beginTransaction().replace(
                R.id.frame_layout,
                carFrag
            ).setTransition(
                FragmentTransaction.TRANSIT_FRAGMENT_OPEN
            ).commit()
             */
        }

        if ((requireActivity() as MainActivity).rdeOnGoing) {
            redDot.visibility = View.VISIBLE
            homeTotalRDETime.visibility = View.VISIBLE
            startUiUpdating()
        } else {
            redDot.visibility = View.INVISIBLE
            homeTotalRDETime.visibility = View.INVISIBLE
        }
        mActivity.backButton.setImageResource(R.drawable.home_icon)
        mActivity.toolBarBar.visibility = View.VISIBLE
    }

    @ExperimentalCoroutinesApi
    private fun startUiUpdating() {
        rdeTimeUpdater = RDEHomeUpdater(
            (requireActivity() as MainActivity).rdeFragment.rdeValidator
                .outputChannel.openSubscription(),
            this
        )
        rdeTimeUpdater.start()
    }

    private fun stopUiUpdating() {
        if ((requireActivity() as MainActivity).rdeOnGoing) {
            rdeTimeUpdater.stop()
        }
    }
}
