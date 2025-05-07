package com.github.jdha.selectpaircontent.services

import com.intellij.json.JsonFileType
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class SelectElementServiceTest : BasePlatformTestCase() {

    fun CodeInsightTestFixture.expandOut() =
        this.performEditorAction(
            "com.github.jdha.selectpaircontent.actions.SelectPairContentShrinkingAction"
        )

    fun CodeInsightTestFixture.shrinkIn() =
        this.performEditorAction(
            "com.github.jdha.selectpaircontent.actions.SelectElementShrinkDownAction"
        )

    fun CodeInsightTestFixture.expandLeft() =
        this.performEditorAction(
            "com.github.jdha.selectpaircontent.actions.SelectElementExpandLeftAction"
        )

    fun CodeInsightTestFixture.expandRight() =
        this.performEditorAction(
            "com.github.jdha.selectpaircontent.actions.SelectElementExpandRightAction"
        )

    fun CodeInsightTestFixture.shrinkLeft() =
        this.performEditorAction(
            "com.github.jdha.selectpaircontent.actions.SelectElementShrinkLeftAction"
        )

    fun CodeInsightTestFixture.shrinkRight() =
        this.performEditorAction(
            "com.github.jdha.selectpaircontent.actions.SelectElementShrinkRightAction"
        )

    override fun getTestDataPath(): String = "src/test/testData"

    fun `test basic expand out - JSON`() {
        myFixture.configureByText(
            JsonFileType.INSTANCE,
            """
                {
                  "foo": "b<caret>ar"
                }
                """
                .trimIndent(),
        )

        myFixture.expandOut()

        myFixture.checkResult(
            """
                {
                  "foo": "<selection>b<caret>ar</selection>"
                }
                """
                .trimIndent()
        )
    }

    // fun `test expand left - Kotlin`() {
    //     myFixture.configureByText(
    //         KotlinFileType.INSTANCE,
    //         """
    //             class Temp(val age: Int) {
    //
    //                 override fun toString(): String =<caret> age.toString()
    //             }
    //             """
    //             .trimIndent(),
    //     )
    //
    //     myFixture.expandLeft()
    //
    //     myFixture.checkResult(
    //         """
    //             class Temp(val age: Int) {
    //
    //                 override fun toString(): String =<selection><caret>
    // </selection>age.toString()
    //             }
    //             """
    //             .trimIndent()
    //     )
    //
    //     myFixture.expandLeft()
    //
    //     myFixture.checkResult(
    //         """
    //             class Temp(val age: Int) {
    //
    //                 override fun toString(): String =<selection><caret>
    // </selection>age.toString()
    //             }
    //             """
    //             .trimIndent()
    //     )
    // }
    //
    // fun `test expand left with existing selection - Kotlin`() {
    //     myFixture.configureByText(
    //         KotlinFileType.INSTANCE,
    //         """
    //             class Temp(val age: Int) {
    //
    //                 override fun toString(): String <selection><caret>=
    // </selection>age.toString()
    //             }
    //             """
    //             .trimIndent(),
    //     )
    //
    //     myFixture.expandLeft()
    //
    //     myFixture.checkResult(
    //         """
    //             class Temp(val age: Int) {
    //
    //                 override fun toString(): =<selection>String <caret>
    // </selection>age.toString()
    //             }
    //             """
    //             .trimIndent()
    //     )
    // }
}
