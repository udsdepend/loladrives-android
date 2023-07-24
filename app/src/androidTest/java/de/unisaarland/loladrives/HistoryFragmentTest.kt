package de.unisaarland.loladrives


import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class HistoryFragmentTest {

    @Before
    fun setUp() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            it.tracking = false
            it.bluetoothConnected = false
            it.changeDonatingAllowed(false) // disable donating
        }
        onView(withId(R.id.historyImageButton)).perform(click())
    }

    /**
     * Checks whether the history fragment is displayed correctly with correct title and data.
     */
    @Test
    @Ignore // Requires a test PCDF file
    fun testHistoryFragmentDisplay() {
        onView(withId(R.id.title_textview)).check(matches(withText("History")))
        onData(Matchers.allOf(Matchers.containsString("A20-1")))
            .check(matches(isDisplayed()))
    }

    /**
     * Checks whether you can can get detailed information about a PCDF file by clicking on it.
     */
    @Ignore // Requires OBD Connection
    @Test
    fun testHistoryNavigation() {
        onData(Matchers.allOf(Matchers.containsString("A20-1")))
            .perform(click())
        onView(withId(R.id.tabEventData)).check(matches(withText("Event Log")))
        onView(withId(R.id.tabSpeedProfile)).check(matches(withText("Speed Profile")))
        onView(withId(R.id.tabRDEResult)).check(matches(withText("RDE Result")))
    }

}