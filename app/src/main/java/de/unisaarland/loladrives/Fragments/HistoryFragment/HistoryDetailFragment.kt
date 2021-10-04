package de.unisaarland.loladrives.Fragments.historyFragment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.tabs.TabLayout
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import de.unisaarland.pcdfanalyser.model.ParameterSupport
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_history_detail.*
import kotlinx.android.synthetic.main.histroy_event_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.rdeapp.pcdftester.Sinks.RDEUIUpdater
import org.rdeapp.pcdftester.Sinks.RDEValidator
import pcdfEvent.EventType
import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.OBDEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.SpeedEvent
import pcdfPattern.PCDFPattern
import pcdfPattern.PatternParser
import serialization.Serializer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HistoryDetailFragment(val file: File,  val rde: Boolean) :
    Fragment(), TabLayout.OnTabSelectedListener {
    private lateinit var activity: MainActivity

    private var rdeResults: DoubleArray? = null
    private var eventsTab: TabLayout.Tab? = null
    private var summaryTab: TabLayout.Tab? = null
    private var rdeAnalysisTab: TabLayout.Tab? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = requireActivity() as MainActivity
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history_detail, container, false)
    }

    override fun onStart() {
        tabLayout.addOnTabSelectedListener(this)
        eventsTab = tabLayout.getTabAt(0)
        summaryTab = tabLayout.getTabAt(1)
        rdeAnalysisTab = tabLayout.getTabAt(2)

        activity.progressBarBluetooth.visibility = View.INVISIBLE
        activity.title_textview.text = file.nameWithoutExtension

        setupAdapter(file)
        setupChart(file)
        //setupSummary()
        runBlocking {
            setupRDEAnalysis()
        }
        initRDEViews()
        initRDEHelpOnClick()
        GlobalScope.launch(Dispatchers.Main) {
            activity.progressBarBluetooth.visibility = View.INVISIBLE
        }

        if (rde) {
            tabLayout.selectTab(rdeAnalysisTab)
            showRDE()
        }

        super.onStart()
    }

    private fun setupAdapter(file: File) {
        val reader = file.bufferedReader()
        val events = reader.lineSequence().take(10_000).map {
            PCDFEvent.fromString(it).toIntermediate()
        }.toMutableList()
        val adapter = HistoryEventListAdapter(requireActivity(), events)
        eventDetailListView.adapter = adapter
        reader.close()

    }

    private fun setupSummary() {
        val cacheManager = activity.analysisCacheManager

        // Set up VIN
        summaryVINTextView.text = cacheManager.vinCache.cachedAnalysisResultForFile(file) ?: "No VIN found"

        //Set up supported/available PIDs
        val paramSupp = cacheManager.supportedPIDsCache.cachedAnalysisResultForFile(file) ?: ParameterSupport()
        var available = 0
        var supported = 0
        for (record in paramSupp.parameterRecords) {
            if (record.available) {
                available++
            }
            if (record.supported) {
                supported++
            }
        }
        summaryPIDTextView.text = "$available / $supported"
        summaryPIDTextView.setOnClickListener {

        }
    }

    private fun setupChart(file: File) {
        val iter = file.bufferedReader().lineSequence().map {
            PCDFEvent.fromString(it).toIntermediate()
        }.iterator()
        if (!iter.hasNext()) return
        val chart = chartView
        val vDataSet = ArrayList<Entry>()

        chart.description.isEnabled = true
        chart.description.text = "Time [min] / v [km/h]"
        val first = iter.next().timestamp
        chart.xAxis.labelCount = 10
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM

        while (iter.hasNext()) {
            val event = iter.next()
            if (event.type == EventType.OBD_RESPONSE) {
                try {
                    if (event is SpeedEvent) {
                        vDataSet.add(
                            Entry(
                                (event.timestamp - first).toFloat() / 60_000_000_000.0.toFloat(),
                                event.speed.toFloat()
                            )
                        )
                    }
                } catch (e: Exception) {
                    continue
                }
            }
        }

        // Check if there was Speed Data
//        chartSwitch.visibility = if (vDataSet.isEmpty()) { View.INVISIBLE } else { View.VISIBLE }
//        switchTextView.visibility = chartSwitch.visibility
        if (vDataSet.isEmpty() && summaryTab != null) {
            tabLayout.removeTab(summaryTab!!)
        }
//        guidelineListTop.setGuidelinePercent(if (vDataSet.isEmpty()) { 0.15f } else { 0.2f })

        val vLineSet = LineDataSet(vDataSet, "v")
        vLineSet.axisDependency = YAxis.AxisDependency.LEFT
        vLineSet.setDrawCircles(false)

        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(vLineSet)

        val data = LineData(dataSets)

        chart.data = data
        chart.invalidate()
    }

    private fun setupRDEAnalysis() {
        val rdeValidator = RDEValidator(null, activity)

        // activity.progressBarBluetooth.visibility = View.VISIBLE
        try {
            val iter = file.bufferedReader().lineSequence().map {
                PCDFEvent.fromString(it).toIntermediate()
            }.iterator()
            rdeResults = rdeValidator.monitorOffline(iter)
        } catch (e: Exception) {
            if (rdeAnalysisTab != null) {
                tabLayout.removeTab(rdeAnalysisTab!!)
            }
        }
    }

    private fun initRDEViews() {
        // rdeView.nameTextView.text = name
        val results = if (rdeResults != null) rdeResults!! else return

        if (results.isEmpty()) {
            if (rdeAnalysisTab != null) {
                tabLayout.removeTab(rdeAnalysisTab!!)
            }
            return
        }

        // Validity
        when {
            results[17] == 1.0 -> {
                validRDEImageView.setImageResource(R.drawable.bt_connected)
            }
            results[18] == 1.0 -> {
                validRDEImageView.setImageResource(R.drawable.bt_not_connected)
            }
        }
        // Total Values
        totalDurationTextView.text = RDEUIUpdater.convertSeconds(
            results[4].toLong() + results[5].toLong() + results[6].toLong()
        )
        totalDistanceTextView.text = RDEUIUpdater.convertMeters(results[0].toLong())

        // NOx per Kilometer
        noxTextView.text = RDEUIUpdater.convert(results[16] * 1000.0, "mg/km")

        // Durations
        urbanDurationTextView.text = RDEUIUpdater.convertSeconds(results[4].toLong())
        ruralDurationTextView.text = RDEUIUpdater.convertSeconds(results[5].toLong())
        motorwayDurationTextView.text = RDEUIUpdater.convertSeconds(results[6].toLong())

        // Distances
        urbanDistanceTextView.text = RDEUIUpdater.convertMeters(results[1].toLong())
        ruralDistanceTextView.text = RDEUIUpdater.convertMeters(results[2].toLong())
        motorwayDistanceTextView.text = RDEUIUpdater.convertMeters(results[3].toLong())

        // Average Velocities
        urbanVelocityTextView.text = RDEUIUpdater.convert(results[7], "km/h")
        ruralVelocityTextView.text = RDEUIUpdater.convert(results[8], "km/h")
        motorwayVelocityTextView.text = RDEUIUpdater.convert(results[9], "km/h")

        // High Dynamic Boundary Conditions
        urbanVATextView.text = RDEUIUpdater.convert(results[10], "m²/s³")
        ruralVATextView.text = RDEUIUpdater.convert(results[11], "m²/s³")
        motorwayVATextView.text = RDEUIUpdater.convert(results[12], "m²/s³")

        // Low Dynamic Boundary Conditions
        urbanRPATextView.text = RDEUIUpdater.convert(results[13], "m/s²")
        println("Urban RPA: " + results[13])
        println(
            "Limit RPA: " + if (results[7] < 94.05) {
                if (results[7] == 0.0) {
                    0.0
                } else {
                    -0.0016 * results[7] + 0.1755
                }
            } else {
                0.025
            }
        )
        println("Avg Speed: " + results[7])
        ruralRPATextView.text = RDEUIUpdater.convert(results[14], "m/s²")
        motorwayRPATextView.text = RDEUIUpdater.convert(results[15], "m/s²")
    }

    // TODO: replace calculations through RTLola-Output-Streams (no double calculation needed)
    private fun initRDEHelpOnClick() {
        // Total Views
        totalDurationHelpImageButton.setOnClickListener {
            showHelp(R.raw.minihelp_duration_total, mapOf())
        }
        validRDEHelpImageButton.setOnClickListener {
            showHelp(R.raw.minihelp_validity, mapOf())
        }
        noxHelpImageButton.setOnClickListener {
            showHelp(R.raw.minihelp_nox, mapOf())
        }

        // Urban
        urbanDistanceHelpImageButton.setOnClickListener {
            val replace = when {
                (rdeResults?.get(0) ?: 0.0) * 0.29 / 1000.0 < 16.0 && (rdeResults?.get(0) ?: 0.0) * 0.44 / 1000.0 < 16.0 -> {
                    mapOf(
                        "between <b>PLACEHOLDER1</b> and <b>PLACEHOLDER2</b>" to "greater than <b>16km</b>"
                    )
                }
                (rdeResults?.get(0) ?: 0.0) * 0.29 / 1000.0 < 16.0 -> {
                    mapOf(
                        "PLACEHOLDER1" to RDEUIUpdater.convertMeters(16000),
                        "PLACEHOLDER2" to RDEUIUpdater.convertMeters(((rdeResults?.get(0) ?: 0.0) * 0.44).toLong())
                    )
                }
                else -> {
                    mapOf(
                        "PLACEHOLDER1" to RDEUIUpdater.convertMeters(((rdeResults?.get(0) ?: 0.0) * 0.29).toLong()),
                        "PLACEHOLDER2" to RDEUIUpdater.convertMeters(((rdeResults?.get(0) ?: 0.0) * 0.44).toLong())
                    )
                }
            }
            showHelp(R.raw.minihelp_distance_urban, replace)
        }
        urbanVAHelpImageButton.setOnClickListener {
            showHelp(
                R.raw.minihelp_dynamics_high,
                mapOf(
                    "SEGMENT" to "Urban",
                    "LIMIT" to calculateHighDynamics(rdeResults?.get(7) ?: 0.0)
                )
            )
        }
        urbanRPAHelpImageButton.setOnClickListener {
            showHelp(
                R.raw.minihelp_dynamics_low,
                mapOf(
                    "SEGMENT" to "Urban",
                    "LIMIT" to calculateLowDynamics(rdeResults?.get(7) ?: 0.0)
                )
            )
        }

        // Rural
        ruralDistanceHelpImageButton.setOnClickListener {
            val replace = when {
                (rdeResults?.get(0) ?: 0.0) * 0.23 / 1000.0 < 16.0 && (rdeResults?.get(0) ?: 0.0) * 0.43 / 1000.0 < 16.0 -> {
                    mapOf(
                        "between <b>PLACEHOLDER1</b> and <b>PLACEHOLDER2</b>" to "greater than <b>16km</b>"
                    )
                }
                (rdeResults?.get(0) ?: 0.0) * 0.23 / 1000.0 < 16.0 -> {
                    mapOf(
                        "PLACEHOLDER1" to RDEUIUpdater.convertMeters(16000),
                        "PLACEHOLDER2" to RDEUIUpdater.convertMeters(((rdeResults?.get(0) ?: 0.0) * 0.43).toLong())
                    )
                }
                else -> {
                    mapOf(
                        "PLACEHOLDER1" to RDEUIUpdater.convertMeters(((rdeResults?.get(0) ?: 0.0) * 0.23).toLong()),
                        "PLACEHOLDER2" to RDEUIUpdater.convertMeters(((rdeResults?.get(0) ?: 0.0) * 0.43).toLong())
                    )
                }
            }
            showHelp(R.raw.minihelp_distance_rural, replace)
        }
        ruralVAHelpImageButton.setOnClickListener {
            showHelp(
                R.raw.minihelp_dynamics_high,
                mapOf(
                    "SEGMENT" to "Rural",
                    "LIMIT" to calculateHighDynamics(rdeResults?.get(8) ?: 0.0)
                )
            )
        }
        ruralRPAHelpImageButton.setOnClickListener {
            showHelp(
                R.raw.minihelp_dynamics_low,
                mapOf(
                    "SEGMENT" to "Rural",
                    "LIMIT" to calculateLowDynamics(rdeResults?.get(8) ?: 0.0)
                )
            )
        }

        // Motorway
        motorwayDistanceHelpImageButton.setOnClickListener {
            val replace = when {
                (rdeResults?.get(0) ?: 0.0) * 0.23 / 1000.0 < 16.0 && (rdeResults?.get(0) ?: 0.0) * 0.43 / 1000.0 < 16.0 -> {
                    mapOf(
                        "between <b>PLACEHOLDER1</b> and <b>PLACEHOLDER2</b>" to "greater than <b>16km</b>"
                    )
                }
                (rdeResults?.get(0) ?: 0.0) * 0.23 / 1000.0 < 16.0 -> {
                    mapOf(
                        "PLACEHOLDER1" to RDEUIUpdater.convertMeters(16000),
                        "PLACEHOLDER2" to RDEUIUpdater.convertMeters(((rdeResults?.get(0) ?: 0.0) * 0.43).toLong())
                    )
                }
                else -> {
                    mapOf(
                        "PLACEHOLDER1" to RDEUIUpdater.convertMeters(((rdeResults?.get(0) ?: 0.0) * 0.23).toLong()),
                        "PLACEHOLDER2" to RDEUIUpdater.convertMeters(((rdeResults?.get(0) ?: 0.0) * 0.43).toLong())
                    )
                }
            }
            showHelp(R.raw.minihelp_distance_motorway, replace)
        }
        motorwayVAHelpImageButton.setOnClickListener {
            showHelp(
                R.raw.minihelp_dynamics_high,
                mapOf(
                    "SEGMENT" to "Motorway",
                    "LIMIT" to calculateHighDynamics(rdeResults?.get(9) ?: 0.0)
                )
            )
        }
        motorwayRPAHelpImageButton.setOnClickListener {
            showHelp(
                R.raw.minihelp_dynamics_low,
                mapOf(
                    "SEGMENT" to "Motorway",
                    "LIMIT" to calculateLowDynamics(rdeResults?.get(9) ?: 0.0)
                )
            )
        }
    }

    // TODO:delete these functions and replace through RTLola Outputstreams
    private fun calculateLowDynamics(avg_speed: Double): String {
        return "%.2f".format(
            if (avg_speed < 94.05) {
                if (avg_speed == 0.0) {
                    0.0
                } else {
                    -0.0016 * avg_speed + 0.1755
                }
            } else {
                0.025
            }
        )
    }

    private fun calculateHighDynamics(avg_speed: Double): String {
        return "%.2f".format(
            if (avg_speed < 74.6) {
                if (avg_speed == 0.0) {
                    0.0
                } else {
                    0.136 * avg_speed + 14.44
                }
            } else {
                0.0742 * avg_speed + 18.966
            }
        )
    }

    private fun showHelp(htmlFile: Int, replace: Map<String, String>) {
        val html = resources.openRawResource(htmlFile).bufferedReader().readText()
        val help = MainActivity.InfoDialog(html, replace)
        help.show(requireActivity().supportFragmentManager, null)
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        when (tab) {
            eventsTab -> showEvents()
            summaryTab -> showSpeedChart()
            rdeAnalysisTab -> showRDE()
            else -> throw IllegalStateException("Unknown tab selected!")
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {
    }

    private fun showSpeedChart() {
        eventDetailListView.visibility = View.INVISIBLE
        chartView.visibility = View.VISIBLE
        rdeView.visibility = View.INVISIBLE
    }

    private fun showEvents() {
        eventDetailListView.visibility = View.VISIBLE
        chartView.visibility = View.INVISIBLE
        rdeView.visibility = View.INVISIBLE
    }

    private fun showRDE() {
        eventDetailListView.visibility = View.INVISIBLE
        chartView.visibility = View.INVISIBLE
        rdeView.visibility = View.VISIBLE
    }

    class HistoryEventListAdapter(
        private val context: Activity,
        private val events: MutableList<PCDFEvent>
    ) : ArrayAdapter<PCDFEvent>(
        context,
        R.layout.histroy_event_item,
        events
    ) {
        private val serializer = Serializer()

        @SuppressLint("ViewHolder", "InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val inflater = context.layoutInflater
            val rowView = inflater.inflate(R.layout.histroy_event_item, null, true)
            val event = events[position]

            val millis = if (event.type == EventType.META) {
                0
            } else {
                event.timestamp - events[1].timestamp
            }

            rowView.typeTextView.text = event.type.toString().replace("_", " ")
            rowView.timeStampTextView.text = convertMillis(millis)
            try {
                rowView.dataTextView.text = event.toString()
            } catch (e: Exception) {
                rowView.typeTextView.setTextColor(ContextCompat.getColor(context, R.color.redColor))
                val str = "Could not parse the following event: \n ${serializer.generateFromPattern(event.getPattern())}"
                rowView.dataTextView.text = str
            }
            return rowView
        }

        private fun convertMillis(nanos: Long): String {
            val millis = nanos / 1_000_000
            val tz = TimeZone.getTimeZone("UTC")
            val df = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
            df.timeZone = tz
            return df.format(Date(millis))
        }
    }
}
