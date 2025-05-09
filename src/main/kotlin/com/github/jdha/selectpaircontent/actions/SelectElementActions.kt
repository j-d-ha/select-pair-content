package com.github.jdha.selectpaircontent.actions

import com.github.jdha.selectpaircontent.services.Direction
import com.github.jdha.selectpaircontent.services.SelectElementService
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actions.TextComponentEditorAction

// Expand out
class SelectElementExpandOutAction : TextComponentEditorAction(Handler()) {
    private class Handler : EditorActionHandler.ForEachCaret() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            CommonDataKeys.PROJECT.getData(dataContext)
                ?.service<SelectElementService>()
                ?.selectElement(editor, caret, dataContext, Direction.EXPAND_UP)
        }
    }
}

// Shrink down
class SelectElementShrinkDownAction : TextComponentEditorAction(Handler()) {
    private class Handler : EditorActionHandler.ForEachCaret() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            CommonDataKeys.PROJECT.getData(dataContext)
                ?.service<SelectElementService>()
                ?.selectElement(editor, caret, dataContext, Direction.SHRINK_DOWN)
        }
    }
}
