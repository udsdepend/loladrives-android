package de.unisaarland.loladrives.Profiles

import de.unisaarland.loladrives.profiles.ProfileParser
import de.unisaarland.loladrives.profiles.RDECommand
import org.junit.Assert.*
import org.junit.Before

import org.junit.Test
import pcdfEvent.events.obdEvents.OBDCommand

class ProfileParserTest {
    val profileParser: ProfileParser = ProfileParser()
    var rdeCommand: RDECommand? = null
    var anotherRdeCommand: RDECommand? = null
    var commands: Array<RDECommand>? = null

    @Before
    fun setUp() {
        rdeCommand = RDECommand(OBDCommand.ENGINE_COOLANT_TEMPERATURE, 0)
        anotherRdeCommand = RDECommand(OBDCommand.RPM, 0)
        commands = arrayOf(rdeCommand!!, anotherRdeCommand!!)
    }

    /**
     * Test if the profile parser can parse a valid to the correct RDE commands.
     */
    @Test
    fun testParseProfileValidInput() {
        val jsonInput = "[{\"command\":\"ENGINE_COOLANT_TEMPERATURE\",\"update_frequency\":0},{\"command\":\"RPM\",\"update_frequency\":0}]"
        val parsedProfile = profileParser.parseProfile(jsonInput)

        assertEquals(commands!![0], parsedProfile!![0])
        assertEquals(commands!![1], parsedProfile[1])
    }

    /**
     * Test that the profile parser will return null if input is not valid RDE commands.
     */
    @Test
    fun testParseProfileWrongInput() {
        val wrongJsonInput = "[{\"command\":\"ENGINE_COOLANT_TEMP\",\"update_frequency\":0},{\"command\":\"RPV\",\"update_frequency\":0}]"
        assertNull(profileParser.parseProfile(wrongJsonInput))
    }

    /**
     * Test that the profile parser will return null if input is not valid RDE commands.
     */
    @Test
    fun testParseProfileInvalidJson() {
        val invalidJsonInput = "[{command: ENGINE_COOLANT_TEMPERATURE,\"update_frequency\":0},{\"command\":\"RPM\",update_frequency:0}]"
        assertNull(profileParser.parseProfile(invalidJsonInput))
    }

    /**
     * Test that the json generator will return the right string for valid RDE commands.
     */
    @Test
    fun testGenerateFromArray() {
        val expectedJson = "[{\"command\":\"ENGINE_COOLANT_TEMPERATURE\",\"update_frequency\":0},{\"command\":\"RPM\",\"update_frequency\":0}]"
        val jsonOutput = profileParser.generateFromArray(commands!!)
        assertEquals(expectedJson, jsonOutput)
    }
}