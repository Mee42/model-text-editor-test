package dev.mee42

import java.io.File
import java.io.FileWriter
import kotlin.system.exitProcess


fun min(vararg numbers: Int): Int {
    if(numbers.isEmpty()) error("no empty")
    var i = Int.MAX_VALUE
    for(x in numbers) i = if(x < i) x else i
    return i
}
fun max(vararg numbers: Int): Int {
    if(numbers.isEmpty()) error("no empty")
    var i = Int.MIN_VALUE
    for(x in numbers) i = if(x > i) x else i
    return i
}


val logs = FileWriter(File("build/logs"), true)


fun log(str: String) {
    logs.write(str + "\n")
    logs.flush()
}


fun quit(): Nothing {
    log("quit with function")
    exitProcess(0)
}
