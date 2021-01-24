package dev.mee42

import java.util.*


// steals the width of the terminal to render roughly in the center
fun generateIntroBuffer(): Buffer {
    log("generating intro buffer")
    val width = globalState.screen.terminalSize.columns - 10
    // IMPORTANT: the :command lines have trailing whitespace so they pad to the same length
    val text = """
 
${'$'}NAME - a Model Text Editor like Vim
 
:help       help               
:tutorial   start the tutorial 
:qa!        forcefully quit all
                  
""".split("\n").map {
        var x = it
        while(x.length < width) x = " $x "
        x
    }.let(::LinkedList)
    return Buffer(text, null, 0, 0, 0, globalState.screen.terminalSize, null, true, false)
}
