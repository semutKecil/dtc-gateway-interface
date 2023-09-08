package com.kawanansemut.dtcgatewayinterface

import com.kawanansemut.dtcgatewayinterface.tcpdecoder.*
import org.apache.mina.core.service.IoAcceptor
import org.apache.mina.core.session.IdleStatus
import org.apache.mina.core.session.IoSession
import org.apache.mina.filter.codec.ProtocolCodecFilter
import org.apache.mina.transport.socket.nio.NioSocketAcceptor
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class GWInterface(
    val port: Int,
    val onMessageReceived: (sendCommandProcessor: SendCommandProcessor, command: String) -> Boolean,
    val boundaries: Boundaries = Boundaries(),
    val autoStart: Boolean = true,
    val onOpen: (session: IoSession) -> Unit = {},
    val onCreated: (session: IoSession) -> Unit = {},
    val onClosed: (session: IoSession) -> Unit = {}
) {
    private val acceptor: IoAcceptor
    private val log = LoggerFactory.getLogger(GWInterface::class.java)
    private val sendCommandProcessor: SendCommandProcessor

    init {
        GwUtility.runQueueThread("unprocessed-command-worker", GwUtility.unprocessedQueue)
        sendCommandProcessor = SendCommandProcessor(boundaries)
        val receiveCommandProcessor = ReceiveCommandProcessor {
            onMessageReceived(sendCommandProcessor, it)
        }
        val codecFac = DtcProtocolCodecFactory(
            boundaries
        )
        codecFac.maxLineLength = 10000

        acceptor = NioSocketAcceptor()
        acceptor.filterChain.addLast("codec", ProtocolCodecFilter(codecFac))

        acceptor.handler = MessageReceiveHandler(
            boundaries,
            receiveCommandProcessor,
            sendCommandProcessor,
            onOpen = onOpen,
            onClosed = onClosed,
            onCreated = onCreated
        )

        acceptor.sessionConfig.readBufferSize = 2048
        acceptor.sessionConfig.setIdleTime(IdleStatus.BOTH_IDLE, 10)
        if (autoStart) {
            acceptor.bind(InetSocketAddress(port))
        }
    }

    fun start() {
        log.info("open gateway interface server at port $port")
        if (!acceptor.isActive) {
            acceptor.bind(InetSocketAddress(port))
        }
    }

    fun send(command: String) {
        sendCommandProcessor.send(command)
    }
}