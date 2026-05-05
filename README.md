# KToon

KToon is a modern, Kotlin-first implementation of the TOON (Token-Oriented Object Notation) format.  
It provides a complete toolchain for parsing, writing and processing TOON data with a strong focus on correctness, performance and developer experience.

---

## Features

- Full TOON v2.1 parser and writer
- Strict parsing with deterministic behavior
- Complete roundtrip support (parse → modify → stringify)
- Kotlin-first API design
- Query system with path and wildcard support
- Processing engine for optimization, filtering and validation
- Support for nested objects, arrays and tabular arrays

---

## Installation

### Gradle (Kotlin DSL) (SOON)

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("de.tylabsx:ktoon:1.0.0")
}
```

---

## Basic Usage

### Parse TOON

```kotlin
val input = """
    user:
      id: 123
      name: "Alice"
""".trimIndent()

val parsed = KToon.parse(input)
```

---

### Access Data

```kotlin
val user = parsed.entries["user"] as ToonObject
val name = (user.entries["name"] as ToonString).value
```

---

### Create Data

```kotlin
val value = KToon.`object`(
    mapOf(
        "name" to "Alice",
        "age" to 25,
        "active" to true
    )
)
```

---

### Stringify

```kotlin
val output = KToon.stringify(value)
println(output)
```

Example output:

```toon
name: Alice
age: 25
active: true
```

---

## KotlinX Serialization Support

```kotlin
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

        println()
        println("---- KOTLINX COMPLEX TOON ----")
        println(toon)

        val decoded = KToonKotlinX.decodeFromString<Company>(toon)

        assertEquals(company, decoded)
    }
```

Example Result:

```text
---- KOTLINX COMPLEX TOON ----
name: TyLabsX
users[2]:
  - id: 1
    name: Alice
    active: true
    role: ADMIN
    tags[2]: core,kotlin
    address:
      city: Berlin
      zip: 10115
    note: hello
  - id: 2
    name: Bob
    active: false
    role: USER
    tags[2]: parser,writer
    address: null
    note: null
metadata:
  version: 1.0.0
  format: TOON
scores[3]: 95.5,88.0,100.25
```

---

## Query System

```kotlin
val result = KToon.query(value, "users.*.name")

result.results.forEach {
    println("${it.path} -> ${it.value}")
}
```

Example result:

```text
users[0].name -> Alice
users[1].name -> Bob
```

---

## Roundtrip

```kotlin
val result = KToon.roundtrip(input)
println(result.finalOutput)
```

---

## Processing

### Optimize

```kotlin
val optimized = KToon.optimize(value)
```

### Filter

```kotlin
val result = KToon.filter(value, criteria)
```

### Validate

```kotlin
val result = KToon.validate(value, constraints)
```

---

## TOON Example

```toon
app:
  name: KToon
  version: "1.0.0"

  users[2]{id,name}:
    1,Alice
    2,Bob

  features[3]: fast,compact,llm
```

---

## Benchmark Notes

These benchmarks are intended as practical reference measurements, not as a replacement for JMH-based microbenchmarks.

Results depend heavily on data shape. KToon performs especially well on numeric and table-like datasets where tabular arrays can be used. JSON may be smaller or faster for deeply object-heavy or string-heavy structures.

| Dataset | JSON Size | KToon Size | Size Change | JSON Time | Streaming FAST Time |
|---|---:|---:|---:|---:|---:|
| Numeric-heavy | 19,851 | 7,641 | -61.51% | 47.87ms | 44.47ms |
| Deeply nested | 14,311 | 10,527 | -26.44% | 19.76ms | 46.65ms |
| Realistic user | 12,811 | 13,311 | +3.90% | 15.35ms | 72.93ms |

---

## Testing

```bash
./gradlew test
```

---

## Architecture

```
ktoon-core
ktoon-parser
ktoon-writer
ktoon-processing
ktoon-engine
ktoon-api
ktoon-kotlinx
```

External usage only requires:

```
ktoon-api
```

Or the Package

---

## Design Principles

- No silent error recovery
- No lossy transformations
- Strict and predictable parsing
- Consistent roundtrip behavior

---

## Roadmap

- ORM layer
- Streaming parser
- Schema validation
- Ktor and Spring integration
- Performance benchmarking

---

## License

MIT License

---

## Author

TyLabsX

---

## Contributing

Contributions and feedback are welcome.
