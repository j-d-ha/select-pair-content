/** Package for the SelectPairContent plugin functionality. */
package com.github.jdha.selectpaircontent.services

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.actions.BaseRefactoringAction

// Initialize logger for the main function
private val LOG = Logger.getInstance("com.github.jdha.selectpaircontent.services.SelectPairContent")

/** Constants for the plugin functionality. */
object PairConstants {
    /**
     * Map of matching typing pairs. Key is the ending character, value is the starting character.
     */
    val TYPING_PAIRS =
        mapOf(
            '\'' to '\'',
            '"' to '"',
            '`' to '`',
            ']' to '[',
            ')' to '(',
            '}' to '{',
            '>' to '<',
            '<' to '>',
        )

    /** Minimum selection size to consider for matching pairs. */
    const val MINIMUM_SELECTION_SIZE = 2
}

/** Utilities for handling typing pairs and text selections. */
object PairSelectionUtils {
    private val LOG = Logger.getInstance(PairSelectionUtils::class.java)

    /**
     * Checks if a character is an ending character of a typing pair.
     *
     * @param character The character to check.
     * @return True if the character is an ending character of a typing pair.
     */
    fun isTypingPairEndingChar(character: Char): Boolean =
        PairConstants.TYPING_PAIRS.containsKey(character)

    /**
     * Checks if the text range is enclosed by matching typing pairs.
     *
     * @param contents The document content.
     * @param textRange The text range to check.
     * @return True if the text range is enclosed by matching typing pairs.
     */
    fun isEnclosingTypingPairsRange(contents: CharSequence, textRange: TextRange): Boolean =
        textRange.length >= PairConstants.MINIMUM_SELECTION_SIZE &&
            isTypingPairEndingChar(contents[textRange.endOffset - 1]) &&
            PairConstants.TYPING_PAIRS[contents[textRange.endOffset - 1]] ==
                contents[textRange.startOffset]

    /**
     * Generates a sequence of PSI elements using the provided operator.
     *
     * @param element The starting PSI element.
     * @param operator A function that returns the next element in the sequence.
     * @return A sequence of PSI elements.
     */
    fun generateElementSequence(
        element: PsiElement?,
        operator: (PsiElement) -> PsiElement?,
    ): Sequence<PsiElement> =
        element?.let { generateSequence(it, operator).filter { e -> e.textRange != null } }
            ?: emptySequence()
}

/**
 * Gets the PSI element at the caret position.
 *
 * @param dataContext The data context for the operation.
 * @return The PSI element at the caret, or null if not found.
 */
fun getElementAtCaret(dataContext: DataContext): PsiElement? {
    val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return null
    val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return null
    val templateFile = PsiUtilCore.getTemplateLanguageFile(psiFile)

    // Use a template file if available, otherwise use a regular psi file
    return BaseRefactoringAction.getElementAtCaret(editor, templateFile ?: psiFile)
}

/** Selection configuration data class to encapsulate selection operation parameters. */
data class SelectionConfig(
    val isSelectionShrinking: Boolean = false,
    val caretOffset: Int = -1,
    val currentSelection: TextRange,
    val isCurrentSelectionMatchingPair: Boolean = false,
)

/** Selection service responsible for handling pair selection operations. */
class PairSelectionService {
    /**
     * Calculate the next selection based on the current selection and mode.
     *
     * @param startOffset The start offset of the potential selection.
     * @param endOffset The end offset of the potential selection.
     * @param config The selection configuration.
     * @return A text range for the new selection, or null if no valid selection is found.
     */
    fun calculateSelection(startOffset: Int, endOffset: Int, config: SelectionConfig): TextRange? {
        if (endOffset - startOffset < PairConstants.MINIMUM_SELECTION_SIZE) return null

        val innerSelection = TextRange.create(startOffset + 1, endOffset - 1)
        val outerSelection = TextRange.create(startOffset, endOffset)

        // For shrinking, ensure the selection contains the caret
        if (config.isSelectionShrinking && !outerSelection.contains(config.caretOffset)) {
            return null
        }

        return when {
            // For shrinking: behavior depends on current selection state
            config.isSelectionShrinking && config.currentSelection.contains(outerSelection) -> {
                if (config.isCurrentSelectionMatchingPair) {
                    // If selection already has matching pairs, get the inner content
                    innerSelection.takeIf {
                        it != config.currentSelection && it.contains(config.caretOffset)
                    }
                } else {
                    // Otherwise, get the full matching pair first
                    outerSelection.takeIf { it != config.currentSelection }
                }
            }

            // For expanding: find pairs containing the current selection
            !config.isSelectionShrinking &&
                innerSelection.contains(config.currentSelection) &&
                innerSelection != config.currentSelection -> innerSelection

            !config.isSelectionShrinking &&
                outerSelection.contains(config.currentSelection) &&
                outerSelection != config.currentSelection -> outerSelection

            else -> null
        }
    }

