package de.unisaarland.loladrives

import androidx.test.core.app.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for the RDE Settings fragment, which is used to display possible configurations for the RDE test.
 */
@RunWith(AndroidJUnit4::class)
class RdeSettingsFragmentTest {
    @Before
    fun setup() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            it.tracking = false
            it.bluetoothConnected = true
        }
        onView(withId(R.id.rdeImageButton)).perform(click())
    }

    /**
     * Test that the settings fragment displays when pressing on the "RDE" button in the home screen
     */
    @Test
    fun testSettingsFragment() {
        onView(withId(R.id.textView2)).check(matches(isDisplayed()))
        onView(withId(R.id.textView2)).check(matches(withText("RDE Test Configuration")))
    }

    /**
     * Test the seek bar in the settings fragment matches the test's distance
     */
    @Test
    fun testSettingsFragmentDistanceBar() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var rdeDistance: Int = 0
        scenario.onActivity {
            it.tracking = false
            it.bluetoothConnected = true
        }
        onView(withId(R.id.rdeImageButton)).perform(click())
        scenario.onActivity {
            rdeDistance = it.rdeFragment.distance.toInt()
        }
        onView(withId(R.id.textViewDistance)).check(matches(isDisplayed()))
        onView(withId(R.id.textViewDistance)).check(matches(withText("$rdeDistance km")))
    }

    /**
     * Test  changing the seek bar  updates the test's distance in the RDE fragment
     */
    @Test
    fun testSettingsFragmentDistanceBarChange() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var rdeDistance: Int = 0
        scenario.onActivity {
            it.tracking = false
            it.bluetoothConnected = true
        }
        onView(withId(R.id.rdeImageButton)).perform(click())
//        onView(withId(R.id.distanceSeekBar)).perform() // TODO: find a way to press somewhere else on the bar
        scenario.onActivity {
            rdeDistance = it.rdeFragment.distance.toInt()
        }
        onView(withId(R.id.textViewDistance)).check(matches(isDisplayed()))
        onView(withId(R.id.textViewDistance)).check(matches(withText("$rdeDistance km")))
    }

    /**
     * Test that the "Start" button redirects to the RDE Fragment
     */
    @Test
    @Ignore // Requires OBD Connection
    fun testSettingsFragmentStart() {
        onView(withId(R.id.startImageButton)).perform(click())
        onView(withId(R.id.textViewValidRDE)).check(matches(isDisplayed()))
        onView(withId(R.id.textViewValidRDE)).check(matches(withText("Valid RDE Trip : ")))
    }
}