package de.tylabsx.ktoon

import de.tylabsx.ktoon.pipeline.TransformRules
import kotlin.test.*

/**
 * Comprehensive tests for KToonEngine.
 * 
 * This test class validates the central orchestration engine,
 * ensuring it properly coordinates all subsystems and provides
 * unified access to parsing, processing, and writing operations.
 * 
 * @since 1.0.0
 * @author TyLabsX
 */
class KToonEngineTest {

    @BeforeTest
    fun resetKToonConfigurationBeforeEachTest() {
        KToon.resetConfiguration()
    }

    private val engine = KToonEngine()

    @Test
    fun `engine should coordinate parsing operations`() {
        val input = "name: Alice"
        val result = engine.parse(input)

        assertNotNull(result.toonValue)
        assertNotNull(result.classifiedLines)
        assertNotNull(result.indentationStructure)
        assertNotNull(result.lines)
        assertNotNull(result.normalizedInput)
    }

    @Test
    fun `engine should coordinate processing operations`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice")
            )
        )

        val rules = de.tylabsx.ktoon.pipeline.TransformRules(
            listOf(
                de.tylabsx.ktoon.pipeline.TransformRule(
                    name = "test-rule",
                    condition = { it is ToonObject },
                    action = { it }
                )
            ))

        val result = engine.transform(value, rules)

        assertNotNull(result.transformedValue)
        assertNotNull(result.appliedRules)
        assertNotNull(result.originalValue)
        assertTrue(result.appliedRules.isNotEmpty())
        assertEquals(1, result.appliedRules.size)
    }

    @Test
    fun `engine should coordinate writing operations`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice")
            )
        )

        val result = engine.stringify(value)

        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("name: Alice"))
    }

    @Test
    fun `engine should coordinate filtering operations`() {
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

        val criteria = de.tylabsx.ktoon.pipeline.FilterCriteria(
            listOf(
                de.tylabsx.ktoon.pipeline.FilterCondition(
                    type = de.tylabsx.ktoon.pipeline.FilterType.PATH,
                    path = "users.*.name"
                )
            )
        )

        val result = engine.filter(value, criteria)

        assertEquals(2, result.matchedCount)
        assertEquals(2, result.matchedPaths.size)
        assertEquals(2, result.matchedValues.size)
    }

    @Test
    fun `engine should coordinate conversion operations`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice")
            )
        )

        val result = engine.convert(value, de.tylabsx.ktoon.pipeline.TargetFormat.JSON)

        assertNotNull(result.convertedData)
        assertEquals(de.tylabsx.ktoon.pipeline.TargetFormat.JSON, result.targetFormat)
        assertTrue(result.convertedData is String)

        val jsonString = result.convertedData as String
        assertTrue(jsonString.contains("\"name\""))
        assertTrue(jsonString.contains("\"Alice\""))
    }

    @Test
    fun `engine should coordinate optimization operations`() {
        val value = KToon.`object`(
            mapOf(
                "name" to ToonString("Alice"),
                "description" to ToonString("This is a very long description that could be optimized")
            )
        )

        val result = engine.optimize(value, de.tylabsx.ktoon.pipeline.OptimizationLevel.MEDIUM)

        assertNotNull(result.optimizedValue)
        assertTrue(result.statistics.originalSize > 0)
        assertTrue(result.statistics.optimizedSize > 0)
        assertTrue(result.statistics.compressionRatio >= 0.0)
    }

    @Test
    fun `engine should coordinate query operations`() {
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

        val result = engine.query(value, "users.*.name")

        assertEquals(2, result.results.size)
        assertEquals(2, result.results.count { it.path.contains("name") })
    }

    @Test
    fun `engine should coordinate merge operations`() {
        val values = listOf(
            KToon.`object`(mapOf("name" to ToonString("Alice"))),
            KToon.`object`(mapOf("name" to ToonString("Bob")))
        )

        val result = engine.merge(values, de.tylabsx.ktoon.pipeline.MergeStrategy.DEEP)

        assertNotNull(result.mergedValue)
        assertTrue(result.mergedValue is ToonObject)
        assertEquals(2, result.originalValues.size)
    }

    @Test
    fun `engine should coordinate roundtrip operations`() {
        val input = """
          user:
            id: 123
            name: "Alice"
            active: true
        """.trimIndent()

        val result = engine.roundtrip(input)

        assertNotNull(result.originalInput)
        assertNotNull(result.parseResult)
        assertNotNull(result.writerResult)
        assertNotNull(result.finalOutput)
        assertTrue(result.totalTime >= 0)
        assertTrue(result.finalOutput.contains("user:"))
        assertTrue(result.finalOutput.contains("id: 123"))
        assertTrue(result.finalOutput.contains("name: Alice"))
        assertTrue(result.finalOutput.contains("active: true"))
    }

    @Test
    fun `engine should provide comprehensive statistics`() {
        val stats = engine.getEngineStatistics()

        assertNotNull(stats)
        assertTrue(stats.supportedOperations.isNotEmpty())
        assertTrue(stats.supportedFormats.isNotEmpty())
        assertTrue(stats.totalPhases > 0)
        assertTrue(stats.contextConfiguration.isNotEmpty())
    }

    @Test
    fun `engine should handle configuration changes`() {
        // Reset to defaults
        engine.resetConfiguration()

        // Configure custom settings
        engine.configure {
            indentationSize = 4
            strictMode = true
            enableDebugLogging = false
        }

        // Verify configuration was applied
        val config = engine.getEngineStatistics().contextConfiguration
        assertTrue(config.contains("indentationSize: 4"))
        assertTrue(config.contains("strictMode: true"))
        assertTrue(config.contains("enableDebugLogging: false"))
    }

    @Test
    fun `engine should handle errors gracefully`() {
        val invalidInput = "name Alice"  // Missing colon

        try {
            engine.parse(invalidInput)
            assertTrue(false) // Should not reach here
        } catch (e: ToonParseException) {
            assertTrue(e.message?.isNotEmpty() == true)
            assertTrue(e.line > 0)
        }
    }

    @Test
    fun `engine should coordinate all subsystems`() {
        // Test that all subsystems are working together
        val input = "name: Alice"
        val parseResult = engine.parse(input)
        val transformResult = engine.transform(parseResult.toonValue, TransformRules(emptyList()))
        val writeResult = engine.stringify(transformResult.transformedValue)

        assertNotNull(parseResult)
        assertNotNull(transformResult)
        assertNotNull(writeResult)

        // Verify consistency across operations
        val originalName = when (val name = parseResult.toonValue) {
            is ToonObject -> (name.entries["name"] as ToonString).value
            else -> null
        }

        val transformedName = when (val name = transformResult.transformedValue) {
            is ToonObject -> (name.entries["name"] as ToonString).value
            else -> null
        }

        val writtenName = when (val parsed = KToon.parse(writeResult)) {
            is ToonObject -> (parsed.entries["name"] as ToonString).value
            else -> null
        }

        assertEquals(originalName, transformedName)
        assertEquals(transformedName, writtenName)
    }
}
