package de.unisaarland.loladrives.Fragments.TrackingFragment

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.tracking_grid_item.view.*
import pcdfEvent.events.obdEvents.OBDCommand

class TrackingGridAdapter(
    private val context: Activity,
    private val trackedEvents: Array<OBDCommand>,
    val fragment: TrackingFragment
) : ArrayAdapter<OBDCommand>(
    context,
    R.layout.tracking_grid_item,
    trackedEvents
) {
    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        var rowView = convertView

        if (convertView == null) {
            rowView = inflater.inflate(R.layout.tracking_grid_item, null, true)
        }

        // Initialize the fix text views
        val name = trackedEvents[position].name.replace("_", " ")
        rowView!!.EventNameTextView.text = name
        rowView.unitTextView.text = trackedEvents[position].unit

        // Pass textview associated with command to the fragment
        fragment.commandTextViews[trackedEvents[position]] = rowView.ValueTextView
        return rowView
    }

    override fun getViewTypeCount(): Int {
        return count
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}
