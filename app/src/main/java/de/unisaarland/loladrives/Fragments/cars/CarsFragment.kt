/*
 * Copyright (c) 2021.
 */

package de.unisaarland.loladrives.Fragments.cars

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import de.unisaarland.loladrives.R.drawable.ic_action_car
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.car_list_item.view.*
import kotlinx.android.synthetic.main.fragment_cars.*


/**
 * A simple [Fragment] subclass.
 * Use the [CarsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CarsFragment : Fragment() {
    private lateinit var activity: MainActivity

    override fun onStart() {
        activity = requireActivity() as MainActivity
        activity.title_textview.text = getString(R.string.my_cars)
        Cars.getCarsFromHistory(activity)
        carListView.adapter = CarListAdapter(activity)
        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cars, container, false)
    }

    class CarListAdapter(
        private val context: MainActivity
    ) : ArrayAdapter<String>(
        context,
        R.layout.car_list_item,
        Cars.carMap.keys.toMutableList()
    ) {

        @SuppressLint("ViewHolder", "InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflater = context.layoutInflater
            val rowView = inflater.inflate(R.layout.car_list_item, null, true)

            val carVin = Cars.carVins[position]
            val car = Cars.carMap[carVin]!!

            rowView.textViewVIN.text = carVin
            rowView.carNameTextView.text = car.manufacturer.toString()
            rowView.manufacturerImage.setImageResource(
                when (car.manufacturer) {
                    //BMW -> bmw
                    //VW -> vw_logo
                    else -> ic_action_car
                }
            )

            rowView.setOnClickListener {
                println("Car: " + car.vin)
                println("FuelType: " + car.fuelType)
                println("Mark: " + car.manufacturer)
                println("Supported Pids: " + car.supportedPids.joinToString())
                println(
                    "Files: " +
                            car.files.joinToString()
                )
                context.supportFragmentManager.beginTransaction().replace(
                    R.id.frame_layout,
                    CarDetailFragment(car)
                ).setTransition(
                    FragmentTransaction.TRANSIT_FRAGMENT_OPEN
                ).commit()
            }

            return rowView
        }
    }

}