    /**
     * Find elements within the current selection.
     *
     * @param dataContext The data context for the operation.
     * @param currentSelection The current selection range.
     * @return A list of PSI elements within the selection.
     */
    fun findElementsWithinSelection(
        dataContext: DataContext,
        currentSelection: TextRange,
    ): List<PsiElement> {
        return CommonDataKeys.PSI_FILE.getData(dataContext)?.let { psiFile ->
            val result = mutableListOf<PsiElement>()

            fun traverse(element: PsiElement) {
                element.textRange?.let { range ->
                    if (currentSelection.contains(range)) {
                        result.add(element)
                    }
                }
                element.children.forEach { traverse(it) }
            }

            traverse(psiFile)
            result
        } ?: emptyList()
    }

    /**
     * Find matching sibling pairs for the given element.
     *
     * @param contents The document content.
     * @param element The PSI element to examine.
     * @param config The selection configuration.
     * @return A text range for the new selection, or null if no valid selection is found.
     */
    fun findMatchingSiblingPairs(
        contents: CharSequence,
        element: PsiElement,
        config: SelectionConfig,
    ): TextRange? {
        val nextSiblings =
            PairSelectionUtils.generateElementSequence(element) { it.nextSibling }
                .filter {
                    it.textLength == 1 &&
                        PairSelectionUtils.isTypingPairEndingChar(contents[it.textOffset])
                }

        val candidateRanges = mutableListOf<TextRange>()

        for (nextSibling in nextSiblings) {
            val endingChar = contents[nextSibling.textOffset]
            val expectedStartChar = PairConstants.TYPING_PAIRS[endingChar] ?: continue

            val prevSiblings =
                PairSelectionUtils.generateElementSequence(element) { it.prevSibling }
                    .filter { it.textLength == 1 && contents[it.textOffset] == expectedStartChar }

            for (prevSibling in prevSiblings) {
                calculateSelection(
                        prevSibling.textRange.startOffset,
                        nextSibling.textRange.endOffset,
                        config,
                    )
                    ?.let { candidateRanges.add(it) }
            }
        }

        return candidateRanges.firstOrNull()
    }

    /**
     * Find enclosing typing pairs for a given element.
     *
     * @param contents The document content.
     * @param element The PSI element to examine.
     * @param config The selection configuration.
     * @return A text range for the new selection, or null if no valid selection is found.
     */
    fun findEnclosingTypingPairs(
        contents: CharSequence,
        element: PsiElement,
        config: SelectionConfig,
    ): TextRange? {
        val elementRange = element.textRange ?: return null

        if (PairSelectionUtils.isEnclosingTypingPairsRange(contents, elementRange)) {
            return calculateSelection(elementRange.startOffset, elementRange.endOffset, config)
        }

        // If no parent elements, try sibling pairs
        if (PairSelectionUtils.generateElementSequence(element.parent) { it.parent }.none()) {
            return null
        }

        return findMatchingSiblingPairs(contents, element, config)
    }
}

/**
 * Extension function for the Caret class to select enclosing typing pairs. This is the main entry
 * point for the selection functionality.
 *
 * @param editor The editor instance.
 * @param dataContext The data context for the operation.
 * @param isSelectionShrinking Whether to shrink the current selection (true) or expand it (false).
 */
fun Caret.selectEnclosingTypingPairs(
    editor: Editor,
    dataContext: DataContext,
    isSelectionShrinking: Boolean = false,
) {
    val elementAtCaret =
        getElementAtCaret(dataContext)
            ?: run {
                LOG.debug("No element found at caret position")
                return
            }

    val currentSelection = TextRange.create(selectionStart, selectionEnd)
    val caretOffset = offset

    // If already just a caret position with no selection, nothing to shrink
    if (isSelectionShrinking && currentSelection.isEmpty) {
        return
    }

    // Determine if the current selection is already a matching pair
    val contents = editor.document.immutableCharSequence
    val isCurrentSelectionMatchingPair =
        PairSelectionUtils.isEnclosingTypingPairsRange(contents, currentSelection)

    val selectionService = PairSelectionService()
    val selectionConfig =
        SelectionConfig(
            isSelectionShrinking = isSelectionShrinking,
            caretOffset = caretOffset,
            currentSelection = currentSelection,
            isCurrentSelectionMatchingPair = isCurrentSelectionMatchingPair,
        )

    editor.project?.let { project ->
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)

        // Collect candidate elements based on expansion or shrinking mode
        val candidateElements =
            when {
                isSelectionShrinking ->
                    selectionService.findElementsWithinSelection(dataContext, currentSelection)
                else ->
                    PairSelectionUtils.generateElementSequence(elementAtCaret) { it.parent }
                        .toList()
            }

        // Find enclosing typing pairs for each candidate
        val candidateRanges =
            candidateElements
                .mapNotNull { candidate ->
                    selectionService.findEnclosingTypingPairs(contents, candidate, selectionConfig)
                }
                .let { ranges ->
                    // When shrinking, only consider ranges that contain the caret
                    if (isSelectionShrinking) ranges.filter { it.contains(caretOffset) } else ranges
                }

        // Select the appropriate range based on the operation mode
        val nextRange =
            when {
                isSelectionShrinking -> candidateRanges.maxByOrNull { it.length }
                else -> candidateRanges.firstOrNull()
            }

        // Apply the selection
        when {
            isSelectionShrinking && nextRange == null -> removeSelection()
            else -> nextRange?.let { setSelection(it.startOffset, it.endOffset) }
        }
    }
}
