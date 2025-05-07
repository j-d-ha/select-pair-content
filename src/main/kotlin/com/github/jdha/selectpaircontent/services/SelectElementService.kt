package com.github.jdha.selectpaircontent.services

import com.github.jdha.selectpaircontent.utils.findElementWithOffset
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase.getElementAtCaret

private val LOG = logger<SelectElementService>()

enum class Direction {
    EXPAND_UP,
    SHRINK_DOWN,
    EXPAND_LEFT,
    EXPAND_RIGHT,
    SHRINK_LEFT,
    SHRINK_RIGHT,
}

private data class History(var previousSelection: List<TextRange> = emptyList())

@Service(Service.Level.PROJECT)
class SelectElementService {

    private val history = mutableMapOf<String, History>()
    private lateinit var filePath: String

    fun selectElement(
        editor: Editor,
        caret: Caret?,
        dataContext: DataContext,
        direction: Direction,
    ) {
        filePath =
            CommonDataKeys.VIRTUAL_FILE.getData(dataContext)?.path
                ?: let {
                    LOG.warn("No file")
                    return
                }

        if (caret == null) {
            LOG.warn("No caret")
            history[filePath]?.previousSelection = emptyList()
            return
        }

        history[filePath] = History()

        val currentSelection = caret.selectionRange
        val caretOffset = caret.offset
        val element = getElementAtCaret(editor) ?: return

        // for all directions but SHRINK_DOWN, if currentSelection.length is 1, set the current
        // element's text range as selection and exit.
        if (direction != Direction.SHRINK_DOWN && currentSelection.length == 0) {
            with(element.textRange) { caret.setSelection(startOffset, endOffset) }
            return
        }

        when (direction) {
            Direction.EXPAND_UP ->
                element.getWholeElementAtSelection(currentSelection).expandUp(currentSelection)
            Direction.SHRINK_DOWN ->
                element
                    .getWholeElementAtSelection(currentSelection)
                    .shrinkDown(currentSelection, caretOffset)
            Direction.EXPAND_RIGHT ->
                element
                    .getElementsAtSelection(currentSelection)
                    .expandRight(currentSelection, caretOffset)
            Direction.EXPAND_LEFT ->
                element
                    .getElementsAtSelection(currentSelection)
                    .expandLeft(currentSelection, caretOffset)
            else -> TODO("Not yet implemented: $direction")
        }?.let {
            caret.setSelection(it.startOffset, it.endOffset)
            if (it.startOffset == it.endOffset) {
                LOG.debug("Empty text range; resetting previous selection")
                history[filePath]?.previousSelection = emptyList()
            }
        }
    }

    //      ╭──────────────────────────────────────────────────────────╮
    //      │                    Direction Actions                     │
    //      ╰──────────────────────────────────────────────────────────╯

    // ── Expand Up ────────────────────────────────────────────────────────────────────

    private fun PsiElement.expandUp(currentSelection: TextRange): TextRange? =
        if (this is PsiDirectory) currentSelection
        else if (this.textRange == currentSelection) parent?.expandUp(currentSelection) ?: textRange
        else
            getWholeElementAtSelection(currentSelection).let { wholeElement ->
                when {
                    wholeElement.parent !is PsiDirectory &&
                        (wholeElement.textRange == currentSelection ||
                            currentSelection.contains(wholeElement.textRange)) -> {
                        wholeElement.parent.expandUp(currentSelection)
                    }
                    currentSelection.containsInside(wholeElement.textRange) -> {
                        currentSelection
                    }
                    else -> {
                        wholeElement.textRange
                    }
                }
            } ?: textRange

    // ── Shrink Down ──────────────────────────────────────────────────────────────────

    private fun PsiElement.shrinkDown(currentSelection: TextRange, caretOffset: Int): TextRange? =
        if (children.none() && textRange == currentSelection)
            TextRange.create(caretOffset, caretOffset)
        else
            generateSequence(firstChild) { it.nextSibling }
                .find { it.textRange.contains(caretOffset) }
                ?.let {
                    when {
                        it.textRange == currentSelection ->
                            it.shrinkDown(currentSelection, caretOffset)
                        else -> it.textRange
                    }
                }

    // ── Expand Right ─────────────────────────────────────────────────────────────────

    private fun Pair<PsiElement, PsiElement>.expandRight(
        currentSelection: TextRange,
        caretOffset: Int,
    ): TextRange? {
        val rightSibling = second.nextSibling ?: return currentSelection

        if (rightSibling.textRange.length == 0)
            return Pair(first, rightSibling).expandRight(currentSelection, caretOffset)

        return TextRange.create(currentSelection.startOffset, rightSibling.textRange.endOffset)
    }

    // ── Expand Left ──────────────────────────────────────────────────────────────────

    private fun Pair<PsiElement, PsiElement>.expandLeft(
        currentSelection: TextRange,
        caretOffset: Int,
    ): TextRange? {
        val leftSibling = first.prevSibling ?: return currentSelection

        if (leftSibling.textRange.length == 0)
            return Pair(leftSibling, second).expandLeft(currentSelection, caretOffset)

        return TextRange.create(leftSibling.textRange.startOffset, currentSelection.endOffset)
    }

    // ── Shrink Right ─────────────────────────────────────────────────────────────────

    // ── Shrink Left ──────────────────────────────────────────────────────────────────

    //      ╭──────────────────────────────────────────────────────────╮
    //      │                         Helpers                          │
    //      ╰──────────────────────────────────────────────────────────╯

    // ── Common ───────────────────────────────────────────────────────────────────────

    private tailrec fun PsiElement.getWholeElementAtSelection(
        currentSelection: TextRange
    ): PsiElement {
        if (parent == null) {
            return this
        }

        if (textRange?.contains(currentSelection) ?: true) {
            return this
        }

        return parent.getWholeElementAtSelection(currentSelection)
    }

    private fun PsiElement.getElementsAtSelection(
        currentSelection: TextRange
    ): Pair<PsiElement, PsiElement> {
        val firstElement =
            generateSequence(this.parent.firstChild) { it.nextSibling }
                .findElementWithOffset(currentSelection.startOffset)
                ?: throw Exception("No first element found for $this")

        val lastElement =
            generateSequence(this.parent.lastChild) { it.prevSibling }
                .findElementWithOffset(currentSelection.endOffset)
                ?: throw Exception("No last element found for $this")

        if (
            currentSelection.startOffset < firstElement.textRange.startOffset ||
                currentSelection.endOffset > lastElement.textRange.endOffset
        )
            return parent.getElementsAtSelection(currentSelection)

        if (parent !is PsiFile && parent.textRange == currentSelection)
            return parent.getElementsAtSelection(currentSelection)

        return Pair(firstElement, lastElement)
    }

    // ── Expand Up ────────────────────────────────────────────────────────────────────

    // ── Shrink Down ──────────────────────────────────────────────────────────────────

    // ── Expand Right ─────────────────────────────────────────────────────────────────

    // ── Expand Left ──────────────────────────────────────────────────────────────────

    // ── Shrink Right ─────────────────────────────────────────────────────────────────

    // ── Shrink Left ──────────────────────────────────────────────────────────────────

}
