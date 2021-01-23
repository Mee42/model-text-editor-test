package dev.mee42

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

object NormalMode: Mode {
    override fun keystrokeIn(key: KeyStroke, buffer: Buffer) {
        when(key.character) {
            'i' -> {
                if(buffer.writeable) globalState.enterMode(InsertMode)
                else buffer.message = "Error: Buffer is flagged as unwritable"
            }
            ':' -> globalState.enterMode(CommandMode())
        }
        when(key.keyType) {
            KeyType.ArrowDown -> buffer.cursorDown()
            KeyType.ArrowUp -> buffer.cursorUp()
            KeyType.ArrowLeft -> buffer.cursorLeft()
            KeyType.ArrowRight -> buffer.cursorRight()
        }
    }



    override val name: String = "NORMAL"

}