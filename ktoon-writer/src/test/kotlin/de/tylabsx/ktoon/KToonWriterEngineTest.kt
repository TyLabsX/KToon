package de.tylabsx.ktoon

import de.tylabsx.ktoon.pipeline.WriterOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for KToonWriterEngine.
 * 
 * This test class validates all aspects of TOON serialization
 * including basic serialization, formatting options, different
 * output formats, and error handling.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonWriterEngineTest {

    private val writerEngine = KToonWriterEngine()

    @Test
    fun `stringify should serialize simple object`() {
        val value = KToon.`object`(
            mapOf(
                "name" to "Alice",
                "age" to 25
            )
        )

        val result = writerEngine.stringify(value).serializedData

        assertTrue(result.contains("name: Alice"))
        assertTrue(result.contains("age: 25"))
        assertTrue(result.contains("name:"))
        assertTrue(result.contains("age:"))
    }

    @Test
    fun `stringify should serialize nested object`() {
        val value = KToon.`object`(
            mapOf(
                "user" to KToon.`object`(
                    mapOf(
                        "id" to 123,
                        "profile" to KToon.`object`(
                            mapOf(
                                "name" to "Bob",
                                "email" to "bob@example.com"
                            )
                        )
                    )
                )
            )
        )

        val result = writerEngine.stringify(value).serializedData

        assertTrue(result.contains("user:"))
        assertTrue(result.contains("id: 123"))
        assertTrue(result.contains("profile:"))
        assertTrue(result.contains("name: Bob"))
        assertTrue(result.contains("email: bob@example.com"))
    }

    @Test
    fun `stringify should serialize array`() {
        val value = ToonArray(
            listOf(
                ToonString("admin"),
                ToonString("user"),
                ToonString("guest")
            )
        )

        val result = writerEngine.stringify(value).serializedData

        assertTrue(result.contains("[3]: admin,user,guest"))
    }

    @Test
    fun `stringify should serialize mixed types`() {
        val value = KToon.`object`(
            mapOf(
                "name" to "Alice",
                "age" to 25,
                "active" to true,
                "score" to 95.5,
                "description" to ToonNull,
                "tags" to ToonArray(listOf(ToonString("admin"), ToonString("user")))
            )
        )

        val result = writerEngine.stringify(value).serializedData

        assertTrue(result.contains("name: Alice"))
        assertTrue(result.contains("age: 25"))
        assertTrue(result.contains("active: true"))
        assertTrue(result.contains("score: 95.5"))
        assertTrue(result.contains("description: null"))
        assertTrue(result.contains("tags[2]: admin,user"))
    }

    @Test
    fun `stringify should handle empty object`() {
        val value = KToon.`object`(emptyMap())

        val result = writerEngine.stringify(value).serializedData
        assertEquals("", result)
    }

    @Test
    fun `stringify should handle empty array`() {
        val value = ToonArray(emptyList())

        val result = writerEngine.stringify(value).serializedData

        assertEquals("[0]: ", result)
    }

    @Test
    fun `stringify should serialize string with quotes`() {
        val value = KToon.`object`(
            mapOf(
                "message" to "Hello, World!"
            )
        )

        val result = writerEngine.stringify(value).serializedData

        // Should be quoted because it contains space and comma
        assertTrue(result.contains("message: \"Hello, World!\""))
    }

    @Test
    fun `stringify should handle special characters`() {
        val value = KToon.`object`(
            mapOf(
                "path" to "C:\\Users\\Admin",
                "unicode" to "Café",
                "newline" to "Line1\nLine2"
            )
        )

        val result = writerEngine.stringify(value).serializedData

        assertTrue(result.contains("path: \"C:\\\\Users\\\\Admin\""))
        assertTrue(result.contains("unicode: Café"))
        assertTrue(result.contains("newline: \"Line1\\nLine2\""))
    }

    @Test
    fun `stringify should respect indentation options`() {
        val value = KToon.`object`(
            mapOf(
                "user" to KToon.`object`(
                    mapOf(
                        "name" to "Alice",
                        "profile" to KToon.`object`(
                            mapOf(
                                "email" to "alice@example.com"
                            )
                        )
                    )
                )
            )
        )

        val options = WriterOptions(indentSize = 4)
        val result = writerEngine.stringify(value, options).serializedData

        // Should use 4-space indentation
        assertTrue(result.contains("    name: Alice"))
        assertTrue(result.contains("    profile:"))
        assertTrue(result.contains("        email: alice@example.com"))
    }

    @Test
    fun `toJson should convert to JSON format`() {
        val value = KToon.`object`(
            mapOf(
                "name" to "Alice",
                "age" to 25
            )
        )

        val result = writerEngine.toJson(value).serializedData

        assertTrue(result.contains("\"name\""))
        assertTrue(result.contains("\"Alice\""))
        assertTrue(result.contains("\"age\""))
        assertTrue(result.contains("25"))
        assertTrue(result.contains("{"))
        assertTrue(result.contains("}"))
    }

    @Test
    fun `toXml should convert to XML format`() {
        val value = KToon.`object`(
            mapOf(
                "user" to KToon.`object`(
                    mapOf(
                        "name" to "Alice",
                        "age" to 25
                    )
                )
            )
        )

        val result = writerEngine.toXml(value).serializedData

        assertTrue(result.contains("<?xml"))
        assertTrue(result.contains("<user>"))
        assertTrue(result.contains("<name>"))
        assertTrue(result.contains("Alice"))
        assertTrue(result.contains("<age>"))
        assertTrue(result.contains("25"))
        assertTrue(result.contains("</user>"))
    }

    @Test
    fun `toYaml should convert to YAML format`() {
        val value = KToon.`object`(
            mapOf(
                "name" to "Alice",
                "age" to 25
            )
        )

        val result = writerEngine.toYaml(value).serializedData

        assertTrue(result.contains("name: Alice"))
        assertTrue(result.contains("age: 25"))
    }

    @Test
    fun `stream should write to output`() {
        val value = KToon.`object`(
            mapOf(
                "name" to "Alice",
                "age" to 25
            )
        )

        val output = StringBuilder()
        val result = writerEngine.stream(value, output)

        assertTrue(output.isNotEmpty())
        assertTrue(result.bytesWritten > 0)
    }

    @Test
    fun `stringify should provide result with metadata`() {
        val value = KToon.`object`(
            mapOf(
                "name" to "Alice"
            )
        )

        val result = writerEngine.stringify(value)

        assertNotNull(result)
        assertEquals(OutputFormat.TOON, result.format)
        assertTrue(result.dataSize > 0)
        assertTrue(result.serializationTime > 0)
    }
}
