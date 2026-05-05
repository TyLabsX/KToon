package de.tylabsx.ktoon.kotlinx.streaming

import de.tylabsx.ktoon.KToonNativeFormat
import de.tylabsx.ktoon.KToonParserEngine
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalKToonStreamingApi::class)
class KToonStreamingFormatTest {

    @Serializable
    data class Tags(val tags: List<String>)

    @Serializable
    data class Samples(val samples: List<Sample>)

    @Serializable
    data class Sample(
        val id: Int,
        val temperature: Double,
        val pressure: Double,
        val valid: Boolean
    )

    @Serializable
    data class Users(val users: List<User>)

    @Serializable
    data class User(val id: Int, val name: String, val active: Boolean)

    @Serializable
    data class UsersWithProfile(val users: List<UserWithProfile>)

    @Serializable
    data class UserWithProfile(val id: Int, val profile: Profile)

    @Serializable
    data class Profile(val city: String)

    @Serializable
    data class UsersWithTags(val users: List<UserWithTags>)

    @Serializable
    data class UserWithTags(val id: Int, val tags: List<String>)

    @Serializable
    data class NullableValues(val name: String?, val count: Int?)

    @Serializable
    data class Metadata(val values: Map<String, String>, val counts: Map<Int, String>)

    @Serializable
    data class StructuredMap(val values: Map<MapKey, String>)

    @Serializable
    data class MapKey(val id: Int)

    @Serializable
    enum class Role {
        ADMIN,
        USER
    }

    @Serializable
    data class RoleBox(val role: Role)

    @Serializable
    data class TrickyStrings(
        val nullString: String,
        val trueString: String,
        val falseString: String,
        val numberString: String,
        val version: String,
        val comma: String,
        val colon: String,
        val empty: String,
        val quoted: String,
        val path: String
    )

    @Test
    fun `FAST mode should encode primitive list inline`() {
        val output = fast().encodeToString(Tags(listOf("kotlin", "toon", "llm")))

        assertTrue(output.contains("tags[3]: kotlin,toon,llm"))
        assertEquals(Tags(listOf("kotlin", "toon", "llm")), KToonNativeFormat.decodeFromString<Tags>(output))
    }

    @Test
    fun `FAST mode should encode object list as block array`() {
        val value = Users(listOf(User(1, "Alice", true), User(2, "Bob", false)))

        val output = fast().encodeToString(value)

        assertTrue(output.contains("users[2]:"))
        assertTrue(output.contains("- id: 1"))
        assertTrue(output.contains("name: Alice"))
        assertFalse(output.contains("users[2]{id,name,active}:"))
        assertEquals(value, KToonNativeFormat.decodeFromString<Users>(output))
    }

    @Test
    fun `COMPACT mode should encode object list as tabular array`() {
        val value = Users(listOf(User(1, "Alice", true), User(2, "Bob", false)))

        val output = compact().encodeToString(value)

        assertTrue(output.contains("users[2]{id,name,active}:"))
        assertTrue(output.contains("  1,Alice,true"))
        assertTrue(output.contains("  2,Bob,false"))
        assertEquals(value, KToonNativeFormat.decodeFromString<Users>(output))
    }

    @Test
    fun `COMPACT mode should write tabular object array without raw block parsing`() {
        val value = Samples(
            listOf(
                Sample(1, 20.0, 1000.0, true),
                Sample(2, 21.0, 1001.0, false)
            )
        )

        val output = compact().encodeToString(value)

        assertTrue(
            output.contains(
                """
                samples[2]{id,temperature,pressure,valid}:
                  1,20.0,1000.0,true
                  2,21.0,1001.0,false
                """.trimIndent()
            )
        )
        assertEquals(value, KToonNativeFormat.decodeFromString<Samples>(output))
    }

    @Test
    fun `COMPACT mode should preserve field order in tabular rows`() {
        val value = Samples(listOf(Sample(1, 20.0, 1000.0, true)))

        val output = compact().encodeToString(value)

        assertTrue(output.contains("samples[1]{id,temperature,pressure,valid}:"))
        assertFalse(output.contains("samples[1]{temperature,id,pressure,valid}:"))
    }

    @Test
    fun `COMPACT mode should fallback to block array for nested object values`() {
        val value = UsersWithProfile(
            listOf(
                UserWithProfile(1, Profile("Berlin")),
                UserWithProfile(2, Profile("Hamburg"))
            )
        )

        val output = compact().encodeToString(value)

        assertFalse(output.contains("users[2]{id,profile}:"))
        assertTrue(output.contains("users[2]:"))
        assertTrue(output.contains("- id: 1"))
        assertTrue(output.contains("profile:"))
        assertEquals(value, KToonNativeFormat.decodeFromString<UsersWithProfile>(output))
    }

