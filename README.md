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
