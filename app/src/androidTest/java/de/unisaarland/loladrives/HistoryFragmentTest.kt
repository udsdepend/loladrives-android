package de.unisaarland.loladrives


import java.io.File
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class HistoryFragmentTest {
    lateinit var file : File;
    lateinit var data : String;

    @Before
    fun setUp() {
        data = "Sample Data"
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            it.tracking = false
            it.bluetoothConnected = true
            val storageDir = it.getExternalFilesDir("pcdfdata")
            storageDir?.canWrite()
            storageDir?.createNewFile().apply {
                file = File(storageDir,"Dir")
                file.mkdir()
                val newFile = File(file, "testFile.ppcdf")
                newFile.appendText(data)
                newFile.canRead()
                newFile.deleteOnExit()
            }
        }

    }

    @Test
    fun testHistoryFragmentDisplay() {
        onView(withId(R.id.historyImageButton)).perform(ViewActions.click())
        onData(Matchers.allOf(Matchers.containsString("testFile")))
            .perform(click())
        onView(withId(R.id.textViewHistory)).check(matches(withText("History")))
    }
}