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
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.isNull

/**
 * Test class for the Tracking fragment, which is used to display tracking information and to monitor the running test.
 */
@RunWith(AndroidJUnit4::class)
class TrackingFragmentTest {
    @Before
    fun setup() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            it.bluetoothConnected = true
        }
        onView(withId(R.id.monitoringImageButton)).perform(click())
    }

    /**
     * Test that the tracking fragment displays when pressing on the "Monitoring" button in the home screen.
     */
    @Test
    fun testTrackingFragment() {
        onView(withId(R.id.trackingGridView)).check(matches(isDisplayed()))
    }

    /**
     * Test that the "Start" button in the tracking fragment displays the correct text
     */
    @Test
    fun testTrackingFragmentStart() {
        onView(withId(R.id.startTrackingButton)).check(matches(withText("Start Monitoring")))
    }

    /**
     * Test that in the initial state of the tracking fragment the test is not running and the
     * monitoring is not activated.
     */
    @Test
    @Suppress("RequiresOBDConnection")
    fun testTrackingFragmentInitialState() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var isRdeOnGoing = false
        var isTracking = false
        scenario.onActivity {
            isTracking = it.tracking
            isRdeOnGoing = it.rdeOnGoing
            it.bluetoothConnected = true
        }
        onView(withId(R.id.monitoringImageButton)).perform(click())
        assertFalse(isRdeOnGoing)
        assertFalse(isTracking)
    }

    /**
     * Test that the "Stop" button in the tracking fragment deactivates the monitoring service when clicked.
     */
    @Test
    @Suppress("RequiresOBDConnection")
    fun testTrackingFragmentStopButton() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var isRdeOnGoing = false
        var isTracking = false
        scenario.onActivity {
            isTracking = it.tracking
            isRdeOnGoing = it.rdeOnGoing
            it.bluetoothConnected = true
        }
        onView(withId(R.id.monitoringImageButton)).perform(click())
        onView(withId(R.id.stopTrackingButton)).perform(click())
        assertFalse(isRdeOnGoing)
        assertFalse(isTracking)
    }

    /**
     * Test that the "Start" button in the tracking fragment activates the monitoring service when clicked.
     */
    @Test
    @Suppress("RequiresOBDConnection")
    fun testTrackingFragmentStartButton() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var isRdeOnGoing = false
        var isTracking = false
        scenario.onActivity {
            isTracking = it.tracking
            isRdeOnGoing = it.rdeOnGoing
            it.bluetoothConnected = true
        }
        onView(withId(R.id.monitoringImageButton)).perform(click())
        onView(withId(R.id.stopTrackingButton)).perform(click())
        onView(withId(R.id.startTrackingButton)).perform(click())
        assertTrue(isRdeOnGoing)
        assertTrue(isTracking)
    }

    /**
     * Test that the "Stop" button becomes invisible after being pressed, and that the "Start" button is visible.
     */
    @Test
    @Suppress("RequiresOBDConnection")
    fun testTrackingFragmentStopIsPressed() {
        onView(withId(R.id.stopTrackingButton)).check(matches(isDisplayed()))
        onView(withId(R.id.stopTrackingButton)).perform(click())
        onView(withId(R.id.startTrackingButton)).check(matches(isDisplayed()))
    }

    /**
     * Test that the "Start" button becomes invisible after being pressed, and that the "Stop" button is visible.
     */
    @Test
    @Suppress("RequiresOBDConnection")
    fun testTrackingFragmentStartIsPressed() {
        onView(withId(R.id.stopTrackingButton)).perform(click())
        onView(withId(R.id.stopTrackingButton)).perform(click())
        onView(withId(R.id.startTrackingButton)).check(matches(isNull()))
        onView(withId(R.id.stopTrackingButton)).check(matches(isDisplayed()))
    }
}