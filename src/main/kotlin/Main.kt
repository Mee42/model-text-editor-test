package dev.mee42

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import java.util.*
import java.io.File


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
        return cursorRow - scrub // scrub rows are removed
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
            if(getVisableCursorRow() == terminalSize.rows - 3) {
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

fun renderBuffer(renderIntro: Boolean = false) {
    val terminal = globalState.screen
    val buffer = if(renderIntro) introBuffer else globalState.buffer
    for(r in 0 until terminal.terminalSize.rows - 2) {
        val line = buffer.getVisableLineOrNull(r)
        for(c in 0 until terminal.terminalSize.columns) {
            val char = when {
                renderIntro && c <= 5 -> ' '
                line == null && c == 0 -> '~'
                line == null -> ' '
                c == 0 -> ' '
                c in 1..3 -> (r + buffer.scrub + 1).toString().getOrNull(c - 1) ?: ' '
                c == 4 -> '|'
                c == 5 -> ' '
                line.length <= c - 6 -> ' '
                else -> {
                    val char = line[c - 6]
                    if(char == ' ' && line.substring(c - 6).all { it == ' ' }) ' ' else char // TODO trailing whitespace issue
                }
            }
            terminal.setCharacter(c, r, TextCharacter.fromCharacter(char)[0])
        }
    }

    val bottomLine = " " + ">".repeat(globalState.temporaryBuffers.size) + " [${globalState.mode.name}]".padEnd(10) + "  " + (buffer.cursorRow + 1) + "/" + buffer.data.size + "  " + buffer.cursorCol +
            (buffer.message?.let { "   $it" } ?: "")
    for(c in 0 until terminal.terminalSize.columns) {
        // we need to render the mode name, and, uh, say the line number/the other line number
        terminal.setCharacter(c, terminal.terminalSize.rows - 1,  TextCharacter.fromCharacter(bottomLine.getOrNull(c) ?: ' ')[0])
    }
    terminal.cursorPosition = TerminalPosition(buffer.cursorCol + 6, buffer.cursorRow - buffer.scrub)
    val mode = globalState.mode
    if(mode is CommandMode) {
        terminal.cursorPosition = TerminalPosition(
                bottomLine.length - mode.current.length + mode.cursorLocation,
                terminal.terminalSize.rows - 1)
    }
    if(renderIntro) terminal.cursorPosition = null // TODO make renderIntro a seperate thing, so it doesn't make the this code more complex than it needs to be
    terminal.refresh()
}

val introBuffer by lazy { Buffer(
        """
            
            
            ${'$'}NAME - a Model Text Editor like Vim
                 
                  :help       help
                  :tutorial   start the tutorial
                  :qa!        forcefully quit all
                  
        """.trimIndent().split("\n").map { "   $it" }.let(::LinkedList),
        null,
        0,
        0,
        0,
        globalState.buffer.terminalSize,
        null,
        writeable = true,
        unsaved = false
) }



class GlobalState(
        var buffer: Buffer,
        val temporaryBuffers: ArrayDeque<Buffer> = ArrayDeque(), // a stack, the oldest are at the front
        var mode: Mode,
        val screen: TerminalScreen,
) {
    fun escapeMode() {
        mode.exit(buffer)
        mode = NormalMode
        mode.enter(buffer)
        renderBuffer()
    }
    fun enterMode(newMode: Mode) {
        mode = newMode
        mode.enter(buffer) // assuming this needs a redraw
        renderBuffer()
    }
    fun newBuffer(newBuffer: Buffer, newMode: Mode = NormalMode) {
        temporaryBuffers.addLast(this.buffer)
        this.buffer = newBuffer
        this.mode = newMode
        renderBuffer()
    }
    // returns true if pop succeeded. Returns false if it didn't.
    // doesn't return if the program exits
    fun popBuffer(force: Boolean = false): Boolean {
        log("popping buffer. Force = $force")
        if(!force && buffer.unsaved) { // if it's unsaved, but forced, then it's fine
            buffer.message = "Error: Buffer unsaved"
            renderBuffer()
            return false
        }
        if(temporaryBuffers.isEmpty()) quit()
        mode.exit(buffer)
        buffer = temporaryBuffers.removeLast()
        mode = NormalMode
        renderBuffer()
        return true
    }

}

fun errorAndBuffer(str: String) {
    globalState.buffer.message = "Error: $str"
    log("Error: $str")
}

interface Mode {
    fun keystrokeIn(key: KeyStroke, buffer: Buffer)
    fun enter(buffer: Buffer){}
    fun exit(buffer: Buffer){}
    val name: String
}

lateinit var globalState: GlobalState

fun main(args: Array<String>) {
    log("\n\n\n\n== starting. Args: ${args.toList()}")
    // for no, no command line flags
    val file = args.firstOrNull()?.let(::File)
    
    val terminal = DefaultTerminalFactory().createScreen()
    terminal.startScreen()
    log("size is ${terminal.terminalSize}")

    val starterBuffer = Buffer(
            LinkedList((if(file?.exists() == true) file.readLines(Charsets.UTF_8) else null) ?: listOf("")),
            if(file != null) args.firstOrNull() else null,
            0,
            0,
            0,
            terminal.terminalSize,
            null,
            writeable = true,
            unsaved = false
    )

    globalState = GlobalState(starterBuffer, ArrayDeque(), NormalMode, terminal)
    var intro = file == null // don't show the intro if there's a file loaded

    while(true) {
        renderBuffer(intro)
        intro = false
        val input = terminal.readInput()
        globalState.buffer.message = null
        globalState.mode.keystrokeIn(input, globalState.buffer)

        if(globalState.buffer.cursorRow < 0) error("overflow top")
        if(globalState.buffer.cursorCol < 0) error("overflow left")
        if(globalState.buffer.cursorCol > terminal.terminalSize.columns) error("overflow right")
        log("after key input (${input.keyType}) ${if(input.keyType == KeyType.Character) input.character else ""}  -  ${globalState.buffer.cursorRow}, ${globalState.buffer.cursorCol}  scrub = ${globalState.buffer.scrub}")
    }
}
