package de.unisaarland.loladrives

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.work.*
import de.unisaarland.loladrives.Constants.Companion.RECONNECTION_TRIES
import de.unisaarland.loladrives.Fragments.HomeFragment
import de.unisaarland.loladrives.Fragments.ProfilesFragment.ProfileDetailFragment
import de.unisaarland.loladrives.Fragments.ProfilesFragment.ProfilesFragment
import de.unisaarland.loladrives.Fragments.RDE.RDEFragment
import de.unisaarland.loladrives.Fragments.RDE.RDESettingsFragment
import de.unisaarland.loladrives.Fragments.SimpleHTMLContentFragment
import de.unisaarland.loladrives.Fragments.TrackingFragment.TrackingFragment
import de.unisaarland.loladrives.Fragments.historyFragment.HistoryDetailFragment
import de.unisaarland.loladrives.Fragments.historyFragment.HistoryFragment
import de.unisaarland.loladrives.Fragments.license.InitialPrivacyFragment
import de.unisaarland.loladrives.Fragments.license.LicenseFragment
import de.unisaarland.loladrives.Fragments.license.PrivacyFragment
import de.unisaarland.loladrives.Sinks.EventLogger
import de.unisaarland.loladrives.Sources.GPSSource
import de.unisaarland.loladrives.Sources.OBDSource
import de.unisaarland.loladrives.cache.CacheManager
import de.unisaarland.loladrives.events.EventDistributor
import de.unisaarland.loladrives.events.MultiSensorReducer
import de.unisaarland.loladrives.profiles.ProfileParser
import de.unisaarland.loladrives.profiles.RDECommand
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.bluetooth_dialog.view.*
import kotlinx.android.synthetic.main.webview.view.*
import kotlinx.coroutines.*
import pcdfEvent.events.obdEvents.OBDCommand
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    // Used for GPS
    private val mPermissionID = 42
    var askedForPermissionForeground = false
        set(value) {
            field = value
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean(FOREGROUND_LOCATION_KEY, field).apply()
        }
    var askedForPermissionBackground = false
        set(value) {
            field = value
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            sharedPref.edit().putBoolean(BACKGROUND_LOCATION_KEY, field).apply()
        }

    private val showForegroundLocationUI: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                true
            }
        }

    private val showBackgroundLocationUI: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        }

    // Fragments of the whole Application
    var homeFragment: HomeFragment = HomeFragment()
    var rdeSettingsFragment: RDESettingsFragment = RDESettingsFragment()
    var profilesFragment: ProfilesFragment = ProfilesFragment()
    var trackingFragment: TrackingFragment = TrackingFragment()
    var historyFragment: HistoryFragment = HistoryFragment()
    var privacyFragment: PrivacyFragment = PrivacyFragment()
    var rdeFragment: RDEFragment = RDEFragment()
    var profileDetialFragment: ProfileDetailFragment = ProfileDetailFragment()
    var simpleHTMLContentFragment = SimpleHTMLContentFragment()

    // Used for tracking and logging
    var eventDistributor: EventDistributor = EventDistributor()
    var sensorReducer: MultiSensorReducer = MultiSensorReducer(eventDistributor.registerReceiver())
    var tracking = false
    var rdeOnGoing = false
    var currentFile = ""
    private var coreJob: Job? = null
    private var obdSource: Job? = null
    private var gpsSource: GPSSource? = null
    private var persistentLogger: Job? = null
    private var obdProducer: OBDSource? = null

    // Used for Bluetooth Connection
    var mBluetoothSocket: BluetoothSocket? = null
    var bluetoothConnected: Boolean = false
    var mBluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    var mAddress = ""
    var mUUID: UUID = UUID.randomUUID()
    var tryingToReconnect = false

    // OBD Profile
    lateinit var selectedProfile: Pair<String, Array<RDECommand>>
    lateinit var editedProfile: Pair<String, Array<RDECommand>>

    // Donation allowance
    private val privacyPolicyVersion = 6
    private var privacyPolicyVersionAccepted = 0
    val donatingAllowed
        get() = privacyPolicyVersion == privacyPolicyVersionAccepted

    // Target Fragment when Bluetooth not connected
    var targetFragment: Fragment? = null

    // Analysis CacheManager
    lateinit var analysisCacheManager: CacheManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSettings()
        setContentView(R.layout.activity_main)

        // Setup Navigation Drawer Menu
        setSupportActionBar(toolBar)

        // Check whether this is the first start
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        privacyPolicyVersionAccepted = sharedPref.getInt(PRIVACY_VERSION_KEY, 0)
        // donatingAllowed = sharedPref.getBoolean("privacy", false)
        val licenseAccepted = sharedPref.getBoolean("license", false)

        if (!licenseAccepted) {
            supportFragmentManager.beginTransaction().replace(R.id.frame_layout, LicenseFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        } else if (kotlin.math.abs(privacyPolicyVersionAccepted) < privacyPolicyVersion) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, InitialPrivacyFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        } else {
            supportFragmentManager.beginTransaction().replace(R.id.frame_layout, homeFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
        }

        title_textview.text = getString(R.string.titleText)
        title = ""

        setupDirectories()
        setupDefaultProfile()
        initUploadWorker()
        analysisCacheManager = CacheManager(getExternalFilesDir(null), this)

        val bluetooothBroadcastReceiver = BTconnectionBroadcastReceiver(this)
        val bfilter = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        bfilter.addAction(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetooothBroadcastReceiver, bfilter)

        val discoveryBroadcastReceiver = DiscoveryBroadcastReceiver(this)
        val dFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        dFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(discoveryBroadcastReceiver, dFilter)

        connected_textview.setOnClickListener {
            showBluetoothDialog()
        }

        bluetoothDot.setOnClickListener {
            showBluetoothDialog()
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

        // analysisCacheManager.addAllFilesToCache()
    }

    /**
     * Starts an RDE-/ or Monitoring-Track.
     * @param isRDE if the started track is an RDE-Track (determines if GPS should be tracked)
     */
    fun startTracking(isRDE: Boolean) {
        if (!tracking) {
            // Determine next file to log to
            val path = getExternalFilesDir(null)
            val letDirectory = File(path, "tmp")
            letDirectory.mkdirs()

            val filename = getNextFileName()
            val file = File(letDirectory, "$filename.ppcdf")
            currentFile = file.nameWithoutExtension

            // Start the Logger
            val logger = EventLogger(eventDistributor.registerReceiver())

            /* Check for events to be tracked and sent by the OBD-Source (either the current profile  or determined sensor
               profile of the connected car. */
            val trackedEvents = mutableListOf<OBDCommand>()
            selectedProfile.second.forEach {
                trackedEvents.add(it.command)
            }

            // Start GPS-Requesting iff the track is an RDE drive
            if (isRDE) {
                gpsSource = GPSSource(eventDistributor.inputChannel, this)
                gpsSource!!.start()
            }

            // Initialize the OBD Source, perform a supported PIDs check and start the OBD-Communication
            obdProducer = OBDSource(
                mBluetoothSocket!!.inputStream,
                mBluetoothSocket!!.outputStream,
                eventDistributor.inputChannel,
                trackedEvents,
                mUUID
            )

            // Suspend until the OBD-Adapter is set up successfully
            val ready = runBlocking {
                try {
                    // initialize ELM dongle
                    obdProducer?.initELM()

                    // Check whether a supported CAN protocol is used by the car and set ELM327 adapter up to use it
                    if (obdProducer?.initProtocol() != true) {
                        throw IllegalStateException()
                    }

                    // get Vehicle Identification Number (VIN)
                    obdProducer?.getVIN()

                    // if RDE, get FuelType
                    if (isRDE) {
                        obdProducer?.getFuelType()
                    }

                    obdProducer!!.performSupportedPidsCheck()
                    true
                } catch (e: Exception) {
                    stopTracking()
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.obd_comm_err),
                        Toast.LENGTH_LONG
                    ).show()
                    false
                }
            }

            // If something went wrong during initialization, do not start the communication
            if (!ready) {
                runBlocking {
                    stopTracking()
                }
                return
            }

            // Start communication with the OBD-Adapter
            obdSource = GlobalScope.launch {
                obdProducer?.start()
            }

            // Loggers do not have to be cancelled by hand, they terminate when the core terminates
            persistentLogger = GlobalScope.launch {
                logger.startLogging(file, false)
            }

            // Start Core
            coreJob = GlobalScope.launch {
                eventDistributor.start()
            }

            tracking = true
        }
    }

    /**
     * Stops an ongoing RDE-/ or Monitoring-Track.
     * Suspends until all critical sources have terminated ([EventDistributor], [OBDSource]).
     */
    suspend fun stopTracking() {
        if (tracking) {
            // Stop the PCDF-Core.
            coreJob?.cancel()
            coreJob?.join()

            // Stop the OBD-Source and the communication with the OBD-Adapter.
            obdProducer?.stop()
            obdSource?.cancel()
            obdSource?.join()

            // Stop logging and the GPS-Source (if running).
            tracking = false
            rdeOnGoing = false
            persistentLogger?.cancel()
            gpsSource?.stop()

            // Reset distributor
            eventDistributor = EventDistributor()

            // Move current file from temporary files to persistent files.
            moveTmpFiles()
        }
    }

    /**
     * Opens the RDESettingsFragment and enables the user to start an RDE Drive.
     * Checks whether we have the necessary permissions (Location Access Fore-/Background) and a Bluetooth connection is
     * established.
     */
    fun openRDEFragment() {
        // Check for location access permission in foreground
        if (!checkPermissionsForeground()) {
            val dialog = if (!askedForPermissionForeground || showForegroundLocationUI) {
                val foregroundStatement = "foreground_location_statement.html"
                InfoDialog(foregroundStatement, null, true, "Yes", true)
            } else {
                val rationalStatement = "location_rational_statement.html"
                InfoDialog(rationalStatement, null)
            }
            dialog.show(supportFragmentManager, null)
            return
        }

        // Check for location access permission in background
        if (!checkPermissionsBackground()) {
            val dialog = if (!askedForPermissionBackground || showBackgroundLocationUI) {
                val backgroundStatement = "background_location_statement.html"
                InfoDialog(backgroundStatement, null, false, "Yes", true)
            } else {
                val rationalStatement = "location_rational_statement.html"
                InfoDialog(rationalStatement, null)
            }
            dialog.show(supportFragmentManager, null)
            return
        }

        // Check for established Bluetooth connection and open RDE-Settings
        if (bluetoothConnected) {
            if (tracking && rdeOnGoing) {
                supportFragmentManager.beginTransaction().replace(
                    R.id.frame_layout,
                    rdeFragment
                ).setTransition(
                    FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                ).commit()
            } else {
                progressBarBluetooth.visibility = View.VISIBLE
                supportFragmentManager.beginTransaction().replace(
                    R.id.frame_layout,
                    rdeSettingsFragment
                ).setTransition(
                    FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                ).commit()
            }
        } else {
            targetFragment = rdeSettingsFragment
            showBluetoothDialog()
        }
    }

    /**
     * Called when a new Permission is granted.
     * We check whether one of the new permissions is location data and try to open the RDE-Settings again
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (checkPermissionsBackground() || checkPermissionsForeground()) {
            openRDEFragment()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getNextFileName(): String {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR).toString() + "-" +
                if (calendar.get(Calendar.MONTH) + 1 < 10) {
                    "0"
                } else {
                    ""
                } + (calendar.get(Calendar.MONTH) + 1) +
                "-" +
                if (calendar.get(Calendar.DAY_OF_MONTH) < 10) {
                    "0"
                } else {
                    ""
                } +
                (calendar.get(Calendar.DAY_OF_MONTH)) +
                "_" +
                if (calendar.get(Calendar.HOUR_OF_DAY) < 10) {
                    "0"
                } else {
                    ""
                } + calendar.get(Calendar.HOUR_OF_DAY) +
                "-" +
                if (calendar.get(Calendar.MINUTE) < 10) {
                    "0"
                } else {
                    ""
                } + calendar.get(Calendar.MINUTE) +
                "-" +
                if (calendar.get(Calendar.SECOND) < 10) {
                    "0"
                } else {
                    ""
                } + calendar.get(Calendar.SECOND)
    }

    /**
     * Checks whether trip-, tmp- and profiles-Directories are existent and creates them if not.
     * Moves all existent temporary files to the persistent directory.
     * @see moveTmpFiles
     */
    private fun setupDirectories() {
        // Set up file directories
        val path = getExternalFilesDir(null)
        var letDirectory = File(path, "pcdfdata")
        val tmpDirectory = File(path, "tmp")
        letDirectory.mkdirs()
        tmpDirectory.mkdirs()
        letDirectory = File(path, "profiles")
        letDirectory.mkdirs()
        moveTmpFiles()
    }

    private fun moveTmpFiles() {
        if (!tracking && !rdeOnGoing) {
            val path = getExternalFilesDir(null)
            val tmpDirectory = File(path, "tmp")
            val letDirectory = File(path, "pcdfdata")

            progressBarBluetooth.visibility = View.VISIBLE
            tmpDirectory.listFiles()?.forEach {
                val newDirectory = File(letDirectory, it.nameWithoutExtension)
                newDirectory.mkdir()
                val newFile = File(newDirectory, it.name)
                if (!newFile.exists()) {
                    it.copyTo(newFile)
                    it.delete()
                }
            }
            progressBarBluetooth.visibility = View.INVISIBLE
        }
    }

    fun openDetailView(file: File, rde: Boolean) {
        progressBarBluetooth.visibility = View.VISIBLE
        /* Check whether the selected file still exists */
        GlobalScope.launch {
            try {
                val detailFragment = HistoryDetailFragment(file, rde)
                supportFragmentManager.beginTransaction().replace(
                    R.id.frame_layout,
                    detailFragment
                ).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
            } catch (e: Exception) {
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Could not read the selected file",
                        Toast.LENGTH_LONG
                    ).show()
                }
                progressBarBluetooth.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * Checks whether the Default Profile and the All Supported Profile are existent and creates them if not.
     * Auto-Selects the selected profile from when the application was last running.
     * Profiles can be changed in the [ProfilesFragment].
     */
    fun setupDefaultProfile() {
        val path = getExternalFilesDir(null)
        val letDirectory = File(path, "profiles")
        val profileParser = ProfileParser()
        val defaultProfile = File(letDirectory, "default_profile.json")
        val allProfile = File(letDirectory, "all_supported.json")

        defaultProfile.delete()
        if (!defaultProfile.exists()) {
            defaultProfile.appendText(
                profileParser.generateFromArray(
                    arrayOf(
                        RDECommand(OBDCommand.SPEED, 0),
                        RDECommand(OBDCommand.RPM, 0),
                        RDECommand(OBDCommand.COMMANDED_EGR, 0),
                        RDECommand(OBDCommand.EGR_ERROR, 0),
                        RDECommand(OBDCommand.ENGINE_EXHAUST_FLOW_RATE, 0),
                        RDECommand(OBDCommand.MAF_AIR_FLOW_RATE, 0),
                        RDECommand(OBDCommand.MAF_AIR_FLOW_RATE_SENSOR, 0),
                        RDECommand(OBDCommand.ENGINE_FUEL_RATE, 0),
                        RDECommand(OBDCommand.ENGINE_FUEL_RATE_MULTI, 0),
                        RDECommand(OBDCommand.AMBIENT_AIR_TEMPERATURE, 0),
                        RDECommand(OBDCommand.FUEL_TYPE, 0),
                        RDECommand(OBDCommand.NOX_SENSOR, 0),
                        RDECommand(OBDCommand.NOX_SENSOR_CORRECTED, 0),
                        RDECommand(OBDCommand.NOX_SENSOR_ALTERNATIVE, 0),
                        RDECommand(OBDCommand.NOX_SENSOR_CORRECTED_ALTERNATIVE, 0)
                    )
                )
            )
        }

        if (!allProfile.exists()) {
            allProfile.appendText(
                profileParser.generateFromArray(
                    OBDCommand.values().subtract(Constants.NOT_TRACKABLE_EVENTS).map {
                        RDECommand(it, 0)
                    }.toTypedArray()
                )
            )
        }
        /*
         Get the remembered selected profile from the Shared Preferences, if this one does not exist anymore, select the
         default profile.
        */
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val savedProfile =
            File(letDirectory, sharedPref.getString("profile", "default_profile.json")!!)

        selectedProfile = if (savedProfile.exists()) {
            Pair(
                savedProfile.nameWithoutExtension,
                profileParser.parseProfile(savedProfile.readText()) ?: arrayOf()
            )
        } else {
            Pair(
                "default_profile",
                profileParser.parseProfile(defaultProfile.readText()) ?: arrayOf()
            )
        }
    }

    /**
     * Initializes the Upload-[WorkRequest], which periodically uploads the stored trips to the CDP-Server.
     * @see Uploader
     */
    fun initUploadWorker() {
        WorkManager.getInstance(this).cancelAllWorkByTag("upload")
        if (!donatingAllowed) {
            return
        }

        val path = getExternalFilesDir(null)
        val letDirectory = File(path, "pcdfdata")
        letDirectory.mkdirs()

        // The data is only uploaded if a WiFi-Connection is present
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()

        // The data is uploaded once an hour
        val uploadWorkRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<Uploader>(1, TimeUnit.HOURS)
                .setInputData(
                    workDataOf(
                        "directory" to letDirectory.absolutePath,
                        "privacyPolicyVersion" to privacyPolicyVersionAccepted
                    )
                )
                .setConstraints(constraints)
                .addTag("upload")
                .build()

        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork(
                "upload",
                ExistingPeriodicWorkPolicy.REPLACE,
                uploadWorkRequest
            )
    }

    /**
     * Fetches privacy settings from the Shared Preferences and initializes the accepted policy version.
     * [Uploader] is only allowed to upload files if the current policy version is accepted.
     */
    private fun initSettings() {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        privacyPolicyVersionAccepted = sharedPref.getInt(PRIVACY_VERSION_KEY, 0)
        askedForPermissionForeground = sharedPref.getBoolean(FOREGROUND_LOCATION_KEY, false)
        askedForPermissionBackground = sharedPref.getBoolean(BACKGROUND_LOCATION_KEY, false)
    }

    /**
     * Toggles whether the [Uploader] is allowed to upload data from history.
     * Called whenever the user wants to change her privacy settings.
     * @param allowed whether data donation is allowed.
     *
     * @see PrivacyFragment
     */
    fun changeDonatingAllowed(allowed: Boolean) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        privacyPolicyVersionAccepted = if (allowed) privacyPolicyVersion else -privacyPolicyVersion
        sharedPref.edit().putInt(PRIVACY_VERSION_KEY, privacyPolicyVersionAccepted).apply()
        if (!donatingAllowed) {
            WorkManager.getInstance(this).cancelAllWorkByTag("upload")
            println("canceled all upload workers with old privacy settings")
        } else {
            initUploadWorker()
        }
        println("Updated privacy policy to version: $privacyPolicyVersionAccepted")
    }

    /**
     * Bluetooth ConnectionReceiver, notified whenever the Bluetooth connection is lost.
     * Starts new discovery and calls Bluetooth disconnection handling to reconnect to the ELM-Adapter.
     * @see handleBluetoothDisconnection
     */
    class BTconnectionBroadcastReceiver(val activity: MainActivity) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    /* Check if the disconnected bluetooth device is our obd device */
                    val device: BluetoothDevice =
                        intent.extras!!.get(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice

                    if (device.address != activity.mBluetoothSocket?.remoteDevice?.address) {
                        /* he disconnect does not affect our tracking */
                        return
                    }

                    if (activity.mAddress == "") {
                        return
                    }
                    /*
                    We first mark the connection as disconnected, then start to observe how long we are
                    disconnected and try to reconnect
                    */
                    activity.bluetoothConnected = false
                    activity.checkConnection()

                    if (activity.rdeOnGoing) {
                        /* Pause RDE */
                        activity.rdeFragment.pause()
                    }

                    Toast.makeText(
                        context,
                        "Bluetooth Connection interrupted, automatically trying to reconnect",
                        Toast.LENGTH_LONG
                    ).show()

                    activity.mBluetoothAdapter.enable()
                    activity.mBluetoothAdapter.startDiscovery()
                    activity.handleBluetoothDisconnection()
                }
            }
        }
    }

    /**
     * Bluetooth disconnection handling, called whenever the Bluetooth connection to the ELM-Adapter is lost.
     * Tries to reconnect to the adapter.
     * If reconnection attempt is successful, current Monitoring or RDE trips are continued.
     * Otherwise the user is informed via [AlertDialog] and able to stop the current trip right away.
     */
    private fun handleBluetoothDisconnection() {
        /* Try to reconnect to last connected device in a 10 seconds interval */
        tryingToReconnect = true
        val reconnectionJob = GlobalScope.launch(Dispatchers.Main) {
            repeat(RECONNECTION_TRIES) {
                if (!bluetoothConnected) {
                    ConnectToDevice(this@MainActivity, this@MainActivity).execute()
                    delay(10000)
                }
            }
        }

        GlobalScope.launch(Dispatchers.Main) {
            reconnectionJob.join()
            if (bluetoothConnected) {
                /* Reconnection Success */
                if (rdeOnGoing) {
                    rdeFragment.resume()
                }
            } else {
                /* Reconnection Failure */
                if (tracking) {
                    val builder = AlertDialog.Builder(this@MainActivity)
                        .setTitle("Bluetooth Disconnected")
                        .setNegativeButton("Try again") { _, _ -> handleBluetoothDisconnection() }
                        .setMessage(
                            "LolaDrives can not find your previously connected OBD-Bluetooth Adapter. " +
                                    "You may try restarting your vehicle or unplugging and replugging the Bluetooth " +
                                    "adapter and try again. \n\nThe ongoing session will " +
                                    "automatically continue once the connection is restored."
                        )
                    if (rdeOnGoing) {
                        /* Currently performing an RDE-Drive */
                        builder.setPositiveButton("Stop RDE") { _, _ ->
                            rdeFragment.stop()
                        }
                    } else {
                        builder.setPositiveButton("Stop Monitoring") { _, _ ->
                            if (trackingFragment.isVisible) {
                                trackingFragment.stopTracking()
                            } else {
                                runBlocking { stopTracking() }
                            }
                        }
                    }
                    builder.create().show()
                }
            }
            tryingToReconnect = false
        }
    }

    /**
     * Bluetooth BroadcastReceiver, tries to automatically connect to the OBD-Bluetooth Adapter.
     * Notified whenever a new bonded Bluetooth device is discovered.
     * @param activity The current [MainActivity].
     */
    class DiscoveryBroadcastReceiver(val activity: MainActivity) : BroadcastReceiver() {
        @SuppressLint("HardwareIds")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    /* Device found, check whether it's the device we lost connection to and reconnect */
                    val device: BluetoothDevice =
                        intent.extras!!.get(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice

                    /* If we are already connected, we can ignore the findings */
                    if (activity.bluetoothConnected) {
                        activity.checkConnection()
                        return
                    }
                    if (activity.mBluetoothAdapter.address == device.address) {
                        ConnectToDevice(activity, activity).execute()
                        activity.checkConnection()
                    }
                }
            }
        }
    }

    /**
     * Checks whether there is a Bluetooth device connected.
     * Changes the ELM-Connection-Logo accordingly.
     */
    fun checkConnection() {
        if (bluetoothConnected) {
            bluetoothDot.setImageResource(R.drawable.elm_logo_green)
            connected_textview.text = resources.getString(R.string.connected)
            connected_textview.setTextColor(ContextCompat.getColor(this, R.color.elmGreen))
        } else {
            bluetoothDot.setImageResource(R.drawable.elm_logo_red)
            connected_textview.text = resources.getString(R.string.not_connected)
            connected_textview.setTextColor(ContextCompat.getColor(this, R.color.elmRed))
        }
    }

    /**
     * Opens the Selection-Dialog for bonded Bluetooth devices.
     * @see BluetoothDialog
     */
    fun showBluetoothDialog() {
        // add all already paired devices
        val mPairedDevices = mBluetoothAdapter.bondedDevices
        val deviceList: ArrayList<BluetoothDevice> = ArrayList()
        if (mPairedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in mPairedDevices) {
                deviceList.add(device)
                Log.i("device:", "" + device.name)
            }
        } else {
            val toast = Toast.makeText(
                applicationContext,
                "No paired devices found",
                Toast.LENGTH_LONG
            )
            toast.show()
        }

        val bluetoothDialog = BluetoothDialog(deviceList, this)
        bluetoothDialog.show(supportFragmentManager, "")
    }

    /**
     * Bluetooth disconnection.
     * Called to manually disconnect the Bluetooth device from the [BluetoothDialog].
     */
    fun disconnectBT() {
        mBluetoothSocket?.close()
        bluetoothConnected = false
        mAddress = ""
        checkConnection()
    }

    /**
     * Dialog Class which shows a Selection-[Dialog] for bonded Bluetooth devices.
     * @property deviceList List of bonded Bluetooth devices
     * @property activity The running MainActivity
     */
    class BluetoothDialog(
        private val deviceList: ArrayList<BluetoothDevice>,
        val activity: MainActivity
    ) :
        DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            /* Inflate the Selection-ListView with the list of bonded devices. */
            val namesList = deviceList.map { d ->
                if (activity.mAddress == d.address && activity.bluetoothConnected) {
                    d.name + "  ✓"
                } else {
                    d.name
                }
            }
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.bluetooth_dialog, null)
            view.listViewBluetooth.adapter =
                ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, namesList)
            view.listViewBluetooth.invalidateViews()
            view.listViewBluetooth.setOnItemClickListener { _, _, position, _ ->
                val device: BluetoothDevice = deviceList[position]
                val address: String = device.address
                activity.mAddress = address
                /* Start Connection attempt to the selected device. */
                ConnectToDevice(requireActivity().applicationContext, activity).execute()
                dismiss()
            }

            /*
            If the Long-Clicked Item is the currently connected Bluetooth device, we offer the user to disconnect
            the device.
            */
            view.listViewBluetooth.setOnItemLongClickListener { _, _, position, _ ->
                val device = deviceList[position]
                if (activity.mAddress == device.address) {
                    val subBuilder = AlertDialog.Builder(activity)
                    subBuilder.setMessage(
                        "Do you really want to disconnect your OBD Bluetooth Adapter? " +
                                "\n \nThis might affect your RDE Trip."
                    )
                        .setPositiveButton("Disconnect") { _, _ ->
                            activity.disconnectBT()
                            dismiss()
                        }.setNegativeButton("Cancel") { _, _ -> }
                    subBuilder.create().show()
                }
                true
            }
            return activity.let {
                val builder = AlertDialog.Builder(it)
                builder.setTitle("Select your Bluetooth device.")
                    .setNegativeButton("Cancel") { _, _ -> }
                    .setView(view)
                builder.create()
            }
        }
    }

    /**
     * [AsyncTask] Class which connects to selected Bluetooth device from device list.
     * Runs in background.
     * @property activity The current [MainActivity]
     */
    class ConnectToDevice(c: Context, val activity: MainActivity) :
        AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context = c
        override fun doInBackground(vararg params: Void?): String? {
            try {
                if (activity.mBluetoothSocket == null || !activity.bluetoothConnected) {
                    activity.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice =
                        activity.mBluetoothAdapter.getRemoteDevice(activity.mAddress)
                    activity.mUUID = device.uuids[0].uuid
                    activity.mBluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(
                        activity.mUUID
                    )
                    // stop looking for other devices , safes battery
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    activity.mBluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPreExecute() {
            if (!activity.tryingToReconnect || activity.tracking) {
                GlobalScope.launch(Dispatchers.Main) {
                    activity.progressBarBluetooth.visibility = View.VISIBLE
                }
            }
            super.onPreExecute()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            GlobalScope.launch(Dispatchers.Main) {
                if (!connectSuccess) {
                    if (!activity.tryingToReconnect || activity.tracking) {
                        val toast =
                            Toast.makeText(
                                context,
                                "Could not connect to device",
                                Toast.LENGTH_LONG
                            )
                        toast.show()
                    }
                    if (!activity.tracking && !activity.tryingToReconnect) {
                        activity.showBluetoothDialog()
                    }
                } else {
                    activity.bluetoothConnected = true
                    activity.checkConnection()

                    /* Update the OBD-Source if a track is ongoing */
                    if (activity.tracking) {
                        activity.obdProducer?.changeIOStreams(
                            activity.mBluetoothSocket!!.inputStream,
                            activity
                                .mBluetoothSocket!!
                                .outputStream
                        )
                        activity.obdProducer?.initELM()
                    } else {
                        if (activity.targetFragment != null) {
                            activity.supportFragmentManager.beginTransaction().replace(
                                R.id.frame_layout,
                                activity.targetFragment!!
                            ).setTransition(
                                FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                            ).commit()
                            activity.targetFragment = null
                        }
                    }
                }
                activity.progressBarBluetooth.visibility = View.INVISIBLE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                if (mBluetoothAdapter.isEnabled) {
                    val toast = Toast.makeText(
                        applicationContext,
                        "Bluetooth has been enabled",
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                } else {
                    val toast = Toast.makeText(
                        applicationContext,
                        "Bluetooth has been disabled",
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // bluetooth enabling has been canceled
                val toast = Toast.makeText(
                    applicationContext,
                    "Bluetooth enabling has been canceled",
                    Toast.LENGTH_LONG
                )
                toast.show()
            }
        }
    }

    /**
     * Checks for permissions to access location data when App is in foreground.
     * @return If foreground location access permission is granted.
     */
    fun checkPermissionsForeground(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    /**
     * Checks for permissions to access location data when App is in background.
     * @return If background location access permission is granted.
     */
    private fun checkPermissionsBackground(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager
                .PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Requests permissions to access location data when App is in foreground.
     */
    private fun requestPermissionsForeground() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            mPermissionID
        )
    }

    /**
     * Requests permissions to access location data when App is in background.
     */
    private fun requestPermissionsBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                mPermissionID
            )
        }
    }

    /**
     * Generic dialog for presenting information from a HTML file to the user.
     * Can be either used to show a HTML file or to show a HTML page represented as a string and replace given placeholders.
     * Used to ask for location permissions.
     * @param html HTML file to show (if replace = null) or HTML String to show (if replace != null).
     * @param replace Map of placeholders and replacements to be replaced in the HTML string.
     * @param foreground Whether we ask for foreground or background location.
     * @param acceptButtonText Text for the accept button in the lower right corner.
     * @param laterButton If a "Later"-Button should be shown.
     *
     * TODO: To overloaded, split this into different generic dialogs.
     */
    class InfoDialog(
        val html: String,
        val replace: Map<String, String>? = null,
        private val foreground: Boolean? = null,
        private val acceptButtonText: String? = null,
        private val laterButton: Boolean? = null
    ) : DialogFragment
        () {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val inflater = requireActivity().layoutInflater
            val helpView = inflater.inflate(R.layout.webview, null)

            if (replace == null) {
                helpView.infoWebView.loadUrl("file:///android_res/raw/$html")
            } else {
                var unencodedHtml = html
                for (pair in replace) {
                    unencodedHtml = unencodedHtml.replace(pair.key, pair.value)
                }
                // val encodedHtml = Base64.encodeToString(unencodedHtml.toByteArray(), Base64.NO_PADDING)
                helpView.infoWebView.loadDataWithBaseURL(
                    "file:///android_res/raw/",
                    unencodedHtml,
                    "text/html",
                    "utf8",
                    null
                )
            }

            return activity?.let {
                val builder = AlertDialog.Builder(it)
                builder.setPositiveButton(acceptButtonText ?: "Okay") { _, _ ->
                    if (foreground == true) {
                        (activity as MainActivity).requestPermissionsForeground()
                        (activity as MainActivity).askedForPermissionForeground = true
                    } else if (foreground == false) {
                        (activity as MainActivity).requestPermissionsBackground()
                        (activity as MainActivity).askedForPermissionBackground = true
                    }
                }.setView(helpView)
                if (laterButton == true) {
                    builder.setNegativeButton("Later") { _, _ -> ; }
                }
                builder.create()
            } ?: throw IllegalStateException("Activity can not be null")
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.fragments.any { it is LicenseFragment || it is InitialPrivacyFragment }) {
            return
        }
        var fragment: Fragment? = null
        for (f in supportFragmentManager.fragments) {
            if (f.isVisible) {
                fragment = f
            }
        }
        val nextFragment: Fragment =
                when (fragment) {
                    is ProfileDetailFragment -> {
                        profileDetialFragment.onBackPressed()
                        profilesFragment
                    }
                    is RDESettingsFragment -> {
                        println("Back pressed in rde settings, stop gps source")
                        gpsSource?.stop()
                        homeFragment
                    }
                    is HistoryDetailFragment -> {
                        historyFragment
                    }
                    else -> {
                        homeFragment
                    }
                }
        supportFragmentManager.beginTransaction().replace(R.id.frame_layout, nextFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit()
    }

    companion object {
        const val PRIVACY_VERSION_KEY = "privacyVersion"
        const val FOREGROUND_LOCATION_KEY = "foregroundLocation"
        const val BACKGROUND_LOCATION_KEY = "backgroundLocation"
    }
}
