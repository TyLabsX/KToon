@file:Suppress("DEPRECATION")

package de.tylabsx.ktoon

import de.tylabsx.ktoon.kotlinx.bridge.KToonKotlinX
import de.tylabsx.ktoon.kotlinx.native.KToonNativeFormat
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
class KToonNativeSerializationTest {

    @Serializable
    data class NativeUser(
        val id: Int,
        val name: String,
        val active: Boolean
    )

    @Serializable
    data class NativeAddress(
        val city: String,
        val zip: Int
    )

    @Serializable
    data class NativeProfile(
        val user: NativeUser,
        val address: NativeAddress
    )

    @Serializable
    data class NativeListBox(
        val tags: List<String>,
        val scores: List<Int>
    )

    @Serializable
    data class NativeMapBox(
        val metadata: Map<String, String>,
        val counts: Map<String, Int>
    )

    @Serializable
    data class NativeNullableBox(
        val name: String?,
        val description: String?,
        val count: Int?
    )

    data class ReflectiveUser(
        val id: Int,
        val name: String
    )

    @Test
    fun `native kotlinx should encode and decode simple data class`() {
        val user = NativeUser(id = 1, name = "Alice", active = true)

        val toon = KToonNativeFormat.encodeToString(user)
        val decoded = KToonNativeFormat.decodeFromString<NativeUser>(toon)

        assertTrue(toon.contains("id: 1"))
        assertTrue(toon.contains("name: Alice"))
        assertTrue(toon.contains("active: true"))
        assertEquals(user, decoded)
    }

    @Test
    fun `native kotlinx should encode and decode nested data class`() {
        val profile = NativeProfile(
            user = NativeUser(id = 2, name = "Bob", active = false),
            address = NativeAddress(city = "Berlin", zip = 10115)
        )

        val toon = KToonNativeFormat.encodeToString(profile)
        val decoded = KToonNativeFormat.decodeFromString<NativeProfile>(toon)

        assertEquals(profile, decoded)
    }

    @Test
    fun `native kotlinx should encode and decode lists`() {
        val box = NativeListBox(
            tags = listOf("kotlin", "toon", "native"),
            scores = listOf(1, 2, 3)
        )

        val toon = KToonNativeFormat.encodeToString(box)
        val decoded = KToonNativeFormat.decodeFromString<NativeListBox>(toon)

        assertEquals(box, decoded)
    }

    @Test
    fun `native kotlinx should encode and decode maps`() {
        val box = NativeMapBox(
            metadata = mapOf("format" to "TOON", "module" to "ktoon-kotlinx"),
            counts = mapOf("parser" to 1, "writer" to 2)
        )

        val toon = KToonNativeFormat.encodeToString(box)
        val decoded = KToonNativeFormat.decodeFromString<NativeMapBox>(toon)

        assertEquals(box, decoded)
    }

    @Test
    fun `native kotlinx should encode and decode nullable values`() {
        val box = NativeNullableBox(
            name = null,
            description = "present",
            count = null
        )

        val toon = KToonNativeFormat.encodeToString(box)
        val decoded = KToonNativeFormat.decodeFromString<NativeNullableBox>(toon)

        assertTrue(toon.contains("name: null"))
        assertTrue(toon.contains("count: null"))
        assertEquals(box, decoded)
    }

    @Test
    fun `bridge and native should decode to same Kotlin object`() {
        val profile = NativeProfile(
            user = NativeUser(id = 3, name = "Cara", active = true),
            address = NativeAddress(city = "Hamburg", zip = 20095)
        )

        val bridgeToon = KToonKotlinX.encodeToString(profile)
        val nativeToon = KToonNativeFormat.encodeToString(profile)

        val decodedFromBridge = KToonNativeFormat.decodeFromString<NativeProfile>(bridgeToon)
        val decodedFromNative = KToonKotlinX.decodeFromString<NativeProfile>(nativeToon)

        assertEquals(profile, decodedFromBridge)
        assertEquals(profile, decodedFromNative)
        assertEquals(decodedFromBridge, decodedFromNative)
    }

    @Test
    fun `KToon public API should use native kotlinx overloads`() {
        val user = NativeUser(id = 4, name = "Dora", active = true)

        val toon = KToon.encodeToString(NativeUser.serializer(), user)
        val decoded = KToon.decodeFromString(NativeUser.serializer(), toon)

        assertEquals(user, decoded)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated reflection API should continue to work`() {
        val user = ReflectiveUser(id = 5, name = "Eve")

        val reflectiveToon = KToon.encodeReflectiveToString(user)
        val decoded = KToon.decodeReflectiveFromString<ReflectiveUser>(reflectiveToon)
        val legacyValue = KToon.encode(user)
        val legacyDecoded = KToon.decode<ReflectiveUser>(legacyValue)

        assertEquals(user, decoded)
        assertEquals(user, legacyDecoded)
    }
}
