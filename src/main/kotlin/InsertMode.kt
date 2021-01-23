package dev.mee42

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType

object InsertMode: Mode {
    override fun keystrokeIn(key: KeyStroke, buffer: Buffer) {
        when (key.keyType) {
            KeyType.Escape -> globalState.escapeMode()
            KeyType.ArrowDown -> buffer.cursorDown()
            KeyType.ArrowUp -> buffer.cursorUp()
            KeyType.ArrowLeft -> buffer.cursorLeft()
            KeyType.ArrowRight -> buffer.cursorRight()
            KeyType.Enter -> {
                val line = buffer.getCurrentLine()
                val firstPart = line.substring(0, buffer.cursorCol)
                val secondPart = if (buffer.cursorCol != line.length) line.substring(buffer.cursorCol) else ""
                buffer.setCurrentLine(secondPart)
                buffer.insertLineBelowCursor(firstPart)
                buffer.cursorDown()
                buffer.cursorCol = 0
                buffer.unsaved = true
            }
            KeyType.Backspace -> {
                if (buffer.cursorCol == 0) {
                    if (buffer.cursorRow != 0) { // do nothing if on the first line
                        // remove the newline
                        val line = buffer.removeLine(buffer.cursorRow)
                        buffer.cursorRow--
                        val still = buffer.getLine(buffer.cursorRow)
                        buffer.setLine(buffer.cursorRow, still + line)
                        buffer.cursorCol = still.length
                        buffer.unsaved = true
                    }
                } else {
                    buffer.cursorCol--
                    val line = buffer.getCurrentLine()
                    val newLine = line.substring(0, buffer.cursorCol) + if (buffer.cursorCol + 1 == line.length) "" else line.substring(buffer.cursorCol + 1)
                    buffer.setLine(buffer.cursorRow, newLine)
                    buffer.unsaved = true
                }
            }
            KeyType.Character -> {
                val line = buffer.getLine(buffer.cursorRow)
                buffer.setLine(buffer.cursorRow, line.substring(0, buffer.cursorCol) + key.character + line.substring(buffer.cursorCol))
                buffer.cursorCol++
                buffer.unsaved = true
            }
            // TODO <Delete>
            else -> {
                errorAndBuffer("don't know how to handle that character: ${key.keyType}")
            }
        }
    }


    override val name: String = "INSERT"
}