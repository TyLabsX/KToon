package de.tylabsx.ktoon.query

import de.tylabsx.ktoon.ToonArray
import de.tylabsx.ktoon.ToonBoolean
import de.tylabsx.ktoon.ToonNull
import de.tylabsx.ktoon.ToonNumber
import de.tylabsx.ktoon.ToonObject
import de.tylabsx.ktoon.ToonString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KToonQueryTests {

    private val root = ToonObject(
        linkedMapOf(
            "user" to ToonObject(
                linkedMapOf(
                    "id" to ToonNumber("42"),
                    "name" to ToonString("Alice"),
                    "active" to ToonBoolean(true),
                    "score" to ToonNumber("98.5"),
                    "missingValue" to ToonNull,
                    "tags" to ToonArray(listOf(ToonString("admin"), ToonString("kotlin")))
                )
            ),
            "users" to ToonArray(
                listOf(
                    ToonObject(
                        linkedMapOf(
                            "id" to ToonNumber("1"),
                            "name" to ToonString("Ada"),
                            "active" to ToonBoolean(true)
                        )
                    ),
                    ToonObject(
                        linkedMapOf(
                            "id" to ToonNumber("2"),
                            "name" to ToonString("Bob"),
                            "active" to ToonBoolean(false)
                        )
                    ),
                    ToonObject(
                        linkedMapOf(
                            "id" to ToonNumber("3")
                        )
                    )
                )
            )
        )
    )

    @Test
    fun `get should resolve simple property`() {
        assertEquals(root.entries["user"], KToonQuery.get(root, "user"))
    }

    @Test
    fun `get should resolve nested property`() {
        assertEquals(ToonString("Alice"), KToonQuery.get(root, "user.name"))
    }

    @Test
    fun `get should resolve array index`() {
        assertEquals(ToonString("Bob"), KToonQuery.get(root, "users[1].name"))
    }

    @Test
    fun `select should expand wildcard arrays`() {
        assertEquals(
            listOf(ToonString("Ada"), ToonString("Bob")),
            KToonQuery.select(root, "users[*].name")
        )
    }

    @Test
    fun `select should return wildcard array items`() {
        assertEquals(3, KToonQuery.select(root, "users[*]").size)
    }

    @Test
    fun `get should reject wildcard paths`() {
        val error = assertFailsWith<KToonQueryException> {
            KToonQuery.get(root, "users[*].name")
        }

        assertEquals("Wildcard is not allowed in get path 'users[*].name'. Use select instead.", error.message)
    }

    @Test
    fun `get should return null for missing property`() {
        assertNull(KToonQuery.get(root, "user.email"))
    }

    @Test
    fun `get should return null for invalid index`() {
        assertNull(KToonQuery.get(root, "users[99].name"))
    }

    @Test
    fun `invalid path should throw exception`() {
        val error = assertFailsWith<KToonQueryException> {
            KToonQuery.get(root, "users[].name")
        }

        assertTrue(error.message.orEmpty().contains("Invalid path 'users[].name'"))
    }

    @Test
    fun `exists should support regular and wildcard paths`() {
        assertTrue(root.exists("users[0].name"))
        assertTrue(root.exists("users[*].name"))
        assertFalse(root.exists("users[99].name"))
        assertFalse(root.exists("users[*].email"))
    }

    @Test
    fun `typed accessors should return expected values`() {
        assertEquals("Alice", root.string("user.name"))
        assertEquals(42, root.int("user.id"))
        assertEquals(42L, root.long("user.id"))
        assertEquals(98.5, root.double("user.score"))
        assertEquals(true, root.boolean("user.active"))
        assertEquals(root.entries["user"], root.obj("user"))
        assertEquals(ToonArray(listOf(ToonString("admin"), ToonString("kotlin"))), root.array("user.tags"))
    }

    @Test
    fun `nullable accessors should return null for missing or mismatched values`() {
        assertNull(root.stringOrNull("user.email"))
        assertNull(root.stringOrNull("user.id"))
        assertNull(root.intOrNull("user.name"))
        assertNull(root.booleanOrNull("user.name"))
        assertNull(root.objOrNull("user.name"))
        assertNull(root.arrayOrNull("user.name"))
    }

    @Test
    fun `non-null typed accessors should throw for missing paths`() {
        val error = assertFailsWith<KToonQueryException> {
            root.string("users[0].email")
        }

        assertEquals("Path 'users[0].email' does not exist", error.message)
    }

    @Test
    fun `non-null typed accessors should throw for type mismatches`() {
        val error = assertFailsWith<KToonQueryException> {
            root.string("user.id")
        }

        assertEquals("Expected ToonString at path 'user.id' but found ToonNumber", error.message)
    }

    @Test
    fun `extension functions should delegate to query api`() {
        assertEquals(ToonString("Alice"), root.get("user.name"))
        assertEquals(listOf(ToonNumber("1"), ToonNumber("2"), ToonNumber("3")), root.select("users[*].id"))
        assertTrue(root.exists("users[1].active"))
    }
}
