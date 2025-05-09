package com.github.jdha.selectpaircontent.services

import com.intellij.json.JsonFileType
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.jetbrains.kotlin.idea.KotlinFileType

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class SelectElementServiceTest : BasePlatformTestCase() {

    fun CodeInsightTestFixture.expandOut() =
        this.performEditorAction(
            "com.github.jdha.selectpaircontent.actions.SelectElementExpandOutAction"
        )

    fun CodeInsightTestFixture.shrinkIn() =
        this.performEditorAction(
            "com.github.jdha.selectpaircontent.actions.SelectElementShrinkDownAction"
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
                  "foo": <selection>"b<caret>ar"</selection>
                }
                """
                .trimIndent()
        )
    }

    fun `test expand out - Kotlin`() {
        myFixture.configureByText(
            KotlinFileType.INSTANCE,
            """
                class Temp(val age: Int) {

                    override fun toString(): String =<caret> age.toString()
                }
                """
                .trimIndent(),
        )

        myFixture.expandOut()

        myFixture.checkResult(
            """
                class Temp(val age: Int) {

                    override fun toString(): String =<selection><caret> </selection>age.toString()
                }
                """
                .trimIndent()
        )

        myFixture.expandOut()

        myFixture.checkResult(
            """
                class Temp(val age: Int) {

                    <selection>override fun toString(): String =<caret> age.toString()</selection>
                }
                """
                .trimIndent()
        )

        myFixture.expandOut()

        myFixture.checkResult(
            """
                class Temp(val age: Int) <selection>{

                    override fun toString(): String =<caret> age.toString()
                }</selection>
                """
                .trimIndent()
        )

        myFixture.expandOut()

        myFixture.checkResult(
            """
                <selection>class Temp(val age: Int) {

                    override fun toString(): String =<caret> age.toString()
                }</selection>
                """
                .trimIndent()
        )

        myFixture.shrinkIn()

        myFixture.checkResult(
            """
                class Temp(val age: Int) <selection>{

                    override fun toString(): String =<caret> age.toString()
                }</selection>
                """
                .trimIndent()
        )

        myFixture.shrinkIn()

        myFixture.checkResult(
            """
                class Temp(val age: Int) {

                    <selection>override fun toString(): String =<caret> age.toString()</selection>
                }
                """
                .trimIndent()
        )

        myFixture.shrinkIn()

        myFixture.checkResult(
            """
                class Temp(val age: Int) {

                    override fun toString(): String =<selection><caret> </selection>age.toString()
                }
                """
                .trimIndent()
        )
    }
}
