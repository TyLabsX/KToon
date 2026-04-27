package de.tylabsx.ktoon

import kotlin.test.*

class KToonCodecTest {

    enum class Role {
        ADMIN,
        USER,
        GUEST
    }

    data class Address(
        val street: String,
        val city: String,
        val zip: Int
    )

    data class User(
        val id: Int,
        val name: String,
        val active: Boolean,
        val role: Role,
        val tags: List<String>,
        val address: Address?
    )

    data class Company(
        val name: String,
        val users: List<User>,
        val metadata: Map<String, String>,
        val scores: List<Double>,
        val nullableValue: String?
    )

    data class Defaults(
        val name: String,
        val active: Boolean = true,
        val retries: Int = 3
    )

    data class Member(
        val name: String,
        val age: Int
    )

    data class Team(
        val members: List<Member>
    )

    private val encoder = KToonEncoder()
    private val decoder = KToonDecoder()
    private val parser = KToonParserEngine()
    private val writer = KToonWriterEngine()

    private fun stringify(value: ToonValue): String {
        return writer.stringify(value).serializedData
    }

    private fun parse(input: String): ToonValue {
        return parser.parse(input).toonValue
    }

    private inline fun <reified T : Any> encodeToString(value: T): String {
        return stringify(encoder.encode(value))
    }

    private inline fun <reified T : Any> decodeFromString(input: String): T {
        return decoder.decode<T>(parse(input))
    }

    @Test
    fun `encoder should encode primitive values`() {
        assertEquals(ToonString("Alice"), encoder.encode("Alice"))
        assertEquals(ToonNumber("123"), encoder.encode(123))
        assertEquals(ToonNumber("95.5"), encoder.encode(95.5))
        assertEquals(ToonBoolean(true), encoder.encode(true))
        assertEquals(ToonNull, encoder.encode(null))
    }

    @Test
    fun `encoder should encode lists arrays and maps`() {
        val value = mapOf(
            "name" to "Alice",
            "age" to 25,
            "active" to true,
            "tags" to listOf("admin", "user"),
            "numbers" to intArrayOf(1, 2, 3)
        )

        val encoded = encoder.encode(value)

        assertIs<ToonObject>(encoded)
        assertEquals(ToonString("Alice"), encoded.entries["name"])
        assertEquals(ToonNumber("25"), encoded.entries["age"])
        assertEquals(ToonBoolean(true), encoded.entries["active"])

        val tags = encoded.entries["tags"]
        assertIs<ToonArray>(tags)
        assertEquals(2, tags.values.size)
        assertEquals(ToonString("admin"), tags.values[0])

        val numbers = encoded.entries["numbers"]
        assertIs<ToonArray>(numbers)
        assertEquals(listOf(ToonNumber("1"), ToonNumber("2"), ToonNumber("3")), numbers.values)
    }

    @Test
    fun `encoder should encode nested data class`() {
        val user = User(
            id = 1,
            name = "Alice",
            active = true,
            role = Role.ADMIN,
            tags = listOf("kotlin", "toon"),
            address = Address(
                street = "Main Street",
                city = "Berlin",
                zip = 10115
            )
        )

        val encoded = encoder.encode(user)

        assertIs<ToonObject>(encoded)
        assertEquals(ToonNumber("1"), encoded.entries["id"])
        assertEquals(ToonString("Alice"), encoded.entries["name"])
        assertEquals(ToonBoolean(true), encoded.entries["active"])
        assertEquals(ToonString("ADMIN"), encoded.entries["role"])

        val address = encoded.entries["address"]
        assertIs<ToonObject>(address)
        assertEquals(ToonString("Berlin"), address.entries["city"])
        assertEquals(ToonNumber("10115"), address.entries["zip"])
    }

    @Test
    fun `decoder should decode simple data class`() {
        data class SimpleUser(
            val id: Int,
            val name: String,
            val active: Boolean
        )

        val value = ToonObject(
            mapOf(
                "id" to ToonNumber("123"),
                "name" to ToonString("Alice"),
                "active" to ToonBoolean(true)
            )
        )

        val decoded = decoder.decode<SimpleUser>(value)

        assertEquals(123, decoded.id)
        assertEquals("Alice", decoded.name)
        assertEquals(true, decoded.active)
    }

    @Test
    fun `decoder should decode nested data class with enum and nullable`() {
        val value = ToonObject(
            mapOf(
                "id" to ToonNumber("7"),
                "name" to ToonString("Bob"),
                "active" to ToonBoolean(false),
                "role" to ToonString("USER"),
                "tags" to ToonArray(
                    listOf(
                        ToonString("backend"),
                        ToonString("llm")
                    )
                ),
                "address" to ToonObject(
                    mapOf(
                        "street" to ToonString("Second Street"),
                        "city" to ToonString("Hamburg"),
                        "zip" to ToonNumber("20095")
                    )
                )
            )
        )

        val decoded = decoder.decode<User>(value)

        assertEquals(7, decoded.id)
        assertEquals("Bob", decoded.name)
        assertFalse(decoded.active)
        assertEquals(Role.USER, decoded.role)
        assertEquals(listOf("backend", "llm"), decoded.tags)
        assertNotNull(decoded.address)
        assertEquals("Hamburg", decoded.address.city)
    }

