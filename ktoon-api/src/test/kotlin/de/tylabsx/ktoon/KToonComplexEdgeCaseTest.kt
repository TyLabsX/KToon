package de.tylabsx.ktoon

import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class KToonComplexEdgeCaseTest {

    @BeforeTest
    fun resetConfig() {
        KToon.resetConfiguration()
    }

    @Test
    fun `KToon should parse and stringify complex nested TOON document`() {
        val input = """
        app:
          name: KToon
          version: "1.0.0"
          enabled: true
          nullable: null
          limits:
            maxUsers: 1000
            timeoutMs: 2500
            ratio: 0.75
            negative: -42
            scientific: 1.25e3
          owner:
            id: 123
            name: "TyLabsX"
            email: "team:core@tylabsx.dev"
            emptyString: ""
            stringNumber: "123"
            stringBoolean: "true"
            stringNull: "null"
          tags[5]: kotlin,toon,llm,"needs,quote"," spaced "
          users[3]{id,name,active,role}:
            1,Ada,true,admin
            2,Bob,false,user
            3,"Charlie, Jr.",true,"power:user"
          environments[2]:
            - name: dev
              url: "http://localhost:8080"
              features[3]: debug,hotreload,logs
            - name: prod
              url: "https://api.tylabsx.dev"
              features[2]: cache,metrics
          deeply:
            nested:
              level1:
                level2:
                  level3:
                    level4:
                      value: final
                      flag: false
                      data:
                        count: 999
                        label: "deep:value"
    """.trimIndent()

        val parsed = KToon.parse(input)

        println("\n================ AST =================")
        println(parsed)
        println("======================================\n")

        val stringified = KToon.stringify(parsed)

        println("\n============== TOON OUTPUT ============")
        println(stringified)
        println("======================================\n")

        val reparsed = KToon.parse(stringified)

        println("\n============= REPARSED AST ============")
        println(reparsed)
        println("======================================\n")

        assertEquals(parsed, reparsed)
    }

    @Test
    fun `KToon should preserve tricky quoted scalar values`() {
        val input = """
            values:
              empty: ""
              numberString: "42"
              negativeNumberString: "-42"
              boolString: "false"
              nullString: "null"
              colonString: "a:b"
              commaString: "a,b"
              bracketString: "[abc]"
              braceString: "{abc}"
              quoteString: "hello \"world\""
              slashString: "path\\to\\file"
              tabString: "hello\tworld"
              newlineString: "hello\nworld"
        """.trimIndent()

        val parsed = KToon.parse(input)
        val stringified = KToon.stringify(parsed)
        val reparsed = KToon.parse(stringified)

        assertEquals(parsed, reparsed)
    }

    @Test
    fun `KToon should parse deeply nested object`() {
        val input = """
            root:
              a:
                b:
                  c:
                    d:
                      e:
                        f:
                          g:
                            h:
                              i:
                                value: done
        """.trimIndent()

        val parsed = KToon.parse(input)
        val output = KToon.stringify(parsed)
        val reparsed = KToon.parse(output)

        assertEquals(parsed, reparsed)
    }
}