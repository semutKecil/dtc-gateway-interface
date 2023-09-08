package com.kawanansemut.dtcgatewayinterface.tcpdecoder

import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

object GwUtility {

    var isAckOk: CompletableFuture<Boolean>? = null
    fun runQueueThread(name: String, queue: LinkedBlockingQueue<Runnable>) {
        Thread({
            val logThread = LoggerFactory.getLogger(name)
            logThread.info("Thread Start")
            while (true) {
                val unp = queue.take()
                try {
                    unp.run()
                } catch (e: Exception) {
                    logThread.error("runnable error", e)
                }
            }
        }, name).start()
    }

    val unprocessedQueue: LinkedBlockingQueue<Runnable> = LinkedBlockingQueue()
    fun readCommandQue(): MutableList<String> {
        val readFuture: CompletableFuture<MutableList<String>> = CompletableFuture()
        unprocessedQueue.put(Runnable {
            if (!File("data/unprocessed-command.txt").exists()) {
                readFuture.complete(mutableListOf())
                return@Runnable
            }

            readFuture.complete(String(Files.readAllBytes(Paths.get("data/unprocessed-command.txt")))
                .split("\n").map { it.trim() }.filter { it != "" }
                .toMutableList())
        })
        // Do whatever
        return readFuture.get(3000, TimeUnit.MILLISECONDS)
    }

    fun writeUnprocessedCommand(str: String) {
        // Do whatever
        val writeFuture: CompletableFuture<Boolean> = CompletableFuture()
        unprocessedQueue.put(Runnable {
            if (!File("data").exists()) {
                File("data").mkdir()
            }
            val writer = BufferedWriter(FileWriter("data/unprocessed-command.txt"))
            writer.write(str)
            writer.close()
            writeFuture.complete(true)
        })
        writeFuture.get(3000, TimeUnit.MILLISECONDS)
    }
}