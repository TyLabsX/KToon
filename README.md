# KToon

KToon is a Kotlin-first library for the TOON (Token-Oriented Object Notation)
format. It provides a TOON v2.1 parser, writer, processing engine, legacy
reflection codec, and native `kotlinx.serialization` support.

The current public API is centered on `kotlinx.serialization`. Reflection-based
codec functions are still available for compatibility, but they are legacy APIs.

---

## Features

- TOON v2.1 parser and writer
- `ToonValue` tree model for objects, arrays, strings, numbers, booleans and null
- Inline arrays and tabular arrays
- Roundtrip support through parse and stringify
- Native `kotlinx.serialization` format: `KToonNativeFormat`
- High-performance streaming encoder: `KToonStreamingFormat`
- Legacy JsonElement bridge: `KToonKotlinX`
- Legacy reflection codec: `KToonEncoder` and `KToonDecoder`
- Central public API through `KToon`

---

## Installation

The project version is managed centrally in `settings.gradle.kts`.

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("de.tylabsx:ktoon:1.1.0")
}
```

For direct access to the native and streaming kotlinx formats:

```kotlin
dependencies {
    implementation("de.tylabsx:ktoon-kotlinx:1.1.0")
}
```

The `ktoon-api` module is published as the main `de.tylabsx:ktoon` artifact.

---

## Basic TOON Usage

### Parse TOON

```kotlin
val input = """
    user:
      id: 123
      name: Alice
      active: true
      tags[2]: kotlin,toon
""".trimIndent()

val parsed: ToonValue = KToon.parse(input)
```

### Access Data

```kotlin
val root = parsed as ToonObject
val user = root.entries["user"] as ToonObject
val name = (user.entries["name"] as ToonString).value
```

### Create Data

```kotlin
val value = KToon.`object`(
    mapOf(
        "name" to "Alice",
        "age" to 25,
        "active" to true,
        "tags" to KToon.array(listOf<Any>("kotlin", "toon"))
    )
)
```

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
tags[2]: kotlin,toon
```

### Validate And Roundtrip

```kotlin
val valid = KToon.isValid(input)
val normalized = KToon.roundtrip(input)
```

---

## Native Kotlinx Serialization

`KToonNativeFormat` is the primary `kotlinx.serialization` format. It encodes
serializable values into the KToon `ToonValue` model and writes TOON text. It
decodes TOON text through the native value parser and `KToonSerializationDecoder`.

```kotlin
import de.tylabsx.ktoon.kotlinx.native.KToonNativeFormat
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val name: String,
    val active: Boolean,
    val tags: List<String>
)

val user = User(
    id = 1,
    name = "Alice",
    active = true,
    tags = listOf("kotlin", "toon")
)

val toon = KToonNativeFormat.encodeToString(user)
val decoded = KToonNativeFormat.decodeFromString<User>(toon)
```

Output:

```toon
id: 1
name: Alice
active: true
tags[2]: kotlin,toon
```

The same native path is available through `KToon`:

```kotlin
val toon = KToon.encodeToString(user)
val decoded = KToon.decodeFromString<User>(toon)
```

Serializer-explicit overloads are also available:

```kotlin
val toon = KToon.encodeToString(User.serializer(), user)
val decoded = KToon.decodeFromString(User.serializer(), toon)
```

---

## Streaming Encoder

`KToonStreamingFormat` is an experimental encode-only format for
`kotlinx.serialization`. It writes directly from serializer events into a
`StringBuilder`.

It does not create:

- `JsonElement`
- `ToonValue`
- `KToonWriterEngine`
- reflection codec state

Streaming decode is not supported yet. Decode streaming output with
`KToonNativeFormat`.

```kotlin
import de.tylabsx.ktoon.kotlinx.native.KToonNativeFormat
import de.tylabsx.ktoon.kotlinx.streaming.ExperimentalKToonStreamingApi
import de.tylabsx.ktoon.kotlinx.streaming.KToonStreamingFormat

@OptIn(ExperimentalKToonStreamingApi::class)
val toon = KToonStreamingFormat.encodeToString(user)

val decoded = KToonNativeFormat.decodeFromString<User>(toon)
```

The `KToon` convenience API uses the default streaming format:

```kotlin
val toon = KToon.streamToString(user)
```

### Streaming Modes

