package com.github.jdha.selectpaircontent.services

import com.github.jdha.selectpaircontent.utils.findElementWithOffset
import com.github.jdha.selectpaircontent.utils.getElementAtCaret
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import kotlin.collections.contains

/** Expands the current selection to include enclosing pairs */
fun Caret.selectEnclosingTypingPairsExpanding(dataContext: DataContext) {
    getElementAtCaret(dataContext)?.let { element ->
        selectionRange.let { currentSelection ->
            expandSelection(element, currentSelection, offset)?.also { newRange ->
                setSelection(newRange.startOffset, newRange.endOffset)
            }
        }
    }
}

/** Shrinks the current selection if bounded by matching pairs */
fun Caret.selectEnclosingTypingPairsShrinking(dataContext: DataContext, editor: Editor) {
    if (selectionRange.isEmpty) return

    getElementAtCaret(dataContext)?.let { element ->
        val currentSelection = selectionRange

        // First try direct check for matching pairs using the editor
        checkSelectionTextForPair(editor, currentSelection, false)?.also { matchingPairRange ->
            setSelection(matchingPairRange.startOffset, matchingPairRange.endOffset)
            return
        }

        // If that fails, try to shrink using the element tree
        shrinkSelection(element, currentSelection, offset)?.also { newRange ->
            setSelection(newRange.startOffset, newRange.endOffset)
        }
    }
}

/** Constants and utility functions for pair matching */
object PairConstants {
    /** Map of matching pairs: opening char -> closing char */
    val PAIR_MATCHES =
        mapOf(
            "'" to "'",
            "\"" to "\"",
            "`" to "`",
            "[" to "]",
            "(" to ")",
            "{" to "}",
            "<" to ">",
            "\"\"\"" to "\"\"\"",
            "'''" to "'''",
            "\${" to "}",
            "$\"" to "\"",
        )

    val MIN_PAIR_SIZE by lazy {
        PAIR_MATCHES.entries.minOf { (key, value) -> minOf(key.length, value.length) }
    }

    val MAX_PAIR_SIZE by lazy {
        PAIR_MATCHES.entries.maxOf { (key, value) -> maxOf(key.length, value.length) }
    }
}

private data class MatchedPair(val range: TextRange, val matchedPair: Pair<String, String>)

private fun processPair(
    pairDetail: MatchedPair,
    currentSelection: TextRange,
    isExpanding: Boolean,
): TextRange? {
    val innerContent =
        with(pairDetail.range) {
            TextRange.create(
                startOffset + pairDetail.matchedPair.first.length,
                endOffset - pairDetail.matchedPair.second.length,
            )
        }

    val isShrinking = !isExpanding

    return when {
        isExpanding && currentSelection == innerContent -> pairDetail.range
        isExpanding && innerContent.contains(currentSelection) && currentSelection.length == 0 ->
            innerContent
        isExpanding && innerContent.containsInside(currentSelection) -> innerContent
        isShrinking && currentSelection.containsInside(pairDetail.range) -> pairDetail.range
        isShrinking && currentSelection == pairDetail.range -> innerContent
        else -> null
    }
}

fun TextRange.containsInside(range: TextRange): Boolean =
    this.startOffset < range.startOffset && this.endOffset > range.endOffset

/** Common handler for element selection (works for both expanding and shrinking) */
private fun handleElementWithNoChildren(
    element: PsiElement,
    currentSelection: TextRange,
    isExpanding: Boolean,
): TextRange? =
    element.text?.let {
        when {
            it.length < PairConstants.MIN_PAIR_SIZE -> null
            else -> it.getPair(element.textRange, currentSelection, isExpanding)
        }
    }

private fun String.getPair(
    elementRange: TextRange,
    currentSelection: TextRange,
    isExpanding: Boolean,
): TextRange? =
    (PairConstants.MAX_PAIR_SIZE downTo PairConstants.MIN_PAIR_SIZE).firstNotNullOfOrNull { i ->
        if (this.length < i * 2) return@firstNotNullOfOrNull null

        val opening = this.take(i)
        val closing = PairConstants.PAIR_MATCHES[opening] ?: return@firstNotNullOfOrNull null

        when (closing) {
            this.takeLast(i) ->
                processPair(
                    MatchedPair(elementRange, opening to closing),
                    currentSelection,
                    isExpanding,
                )
            else -> null
        }
    }

/** Handle expanding selection */
private fun expandSelection(
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
): TextRange? {
    // Handle elements with no children for expanding
    if (
        element.children.isEmpty() &&
            !currentSelection.contains(element.textRange) &&
            element.textRange.let {
                currentSelection.endOffset > it.startOffset &&
                    currentSelection.startOffset < it.endOffset
            }
    ) {
        handleElementWithNoChildren(element, currentSelection, true)?.let {
            return it
        }
    }

    return element.parent?.let { walkElementTreeExpanding(it, currentSelection, caretOffset) }
}

/** Handle shrinking selection */
private fun shrinkSelection(
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
): TextRange? =
    getCurrentElement(element, currentSelection)?.let {
        walkElementTreeShrinking(it, currentSelection, caretOffset)
    }

