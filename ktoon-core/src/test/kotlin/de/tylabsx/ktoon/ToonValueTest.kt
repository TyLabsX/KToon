package de.tylabsx.ktoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for ToonValue data model.
 * 
 * This test class validates all aspects of the ToonValue sealed interface
 * and its implementations, ensuring proper behavior, type safety,
 * and string representation.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class ToonValueTest {

    @Test
    fun `ToonObject should create object with entries`() {
        val entries = mapOf(
            "name" to ToonString("Alice"),
            "age" to ToonNumber("25"),
            "active" to ToonBoolean(true)
        )

        val obj = ToonObject(entries)

        assertEquals(3, obj.entries.size)
        assertEquals("Alice", (obj.entries["name"] as ToonString).value)
        assertEquals("25", (obj.entries["age"] as ToonNumber).raw)
        assertEquals(true, (obj.entries["active"] as ToonBoolean).value)
    }

    @Test
    fun `ToonArray should create array with values`() {
        val values = listOf(
            ToonString("admin"),
            ToonString("user"),
            ToonString("guest")
        )

        val array = ToonArray(values)

        assertEquals(3, array.values.size)
        assertEquals("admin", (array.values[0] as ToonString).value)
        assertEquals("user", (array.values[1] as ToonString).value)
        assertEquals("guest", (array.values[2] as ToonString).value)
    }

    @Test
    fun `ToonString should create string with value`() {
        val str = ToonString("Hello, World!")

        assertEquals("Hello, World!", str.value)
    }

    @Test
    fun `ToonNumber should create number with raw value`() {
        val num = ToonNumber("123.45")

        assertEquals("123.45", num.raw)
    }

    @Test
    fun `ToonBoolean should create boolean with value`() {
        val bool = ToonBoolean(true)

        assertEquals(true, bool.value)
    }

    @Test
    fun `ToonNull should create null instance`() {
        val nullValue = ToonNull

        // ToonNull is a singleton, so we can check identity
        assertEquals(ToonNull, nullValue)
    }

    @Test
    fun `ToonValue toString should provide debug representation`() {
        val obj = ToonObject(
            mapOf(
                "name" to ToonString("Alice"),
                "age" to ToonNumber("25")
            )
        )

        val result = obj.toString()

        assertTrue(result.contains("ToonObject"))
        assertTrue(result.contains("name=ToonString"))
        assertTrue(result.contains("Alice"))
        assertTrue(result.contains("age=ToonNumber"))
        assertTrue(result.contains("25"))
    }

    @Test
    fun `ToonValue when should work with all types`() {
        val values: List<ToonValue> = listOf(
            ToonObject(mapOf("key" to ToonString("value"))),
            ToonArray(listOf(ToonString("item1"), ToonString("item2"))),
            ToonString("test"),
            ToonNumber("42"),
            ToonBoolean(true),
            ToonNull
        )

        values.forEach { value ->
            when (value) {
                is ToonObject -> assertTrue(value.entries.isNotEmpty())
                is ToonArray -> assertTrue(value.values.isNotEmpty())
                is ToonString -> assertTrue(value.value.isNotEmpty())
                is ToonNumber -> assertTrue(value.raw.isNotEmpty())
                is ToonBoolean -> assertTrue(true) // Always true for boolean
                is ToonNull -> assertTrue(true) // Always true for null
            }
        }
    }

    @Test
    fun `ToonObject should handle empty entries`() {
        val emptyObj = ToonObject(emptyMap())

        assertEquals(0, emptyObj.entries.size)
        assertTrue(emptyObj.entries.isEmpty())
    }

    @Test
    fun `ToonArray should handle empty values`() {
        val emptyArray = ToonArray(emptyList())

        assertEquals(0, emptyArray.values.size)
        assertTrue(emptyArray.values.isEmpty())
    }

    @Test
    fun `ToonString should handle empty value`() {
        val emptyString = ToonString("")

        assertEquals("", emptyString.value)
    }

    @Test
    fun `ToonNumber should handle various formats`() {
        val integer = ToonNumber("42")
        val decimal = ToonNumber("3.14159")
        val scientific = ToonNumber("1.23e-4")
        val negative = ToonNumber("-123")

        assertEquals("42", integer.raw)
        assertEquals("3.14159", decimal.raw)
        assertEquals("1.23e-4", scientific.raw)
        assertEquals("-123", negative.raw)
    }

    @Test
    fun `ToonBoolean should handle true and false`() {
        val trueValue = ToonBoolean(true)
        val falseValue = ToonBoolean(false)

        assertEquals(true, trueValue.value)
        assertEquals(false, falseValue.value)
    }
}
