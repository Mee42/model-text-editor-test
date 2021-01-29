package dev.mee42

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import java.io.File
import java.util.*


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
        log("after key input (${input.keyType}) ${if(input.keyType == KeyType.Character) input.character else ""}  -  ${globalState.buffer.cursorRow}, ${globalState.buffer.cursorCol}  scrub = ${globalState.buffer.scrub}")
    }
}