/** Check if we should skip this element during expansion */
private fun shouldSkipElement(
    elementRange: TextRange,
    currentSelection: TextRange,
    caretOffset: Int,
): Boolean {
    // Skip if element range is exactly the same as current selection
    if (elementRange == currentSelection) return true

    // Skip if element is completely contained within the current selection
    if (
        elementRange.startOffset >= currentSelection.startOffset &&
            elementRange.endOffset <= currentSelection.endOffset
    )
        return true

    // Skip if element starts or ends exactly at caret position
    if (elementRange.startOffset == caretOffset || elementRange.endOffset == caretOffset)
        return true

    return false
}

/** Walk up element tree for expansion */
private fun walkElementTreeExpanding(
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
): TextRange? {
    // Stop at file level
    element.firstChild?.let { if (it.node?.elementType.toString() == "FILE") return null }

    val elementRange =
        element.textRange
            ?: return element.parent?.let {
                walkElementTreeExpanding(it, currentSelection, caretOffset)
            }

    // Skip this element if it doesn't provide a useful selection
    if (shouldSkipElement(elementRange, currentSelection, caretOffset)) {
        return element.parent?.let { walkElementTreeExpanding(it, currentSelection, caretOffset) }
    }

    // Process element with children
    return processElementForPairs(element, currentSelection, caretOffset, true)
}

/** Walk down element tree for shrinking */
private fun walkElementTreeShrinking(
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
): TextRange? =
    element.children.firstNotNullOfOrNull { child ->
        when {
            !child.textRange.contains(caretOffset) -> null // Skip this child
            child.children.isEmpty() -> handleElementWithNoChildren(child, currentSelection, false)
            else ->
                processElementForPairs(child, currentSelection, caretOffset, false)
                    ?: walkElementTreeShrinking(child, currentSelection, caretOffset)
        }
    }
        // If no suitable match found in any child, fallback to zero-width selection at caret
        ?: TextRange.create(caretOffset, caretOffset)

/** Process element for pairs, broken down into smaller functions */
private fun processElementForPairs(
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
    isExpanding: Boolean,
): TextRange? =
    element.node.getChildren(null).let { children ->
        when {
            children.size < 2 && isExpanding ->
                element.parent?.let { walkElementTreeExpanding(it, currentSelection, caretOffset) }
            children.size < 2 -> null
            else -> processPairElements(element, currentSelection, caretOffset, isExpanding)
        }
    }

/** Process element children for pair matching by walking from first to current element */
private fun processPairElements(
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
    isExpanding: Boolean,
): TextRange? {
    // Get all children
    val children = generateSequence(element.firstChild) { it.nextSibling }.toList()

    // Find the child that contains the caret or is closest to it
    val caretChild = children.findElementWithOffset(caretOffset)

    // Index of the child containing the caret
    val caretIndex = children.indexOf(caretChild)

    // Find matching pairs from first to caret child
    return children
        .take(caretIndex + 1)
        .mapNotNull { child ->
            val pairOpen =
                child
                    .takeIf { it.textLength <= PairConstants.MAX_PAIR_SIZE }
                    ?.text
                    .takeIf { it in PairConstants.PAIR_MATCHES } ?: return@mapNotNull null

            val pairClose = PairConstants.PAIR_MATCHES[pairOpen] ?: return@mapNotNull null

            // Find matching closing pair from the end back towards the caret
            val endChild =
                generateSequence(element.lastChild) { it.prevSibling }
                    .take(children.size - caretIndex)
                    .find { child ->
                        child.takeIf { it.textLength == pairClose.length }?.text == pairClose
                    } ?: return@mapNotNull null

            // Return the pair if found
            val pair =
                MatchedPair(
                    TextRange.create(child.textRange.startOffset, endChild.textRange.endOffset),
                    Pair(pairOpen, pairClose),
                )

            when {
                pair.range == currentSelection && isExpanding ->
                    element.parent?.let {
                        walkElementTreeExpanding(it, currentSelection, caretOffset)
                    }
                pair.range == currentSelection -> null
                else -> processPair(pair, currentSelection, isExpanding)
            }
        }
        .firstOrNull()
        ?: when {
            // No match found, continue with parent if expanding
            isExpanding ->
                element.parent?.let { walkElementTreeExpanding(it, currentSelection, caretOffset) }

            else -> null
        }
}

/** Check if selection is bordered by matching pair characters */
private fun checkSelectionTextForPair(
    editor: Editor,
    selection: TextRange,
    isExpanding: Boolean,
): TextRange? =
    editor.document.immutableCharSequence
        .subSequence(selection.startOffset, selection.endOffset)
        .toString()
        .getPair(selection, selection, isExpanding)

/** Find the element containing a selection */
tailrec fun getCurrentElement(element: PsiElement, currentSelection: TextRange): PsiElement? =
    when {
        element.textRange.contains(currentSelection) -> element
        element.parent != null -> getCurrentElement(element.parent, currentSelection)
        else -> null
    }
