package de.tylabsx.ktoon

import kotlin.test.*

/**
 * Comprehensive tests for KToonParserEngine.
 * 
 * This test class validates all aspects of the TOON parsing
 * pipeline including input normalization, line splitting,
 * indentation analysis, syntax classification, key-value extraction,
 * structure building, and value resolution.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonParserEngineTest {

    private val parserEngine = KToonParserEngine()

    @Test
    fun `parse should handle simple key-value pair`() {
        val input = "name: Alice"
        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val obj = result.toonValue as ToonObject
        assertEquals(1, obj.entries.size)
        assertEquals("Alice", (obj.entries["name"] as ToonString).value)
    }

    @Test
    fun `parse should handle nested object`() {
        val input = """
            user:
              id: 123
              name: "Bob"
              active: true
        """.trimIndent()

        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val userObj = result.toonValue as ToonObject
        assertEquals(1, userObj.entries.size)

        val userValue = userObj.entries["user"] as ToonObject
        assertTrue(userValue is ToonObject)
        assertEquals(3, userValue.entries.size)
        assertEquals("123", (userValue.entries["id"] as ToonNumber).raw)
        assertEquals("Bob", (userValue.entries["name"] as ToonString).value)
        assertEquals(true, (userValue.entries["active"] as ToonBoolean).value)
    }

    @Test
    fun `parse should handle key folding`() {
        val input = "user.id: 456"
        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val obj = result.toonValue as ToonObject
        assertEquals(1, obj.entries.size)

        val userValue = obj.entries["user"] as ToonObject
        assertTrue(userValue is ToonObject)
        assertEquals(1, userValue.entries.size)
        assertEquals("456", (userValue.entries["id"] as ToonNumber).raw)
    }

    @Test
    fun `parse should handle inline array`() {
        val input = "roles: admin, user, guest"
        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val obj = result.toonValue as ToonObject
        assertEquals(1, obj.entries.size)

        val rolesArray = obj.entries["roles"] as ToonArray
        assertEquals(3, rolesArray.values.size)
        assertEquals("admin", (rolesArray.values[0] as ToonString).value)
        assertEquals("user", (rolesArray.values[1] as ToonString).value)
        assertEquals("guest", (rolesArray.values[2] as ToonString).value)
    }

    @Test
    fun `parse should handle boolean values`() {
        val input = """
            active: true
            deleted: false
        """.trimIndent()

        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val obj = result.toonValue as ToonObject
        assertEquals(2, obj.entries.size)
        assertEquals(true, (obj.entries["active"] as ToonBoolean).value)
        assertEquals(false, (obj.entries["deleted"] as ToonBoolean).value)
    }

    @Test
    fun `parse should handle null values`() {
        val input = """
            middle_name: null
            phone: null
        """.trimIndent()

        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val obj = result.toonValue as ToonObject
        assertEquals(2, obj.entries.size)
        assertTrue(obj.entries["middle_name"] is ToonNull)
        assertTrue(obj.entries["phone"] is ToonNull)
    }

    @Test
    fun `parse should handle number values`() {
        val input = """
            count: 42
            price: 19.99
            big: 12345678901234567890
        """.trimIndent()

        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val obj = result.toonValue as ToonObject
        assertEquals(3, obj.entries.size)
        assertEquals("42", (obj.entries["count"] as ToonNumber).raw)
        assertEquals("19.99", (obj.entries["price"] as ToonNumber).raw)
        assertEquals("12345678901234567890", (obj.entries["big"] as ToonNumber).raw)
    }

    @Test
    fun `parse should handle quoted strings`() {
        val input = """
            message: "Hello, World!"
            path: "C:\\Users\\Admin"
        """.trimIndent()

        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val obj = result.toonValue

        assertEquals(2, obj.entries.size)
        assertEquals("Hello, World!", (obj.entries["message"] as ToonString).value)
        assertEquals("C:\\Users\\Admin", (obj.entries["path"] as ToonString).value)
    }

    @Test
    fun `parse should handle comments`() {
        val input = """
            # This is a comment
            name: Alice  # This is an inline comment
            # Another comment
            age: 25
        """.trimIndent()

        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val obj = result.toonValue as ToonObject
        assertEquals(2, obj.entries.size)
        assertEquals("Alice", (obj.entries["name"] as ToonString).value)
        assertEquals("25", (obj.entries["age"] as ToonNumber).raw)
    }

    @Test
    fun `parse should handle empty lines`() {
        val input = """
            name: Alice
            
            
            age: 25
            
        """.trimIndent()

        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val obj = result.toonValue as ToonObject
        assertEquals(2, obj.entries.size)
        assertEquals("Alice", (obj.entries["name"] as ToonString).value)
        assertEquals("25", (obj.entries["age"] as ToonNumber).raw)
    }

    @Test
    fun `parse should provide detailed error information`() {
        val input = "name Alice"  // Missing colon

        assertFailsWith<ToonParseException> {
            parserEngine.parse(input)
        }

        try {
            parserEngine.parse(input)
        } catch (e: ToonParseException) {
            assertEquals("Missing colon in key-value pair", e.message)
            assertEquals(1, e.line)
            assertTrue(e.column > 0)
        }
    }

    @Test
    fun `parse should reject invalid indentation under scalar`() {
        val input = """
        user:
          id: 123
            name: "Bob"
    """.trimIndent()

        assertFailsWith<ToonParseException> {
            parserEngine.parse(input)
        }
    }

    @Test
    fun `parse should handle tabular array`() {
        val input = """
            users{id,name}:
              1,Alice
              2,Bob
        """.trimIndent()

        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val obj = result.toonValue as ToonObject
        assertEquals(1, obj.entries.size)

        val usersArray = obj.entries["users"] as ToonArray
        assertEquals(2, usersArray.values.size)

        // First user object
        val firstUser = usersArray.values[0] as ToonObject
        assertEquals(2, firstUser.entries.size)
        assertEquals("1", (firstUser.entries["id"] as ToonNumber).raw)
        assertEquals("Alice", (firstUser.entries["name"] as ToonString).value)

        // Second user object
        val secondUser = usersArray.values[1] as ToonObject
        assertEquals(2, secondUser.entries.size)
        assertEquals("2", (secondUser.entries["id"] as ToonNumber).raw)
        assertEquals("Bob", (secondUser.entries["name"] as ToonString).value)
    }

    @Test
    fun `parse should validate input`() {
        val validInput = "name: Alice"
        val invalidInput = "name Alice"  // Missing colon

        val validResult = parserEngine.validate(validInput)
        val invalidResult = parserEngine.validate(invalidInput)

        assertTrue(validResult.isValid)
        assertFalse(invalidResult.isValid)
        assertEquals(0, validResult.errors.size)
        assertEquals(1, invalidResult.errors.size)
    }

    @Test
    fun `parse should handle complex nested structure`() {
        val input = """
            company:
              name: "TechCorp"
              employees:
                count: 100
                manager:
                  id: 1
                  name: "John"
                  contact:
                    email: "john@techcorp.com"
                    phone: "555-1234"
        """.trimIndent()

        val result = parserEngine.parse(input)

        assertTrue(result.toonValue is ToonObject)
        val root = result.toonValue
        assertEquals(1, root.entries.size)

        val companyObj = root.entries["company"] as ToonObject
        assertEquals(2, companyObj.entries.size)

        val companyValue = root.entries["company"] as ToonObject
        assertEquals(2, companyValue.entries.size)
        assertEquals("TechCorp", (companyValue.entries["name"] as ToonString).value)

        val employeesValue = companyValue.entries["employees"] as ToonObject
        assertEquals(2, employeesValue.entries.size)

        val managerValue = employeesValue.entries["manager"] as ToonObject
        assertEquals(3, managerValue.entries.size)

        val contactValue = managerValue.entries["contact"] as ToonObject
        assertEquals(2, contactValue.entries.size)
    }

    @Test
    fun `parse should provide parse result with metadata`() {
        val input = "name: Alice"
        val result = parserEngine.parse(input)

        assertNotNull(result.toonValue)
        assertNotNull(result.classifiedLines)
        assertNotNull(result.indentationStructure)
        assertNotNull(result.lines)
        assertNotNull(result.normalizedInput)
        assertEquals(1, result.lines.size)
        assertEquals(1, result.classifiedLines.size)
    }
}
