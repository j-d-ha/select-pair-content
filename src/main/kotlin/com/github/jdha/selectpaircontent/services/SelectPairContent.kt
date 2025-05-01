package com.github.jdha.selectpaircontent.services

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.refactoring.actions.BaseRefactoringAction
import kotlin.collections.get

/** Package for the SelectPairContent plugin functionality. */
// Initialize logger for the main function
private val LOG = Logger.getInstance("com.github.jdha.selectpaircontent.services.SelectPairContent")

/** Constants for the plugin functionality. */
object PairConstants {
    /**
     * Map of matching typing pairs. Key is the ending character, value is the starting character.
     */
    val MATCHED_PAIRS =
        mapOf('\'' to '\'', '"' to '"', '`' to '`', '[' to ']', '(' to ')', '{' to '}', '<' to '>')

    /** Minimum selection size to consider for matching pairs. */
    const val MINIMUM_SELECTION_SIZE = 2
}

fun Caret.selectEnclosingTypingPairsExpanding(dataContext: DataContext) {
    val elementAtCaret = getElementAtCaret(dataContext) ?: return

    val currentSelection = TextRange.create(selectionStart, selectionEnd)
    val caretOffset = offset

    val newSelection = getExpandSelection(elementAtCaret, currentSelection, caretOffset)

    newSelection?.let { setSelection(it.startOffset, it.endOffset) }
}

fun Caret.selectEnclosingTypingPairsShrinking(dataContext: DataContext, editor: Editor) {
    val elementAtCaret = getElementAtCaret(dataContext) ?: return

    val currentSelection = TextRange.create(selectionStart, selectionEnd)
    val caretOffset = offset

    val newSelection = getShrinkSelection(elementAtCaret, editor, currentSelection, caretOffset)

    // If already just a caret position with no selection, nothing to shrink
    if (currentSelection.isEmpty) {
        return
    }

    newSelection?.let { setSelection(it.startOffset, it.endOffset) }
}

fun walkElementAndParents(element: PsiElement) {
    element.firstChild?.let { child -> if (child.node.elementType.toString() == "FILE") return }

    println("Element: ${element.javaClass.simpleName}, Text: ${element.text}")

    // Get all child nodes from the AST
    val childNodes = element.node.getChildren(null)

    // Print all child nodes
    childNodes.forEachIndexed { index, node ->
        val childElement = node.psi
        println("  Child $index: ${childElement.javaClass.simpleName}, Text: ${childElement.text}")

        // Check if this is a leaf element (no further children in the AST)
        val isLeaf = node.firstChildNode == null
        if (isLeaf) {
            println("    --> This is a leaf element")
        }
    }

    // Process parent if it exists
    val parent = element.parent
    if (parent != null) {
        println("\nMoving up to parent: ${parent.javaClass.simpleName}\n")
        walkElementAndParents(parent)
    }
}

fun getElementAtCaret(dataContext: DataContext): PsiElement? {
    val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return null
    val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return null
    val templateFile = PsiUtilCore.getTemplateLanguageFile(psiFile)

    // Use a template file if available, otherwise use a regular psi file
    return BaseRefactoringAction.getElementAtCaret(editor, templateFile ?: psiFile)
}

fun getExpandSelection(
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
): TextRange? {
    // if element has no children and end of current selection is not the start of the element,
    // attempt to get selection from element
    if (
        element.children.none() &&
            !currentSelection.contains(element.textRange) &&
            currentSelection.endOffset > element.textRange.startOffset &&
            currentSelection.startOffset < element.textRange.endOffset
    ) {
        handleNoChildrenExpanding(element, currentSelection)?.let {
            return it
        }
    }

    return getParentSelection(element.parent ?: return null, currentSelection, caretOffset)
}

fun handleNoChildrenExpanding(element: PsiElement, currentSelection: TextRange): TextRange? {
    val elementText = element.text

    if (elementText.length < PairConstants.MINIMUM_SELECTION_SIZE) {
        return null
    }

    val firstChar = elementText.first()
    val lastChar = elementText.last()

    if (
        lastChar !in PairConstants.MATCHED_PAIRS.keys ||
            PairConstants.MATCHED_PAIRS[lastChar] != firstChar
    )
        return null

    val fullPairRange = element.textRange

    // if active selection is same as full pair range, return null
    if (currentSelection == fullPairRange) return null

    return determineNextSelectionRange(currentSelection, fullPairRange)
}

fun determineNextSelectionRange(currentSelection: TextRange, fullPairRange: TextRange): TextRange {
    val innerContent = TextRange.create(fullPairRange.startOffset + 1, fullPairRange.endOffset - 1)

    return if (currentSelection == innerContent) {
        fullPairRange
    } else {
        innerContent
    }
}

