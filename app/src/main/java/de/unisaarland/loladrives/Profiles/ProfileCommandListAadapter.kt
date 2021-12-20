package de.unisaarland.loladrives.profiles

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import de.unisaarland.loladrives.R
import de.unisaarland.loladrives.hideKeyboard
import kotlinx.android.synthetic.main.profiles_command_select.view.*
import pcdfEvent.events.obdEvents.OBDCommand

class ProfileCommandListAadapter(
    private val context: Activity,
    private val commands: Array<String>,
    var checkedEvents: MutableMap<OBDCommand, Int>,
    private val fragment: Fragment
) : ArrayAdapter<String>(
    context,
    R.layout.profiles_command_select,
    commands
) {
    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.profiles_command_select, null, true)

        // Setup command name
        rowView.commandNameTextView.text = commands[position].replace("_", " ")

        // checks wether the command is checked in profile and
        // setup commands and update frequency
        val checked = checkedEvents.keys.contains(OBDCommand.valueOf(commands[position]))
        rowView.selectCommandSwitch.isChecked = checked
        rowView.editText.isEnabled = checked
        if (checked) {
            rowView.editText.setText(checkedEvents[OBDCommand.valueOf(commands[position])].toString())
        }

        // setup on click listeners of switch and EditText
        rowView.selectCommandSwitch.setOnClickListener {
            if (rowView.editText.text.toString() == "") {
                rowView.editText.setText("-1")
            }
            if (rowView.selectCommandSwitch.isChecked) {
                checkedEvents[OBDCommand.valueOf(commands[position])] =
                    rowView.editText.text.toString().toInt()
            } else {
                checkedEvents.remove(OBDCommand.valueOf(commands[position]))
            }
            rowView.editText.isEnabled = rowView.selectCommandSwitch.isChecked
        }

        rowView.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkedEvents[OBDCommand.valueOf(commands[position])] =
                    rowView.editText.text.toString().toInt()
                fragment.hideKeyboard()
                true
            } else {
                false
            }
        }
        return rowView
    }
}
