package com.github.jdha.selectpaircontent.services

import com.intellij.json.JsonFileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.jetbrains.kotlin.idea.KotlinFileType

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class SelectPairContentTest : BasePlatformTestCase() {

    fun CodeInsightTestFixture.expand() =
        this.performEditorAction(
            "com.github.jdha.selectpaircontent.actions.SelectPairContentExpandingAction"
        )

    fun CodeInsightTestFixture.shrink() =
        this.performEditorAction(
            "com.github.jdha.selectpaircontent.actions.SelectPairContentShrinkingAction"
        )

    override fun getTestDataPath(): String = "src/test/testData"

    fun `test simple json selection with double quotes`() {
        myFixture.configureByText(
            JsonFileType.INSTANCE,
            """
                {
                  "foo": "b<caret>ar"
                }
                """
                .trimIndent(),
        )

        myFixture.expand()

        myFixture.checkResult(
            """
                {
                  "foo": "<selection>b<caret>ar</selection>"
                }
                """
                .trimIndent()
        )
    }

    // fun testSelectEnclosingTypingPairs_Json_Reverse() {
    //     // This test verifies that when shrinking from a selection that includes a key-value
    // pair,
    //     // we select just the value part
    //     myFixture.configureByText(
    //         JsonFileType.INSTANCE,
    //         """
    //         {<selection>
    //           "foo": "b<caret>ar"
    //         </selection>}
    //         """
    //             .trimIndent(),
    //     )
    //
    //     // When we shrink, we should select just the value part
    //     myFixture.performEditorAction(
    //         "com.github.jdha.selectpaircontent.actions.SelectPairContentReverseAction"
    //     )
    //
    //     // Verify the result
    //     myFixture.checkResult(
    //         """
    //         {
    //           "foo": <selection>"b<caret>ar"</selection>
    //         }
    //         """
    //             .trimIndent()
    //     )
    // }

    fun testSelectEnclosingTypingPairs_Json_complex_Reverse() {
        myFixture.configureByText(
            JsonFileType.INSTANCE,
            """
            <selection>{
              "teams": [
                {
                  "name": "Frontend",
                  "members": [
                    {
                      "id": "E001"
                    },
                    {
                      "id": "E002"<caret>
                    }
                  ]
                },
                {
                  "name": "Backend",
                  "members": [
                    {
                      "id": "E003"
                    }
                  ]
                }
              ]
            }</selection>
            """
                .trimIndent(),
        )

        myFixture.shrink()
        myFixture.shrink()

        myFixture.checkResult(
            """
            {
              "teams": <selection>[
                {
                  "name": "Frontend",
                  "members": [
                    {
                      "id": "E001"
                    },
                    {
                      "id": "E002"<caret>
                    }
                  ]
                },
                {
                  "name": "Backend",
                  "members": [
                    {
                      "id": "E003"
                    }
                  ]
                }
              ]</selection>
            }
            """
                .trimIndent()
        )
    }

    fun testSelectEnclosingTypingPairs_Json_complex_Reverse2() {
        myFixture.configureByText(
            JsonFileType.INSTANCE,
            """
            {
              "teams": [
                {
                  "name": "Frontend",
                  "members": <selection>[
                    {
                      "id": "E001"
                    },
                    {
                      "id": "E002"<caret>
                    }
                  ]</selection>
                },
                {
                  "name": "Backend",
                  "members": [
                    {
                      "id": "E003"
                    }
                  ]
                }
              ]
            }
            """
                .trimIndent(),
        )

        myFixture.shrink()

        myFixture.checkResult(
            """
            {
              "teams": [
                {
                  "name": "Frontend",
                  "members": [<selection>
                    {
                      "id": "E001"
                    },
                    {
                      "id": "E002"<caret>
                    }
                  </selection>]
                },
                {
                  "name": "Backend",
                  "members": [
                    {
                      "id": "E003"
                    }
                  ]
                }
              ]
            }
            """
                .trimIndent()
        )

        myFixture.shrink()

        myFixture.checkResult(
            """
            {
              "teams": [
                {
                  "name": "Frontend",
                  "members": [
                    {
                      "id": "E001"
                    },
                    <selection>{
                      "id": "E002"<caret>
                    }</selection>
                  ]
                },
                {
                  "name": "Backend",
                  "members": [
                    {
                      "id": "E003"
                    }
                  ]
                }
              ]
            }
            """
                .trimIndent()
        )
    }

    fun testSelectEnclosingTypingPairs_Kotlin_Reverse_insideBracket() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            object PairConstants {<selection>
                val TYPING_PAIRS =
                    mapOf(
                        ']' to '[',
                        ')' to '(',<caret>
                        '}' to '{',
                    )

                const val MINIMUM_SELECTION_SIZE = 2
            </selection>}
            """
                .trimIndent(),
        )

        myFixture.shrink()

        myFixture.checkResult(
            """
            object PairConstants {
                val TYPING_PAIRS =
                    mapOf<selection>(
                        ']' to '[',
                        ')' to '(',<caret>
                        '}' to '{',
                    )</selection>

                const val MINIMUM_SELECTION_SIZE = 2
            }
            """
                .trimIndent()
        )
    }

    fun testSelectEnclosingTypingPairs_Kotlin_Reverse_outsideBracket() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            object PairConstants <selection>{
                val TYPING_PAIRS =
                    mapOf(
                        ']' to '[',
                        ')' to '(',<caret>
                        '}' to '{',
                    )

                const val MINIMUM_SELECTION_SIZE = 2
            }</selection>
            """
                .trimIndent(),
        )

        myFixture.shrink()

        myFixture.checkResult(
            """
            object PairConstants {<selection>
                val TYPING_PAIRS =
                    mapOf(
                        ']' to '[',
                        ')' to '(',<caret>
                        '}' to '{',
                    )

                const val MINIMUM_SELECTION_SIZE = 2
            </selection>}
            """
                .trimIndent()
        )
    }

    // Test expanding selection in Kotlin files
    fun testSelectEnclosingTypingPairs_Kotlin_Expand() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            fun test() {
                val text = "Hello<caret> World"
            }
            """
                .trimIndent(),
        )

        myFixture.expand()

        myFixture.checkResult(
            """
            fun test() {
                val text = "<selection>Hello<caret> World</selection>"
            }
            """
                .trimIndent()
        )
    }

    // Test consecutive expansions in Kotlin files
    fun testSelectEnclosingTypingPairs_Kotlin_ConsecutiveExpand() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            fun test() {
                val result = calculate(<caret>42)
            }
            """
                .trimIndent(),
        )

        // First expansion - select the content inside parentheses
        myFixture.expand()
        myFixture.checkResult(
            """
            fun test() {
                val result = calculate(<selection>42</selection>)
            }
            """
                .trimIndent()
        )

        // Second expansion - select the entire parentheses
        myFixture.expand()
        myFixture.checkResult(
            """
            fun test() {
                val result = calculate<selection>(42)</selection>
            }
            """
                .trimIndent()
        )
    }

    // Test with single quotes
    fun testSelectEnclosingTypingPairs_SingleQuotes() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            fun test() {
                val c = '<caret>A'
            }
            """
                .trimIndent(),
        )

        myFixture.expand()

        myFixture.checkResult(
            """
            fun test() {
                val c = '<selection>A</selection>'
            }
            """
                .trimIndent()
        )
    }

    // Test with backticks (used in Kotlin for escaping identifiers)
    fun testSelectEnclosingTypingPairs_Backticks() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            fun test() {
                val obj = `special<caret>Name`
            }
            """
                .trimIndent(),
        )

        myFixture.expand()

        myFixture.checkResult(
            """
            fun test() {
                val obj = `<selection>special<caret>Name</selection>`
            }
            """
                .trimIndent()
        )
    }

    // Test with angle brackets (generics in Kotlin)
    fun testSelectEnclosingTypingPairs_AngleBrackets() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            fun test() {
                val list = List<Int<caret>>()
            }
            """
                .trimIndent(),
        )

        myFixture.expand()

        myFixture.checkResult(
            """
            fun test() {
                val list = List<<selection>Int<caret></selection>>()
            }
            """
                .trimIndent()
        )
    }

    // Test with empty content between pairs
    fun testSelectEnclosingTypingPairs_EmptyContent() {
        myFixture.configureByText(
            PlainTextFileType.INSTANCE,
            """
            This is a test with empty brackets [<caret>] in the text.
            """
                .trimIndent(),
        )

        myFixture.expand()

        myFixture.checkResult(
            """
            This is a test with empty brackets [<selection><caret></selection>] in the text.
            """
                .trimIndent()
        )
    }

    // Test with nested pairs of different types
    fun testSelectEnclosingTypingPairs_NestedDifferentPairs() {
        myFixture.configureByText(
            JsonFileType.INSTANCE,
            """
            {
              "data": {
                "items": [
                  { "name": "item<caret>1" }
                ]
              }
            }
            """
                .trimIndent(),
        )

        // First expansion - select the content inside quotes
        myFixture.expand()
        myFixture.checkResult(
            """
            {
              "data": {
                "items": [
                  { "name": "<selection>item<caret>1</selection>" }
                ]
              }
            }
            """
                .trimIndent()
        )

        // Second expansion - select the key-value pair
        myFixture.expand()
        myFixture.expand()
        myFixture.checkResult(
            """
            {
              "data": {
                "items": [
                  {<selection> "name": "item<caret>1" </selection>}
                ]
              }
            }
            """
                .trimIndent()
        )

        // Third expansion - select the array content
        myFixture.expand()
        myFixture.checkResult(
            """
            {
              "data": {
                "items": [
                  <selection>{ "name": "item<caret>1" }</selection>
                ]
              }
            }
            """
                .trimIndent()
        )
    }

    // Test with XML/HTML-like tags
    // fun testSelectEnclosingTypingPairs_XmlTags() {
    //     myFixture.configureByText(
    //         PlainTextFileType.INSTANCE,
    //         """
    //         <div>
    //           <span>Hello <b>Wor<caret>ld</b> Text</span>
    //         </div>
    //         """
    //             .trimIndent(),
    //     )
    //
    //     // First expansion - select content inside <b> tags
    //     myFixture.expand()
    //     myFixture.checkResult(
    //         """
    //         <selection><div>
    //           <span>Hello <b>Wor<caret>ld</b> Text</span>
    //         </div></selection>
    //         """
    //             .trimIndent()
    //     )
    //
    //     // Second expansion - select the <b> tags with content
    //     myFixture.expand()
    //     myFixture.checkResult(
    //         """
    //         <div>
    //           <span>Hello <selection><b>World</b></selection> Text</span>
    //         </div>
    //         """
    //             .trimIndent()
    //     )
    // }

    // Test shrinking with empty content
    fun testSelectEnclosingTypingPairs_EmptyContent_Shrink() {
        myFixture.configureByText(
            PlainTextFileType.INSTANCE,
            """
            This is a test with <selection>empty brackets [<caret>]</selection> in the text.
            """
                .trimIndent(),
        )

        myFixture.shrink()

        myFixture.checkResult(
            """
            This is a test with empty brackets [<selection><caret></selection>] in the text.
            """
                .trimIndent()
        )
    }

    // Test with mixed content including comments
    fun testSelectEnclosingTypingPairs_MixedContentWithComments() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            fun test() {
                // This is a comment
                val result = calculate(
                    42, // The answer
                    "te<caret>xt" // Another comment
                )
            }
            """
                .trimIndent(),
        )

        // First expansion - select the string content
        myFixture.expand()
        myFixture.checkResult(
            """
            fun test() {
                // This is a comment
                val result = calculate(
                    42, // The answer
                    "<selection>te<caret>xt</selection>" // Another comment
                )
            }
            """
                .trimIndent()
        )

        // Second expansion - select the function arguments
        myFixture.expand()
        myFixture.expand()
        myFixture.checkResult(
            """
            fun test() {
                // This is a comment
                val result = calculate(<selection>
                    42, // The answer
                    "te<caret>xt" // Another comment
                </selection>)
            }
            """
                .trimIndent()
        )
    }

    // // Test with multiple carets (should handle the first caret) -> plain text feature needs to
    // be added to support this
    // fun testSelectEnclosingTypingPairs_MultipleCaret() {
    //     myFixture.configureByText(
    //         PlainTextFileType.INSTANCE,
    //         """
    //         [item<caret>1] and [item<caret>2]
    //         """
    //             .trimIndent(),
    //     )
    //
    //     myFixture.expand()
    //
    //     myFixture.checkResult(
    //         """
    //         [<selection>item<caret>1</selection>] and [<selection>item2</selection>]
    //         """
    //             .trimIndent()
    //     )
    // }

    // Test with deeply nested JSON structure (4+ levels)
    fun testSelectEnclosingTypingPairs_DeeplyNestedJson() {
        myFixture.configureByText(
            JsonFileType.INSTANCE,
            """
            {
              "config": {
                "settings": {
                  "display": {
                    "theme": {
                      "colors": {
                        "primary": "#00<caret>FF00"
                      }
                    }
                  }
                }
              }
            }
            """
                .trimIndent(),
        )

        // First expansion - select the string content
        myFixture.expand()
        myFixture.checkResult(
            """
            {
              "config": {
                "settings": {
                  "display": {
                    "theme": {
                      "colors": {
                        "primary": "<selection>#00<caret>FF00</selection>"
                      }
                    }
                  }
                }
              }
            }
            """
                .trimIndent()
        )

        // Second expansion - select the key-value pair
        myFixture.expand()
        myFixture.expand()
        myFixture.checkResult(
            """
            {
              "config": {
                "settings": {
                  "display": {
                    "theme": {
                      "colors": {<selection>
                        "primary": "#00<caret>FF00"
                      </selection>}
                    }
                  }
                }
              }
            }
            """
                .trimIndent()
        )

        // Continue expanding to test deep nesting
        myFixture.expand()
        myFixture.checkResult(
            """
            {
              "config": {
                "settings": {
                  "display": {
                    "theme": {
                      "colors": <selection>{
                        "primary": "#00<caret>FF00"
                      }</selection>
                    }
                  }
                }
              }
            }
            """
                .trimIndent()
        )

        // One more level
        myFixture.expand()
        myFixture.checkResult(
            """
            {
              "config": {
                "settings": {
                  "display": {
                    "theme": {<selection>
                      "colors": {
                        "primary": "#00<caret>FF00"
                      }
                    </selection>}
                  }
                }
              }
            }
            """
                .trimIndent()
        )
    }

    // Test with mixed nested pair types in complex arrangement
    fun testSelectEnclosingTypingPairs_MixedNestedPairs() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            fun complexFunction() {
                val result = listOf(
                    mapOf(
                        "key" to setOf(
                            Triple(
                                "first",
                                { x: Int -> x * 2 },
                                "th<caret>ird"
                            )
                        )
                    )
                )
            }
            """
                .trimIndent(),
        )

        // First expansion - select the string content
        myFixture.expand()
        myFixture.checkResult(
            """
            fun complexFunction() {
                val result = listOf(
                    mapOf(
                        "key" to setOf(
                            Triple(
                                "first",
                                { x: Int -> x * 2 },
                                "<selection>th<caret>ird</selection>"
                            )
                        )
                    )
                )
            }
            """
                .trimIndent()
        )

        // Second expansion - select the Triple arguments
        myFixture.expand()
        myFixture.expand()
        myFixture.checkResult(
            """
            fun complexFunction() {
                val result = listOf(
                    mapOf(
                        "key" to setOf(
                            Triple(<selection>
                                "first",
                                { x: Int -> x * 2 },
                                "th<caret>ird"
                            </selection>)
                        )
                    )
                )
            }
            """
                .trimIndent()
        )

        // Third expansion - select the setOf content
        myFixture.expand()
        myFixture.checkResult(
            """
            fun complexFunction() {
                val result = listOf(
                    mapOf(
                        "key" to setOf(
                            Triple<selection>(
                                "first",
                                { x: Int -> x * 2 },
                                "th<caret>ird"
                            )</selection>
                        )
                    )
                )
            }
            """
                .trimIndent()
        )

        // Fourth expansion - select the mapOf entry
        myFixture.expand()
        myFixture.expand()
        myFixture.checkResult(
            """
            fun complexFunction() {
                val result = listOf(
                    mapOf(
                        "key" to setOf<selection>(
                            Triple(
                                "first",
                                { x: Int -> x * 2 },
                                "th<caret>ird"
                            )
                        )</selection>
                    )
                )
            }
            """
                .trimIndent()
        )
    }

    // Test with complex nested structure and multiple shrink operations
    fun testSelectEnclosingTypingPairs_ComplexNestedShrink() {
        myFixture.configureByText(
            JsonFileType.INSTANCE,
            """
            {
              "data": <selection>[
                {
                  "items": [
                    {
                      "values": [
                        {
                          "id": "123",
                          "name": "test<caret>_item"
                        }
                      ]
                    }
                  ]
                }
              ]</selection>
            }
            """
                .trimIndent(),
        )

        // First shrink - select the object containing the items
        myFixture.shrink()
        myFixture.checkResult(
            """
            {
              "data": [<selection>
                {
                  "items": [
                    {
                      "values": [
                        {
                          "id": "123",
                          "name": "test<caret>_item"
                        }
                      ]
                    }
                  ]
                }
              </selection>]
            }
            """
                .trimIndent()
        )

        // Second shrink - select the items array
        myFixture.shrink()
        myFixture.checkResult(
            """
            {
              "data": [
                <selection>{
                  "items": [
                    {
                      "values": [
                        {
                          "id": "123",
                          "name": "test<caret>_item"
                        }
                      ]
                    }
                  ]
                }</selection>
              ]
            }
            """
                .trimIndent()
        )

        // Third shrink - select the object inside items array
        myFixture.shrink()
        myFixture.checkResult(
            """
            {
              "data": [
                {<selection>
                  "items": [
                    {
                      "values": [
                        {
                          "id": "123",
                          "name": "test<caret>_item"
                        }
                      ]
                    }
                  ]
                </selection>}
              ]
            }
            """
                .trimIndent()
        )

        // Fourth shrink - select the values array
        myFixture.shrink()
        myFixture.checkResult(
            """
            {
              "data": [
                {
                  "items": <selection>[
                    {
                      "values": [
                        {
                          "id": "123",
                          "name": "test<caret>_item"
                        }
                      ]
                    }
                  ]</selection>
                }
              ]
            }
            """
                .trimIndent()
        )
    }

    // Test with nested empty pairs
    fun testSelectEnclosingTypingPairs_NestedEmptyPairs() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            fun test() {
                val data = mapOf(
                    "empty" to listOf(
                        setOf(<caret>)
                    )
                )
            }
            """
                .trimIndent(),
        )

        // First expansion - select the empty setOf
        myFixture.expand()
        myFixture.checkResult(
            """
            fun test() {
                val data = mapOf(
                    "empty" to listOf(
                        setOf<selection>(<caret>)</selection>
                    )
                )
            }
            """
                .trimIndent()
        )

        // Second expansion - select the listOf content
        myFixture.expand()
        myFixture.checkResult(
            """
            fun test() {
                val data = mapOf(
                    "empty" to listOf(<selection>
                        setOf(<caret>)
                    </selection>)
                )
            }
            """
                .trimIndent()
        )

        // Third expansion - select the mapOf entry
        myFixture.expand()
        myFixture.expand()
        myFixture.checkResult(
            """
            fun test() {
                val data = mapOf(<selection>
                    "empty" to listOf(
                        setOf(<caret>)
                    )
                </selection>)
            }
            """
                .trimIndent()
        )
    }

    // Test with nested structures containing comments
    fun testSelectEnclosingTypingPairs_NestedWithComments() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
            fun processData() {
                val result = process(
                    // Input configuration
                    Config(
                        /* User settings */
                        Settings(
                            // Theme configuration
                            Theme(
                                // Color scheme
                                ColorScheme(
                                    primary = "bl<caret>ue", // Primary color
                                    secondary = "green" // Secondary color
                                ) // End of color scheme
                            ) // End of theme
                        ) // End of settings
                    ) // End of config
                ) // End of process call
            }
            """
                .trimIndent(),
        )

        // First expansion - select the string content
        myFixture.expand()
        myFixture.checkResult(
            """
            fun processData() {
                val result = process(
                    // Input configuration
                    Config(
                        /* User settings */
                        Settings(
                            // Theme configuration
                            Theme(
                                // Color scheme
                                ColorScheme(
                                    primary = "<selection>bl<caret>ue</selection>", // Primary color
                                    secondary = "green" // Secondary color
                                ) // End of color scheme
                            ) // End of theme
                        ) // End of settings
                    ) // End of config
                ) // End of process call
            }
            """
                .trimIndent()
        )

        // Second expansion - select the ColorScheme arguments
        myFixture.expand()
        myFixture.expand()
        myFixture.checkResult(
            """
            fun processData() {
                val result = process(
                    // Input configuration
                    Config(
                        /* User settings */
                        Settings(
                            // Theme configuration
                            Theme(
                                // Color scheme
                                ColorScheme(<selection>
                                    primary = "bl<caret>ue", // Primary color
                                    secondary = "green" // Secondary color
                                </selection>) // End of color scheme
                            ) // End of theme
                        ) // End of settings
                    ) // End of config
                ) // End of process call
            }
            """
                .trimIndent()
        )

        // Third expansion - select the Theme arguments
        myFixture.expand()
        myFixture.checkResult(
            """
            fun processData() {
                val result = process(
                    // Input configuration
                    Config(
                        /* User settings */
                        Settings(
                            // Theme configuration
                            Theme(
                                // Color scheme
                                ColorScheme<selection>(
                                    primary = "bl<caret>ue", // Primary color
                                    secondary = "green" // Secondary color
                                )</selection> // End of color scheme
                            ) // End of theme
                        ) // End of settings
                    ) // End of config
                ) // End of process call
            }
            """
                .trimIndent()
        )
    }
}