tailrec fun getParentSelection(
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
): TextRange? {
    element.firstChild?.let { child ->
        if (child.node?.elementType.toString() == "FILE") return null
    }

    val elementTextRange = element.textRange

    if (
        elementTextRange == null ||
            elementTextRange == currentSelection ||
            (elementTextRange.startOffset >= currentSelection.startOffset &&
                elementTextRange.endOffset <= currentSelection.endOffset) ||
            elementTextRange.startOffset == caretOffset ||
            elementTextRange.endOffset == caretOffset
    )
        return getParentSelection(element.parent ?: return null, currentSelection, caretOffset)

    // Check if the element has at least two children
    val children = element.node.getChildren(null)

    if (children.size < 2)
        return getParentSelection(element.parent ?: return null, currentSelection, caretOffset)

    val firstChild = children.first().psi
    val lastChild = children.last().psi

    firstChild.text
    lastChild.text

    // Get the first and last characters from the children
    val firstChar = if (firstChild.textLength == 1) firstChild.text.first() else null
    val lastChar = if (lastChild.textLength == 1) lastChild.text.first() else null

    if (firstChar == null && lastChar == null)
        return getParentSelection(element.parent ?: return null, currentSelection, caretOffset)

    // Perfect match case - existing functionality
    if (
        firstChar in PairConstants.MATCHED_PAIRS &&
            lastChar == PairConstants.MATCHED_PAIRS[firstChar]
    ) {
        // Check if the last character is the matching value for the first character
        if (element.textRange == currentSelection)
            return getParentSelection(element.parent ?: return null, currentSelection, caretOffset)

        return determineNextSelectionRange(currentSelection, element.textRange)
    }

    // New functionality - first letter matches but last doesn't
    if (
        firstChar in PairConstants.MATCHED_PAIRS &&
            (lastChar != PairConstants.MATCHED_PAIRS[firstChar])
    ) {

        // Start with the last child and move to previous siblings
        var currentElement = lastChild
        var previousSibling = lastChild.prevSibling

        while (previousSibling != null) {
            val prevChar =
                if (previousSibling.textLength == 1) previousSibling.text.firstOrNull() else null

            // If we found a match, create a new text range and determine the next selection
            if (prevChar == PairConstants.MATCHED_PAIRS[firstChar]) {
                val newRange =
                    TextRange.create(
                        firstChild.textRange.startOffset,
                        previousSibling.textRange.endOffset,
                    )

                if (currentSelection == newRange)
                    return getParentSelection(
                        element.parent ?: return null,
                        currentSelection,
                        caretOffset,
                    )

                return determineNextSelectionRange(currentSelection, newRange)
            }

            currentElement = previousSibling
            previousSibling = currentElement.prevSibling
        }
    }

    // New functionality - last letter matches but first doesn't
    val matchingFirstChars = PairConstants.MATCHED_PAIRS.filterValues { it == lastChar }.keys
    if (lastChar != null && matchingFirstChars.isNotEmpty() && (firstChar !in matchingFirstChars)) {

        // Start with the first child and move to next siblings
        var currentElement = firstChild
        var nextSibling = firstChild.nextSibling

        while (nextSibling != null) {
            val nextChar = if (nextSibling.textLength == 1) nextSibling.text.firstOrNull() else null

            // If we found a match, create a new text range and determine the next selection
            if (nextChar in matchingFirstChars) {
                val newRange =
                    TextRange.create(
                        nextSibling.textRange.startOffset,
                        lastChild.textRange.endOffset,
                    )

                if (currentSelection == newRange)
                    return getParentSelection(
                        element.parent ?: return null,
                        currentSelection,
                        caretOffset,
                    )

                return determineNextSelectionRange(currentSelection, newRange)
            }

            currentElement = nextSibling
            nextSibling = currentElement.nextSibling
        }
    }

    // Recursively check the parent element
    return getParentSelection(element.parent ?: return null, currentSelection, caretOffset)
}

fun checkSelectionForMatchingPair(editor: Editor, selection: TextRange): TextRange? {
    // Ensure the selection has at least 2 characters
    if (selection.length < PairConstants.MINIMUM_SELECTION_SIZE) {
        return null
    }

    val content = editor.document.immutableCharSequence
    val firstChar = content[selection.startOffset]
    val lastChar = content[selection.endOffset - 1]

    return when {
        firstChar in PairConstants.MATCHED_PAIRS &&
            lastChar == PairConstants.MATCHED_PAIRS[firstChar] ->
            TextRange.create(selection.startOffset + 1, selection.endOffset - 1)
        else -> null
    }
}

