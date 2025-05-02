package com.github.jdha.selectpaircontent.services

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.actions.BaseRefactoringAction

/** Constants and utility functions for pair matching */
object PairConstants {
    /** Map of matching pairs: opening char -> closing char */
    val PAIR_MATCHES =
        mapOf('\'' to '\'', '"' to '"', '`' to '`', '[' to ']', '(' to ')', '{' to '}', '<' to '>')

    /** Reverse map: closing char -> opening char */
    val OPENING_CHARS by lazy { PAIR_MATCHES.entries.associate { (k, v) -> v to k } }

    /** Minimum size for pair selection */
    const val MINIMUM_SELECTION_SIZE = 2

    /** Check if char is an opening character */
    fun isOpeningChar(char: Char): Boolean = char in PAIR_MATCHES

    /** Check if char is a closing character */
    fun isClosingChar(char: Char): Boolean = char in OPENING_CHARS

    /** Get matching opening char for a closing char */
    fun getMatchingOpeningChar(char: Char): Char? = OPENING_CHARS[char]

    /** Get matching closing char for an opening char */
    fun getMatchingClosingChar(char: Char): Char? = PAIR_MATCHES[char]

    /** Check if two chars form a valid pair */
    fun isValidPair(first: Char, last: Char): Boolean =
        isOpeningChar(first) && PAIR_MATCHES[first] == last
}

/** Get inner content of a range (excluding first and last characters) */
private fun TextRange.inner(): TextRange = TextRange.create(startOffset + 1, endOffset - 1)

/** Sealed class to represent a pair of characters with validity */
private sealed class PairResult {
    data class Valid(val range: TextRange) : PairResult()

    data class Invalid(val range: TextRange) : PairResult()
}

/** Extension functions for PsiElement to check for valid pairs */
private fun PsiElement.hasValidPair(): Boolean {
    val text = this.text
    if (text.length < PairConstants.MINIMUM_SELECTION_SIZE) return false
    return PairConstants.isValidPair(text.first(), text.last())
}

private fun PsiElement.pairTextRange(): TextRange? = if (hasValidPair()) textRange else null

/** Extract and validate character pair from element */
private fun validateElementPair(element: PsiElement): PairResult? {
    val elementText = element.text
    if (elementText.length < PairConstants.MINIMUM_SELECTION_SIZE) return null

    val firstChar = elementText.first()
    val lastChar = elementText.last()

    return when {
        PairConstants.isValidPair(firstChar, lastChar) -> PairResult.Valid(element.textRange)
        else -> PairResult.Invalid(element.textRange)
    }
}

/** Find and process character pairs in different modes */
private fun processPair(
    pairResult: PairResult,
    currentSelection: TextRange,
    isExpanding: Boolean,
): TextRange? {
    val fullRange =
        when (pairResult) {
            is PairResult.Valid -> pairResult.range
            is PairResult.Invalid -> return null
        }

    if (currentSelection == fullRange) return null

    val innerContent = fullRange.inner()

    return when {
        isExpanding -> if (currentSelection == innerContent) fullRange else innerContent
        else -> if (currentSelection == innerContent) innerContent else fullRange
    }
}

/** Common handler for element selection (works for both expanding and shrinking) */
private fun handleElementSelection(
    element: PsiElement,
    currentSelection: TextRange,
    isExpanding: Boolean,
): TextRange? = validateElementPair(element)?.let { processPair(it, currentSelection, isExpanding) }

/** Get PSI element at caret position */
fun getElementAtCaret(dataContext: DataContext): PsiElement? {
    val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return null
    val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return null
    val templateFile = PsiUtilCore.getTemplateLanguageFile(psiFile)

    return BaseRefactoringAction.getElementAtCaret(editor, templateFile ?: psiFile)
}

/** Check if a text range contains an offset */
private fun TextRange.contains(offset: Int): Boolean = startOffset <= offset && offset <= endOffset

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
        checkSelectionForMatchingPair(editor, currentSelection)?.also { matchingPairRange ->
            setSelection(matchingPairRange.startOffset, matchingPairRange.endOffset)
            return
        }

        // If that fails, try to shrink using the element tree
        shrinkSelection(element, currentSelection, offset)?.also { newRange ->
            setSelection(newRange.startOffset, newRange.endOffset)
        }
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
        handleElementSelection(element, currentSelection, true)?.let {
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
            child.children.isEmpty() -> handleElementSelection(child, currentSelection, false)
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
            children.size < 2 ->
                handleInsufficientChildren(element, currentSelection, caretOffset, isExpanding)
            else ->
                processPairElements(
                    children.first().psi,
                    children.last().psi,
                    element,
                    currentSelection,
                    caretOffset,
                    isExpanding,
                )
        }
    }

/** Handle case where element has insufficient children */
private fun handleInsufficientChildren(
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
    isExpanding: Boolean,
): TextRange? =
    when {
        isExpanding ->
            element.parent?.let { walkElementTreeExpanding(it, currentSelection, caretOffset) }
        else -> null
    }

