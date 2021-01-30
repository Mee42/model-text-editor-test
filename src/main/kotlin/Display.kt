package dev.mee42

import com.googlecode.lanterna.TerminalPosition
import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.TextColor


val backgroundColor = TextColor.RGB(30, 30, 30)

fun renderBuffer(introBuffer: Boolean = false) {
    // Render in passes, not with r, c iterations
    if(globalState.fullRewrite) {
        globalState.screen.clear()
        globalState.fullRewrite = false
    }

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
        for ((index, line) in linesToRender.withIndex()) {
            if(row >= textRows) break // no need to do extra work

            val chunks = line.chunked(textColumns).takeUnless { it.isEmpty() } ?: listOf("")
            lineNumbers.add(index + buffer.scrub + 1)
            lineNumbers.addAll((-1).repeatIntoList(chunks.size - 1))
            lines.addAll(chunks)
            row += chunks.size
        }
    }
    for(r in 0 until buffer.terminalSize.rows - 2) {
        val lineToRender = lineNumbers.getOrNull(r)?.let {
            (it.takeUnless { it == -1 }?.toString() ?: "" ).padEnd(3) + "| " + lines[r]
        } ?: "~ "
        val colorsToRender = CharData(TextColor.ANSI.WHITE, null).repeatIntoList(5) +
                (lines.getOrNull(r)?.let { highlight(it) } ?: emptyList())

        for(c in 0 until buffer.terminalSize.columns) {
            globalState.screen.setCharacter(c, r, TextCharacter.fromCharacter(if(c < lineToRender.length)  lineToRender[c] else ' ',
                    colorsToRender.getOrNull(c)?.foreground ?: TextColor.ANSI.DEFAULT,
                    colorsToRender.getOrNull(c)?.background ?: backgroundColor)[0])
        }
    }
    val bottomLine = "" + ">".repeat(globalState.temporaryBuffers.size) + " [${globalState.mode.name}]".padEnd(10) + 
          "  " + (buffer.cursorRow + 1) + "/" + buffer.data.size + "  " + buffer.cursorCol +
            (buffer.message?.let { "   $it" } ?: "")
    for(c in 0 until globalState.screen.terminalSize.columns) {
        // we need to render the mode name, and, uh, say the line number/the other line number
        // https://ux.stackexchange.com/questions/37641/will-a-paste-button-increase-usability?rq=1
        globalState.screen.setCharacter(
            c,
            globalState.screen.terminalSize.rows - 1,  
            TextCharacter.fromCharacter(bottomLine.getOrNull(c) ?: ' ', TextColor.ANSI.DEFAULT, backgroundColor)[0]
        )    
    }


    val cursorRow = lineNumbers.indexOfFirst { buffer.cursorRow == it - 1 } + buffer.cursorCol / textColumns
    val cursorCol = buffer.cursorCol % textColumns + 5
    globalState.screen.cursorPosition = TerminalPosition(cursorCol, cursorRow)
    globalState.visualCursorRow = cursorRow
    globalState.screen.refresh()
}


 // this is actually the code that does the syntax highlighting

// highlighting code

class CharData(val foreground: TextColor, val background: TextColor?)


val tokenTypes = listOf("KEYWORD","IDENTIFIER","NUMBER","STRING","OPERATOR", "DEFAULT")

val regexs = listOf(
        Regex("^(fun|val|var|import|for|if|else|switch|package|return|class|interface|enum|data)\\b") to "KEYWORD",
        Regex("^[a-z_][a-zA-Z0-9_]*") to "IDENTIFIER",
        Regex("^[A-Z][a-zA-z0-9_]*") to "TYPE",
        Regex("^[0-9]+") to "NUMBER",
        Regex("^\".*?\"") to "STRING",
        Regex("^//.*") to "COMMENT",
        Regex("^[+\\-*=/]") to "OPERATOR"
)
fun highlightFor(type: String): CharData {
    val foreground = when(type) {
        "KEYWORD" -> TextColor.ANSI.YELLOW
        "IDENTIFIER" -> TextColor.ANSI.WHITE
        "TYPE" -> TextColor.ANSI.MAGENTA_BRIGHT
        "NUMBER" -> TextColor.ANSI.BLUE_BRIGHT
        "STRING" -> TextColor.ANSI.GREEN
        "OPERATOR" -> TextColor.ANSI.RED
        "COMMENT" -> TextColor.ANSI.BLACK_BRIGHT;
        else -> TextColor.ANSI.WHITE
    }
    return CharData(foreground, null)
}

fun highlight(line: String): List<CharData> {
    val list = mutableListOf<CharData>()
    var restOfLine = line.subSequence(0, line.length)
    while(restOfLine.isNotBlank()) {
        val result = regexs.firstOrNull { (regex, _) -> regex.find(restOfLine) != null }
        if(result == null) {
            list.add(CharData(TextColor.ANSI.WHITE, null))
            restOfLine = restOfLine.subSequence(1, restOfLine.length)
        } else {
            val (regex, type) = result
            val token = regex.find(restOfLine)!!.value
            restOfLine = restOfLine.subSequence(token.length, restOfLine.length)
            for (n in token.indices) list.add(highlightFor(type))
        }
    }
    return list
}











