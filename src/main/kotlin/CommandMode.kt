package dev.mee42

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import java.io.File
import java.io.IOException
import java.util.*


class CommandMode: Mode {
    var current = ""
    var cursorLocation = 0
    override fun keystrokeIn(key: KeyStroke, buffer: Buffer) {
        when (key.keyType) {
            KeyType.Character -> {
                if(cursorLocation == current.length) {
                    current += key.character
                } else {
                    current = current.substring(0, cursorLocation) + key.character + current.substring(cursorLocation)
                }
                cursorLocation++
            }
            KeyType.Backspace -> {
                current = when (cursorLocation) {
                    0 -> current
                    current.length -> current.substring(0, current.length - 1).also { cursorLocation-- }
                    else -> current.substring(0, cursorLocation - 1) + current.substring(cursorLocation).also { cursorLocation-- }
                }
            }
            KeyType.Delete -> {
                current = when {
                    current == "" -> "" // nothin to delete
                    cursorLocation == current.length -> current.substring(0, current.length - 1)
                    else -> current.substring(0, cursorLocation) + current.substring(cursorLocation + 1)
                }
            }
            KeyType.ArrowLeft -> {
                cursorLocation = max(0, cursorLocation - 1)
            }
            KeyType.ArrowRight -> {
                cursorLocation = min(current.length, cursorLocation + 1)
            }
            KeyType.Escape -> { globalState.escapeMode(); return }
            KeyType.Enter -> {
                // we need to consume it, parse the command, and run it
                val command0 = current.trim()
                val xs = command0.split(" ", limit = 2)
                val (command, args) = xs.first() to (xs.getOrNull(1) ?: "")
                val commanded = commands.firstOrNull { command in it.names } ?: 
                    Command { b, _ -> b.message = "Error: Unknown command ':$command'"}
                commanded.runner(buffer, args)
                
                current = ""
                cursorLocation = 0
                globalState.escapeMode()
                return // stop the message from getting overwritten
            }
        }
        buffer.message = ":$current"
    }
    override fun enter(buffer: Buffer) {
        buffer.message = ":$current"
    }

    override fun exit(buffer: Buffer) {
        if(buffer.message?.startsWith(":") == true) {
            buffer.message = null
        }

    }

    override val name: String = "COMMAND"
    
    // consume as many characters as you want, and then return the rest of the string to be reevaluated. Return "" to stop all evaluation
    class Command(vararg val names: String, val runner: (Buffer, String) -> Unit)
    
    
    
    fun quit(force: Boolean, all: Boolean) {
        do {
            if(!globalState.popBuffer(force)) break
        } while(all)
    }
    // takes in the arg. If arg is blank, assumed to be none. Returns true on success, false on error writing file
    fun write(arg: String, b: Buffer): Boolean {
        val filename = arg.takeUnless { it.isBlank() } ?: b.filename
        if(filename == null) {
            b.message = "Error: No filename"
            return false
        }
        try {
            File(filename).writeText(b.data.joinToString("\n") + "\n")
            b.unsaved = false
        } catch (e: IOException) {
            e.printStackTrace()
            b.message = "Error writing file: ${e.message}"
            return false
        }
        return true
    }

    private val commands = setOf(
            Command("q", "quit") { _, _ ->       quit(force = false, all = false) },
            Command("q!","quit!"){ _, _ ->       quit(force = true,  all = false) },
            Command("qa","quitall") { _, _ ->    quit(force = false, all = true)  },
            Command("qa!", "quitall!") { _, _ -> quit(force = true,  all = true)  },
            Command("w", "write") { b, str ->
                write(str, b)
            },
            Command("wq","writequit") { b, str ->
                if(write(str, b)) quit()
            },
            Command("wqa", "writequitall") { b, _ ->
                b.message = "Error: incomplete"
            },
            Command("h", "help") { b, _ ->
                // lets pretend there's only one help page lel
                val data = File("src/main/resources/help.txt").readLines().let(::LinkedList)
                val newBuffer = Buffer(
                        data = data,
                        filename = null,
                        cursorRow = 0,
                        cursorCol = 0,
                        scrub = 0,
                        terminalSize = b.terminalSize,
                        message = null,
                        writeable = false,
                        unsaved = false
                )
                globalState.newBuffer(newBuffer)
            },
            Command("b","buffer") { b, _ ->
                // print information about the current buffer
                b.message = ""//"Buffer: " + (if(b.writeable) "Writeable" else "Read-Only") + "  " +  (b.filename ?: "No File Name")
            }
    )
    
    
    
}