    @Test
    fun `COMPACT mode should fallback to block array for nested list values`() {
        val value = UsersWithTags(
            listOf(
                UserWithTags(1, listOf("kotlin", "toon")),
                UserWithTags(2, listOf("llm", "format"))
            )
        )

        val output = compact().encodeToString(value)

        assertFalse(output.contains("users[2]{id,tags}:"))
        assertTrue(output.contains("users[2]:"))
        assertTrue(output.contains("- id: 1"))
        assertTrue(output.contains("tags[2]: kotlin,toon"))
        assertEquals(value, KToonNativeFormat.decodeFromString<UsersWithTags>(output))
    }

    @Test
    fun `streaming output should be parseable`() {
        val output = compact().encodeToString(Users(listOf(User(1, "Alice", true), User(2, "Bob", false))))

        KToonParserEngine().parse(output)
    }

    @Test
    fun `streaming compact output should decode through KToonNativeFormat`() {
        val value = Users(listOf(User(1, "Alice", true), User(2, "Bob", false)))

        val output = compact().encodeToString(value)
        val decoded = KToonNativeFormat.decodeFromString<Users>(output)

        assertEquals(value, decoded)
    }

    @Test
    fun `nullable values should encode correctly`() {
        val value = NullableValues(name = null, count = null)

        val output = compact().encodeToString(value)

        assertTrue(output.contains("name: null"))
        assertTrue(output.contains("count: null"))
        assertEquals(value, KToonNativeFormat.decodeFromString<NullableValues>(output))
    }

    @Test
    fun `tricky strings should be quoted correctly`() {
        val value = TrickyStrings(
            nullString = "null",
            trueString = "true",
            falseString = "false",
            numberString = "123",
            version = "1.0.0",
            comma = "a,b",
            colon = "a:b",
            empty = "",
            quoted = "hello \"world\"",
            path = "C:\\Users\\Admin"
        )

        val output = compact().encodeToString(value)

        assertTrue(output.contains("nullString: \"null\""))
        assertTrue(output.contains("trueString: \"true\""))
        assertTrue(output.contains("falseString: \"false\""))
        assertTrue(output.contains("numberString: \"123\""))
        assertTrue(output.contains("version: \"1.0.0\""))
        assertTrue(output.contains("comma: \"a,b\""))
        assertTrue(output.contains("colon: \"a:b\""))
        assertTrue(output.contains("empty: \"\""))
        assertTrue(output.contains("quoted: \"hello \\\"world\\\"\""))
        assertTrue(output.contains("path: \"C:\\\\Users\\\\Admin\""))
        assertEquals(value, KToonNativeFormat.decodeFromString<TrickyStrings>(output))
    }

    @Test
    fun `map with primitive keys should encode correctly`() {
        val value = Metadata(
            values = mapOf("format" to "TOON", "module" to "ktoon-kotlinx"),
            counts = mapOf(1 to "one", 2 to "two")
        )

        val output = compact().encodeToString(value)

        assertTrue(output.contains("values:"))
        assertTrue(output.contains("format: TOON"))
        assertTrue(output.contains("counts:"))
        assertTrue(output.contains("1: one"))
        assertEquals(value, KToonNativeFormat.decodeFromString<Metadata>(output))
    }

    @Test
    fun `structured map keys should fail`() {
        val value = StructuredMap(mapOf(MapKey(1) to "one"))

        val error = assertFailsWith<KToonStreamingSerializationException> {
            compact().encodeToString(value)
        }

        assertTrue(error.message.orEmpty().contains("Structured TOON map keys are not supported"))
    }

    @Test
    fun `enum values should encode as names`() {
        val value = RoleBox(Role.ADMIN)

        val output = compact().encodeToString(value)

        assertEquals("role: ADMIN", output)
        assertEquals(value, KToonNativeFormat.decodeFromString<RoleBox>(output))
    }

    private fun fast(): KToonStreamingFormat {
        return KToonStreamingFormat(KToonStreamingOptions(mode = KToonStreamingMode.FAST))
    }

    private fun compact(): KToonStreamingFormat {
        return KToonStreamingFormat(KToonStreamingOptions(mode = KToonStreamingMode.COMPACT))
    }
}
