package com.kawanansemut.dtcgatewayinterface.tcpdecoder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class ReceiveCommandProcessor(val onReceiveMessage: (command:String) -> Boolean) {
    private val log = LoggerFactory.getLogger(ReceiveCommandProcessor::class.java)
    private val commandBlockedQueue = LinkedBlockingQueue<String>()
    private val unprocessedCommand: MutableList<String> = GwUtility.readCommandQue()

    init {
        commandBlockedQueue.addAll(unprocessedCommand)

        Thread({
            while (true) {
                val command = commandBlockedQueue.take()
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val com = command.split(spr)[1].replace(ent, System.lineSeparator())
                            log.info("received command : $com")
                            if (onReceiveMessage(com)) {
                                unprocessedCommand.remove(command)
                                GwUtility.writeUnprocessedCommand(unprocessedCommand.joinToString("\n"))
                            }
                        }catch (e:Exception){
                            log.error("receive-command-worker error", e)
                        }
                    }
                } catch (e: Exception) {
                    log.error("receive-command-worker error", e)
                }

            }
        }, "receive-command-worker").start()
    }

    private val spr: String = "&8^5**"
    private val ent: String = "[[ENTER]]"

    //    @Synchronized
    fun add(c: String) {
        val spiltCmd = c.split("|")
        val dp = spiltCmd.first().substring(spiltCmd.first().length - 2)
        if (dp != "CD") {
            val cmdList = spiltCmd.toMutableList()
            cmdList.removeAt(0)
            cmdList.add(0, dp)
            val cmd =
                UUID.randomUUID().toString() + spr + cmdList.joinToString("|").replace("\r\n", ent).replace("\n", ent)
            unprocessedCommand.add(cmd)
            GwUtility.writeUnprocessedCommand(unprocessedCommand.joinToString("\n"))
            commandBlockedQueue.put(cmd)
        }
//        processQue()
    }

}