`KToonStreamingFormat` supports two modes.

#### FAST

FAST is speed-oriented. It keeps analysis minimal and writes primitive lists
inline. Object lists are generally written as block arrays, with optimized fast
paths for known flat shapes.

```kotlin
import de.tylabsx.ktoon.kotlinx.streaming.KToonStreamingFormat
import de.tylabsx.ktoon.kotlinx.streaming.KToonStreamingMode
import de.tylabsx.ktoon.kotlinx.streaming.KToonStreamingOptions
import de.tylabsx.ktoon.kotlinx.streaming.ExperimentalKToonStreamingApi

@OptIn(ExperimentalKToonStreamingApi::class)
val fastFormat = KToonStreamingFormat(
    KToonStreamingOptions(mode = KToonStreamingMode.FAST)
)

val toon = fastFormat.encodeToString(user)
```

Primitive lists are written inline:

```toon
tags[3]: kotlin,toon,llm
```

Object lists may be written as block arrays:

```toon
users[2]:
  - id: 1
    name: Alice
    active: true
  - id: 2
    name: Bob
    active: false
```

#### COMPACT

COMPACT is size-oriented. It buffers list items enough to detect tabular object
arrays. If every row has the same fields in the same order and every row value
is primitive, it writes TOON tabular arrays.

```kotlin
import de.tylabsx.ktoon.kotlinx.streaming.ExperimentalKToonStreamingApi
import de.tylabsx.ktoon.kotlinx.streaming.KToonStreamingFormat
import de.tylabsx.ktoon.kotlinx.streaming.KToonStreamingMode
import de.tylabsx.ktoon.kotlinx.streaming.KToonStreamingOptions

@OptIn(ExperimentalKToonStreamingApi::class)
val compactFormat = KToonStreamingFormat(
    KToonStreamingOptions(mode = KToonStreamingMode.COMPACT)
)

val toon = compactFormat.encodeToString(users)
```

Example output:

```toon
users[2]{id,name,active}:
  1,Alice,true
  2,Bob,false
```

If a row contains a nested object, nested list or unsupported structure, COMPACT
falls back to block array output without changing the represented data.

### Streaming Options

```kotlin
KToonStreamingOptions(
    indentSize = 2,
    quoteStrings = false,
    trailingNewline = false,
    mode = KToonStreamingMode.COMPACT
)
```

String rendering follows TOON escaping rules. Empty strings, strings that look
like numbers, `null`, `true`, `false`, and strings containing special TOON
characters are quoted.

Unsupported streaming features fail explicitly:

- structured map keys are rejected
- polymorphic serialization is not supported yet
- streaming decode always throws `UnsupportedOperationException`

---

## Complex Kotlinx Example

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
        "version" to "1.1.0",
        "format" to "TOON"
    ),
    scores = listOf(95.5, 88.0, 100.25)
)

val toon = KToonNativeFormat.encodeToString(company)
val decoded = KToonNativeFormat.decodeFromString<Company>(toon)
```

Example output:

```toon
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
  version: "1.1.0"
  format: TOON
scores[3]: 95.5,88.0,100.25
```

---

## Legacy APIs

### JsonElement Bridge

`KToonKotlinX` is the older `kotlinx.serialization` bridge. It converts through
`JsonElement` and then to `ToonValue`. It remains available for compatibility,
but new code should use `KToonNativeFormat`.

```kotlin
import de.tylabsx.ktoon.kotlinx.bridge.KToonKotlinX

@Suppress("DEPRECATION")
val toon = KToonKotlinX.encodeToString(user)
```

### Reflection Codec

The reflection codec remains available through explicit reflective functions:

```kotlin
val value: ToonValue = KToon.encodeReflective(user)
val toon: String = KToon.encodeReflectiveToString(user)

val decoded: User = KToon.decodeReflectiveFromString(toon)
```

The older `KToon.encode(value: Any?)` and reflective `KToon.decode<T>(value)`
entry points are deprecated.

---

## TOON Example

```toon
app:
  name: KToon
  version: "1.1.0"

  users[2]{id,name,active}:
    1,Alice,true
    2,Bob,false

  features[4]: parser,writer,kotlinx,streaming
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

## License

MIT License

---

## Author

TyLabsX

---

## Contributing

Contributions and feedback are welcome.
