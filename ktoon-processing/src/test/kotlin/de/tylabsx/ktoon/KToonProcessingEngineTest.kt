package de.tylabsx.ktoon

import de.tylabsx.ktoon.pipeline.*
import kotlin.test.*

/**
 * Comprehensive tests for KToonProcessingEngine.
 * 
 * This test class validates all aspects of TOON processing
 * including transformation, filtering, validation, conversion,
 * optimization, and querying capabilities.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonProcessingEngineTest {

    private val processingEngine = KToonProcessingEngine()

    @BeforeTest
    fun resetKToonConfigurationBeforeEachTest() {
        KToon.resetConfiguration()
    }

    @Test
    fun `transform should apply transformation rules`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice"),
                "age" to ToonNumber("25")
            )
        )

        val rules = TransformRules(
            listOf(
                TransformRule(
                    name = "uppercase-name",
                    condition = { it is ToonObject && it.entries.containsKey("name") },
                    action = { value ->
                        if (value is ToonObject) {
                            val nameValue = value.entries["name"] as ToonString
                            KToon.`object`(value.entries + ("name" to ToonString(nameValue.value.uppercase())))
                        } else {
                            value
                        }
                    }
                )
            ))

        val result = processingEngine.transform(value, rules)

        assertTrue(result.appliedRules.isNotEmpty())
        assertEquals(1, result.appliedRules.size)

        val transformedValue = result.transformedValue
        assertTrue(transformedValue is ToonObject)
        val transformedObj = transformedValue as ToonObject
        assertEquals("ALICE", (transformedObj.entries["name"] as ToonString).value)
    }

    @Test
    fun `filter should apply filter criteria`() {
        val value = KToon.`object`(
            mapOf(
                "users" to ToonArray(
                    listOf(
                        KToon.`object`(mapOf("name" to ToonString("Alice"))),
                        KToon.`object`(mapOf("name" to ToonString("Bob")))
                    )
                )
            )
        )

        val criteria = FilterCriteria(
            listOf(
                FilterCondition(
                    type = FilterType.PATH,
                    path = "users.*.name"
                )
            )
        )

        val result = processingEngine.filter(value, criteria)

        assertEquals(2, result.matchedCount)
        assertEquals(2, result.matchedPaths.size)
        assertEquals(2, result.matchedValues.size)

        val firstMatch = result.matchedValues[0]
        assertIs<ToonString>(firstMatch)
        assertEquals("Alice", firstMatch.value)

        val secondMatch = result.matchedValues[1]
        assertIs<ToonString>(secondMatch)
        assertEquals("Bob", secondMatch.value)
    }

    @Test
    fun `validate should check structure constraints`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice"),
                "age" to ToonNumber("25")
            )
        )

        val constraints = ValidationConstraints(
            requiredPaths = listOf("name", "age"),
            typeConstraints = mapOf("age" to "number")
        )

        val result = processingEngine.validate(value, constraints)

        assertTrue(result.isValid)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun `validate should detect missing required paths`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice")
            )
        )

        val constraints = ValidationConstraints(
            requiredPaths = listOf("name", "age")
        )

        val result = processingEngine.validate(value, constraints)

        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors.any { it.message?.contains("age") == true })
    }

    @Test
    fun `convert should convert to JSON`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice"),
                "age" to ToonNumber("25")
            )
        )

        val result = processingEngine.convert(value, TargetFormat.JSON)

        assertTrue(result.convertedData is String)
        val jsonString = result.convertedData as String
        assertTrue(jsonString.contains("\"name\""))
        assertTrue(jsonString.contains("\"Alice\""))
        assertTrue(jsonString.contains("\"age\""))
        assertTrue(jsonString.contains("25"))
        assertTrue(jsonString.contains("{"))
        assertTrue(jsonString.contains("}"))
    }

    @Test
    fun `convert should convert to XML`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice")
            )
        )

        val result = processingEngine.convert(value, TargetFormat.XML)

        assertTrue(result.convertedData is String)
        val xmlString = result.convertedData as String
        assertTrue(xmlString.contains("<?xml"))
        assertTrue(xmlString.contains("<root>"))
        assertTrue(xmlString.contains("<name>"))
        assertTrue(xmlString.contains("Alice"))
        assertTrue(xmlString.contains("</name>"))
        assertTrue(xmlString.contains("</root>"))
    }

    @Test
    fun `convert should convert to YAML`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice"),
                "age" to ToonNumber("25")
            )
        )

        val result = processingEngine.convert(value, TargetFormat.YAML)

        assertTrue(result.convertedData is String)
        val yamlString = result.convertedData as String
        assertTrue(yamlString.contains("name: Alice"))
        assertTrue(yamlString.contains("age: 25"))
    }

    @Test
    fun `optimize should apply tabular array optimization`() {
        val value = KToon.`object`(
            mapOf(
                "users" to ToonArray(
                    listOf(
                        KToon.`object`(mapOf("id" to 1, "name" to "Alice")),
                        KToon.`object`(mapOf("id" to 2, "name" to "Bob"))
                    )
                )
            )
        )

        val result = processingEngine.optimize(value, OptimizationLevel.MEDIUM)

        assertNotNull(result.optimizedValue)
        assertTrue(result.statistics.optimizedSize <= result.statistics.originalSize)
        assertTrue(result.statistics.compressionRatio >= 0.0)
    }

    @Test
    fun `query should find values by path`() {
        val value = KToon.`object`(
            mapOf(
                "user" to KToon.`object`(
                    mapOf(
                        "name" to ToonString("Alice"),
                        "profile" to KToon.`object`(
                            mapOf(
                                "email" to ToonString("alice@example.com")
                            )
                        )
                    )
                )
            )
        )

        val result = processingEngine.query(value, "user.profile.email")

        assertEquals(1, result.results.size)

        val match = result.results[0]
        assertEquals("alice@example.com", (match.value as ToonString).value)
        assertEquals("user.profile.email", match.path)
    }

    @Test
    fun `merge should combine multiple values`() {
        val values = listOf(
            KToon.`object`(mapOf("name" to ToonString("Alice"))),
            KToon.`object`(mapOf("name" to ToonString("Bob")))
        )

        val result = processingEngine.merge(values, MergeStrategy.DEEP)

        assertTrue(result.mergedValue is ToonObject)
        val mergedObj = result.mergedValue as ToonObject
        assertEquals(1, mergedObj.entries.size)
        // Deep merge should keep both values, so this might need adjustment based on implementation
    }

    @Test
    fun `optimize should handle different levels`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice")
            )
        )

        val noneResult = processingEngine.optimize(value, OptimizationLevel.NONE)
        val lowResult = processingEngine.optimize(value, OptimizationLevel.LOW)
        val mediumResult = processingEngine.optimize(value, OptimizationLevel.MEDIUM)
        val highResult = processingEngine.optimize(value, OptimizationLevel.HIGH)
        val aggressiveResult = processingEngine.optimize(value, OptimizationLevel.AGGRESSIVE)

        // All should return the same value for NONE optimization
        assertEquals(value, noneResult.optimizedValue)

        // Higher optimization levels should potentially reduce size or improve structure
        assertTrue(lowResult.statistics.optimizedSize <= value.toString().length)
        assertTrue(mediumResult.statistics.optimizedSize <= lowResult.statistics.optimizedSize)
        assertTrue(highResult.statistics.optimizedSize <= mediumResult.statistics.optimizedSize)
        assertTrue(aggressiveResult.statistics.optimizedSize <= highResult.statistics.optimizedSize)
    }
}
