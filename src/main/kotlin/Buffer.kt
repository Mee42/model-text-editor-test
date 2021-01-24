package dev.mee42

import com.googlecode.lanterna.TerminalSize
import java.util.*

data class Buffer(
        var data: LinkedList<String>,
        var filename: String?,
        var cursorRow: Int,
        var cursorCol: Int,
        var scrub: Int, // the cursorRow is independent of the scrubAmount
        var terminalSize: TerminalSize,
        var message: String?,
        var writeable: Boolean,
        var unsaved: Boolean
) {
    fun getLine(lineNumber: Int): String {
        return data[lineNumber]
    }
    fun setLine(lineNumber: Int, new: String) {
        data[lineNumber] = new
    }
    fun addLine(new: String, index: Int = data.size) {
        data.add(index, new)
    }
    fun getVisableLine(row: Int): String {
        return getVisableLineOrNull(row) ?: error("row $row does not exist")
    }
    fun getVisableLineOrNull(row: Int): String? {
        return data.getOrNull(row + scrub)
    }
    fun getVisableCursorRow(): Int {
        return cursorRow - scrub + 1 // scrub rows are removed
    }
    fun getCurrentLine(): String {
        return getLine(cursorRow)
    }
    fun setCurrentLine(new: String) {
        setLine(cursorRow, new)
    }
    fun shiftColLeftIfNeeded() {
        cursorCol = min(cursorCol, getCurrentLine().length)
    }
    fun removeLine(row: Int): String {
        return data.removeAt(row)
    }
    fun cursorDown(): Boolean {
        if(cursorRow < data.size - 1) {
            if(terminalSize.rows - 3 <= globalState.visualCursorRow) {
                scrub++
            }
            cursorRow++
            shiftColLeftIfNeeded()
            return true
        }
        return false // didn't move down, end of file
    }
    fun cursorUp(): Boolean {
        if(cursorRow > 0) {
            cursorRow--
            if(getVisableCursorRow() == 0) {
                if(scrub > 0) scrub--
            }
            shiftColLeftIfNeeded()
            return true
        }
        return false
    }
    fun x() {

    }
    fun cursorLeft() {
        if(cursorCol > 0) cursorCol--
    }
    fun cursorRight() {
        if(cursorCol < getCurrentLine().length) cursorCol++
    }
    fun insertLineBelowCursor(content: String = "") {
        data.add(cursorRow, content)
    }
}