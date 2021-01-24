package dev.mee42

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import java.io.File
import java.util.*


fun renderBuffer(introBuffer: Boolean = false) {
    // Render in passes, not with r, c iterations
    
    // start with the text
    val buffer = if(introBuffer) generateIntroBuffer() else globalState.buffer
    val bufferWidth = buffer.terminalSize.columns // the total number of columns

    val lineNumbers = mutableListOf<Int>()
    val lines = mutableListOf<String>() // TODO optimize out to immediate set


    val textColumns = bufferWidth - 5 // how many columns the text is allowed to take up
    val textRows = buffer.terminalSize.rows - 1 // idk, lets just say
    run {
        val linesToRender = buffer.data.subList(buffer.scrub, buffer.data.size) // trim the unrendered part of the list off
        var row = 0
//        log("lines to render: $linesToRender")
        for ((index, line) in linesToRender.withIndex()) {
            if(row >= textRows) break // no need to do extra work
            val chunks = line.chunked(textColumns).takeUnless { it.isEmpty() } ?: listOf("")
            lineNumbers.add(index + buffer.scrub + 1)
//            log("line \"$line\", $chunks adding ${row + buffer.scrub + 1} ")
            lineNumbers.addAll((-1).repeatIntoList(chunks.size - 1))
            lines.addAll(chunks)
            row += chunks.size
        }
    }
//    log("$lines; $lineNumbers")
    for(r in 0 until buffer.terminalSize.rows - 2) {
        val lineToRender = lineNumbers.getOrNull(r)?.let {
            (it.takeUnless { it == -1 }?.toString() ?: "" ).padEnd(3) + "| " + lines[r]
        } ?: "~ "
        for(c in 0 until buffer.terminalSize.columns) {
            globalState.screen.setCharacter(c, r, TextCharacter.fromCharacter(if(c < lineToRender.length)  lineToRender[c] else ' ')[0])
        }
    }
    val bottomLine = "" + ">".repeat(globalState.temporaryBuffers.size) + " [${globalState.mode.name}]".padEnd(10) + "  " + (buffer.cursorRow + 1) + "/" + buffer.data.size + "  " + buffer.cursorCol +
            (buffer.message?.let { "   $it" } ?: "")
    for(c in 0 until globalState.screen.terminalSize.columns) {
        // we need to render the mode name, and, uh, say the line number/the other line number
        globalState.screen.setCharacter(c, globalState.screen.terminalSize.rows - 1,  TextCharacter.fromCharacter(bottomLine.getOrNull(c) ?: ' ')[0])
    }

    val cursorRow = lineNumbers.indexOfFirst { buffer.cursorRow == it - 1 } + buffer.cursorCol / textColumns
    val cursorCol = buffer.cursorCol % textColumns + 5
    globalState.screen.cursorPosition = TerminalPosition(cursorCol, cursorRow)
    globalState.visualCursorRow = cursorRow
    globalState.screen.refresh()
}

fun <T> T.repeatIntoList(n: Int): List<T> {
    val x = mutableListOf<T>()
    for(i in 0 until n) x.add(this)
    return x
}


class GlobalState(
        var buffer: Buffer,
        val temporaryBuffers: ArrayDeque<Buffer> = ArrayDeque(), // a stack, the oldest are at the front
        var mode: Mode,
        val screen: TerminalScreen,
) {
    
    var visualCursorRow = 0
    
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

//        if(globalState.buffer.cursorRow < 0) error("overflow top")
//        if(globalState.buffer.cursorCol < 0) error("overflow left")
//        if(globalState.buffer.cursorCol > terminal.terminalSize.columns) error("overflow right")
        log("after key input (${input.keyType}) ${if(input.keyType == KeyType.Character) input.character else ""}  -  ${globalState.buffer.cursorRow}, ${globalState.buffer.cursorCol}  scrub = ${globalState.buffer.scrub}")
    }
}