fun getShrinkSelection(
    element: PsiElement,
    editor: Editor,
    currentSelection: TextRange,
    caretOffset: Int,
): TextRange? {
    return getCurrentElement(element, currentSelection)?.let {
        checkSelectionForMatchingPair(editor, currentSelection)
            ?: walkElementsShrinking(it, currentSelection, caretOffset)
    }
}

tailrec fun getCurrentElement(element: PsiElement, currentSelection: TextRange): PsiElement? =
    when {
        element.textRange.contains(currentSelection) -> element
        element.parent != null -> getCurrentElement(element.parent, currentSelection)
        else -> null
    }

tailrec fun walkElementsShrinking(
    element: PsiElement,
    currentSelection: TextRange,
    caretOffset: Int,
): TextRange? {
    element.children.forEach { child ->
        if (!child.textRange.contains(caretOffset)) return@forEach

        if (child.children.none()) return handleNoChildrenShrinking(child, currentSelection)

        val children = child.node.getChildren(null)
        if (children.size < 2) return walkElementsShrinking(child, currentSelection, caretOffset)

        val firstChild = children.first().psi
        val lastChild = children.last().psi

        // Get the first and last characters from the children
        val firstChar = if (firstChild.textLength == 1) firstChild.text.first() else null
        val lastChar = if (lastChild.textLength == 1) lastChild.text.first() else null

        if (firstChar == null && lastChar == null)
            return walkElementsShrinking(child, currentSelection, caretOffset)

        // Perfect match case - existing functionality
        if (
            firstChar in PairConstants.MATCHED_PAIRS &&
                lastChar == PairConstants.MATCHED_PAIRS[firstChar]
        ) {
            if (child.textRange == currentSelection)
                return walkElementsShrinking(child, currentSelection, caretOffset)
            return child.textRange
        }

        // New functionality - first letter matches but last doesn't
        if (
            firstChar in PairConstants.MATCHED_PAIRS &&
                (lastChar != PairConstants.MATCHED_PAIRS[firstChar])
        ) {

            // Start with the last child and move to previous siblings
            var currentElement = lastChild
            var previousSibling = lastChild.prevSibling

            while (previousSibling != null) {
                val prevChar =
                    if (previousSibling.textLength == 1) previousSibling.text.first() else null

                // If we found a match, return the range from firstChild to previousSibling
                if (prevChar == PairConstants.MATCHED_PAIRS[firstChar]) {
                    return TextRange.create(
                        firstChild.textRange.startOffset,
                        previousSibling.textRange.endOffset,
                    )
                }

                currentElement = previousSibling
                previousSibling = currentElement.prevSibling
            }

            // No match found, continue recursion
            return walkElementsShrinking(child, currentSelection, caretOffset)
        }

        // New functionality - last letter matches but first doesn't
        val matchingFirstChars = PairConstants.MATCHED_PAIRS.filterValues { it == lastChar }.keys
        if (matchingFirstChars.isNotEmpty() && (firstChar !in matchingFirstChars)) {

            // Start with the first child and move to next siblings
            var currentElement = firstChild
            var nextSibling = firstChild.nextSibling

            while (nextSibling != null) {
                val nextChar = if (nextSibling.textLength == 1) nextSibling.text.first() else null

                // If we found a match, return the range from nextSibling to lastChild
                if (nextChar in matchingFirstChars) {
                    return TextRange.create(
                        nextSibling.textRange.startOffset,
                        lastChild.textRange.endOffset,
                    )
                }

                currentElement = nextSibling
                nextSibling = currentElement.nextSibling
            }

            // No match found, continue recursion
            return walkElementsShrinking(child, currentSelection, caretOffset)
        }

        return walkElementsShrinking(child, currentSelection, caretOffset)
    }

    // return null
    return TextRange.create(caretOffset, caretOffset)
}

fun handleNoChildrenShrinking(element: PsiElement, currentSelection: TextRange): TextRange? {
    val elementText = element.text

    if (elementText.length < PairConstants.MINIMUM_SELECTION_SIZE) {
        return null
    }

    val firstChar = elementText.first()
    val lastChar = elementText.last()

    if (
        lastChar !in PairConstants.MATCHED_PAIRS.keys ||
            PairConstants.MATCHED_PAIRS[lastChar] != firstChar
    )
        return null

    val fullPairRange = element.textRange

    // if active selection is same as full pair range, return null
    if (currentSelection == fullPairRange) return null

    val innerContent = TextRange.create(fullPairRange.startOffset + 1, fullPairRange.endOffset - 1)

    return if (currentSelection == innerContent) {
        innerContent
    } else {
        fullPairRange
    }
}
