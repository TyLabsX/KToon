package de.tylabsx

import de.tylabsx.ktoon.KToonNativeFormat
import de.tylabsx.ktoon.KToonParserEngine
import de.tylabsx.ktoon.kotlinx.streaming.ExperimentalKToonStreamingApi
import de.tylabsx.ktoon.kotlinx.streaming.KToonStreamingFormat
import de.tylabsx.ktoon.kotlinx.streaming.KToonStreamingMode
import de.tylabsx.ktoon.kotlinx.streaming.KToonStreamingOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalKToonStreamingApi::class)
class KToonEncodingBenchmarkTests {

    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        prettyPrint = false
    }

    private val iterations = 1_000
    private val streamingFast = KToonStreamingFormat(
        KToonStreamingOptions(mode = KToonStreamingMode.FAST)
    )
    private val streamingCompact = KToonStreamingFormat(
        KToonStreamingOptions(mode = KToonStreamingMode.COMPACT)
    )

    @Serializable
    data class UserDataset(
        val users: List<UserRecord>
    )

    @Serializable
    data class UserRecord(
        val id: Int,
        val username: String,
        val email: String,
        val active: Boolean,
        val role: String,
        val tags: List<String>
    )

    @Serializable
    data class NumericDataset(
        val samples: List<NumericSample>
    )

    @Serializable
    data class NumericSample(
        val id: Int,
        val temperature: Double,
        val pressure: Double,
        val humidity: Double,
        val valid: Boolean
    )

    @Serializable
    data class NestedDataset(
        val company: Company
    )

    @Serializable
    data class Company(
        val name: String,
        val departments: List<Department>
    )

    @Serializable
    data class Department(
        val id: Int,
        val name: String,
        val teams: List<Team>
    )

    @Serializable
    data class Team(
        val name: String,
        val members: List<Member>
    )

    @Serializable
    data class Member(
        val id: Int,
        val name: String,
        val title: String,
        val active: Boolean
    )

    @Test
    fun `benchmark numeric heavy encoding`() {
        val dataset = NumericDataset(
            samples = List(250) { index ->
                NumericSample(
                    id = index,
                    temperature = 20.0 + index * 0.1,
                    pressure = 1000.0 + index * 0.25,
                    humidity = 40.0 + index * 0.05,
                    valid = index % 3 != 0
                )
            }
        )

        benchmark("Numeric-heavy encoding", dataset)
    }

    @Test
    fun `benchmark realistic user encoding`() {
        val dataset = UserDataset(
            users = List(100) { index ->
                UserRecord(
                    id = index,
                    username = "user_$index",
                    email = "user_$index@example.com",
                    active = index % 2 == 0,
                    role = if (index % 5 == 0) "admin" else "member",
                    tags = listOf("kotlin", "toon", "benchmark")
                )
            }
        )

        benchmark("Realistic user encoding", dataset)
    }

    @Test
    fun `benchmark deeply nested encoding`() {
        val dataset = NestedDataset(
            company = Company(
                name = "TyLabsX",
                departments = List(5) { departmentIndex ->
                    Department(
                        id = departmentIndex,
                        name = "Department-$departmentIndex",
                        teams = List(4) { teamIndex ->
                            Team(
                                name = "Team-$departmentIndex-$teamIndex",
                                members = List(10) { memberIndex ->
                                    Member(
                                        id = departmentIndex * 1000 + teamIndex * 100 + memberIndex,
                                        name = "Member-$departmentIndex-$teamIndex-$memberIndex",
                                        title = if (memberIndex % 2 == 0) "Engineer" else "Designer",
                                        active = memberIndex % 3 != 0
                                    )
                                }
                            )
                        }
                    )
                }
            )
        )

        benchmark("Deeply nested encoding", dataset)
    }

    private inline fun <reified T> benchmark(name: String, value: T) {
        repeat(100) {
            json.encodeToString(value)
            KToonNativeFormat.encodeToString(value)
            streamingFast.encodeToString(value)
            streamingCompact.encodeToString(value)
        }

        val jsonOutput = json.encodeToString(value)
        val nativeOutput = KToonNativeFormat.encodeToString(value)
        val fastOutput = streamingFast.encodeToString(value)
        val compactOutput = streamingCompact.encodeToString(value)

        val jsonTime = measureNanoTime {
            repeat(iterations) {
                json.encodeToString(value)
            }
        }
        val nativeTime = measureNanoTime {
            repeat(iterations) {
                KToonNativeFormat.encodeToString(value)
            }
        }
        val fastTime = measureNanoTime {
            repeat(iterations) {
                streamingFast.encodeToString(value)
            }
        }
        val compactTime = measureNanoTime {
            repeat(iterations) {
                streamingCompact.encodeToString(value)
            }
        }

        println()
        println("============================================================")
        println("Encoding benchmark: $name")
        println("============================================================")
        println("JSON size: ${jsonOutput.length}")
        println("Native TOON size: ${nativeOutput.length}")
        println("Streaming FAST size: ${fastOutput.length}")
        println("Streaming COMPACT size: ${compactOutput.length}")
        println()
        println("JSON encode time: ${jsonTime / 1_000_000.0} ms")
        println("Native TOON encode time: ${nativeTime / 1_000_000.0} ms")
        println("Streaming FAST encode time: ${fastTime / 1_000_000.0} ms")
        println("Streaming COMPACT encode time: ${compactTime / 1_000_000.0} ms")
        println()
        println("Native savings vs JSON: ${"%.2f".format(savings(jsonOutput, nativeOutput))}%")
        println("Streaming FAST savings vs JSON: ${"%.2f".format(savings(jsonOutput, fastOutput))}%")
        println("Streaming COMPACT savings vs JSON: ${"%.2f".format(savings(jsonOutput, compactOutput))}%")
        println()
        println("============================================================")
        println()

        assertTrue(jsonOutput.isNotEmpty())
        assertTrue(nativeOutput.isNotEmpty())
        assertTrue(fastOutput.isNotEmpty())
        assertTrue(compactOutput.isNotEmpty())

        KToonParserEngine().parse(nativeOutput)
        KToonParserEngine().parse(fastOutput)
        KToonParserEngine().parse(compactOutput)
        assertEquals(value, KToonNativeFormat.decodeFromString<T>(nativeOutput))
        assertEquals(value, KToonNativeFormat.decodeFromString<T>(fastOutput))
        assertEquals(value, KToonNativeFormat.decodeFromString<T>(compactOutput))
    }

    private fun savings(jsonOutput: String, output: String): Double {
        return (1.0 - output.length.toDouble() / jsonOutput.length.toDouble()) * 100.0
    }
}
