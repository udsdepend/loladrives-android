package de.unisaarland.loladrives.profiles

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.worldturner.medeia.api.JsonSchemaVersion
import com.worldturner.medeia.api.StringSchemaSource
import com.worldturner.medeia.api.jackson.MedeiaJacksonApi
import com.worldturner.medeia.schema.validation.SchemaValidator
import java.io.StringWriter

/**
 * This class is capable of JSON Schema validated parsing of profiles
 */
class ProfileParser {
    private val api = MedeiaJacksonApi()
    private val objectMapper = jacksonObjectMapper()
    private val s = StringWriter()
    private val validator = loadSchema()

    /**
     * validates JSON string against profile specification and parses into List of RDECommands
     * @param input json string to be validated and parsed into a List of RDECommands
     */
    fun parseProfile(input: String): Array<RDECommand>? {
        val unvalidatedParser = objectMapper.factory.createParser(input)
        val validatedParser = api.decorateJsonParser(validator, unvalidatedParser)

        return try {
            val commands = objectMapper.readValue(validatedParser, Array<RDECommand>::class.java)
            commands
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateFromArray(profile: Array<RDECommand>): String {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

        val unvalidatedGenerator = objectMapper.factory.createGenerator(s)
        val validatedGenerator = api.decorateJsonGenerator(validator, unvalidatedGenerator)

        objectMapper.writeValue(validatedGenerator, profile)

        val tmp = s.toString()
        s.buffer.setLength(0)
        return tmp
    }

    // loads json-schema profile specification
    private fun loadSchema(): SchemaValidator {
        // TODO: replace through file
        val source = StringSchemaSource(
            "{\n" +
                "    \"type\": \"array\",\n" +
                "    \"items\": {\n" +
                "        \"type\":\"object\",\n" +
                "        \"properties\":{\n" +
                "            \"command\":{\n" +
                "                \"type\":\"string\"\n" +
                "            },\n" +
                "            \"update_frequency\":{\n" +
                "                \"type\":\"integer\",\n" +
                "                \"minimum\": -1\n" +
                "            }\n" +
                "        },\n" +
                "        \"required\":[\"command\",\"update_frequency\"]\n" +
                "    },\n" +
                "    \"uniqueItems\":true\n" +
                "}",
            JsonSchemaVersion.DRAFT07
        )
        return api.loadSchema(source)
    }
}