    @Test
    fun `decoder should decode null into nullable property`() {
        val value = ToonObject(
            mapOf(
                "id" to ToonNumber("1"),
                "name" to ToonString("Alice"),
                "active" to ToonBoolean(true),
                "role" to ToonString("GUEST"),
                "tags" to ToonArray(emptyList()),
                "address" to ToonNull
            )
        )

        val decoded = decoder.decode<User>(value)

        assertNull(decoded.address)
    }

    @Test
    fun `decoder should use default constructor values when property is missing`() {
        val value = ToonObject(
            mapOf(
                "name" to ToonString("Alice")
            )
        )

        val decoded = decoder.decode<Defaults>(value)

        assertEquals("Alice", decoded.name)
        assertTrue(decoded.active)
        assertEquals(3, decoded.retries)
    }

    @Test
    fun `decoder should fail when required property is missing`() {
        data class RequiredUser(
            val id: Int,
            val name: String
        )

        val value = ToonObject(
            mapOf(
                "id" to ToonNumber("1")
            )
        )

        assertFailsWith<KToonCodecException> {
            decoder.decode<RequiredUser>(value)
        }
    }

    @Test
    fun `decoder should fail when null is assigned to non nullable property`() {
        data class RequiredName(
            val name: String
        )

        val value = ToonObject(
            mapOf(
                "name" to ToonNull
            )
        )

        assertFailsWith<KToonCodecException> {
            decoder.decode<RequiredName>(value)
        }
    }

    @Test
    fun `codec should roundtrip complex object`() {
        val company = Company(
            name = "TyLabsX",
            users = listOf(
                User(
                    id = 1,
                    name = "Alice",
                    active = true,
                    role = Role.ADMIN,
                    tags = listOf("core", "kotlin"),
                    address = Address("Main Street", "Berlin", 10115)
                ),
                User(
                    id = 2,
                    name = "Bob",
                    active = false,
                    role = Role.USER,
                    tags = listOf("parser", "writer"),
                    address = null
                )
            ),
            metadata = mapOf(
                "version" to "1.0.0",
                "format" to "TOON",
                "owner" to "TyLabsX"
            ),
            scores = listOf(95.5, 88.0, 100.25),
            nullableValue = null
        )

        val encoded = encoder.encode(company)
        val toon = stringify(encoded)
        val parsed = parse(toon)
        val decoded = decoder.decode<Company>(parsed)

        assertEquals(company, decoded)
    }

    @Test
    fun `codec should handle strings that look like primitives`() {
        data class WeirdStrings(
            val nullString: String,
            val trueString: String,
            val falseString: String,
            val numberString: String,
            val version: String,
            val colon: String,
            val comma: String,
            val empty: String
        )

        val original = WeirdStrings(
            nullString = "null",
            trueString = "true",
            falseString = "false",
            numberString = "123",
            version = "1.0.0",
            colon = "a:b",
            comma = "a,b",
            empty = ""
        )

        val toon = encodeToString(original)
        val decoded = decodeFromString<WeirdStrings>(toon)

        assertEquals(original, decoded)
    }

    @Test
    fun `codec should handle deeply nested object`() {
        data class Level5(val value: String)
        data class Level4(val level5: Level5)
        data class Level3(val level4: Level4)
        data class Level2(val level3: Level3)
        data class Level1(val level2: Level2)
        data class Root(val level1: Level1)

        val original = Root(
            Level1(
                Level2(
                    Level3(
                        Level4(
                            Level5("deep")
                        )
                    )
                )
            )
        )

        val toon = encodeToString(original)
        val decoded = decodeFromString<Root>(toon)

        assertEquals(original, decoded)
    }

    @Test
    fun `decoder should decode list of objects`() {
        val value = ToonObject(
            mapOf(
                "members" to ToonArray(
                    listOf(
                        ToonObject(
                            mapOf(
                                "name" to ToonString("Alice"),
                                "age" to ToonNumber("25")
                            )
                        ),
                        ToonObject(
                            mapOf(
                                "name" to ToonString("Bob"),
                                "age" to ToonNumber("30")
                            )
                        )
                    )
                )
            )
        )

        val decoded = decoder.decode<Team>(value)

        assertEquals(2, decoded.members.size)
        assertEquals("Alice", decoded.members[0].name)
        assertEquals(30, decoded.members[1].age)
    }

    @Test
    fun `decoder should decode map values`() {
        data class MetadataHolder(
            val metadata: Map<String, String>
        )

        val value = ToonObject(
            mapOf(
                "metadata" to ToonObject(
                    mapOf(
                        "version" to ToonString("1.0.0"),
                        "format" to ToonString("TOON")
                    )
                )
            )
        )

        val decoded = decoder.decode<MetadataHolder>(value)

        assertEquals("1.0.0", decoded.metadata["version"])
        assertEquals("TOON", decoded.metadata["format"])
    }

    @Test
    fun `decoder should fail on invalid enum value`() {
        data class Account(
            val role: Role
        )

        val value = ToonObject(
            mapOf(
                "role" to ToonString("SUPER_ADMIN")
            )
        )

        assertFailsWith<KToonCodecException> {
            decoder.decode<Account>(value)
        }
    }

    @Test
    fun `codec should encode and decode object without public KToon facade`() {
        data class ApiUser(
            val id: Int,
            val name: String,
            val active: Boolean
        )

        val original = ApiUser(
            id = 99,
            name = "Public API",
            active = true
        )

        val toon = encodeToString(original)
        val decoded = decodeFromString<ApiUser>(toon)

        assertEquals(original, decoded)
    }
}