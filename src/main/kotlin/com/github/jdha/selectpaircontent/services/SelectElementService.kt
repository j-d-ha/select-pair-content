package com.github.jdha.selectpaircontent.services

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilBase.getElementAtCaret

private val LOG = logger<SelectElementService>()

enum class Direction {
    EXPAND_UP,
    SHRINK_DOWN,
}

@Service(Service.Level.PROJECT)
class SelectElementService {

    fun selectElement(
        editor: Editor,
        caret: Caret?,
        dataContext: DataContext,
        direction: Direction,
    ) {

        if (caret == null) {
            LOG.warn("No caret")
            return
        }

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
        }?.let { caret.setSelection(it.startOffset, it.endOffset) }
    }

    //      ╭──────────────────────────────────────────────────────────╮
    //      │                    Direction Actions                     │
    //      ╰──────────────────────────────────────────────────────────╯

    // ── Expand Up ────────────────────────────────────────────────────────────────────

    private fun PsiElement.expandUp(currentSelection: TextRange): TextRange? =
        if (this is PsiDirectory) {
            currentSelection
        } else if (this.textRange == currentSelection) {
            parent?.expandUp(currentSelection) ?: textRange
        } else {
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
        }

    // ── Shrink Down ──────────────────────────────────────────────────────────────────

    private fun PsiElement.shrinkDown(currentSelection: TextRange, caretOffset: Int): TextRange? =
        if (children.none() && textRange == currentSelection) {
            TextRange.create(caretOffset, caretOffset)
        } else {
            generateSequence(firstChild) { it.nextSibling }
                .find { it.textRange.contains(caretOffset) }
                ?.let {
                    when {
                        it.textRange == currentSelection -> {
                            it.shrinkDown(currentSelection, caretOffset)
                        }
                        else -> {
                            it.textRange
                        }
                    }
                }
        }

    //      ╭──────────────────────────────────────────────────────────╮
    //      │                         Helpers                          │
    //      ╰──────────────────────────────────────────────────────────╯

    // ── Common ───────────────────────────────────────────────────────────────────────

    private tailrec fun PsiElement.getWholeElementAtSelection(
        currentSelection: TextRange
    ): PsiElement =
        if (parent == null) {
            this
        } else if (textRange?.contains(currentSelection) ?: true) {
            this
        } else {
            parent.getWholeElementAtSelection(currentSelection)
        }
}
