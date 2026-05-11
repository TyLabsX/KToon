package de.tylabsx.ktoon.kotlinx.bridge

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
class KToonKotlinxTest {

    @Serializable
    data class Address(
        val city: String,
        val zip: Int
    )

    @Serializable
    enum class Role {
        ADMIN,
        USER
    }

    @Serializable
    data class User(
        val id: Int,
        val name: String,
        val active: Boolean,
        val role: Role,
        val tags: List<String>,
        val address: Address?,
        val note: String?
    )

    @Serializable
    data class Company(
        val name: String,
        val users: List<User>,
        val metadata: Map<String, String>,
        val scores: List<Double>
    )

    @Test
    fun `kotlinx should encode simple data class to TOON`() {
        val user = User(
            id = 1,
            name = "Alice",
            active = true,
            role = Role.ADMIN,
            tags = listOf("kotlin", "toon"),
            address = Address("Berlin", 10115),
            note = null
        )

        val toon = KToonKotlinX.encodeToString(user)

        assertTrue(toon.contains("id: 1"))
        assertTrue(toon.contains("name: Alice"))
        assertTrue(toon.contains("active: true"))
        assertTrue(toon.contains("role: ADMIN"))
        assertTrue(toon.contains("tags[2]: kotlin,toon"))
        assertTrue(toon.contains("note: null"))

        val decoded = KToonKotlinX.decodeFromString<User>(toon)

        assertEquals(user, decoded)
    }

    @Test
    fun `kotlinx should roundtrip complex data class`() {
        val company = Company(
            name = "TyLabsX",
            users = listOf(
                User(
                    id = 1,
                    name = "Alice",
                    active = true,
                    role = Role.ADMIN,
                    tags = listOf("core", "kotlin"),
                    address = Address("Berlin", 10115),
                    note = "hello"
                ),
                User(
                    id = 2,
                    name = "Bob",
                    active = false,
                    role = Role.USER,
                    tags = listOf("parser", "writer"),
                    address = null,
                    note = null
                )
            ),
            metadata = mapOf(
                "version" to "1.0.0",
                "format" to "TOON"
            ),
            scores = listOf(95.5, 88.0, 100.25)
        )

        val toon = KToonKotlinX.encodeToString(company)

        val decoded = KToonKotlinX.decodeFromString<Company>(toon)

        assertEquals(company, decoded)
    }

    @Test
    fun `kotlinx should preserve strings that look like primitives`() {
        @Serializable
        data class WeirdStrings(
            val nullString: String,
            val trueString: String,
            val falseString: String,
            val numberString: String,
            val version: String,
            val comma: String,
            val colon: String,
            val empty: String
        )

        val original = WeirdStrings(
            nullString = "null",
            trueString = "true",
            falseString = "false",
            numberString = "123",
            version = "1.0.0",
            comma = "a,b",
            colon = "a:b",
            empty = ""
        )

        val toon = KToonKotlinX.encodeToString(original)

        val decoded = KToonKotlinX.decodeFromString<WeirdStrings>(toon)

        assertEquals(original, decoded)
    }

    @Serializable
    data class UserX(
        val id: Int,
        val name: String,
        val active: Boolean,
        val role: String
    )

    @Serializable
    data class Dataset(
        val users: List<UserX>
    )

    @Test
    fun `compare json and toon output size from kotlinx object`() {
        val data = Dataset(
            users = List(100) {
                UserX(
                    id = it,
                    name = "User$it",
                    active = it % 2 == 0,
                    role = "member"
                )
            }
        )

        val json = Json.encodeToString(data)
        val toon = KToonKotlinX.encodeToString(data)

        val savingPercent =
            (1.0 - toon.length.toDouble() / json.length.toDouble()) * 100.0

        assertTrue(toon.length < json.length)
        assertTrue(savingPercent > 0.0)
    }
}
