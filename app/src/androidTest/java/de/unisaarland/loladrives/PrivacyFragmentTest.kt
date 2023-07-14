package de.unisaarland.loladrives

import androidx.test.core.app.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for the Privacy fragment, which is used to display the agreed Privacy Policy and
 * allow the user to revisit their consent status for data donations.
 */
@RunWith(AndroidJUnit4::class)
class PrivacyFragmentTest {
    @Before
    fun setup() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            it.tracking = false
        }
        onView(withId(R.id.privacyImageButton)).perform(click())
    }

    /**
     * Test that the privacy fragment displays when pressing on the "Privacy" button in the home screen
     */
    @Test
    fun testPrivacyFragment() {
        onView(withId(R.id.privacyWebView)).check(matches(isDisplayed()))
    }

    /**
     * Test that the bar in the privacy fragment displays the correct text
     */
    @Test
    fun testPrivacyFragmentBar() {
        onView(withId(R.id.textView)).check(matches(withText("Enable data donations:")))
    }

    /**
     * Test the switch in the privacy fragment matches the user's choice
     */
    @Test
    fun testPrivacyFragmentSwitch() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var userDonating: Boolean = false
        scenario.onActivity {
            userDonating = it.donatingAllowed
        }
        onView(withId(R.id.privacyImageButton)).perform(click())
        if (userDonating) {
            onView(withId(R.id.privacySwitch)).check(matches(isChecked()))
        } else {
            onView(withId(R.id.privacySwitch)).check(matches(isNotChecked()))
        }
    }

    /**
     * Test the switch in the privacy fragment will alternate the user's choice if clicked
     */
    @Test
    fun testPrivacyFragmentSwitchChange() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var userDonating: Boolean = false
        scenario.onActivity {
            userDonating = it.donatingAllowed
        }
        onView(withId(R.id.privacyImageButton)).perform(click())
        if (userDonating) {
            onView(withId(R.id.privacySwitch)).perform(click())
            onView(withId(R.id.privacySwitch)).check(matches(isNotChecked())) // toggled to not enable donations
            scenario.onActivity {
                assertFalse(it.donatingAllowed) // donations are not enabled
            }
        } else {
            onView(withId(R.id.privacySwitch)).perform(click())
            onView(withId(R.id.privacySwitch)).check(matches(isChecked())) // toggled to enable donations
            scenario.onActivity {
                assertTrue(it.donatingAllowed) // donations are enabled
            }
        }
    }
}