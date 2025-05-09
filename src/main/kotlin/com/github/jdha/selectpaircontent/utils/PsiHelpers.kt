package com.github.jdha.selectpaircontent.utils

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.actions.BaseRefactoringAction
import kotlin.sequences.find
import kotlin.sequences.minByOrNull

/** Get PSI element at caret position */
fun getElementAtCaret(dataContext: DataContext): PsiElement? {
    val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return null
    val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return null
    val templateFile = PsiUtilCore.getTemplateLanguageFile(psiFile)

    return BaseRefactoringAction.getElementAtCaret(editor, templateFile ?: psiFile)
}

/** Extension function for Iterable<PsiElement> */
fun Iterable<PsiElement>.findElementWithOffset(offset: Int): PsiElement? =
    find { it.textRange.contains(offset) } ?: minByOrNull { it.offsetDelta(offset) }

/** Extension function for Sequence<PsiElement> */
fun Sequence<PsiElement>.findElementWithOffset(offset: Int): PsiElement? =
    find { it.textRange.contains(offset) } ?: minByOrNull { it.offsetDelta(offset) }

fun Sequence<PsiElement>.findElementIndexWithOffset(offset: Int): Int? =
    withIndex().find { it.value.textRange.contains(offset) }?.index
        ?: withIndex().minByOrNull { it.value.offsetDelta(offset) }?.index

private fun PsiElement.offsetDelta(offset: Int): Int =
    // If caret is before element, calculate distance from start
    if (offset > textRange.endOffset) offset - textRange.endOffset
    // If caret is after element, calculate distance from end
    else textRange.startOffset - offset
