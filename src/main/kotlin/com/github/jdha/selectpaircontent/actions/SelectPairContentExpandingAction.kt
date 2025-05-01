package com.github.jdha.selectpaircontent.actions

import com.github.jdha.selectpaircontent.services.selectEnclosingTypingPairsExpanding
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actions.TextComponentEditorAction

class SelectPairContentExpandingAction : TextComponentEditorAction(Handler()) {
    private class Handler : EditorActionHandler.ForEachCaret() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
            caret?.selectEnclosingTypingPairsExpanding(dataContext)
        }
    }
}
