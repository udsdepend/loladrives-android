/*
 * Copyright (c) 2021.
 */

package de.unisaarland.loladrives.Fragments.cars

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.fragment_car_detail.*

/**
 * A simple [Fragment] subclass.
 * Use the [CarDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CarDetailFragment(val car: Car) : Fragment() {

    override fun onStart() {
        manufacturerTextview.text = car.manufacturer.toString()
        fuelTypeTextview.text = car.fuelType

        super.onStart()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_car_detail, container, false)
    }
}
