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

@RunWith(AndroidJUnit4::class)
class RdeFragmentTest {
    @Before
    fun setup() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            it.tracking = false
            it.bluetoothConnected = true
        }
    }

    /**
     * Test that the RDE Fragment displays all views correctly
     */
    @Test
    @Suppress("RequiresOBDConnection")
    fun testRdeFragmentDisplay() {
        onView(withId(R.id.rdeImageButton)).perform(click())
        onView(withId(R.id.startImageButton)).perform(click())
        onView(withId(R.id.textViewRDEPrompt)).check(matches(withText("")))
        onView(withId(R.id.textViewValidRDE)).check(matches(withText("Valid RDE Trip : ")))
        onView(withId(R.id.progressBarTime)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBarDistance)).check(matches(isDisplayed()))
        onView(withId(R.id.textViewNOX)).check(matches(withText("NOₓ")))
        onView(withId(R.id.textView7)).check(matches(withText("Urban")))
        onView(withId(R.id.textView8)).check(matches(withText("Rural")))
        onView(withId(R.id.textView9)).check(matches(withText("Motorway")))
    }


}