package de.unisaarland.loladrives

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ProfilesFragmentTest {
    private var scenario: ActivityScenario<MainActivity> = ActivityScenario.launch(MainActivity::class.java)
    @Before
    fun setUp() {
        scenario.onActivity {
            it.tracking = false
            it.bluetoothConnected = true
        }
        onView(withId(R.id.profilesImageButton)).perform(click())
    }

    /**
     * Tests that the basic layout of the profiles fragment is correct and default profile is present
     */
    @Test
    fun testProfileBasicLayout() {
        onView(withId(R.id.title_textview)).check(matches(withText("Monitoring Profiles")))
        onView(withId(R.id.addProfileButton)).check(matches(isDisplayed()))
        onData(allOf(containsString("default_profile")))
            .onChildView(withId(R.id.profileName))
            .check(matches(withText("default_profile")))
    }

    /**
     * Tests that the navigation to the add profile fragment works
     */
    @Test
    fun testNavigationToAddProfile(){
        onView(withId(R.id.addProfileButton)).perform(click())
        onView(withId(R.id.title_textview)).check(matches(withText("Edit Profile")))
    }

    /**
     * Tests that the creation of new profiles works
     */
    @Test
    fun testCanCreateNewProfile(){
        onView(withId(R.id.addProfileButton)).perform(click())
        onView(withId(R.id.profileNameEditText)).perform(typeText("Test Profile"))
        onView(withId(R.id.backButton)).perform(click())
        scenario.onActivity {
            assertEquals(3, it.profilesFragment.profiles.size)
            assertTrue( it.profilesFragment.profiles.keys.contains("Test Profile.json"))
        }
        onData(allOf(containsString("Test Profile")))
            .check(matches(isDisplayed()))
    }

    /**
     * Tests that the navigation to an existing profile works and can edit it
     */
    @Test
    fun testCanEditProfile(){
        onData(allOf(containsString("Test Profile")))
            .onChildView(withId(R.id.editProfileButton))
            .perform(click())
        onView(withId(R.id.title_textview)).check(matches(withText("Edit Profile")))
        onData(allOf(containsString("RPM"))).onChildView(withId(R.id.selectCommandSwitch)).perform(click())
        onData(allOf(containsString("SPEED"))).onChildView(withId(R.id.selectCommandSwitch)).perform(click())
        onView(withId(R.id.backButton)).perform(click())
    }
}