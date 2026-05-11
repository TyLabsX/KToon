package de.tylabsx.ktoon

import kotlin.test.*

/**
 * Comprehensive tests for KToon public API.
 * 
 * This test class validates the main entry point for the KToon
 * library, ensuring that the simple API works correctly
 * and provides proper integration with the underlying engine.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonApiTest {

    @BeforeTest
    fun resetKToonConfigurationBeforeEachTest() {
        KToon.resetConfiguration()
    }

    @Test
    fun `KToon parse should parse simple TOON`() {
        val input = "name: Alice"
        val result = KToon.parse(input)

        assertTrue(result is ToonObject)
        val obj = result as ToonObject
        assertEquals(1, obj.entries.size)
        assertEquals("Alice", (obj.entries["name"] as ToonString).value)
    }

    @Test
    fun `KToon parse should parse nested TOON`() {
        val input = """
            user:
              id: 123
              name: "Bob"
              active: true
        """.trimIndent()

        val result = KToon.parse(input)

        assertTrue(result is ToonObject)
        val userObj = result
        assertEquals(1, userObj.entries.size)

        val userValue = userObj.entries["user"] as ToonObject
        assertEquals(3, userValue.entries.size)
    }

    @Test
    fun `KToon stringify should serialize simple object`() {
        val value = KToon.`object`(
            mapOf(
                "name" to "Alice",
                "age" to 25
            )
        )

        val result = KToon.stringify(value)

        assertTrue(result.contains("name: Alice"))
        assertTrue(result.contains("age: 25"))
        assertTrue(result.contains("name:"))
        assertTrue(result.contains("age:"))
    }

    @Test
    fun `KToon stringify should serialize nested object`() {
        val value = KToon.`object`(
            mapOf(
                "user" to KToon.`object`(
                    mapOf(
                        "id" to 123,
                        "name" to "Bob"
                    )
                )
            )
        )

        val result = KToon.stringify(value)

        assertTrue(result.contains("user:"))
        assertTrue(result.contains("id: 123"))
        assertTrue(result.contains("name: Bob"))
    }

    @Test
    fun `KToon isValid should validate correct TOON`() {
        val validInput = "name: Alice"
        val invalidInput = "name Alice"  // Missing colon

        assertTrue(KToon.isValid(validInput))
        assertFalse(KToon.isValid(invalidInput))
    }

    @Test
    fun `KToon configure should update global settings`() {
        // Reset to defaults first
        KToon.resetConfiguration()

        // Configure new settings
        KToon.configure {
            indentationSize = 4
            strictMode = true
            enableDebugLogging = false
        }

        // Verify configuration was applied
        val config = KToon.getConfiguration()
        assertTrue(config.contains("indentationSize: 4"))
        assertTrue(config.contains("strictMode: true"))
        assertTrue(config.contains("enableDebugLogging: false"))
    }

    @Test
    fun `KToon getConfiguration should return current settings`() {
        // Configure known settings
        KToon.configure {
            indentationSize = 3
            maxNestingDepth = 50
            enableKeyFolding = false
        }

        val config = KToon.getConfiguration()

        assertTrue(config.contains("indentationSize: 3"))
        assertTrue(config.contains("maxNestingDepth: 50"))
        assertTrue(config.contains("enableKeyFolding: false"))
    }

    @Test
    fun `KToon getStatistics should return engine information`() {
        val stats = KToon.getStatistics()

        assertNotNull(stats)
        assertTrue(stats.supportedOperations.isNotEmpty())
        assertTrue(stats.supportedFormats.isNotEmpty())
        assertTrue(stats.totalPhases > 0)
    }

    @Test
    fun `KToon roundtrip should perform parse and stringify`() {
        val input = """
            user:
              id: 123
              name: "Alice"
              active: true
        """.trimIndent()

        val result = KToon.roundtrip(input)

        assertNotNull(result)
        assertTrue(result.contains("user:"))
        assertTrue(result.contains("id: 123"))
        assertTrue(result.contains("name: Alice"))
        assertTrue(result.contains("active: true"))
    }

    @Test
    fun `KToon object should create ToonObject from map`() {
        val map = mapOf(
            "name" to "Alice",
            "age" to 25,
            "active" to true
        )

        val result = KToon.`object`(map)

        assertTrue(result is ToonObject)
        val obj = result as ToonObject
        assertEquals(3, obj.entries.size)
        assertEquals("Alice", (obj.entries["name"] as ToonString).value)
        assertEquals("25", (obj.entries["age"] as ToonNumber).raw)
        assertEquals(true, (obj.entries["active"] as ToonBoolean).value)
    }

    @Test
    fun `KToon array should create ToonArray from list`() {
        val list: List<String> = listOf("admin", "user", "guest")

        val result = KToon.array(list)

        assertTrue(result is ToonArray)
        val array = result as ToonArray
        assertEquals(3, array.values.size)
        assertEquals("admin", (array.values[0] as ToonString).value)
        assertEquals("user", (array.values[1] as ToonString).value)
        assertEquals("guest", (array.values[2] as ToonString).value)
    }

    @Test
    fun `KToon string should create ToonString`() {
        val result = KToon.string("Hello, World!")

        assertTrue(result is ToonString)
        assertEquals("Hello, World!", result.value)
    }

    @Test
    fun `KToon number should create ToonNumber`() {
        val result = KToon.number(123.45)

        assertTrue(result is ToonNumber)
        assertEquals("123.45", result.raw)
    }

    @Test
    fun `KToon boolean should create ToonBoolean`() {
        val result = KToon.boolean(true)

        assertTrue(result is ToonBoolean)
        assertEquals(true, result.value)
    }

    @Test
    fun `KToon null should create ToonNull`() {
        val result = KToon.`null`()

        assertTrue(result is ToonNull)
        assertEquals(ToonNull, result)
    }

    @Test
    fun `KToon factory methods should handle mixed types`() {
        val mixedMap: Map<String, Any> = mapOf(
            "string" to "Alice",
            "number" to 25,
            "boolean" to true
        )

        val obj = KToon.`object`(mixedMap)

        assertEquals("Alice", (obj.entries["string"] as ToonString).value)
        assertEquals("25", (obj.entries["number"] as ToonNumber).raw)
        assertEquals(true, (obj.entries["boolean"] as ToonBoolean).value)
        assertEquals(3, obj.entries.size)
    }
}