/** Process first and last element children for pair matching */
private fun processPairElements(
    firstChild: PsiElement,
    lastChild: PsiElement,
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
    isExpanding: Boolean,
): TextRange? {
    // Extract characters if possible
    val firstChar = firstChild.takeIf { it.textLength == 1 }?.text?.firstOrNull()
    val lastChar = lastChild.takeIf { it.textLength == 1 }?.text?.firstOrNull()

    if (firstChar == null && lastChar == null) {
        return when {
            isExpanding ->
                element.parent?.let { walkElementTreeExpanding(it, currentSelection, caretOffset) }
            else -> null
        }
    }

    // Perfect matching pair case
    if (PairConstants.isValidPair(firstChar ?: Char.MIN_VALUE, lastChar ?: Char.MIN_VALUE)) {
        val pairRange = element.textRange
        if (pairRange == currentSelection) {
            return when {
                isExpanding ->
                    element.parent?.let {
                        walkElementTreeExpanding(it, currentSelection, caretOffset)
                    }
                else -> null
            }
        }

        return processPair(PairResult.Valid(pairRange), currentSelection, isExpanding)
    }

    // Try to find partial pairs
    return findPartialPair(
        firstChild,
        lastChild,
        firstChar,
        lastChar,
        currentSelection,
        caretOffset,
        isExpanding,
        element,
    )
}

/** Find partial pair matches (opening at start or closing at end) */
private fun findPartialPair(
    firstChild: PsiElement,
    lastChild: PsiElement,
    firstChar: Char?,
    lastChar: Char?,
    currentSelection: TextRange,
    caretOffset: Int,
    isExpanding: Boolean,
    element: PsiElement,
): TextRange? {
    // Try both cases with a helper function
    return findMatchInDirection(
        // Case 1: Opening at start, find matching closing
        startChar = firstChar,
        isStartCharValid = { PairConstants.isOpeningChar(it) },
        getMatchingChar = { PairConstants.getMatchingClosingChar(it) },
        startElement = lastChild,
        siblingFn = { it.prevSibling },
        createRange = { match ->
            TextRange.create(firstChild.textRange.startOffset, match.textRange.endOffset)
        },
        element,
        currentSelection,
        caretOffset,
        isExpanding,
    )
        ?: findMatchInDirection(
            // Case 2: Closing at end, find matching opening
            startChar = lastChar,
            isStartCharValid = { PairConstants.isClosingChar(it) },
            getMatchingChar = { PairConstants.getMatchingOpeningChar(it) },
            startElement = firstChild,
            siblingFn = { it.nextSibling },
            createRange = { match ->
                TextRange.create(match.textRange.startOffset, lastChild.textRange.endOffset)
            },
            element,
            currentSelection,
            caretOffset,
            isExpanding,
        )
        ?: when {
            // No match found, continue with parent if expanding
            isExpanding ->
                element.parent?.let { walkElementTreeExpanding(it, currentSelection, caretOffset) }
            else -> null
        }
}

/** Improved function to find matching direction using sequences */
private fun findMatchInDirection(
    startChar: Char?,
    isStartCharValid: (Char) -> Boolean,
    getMatchingChar: (Char) -> Char?,
    startElement: PsiElement,
    siblingFn: (PsiElement) -> PsiElement?,
    createRange: (PsiElement) -> TextRange,
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
    isExpanding: Boolean,
): TextRange? {
    // Skip if the starting character isn't valid
    if (startChar == null || !isStartCharValid(startChar)) return null

    // Find the matching character
    val matchingChar = getMatchingChar(startChar)

    // Use sequence for better lazy evaluation
    val matchElement =
        generateSequence(siblingFn(startElement)) { siblingFn(it) }
            .find { it.textLength == 1 && it.text.firstOrNull() == matchingChar } ?: return null

    // Create the pair range
    val pairRange = createRange(matchElement)

    // Handle the case where the selection already matches the pair
    if (pairRange == currentSelection)
        return when {
            isExpanding ->
                element.parent?.let { walkElementTreeExpanding(it, currentSelection, caretOffset) }
            else -> null
        }

    // Process the valid pair
    return processPair(PairResult.Valid(pairRange), currentSelection, isExpanding)
}

/** Check if selection is bordered by matching pair characters */
fun checkSelectionForMatchingPair(editor: Editor, selection: TextRange): TextRange? {
    if (selection.length < PairConstants.MINIMUM_SELECTION_SIZE) return null

    val content = editor.document.immutableCharSequence
    val firstChar = content[selection.startOffset]
    val lastChar = content[selection.endOffset - 1]

    return when {
        PairConstants.isValidPair(firstChar, lastChar) -> selection.inner()
        else -> null
    }
}

/** Find the element containing a selection */
tailrec fun getCurrentElement(element: PsiElement, currentSelection: TextRange): PsiElement? =
    when {
        element.textRange.contains(currentSelection) -> element
        element.parent != null -> getCurrentElement(element.parent, currentSelection)
        else -> null
    }
