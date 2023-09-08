package com.kawanansemut.dtcgatewayinterface.tcpdecoder

import org.apache.mina.core.session.IoSession
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class SendCommandProcessor(private val boundaries: Boundaries) {
    var session: IoSession? = null
    private val commandSendBlockedQueue = LinkedBlockingQueue<String>()

    private val log = LoggerFactory.getLogger(SendCommandProcessor::class.java)

    init {
        Thread({
            while (true) {
                val c = commandSendBlockedQueue.take()
                try {
                    GwUtility.isAckOk = CompletableFuture()
                    session?.write(boundaries.STX + c + boundaries.ETX)
                    // wait ack or nak before proceed
                    GwUtility.isAckOk?.get(20, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    log.error("send-command-worker error", e)
                }
            }
        }, "send-command-worker").start()
    }

    fun send(command: String) {
        //must wait for ack
        session ?: throw Exception("No Gateway Connected")
        commandSendBlockedQueue.put(command)
    }
}