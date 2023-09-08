package com.kawanansemut.dtcgatewayinterface.tcpdecoder


import com.kawanansemut.dtcgatewayinterface.tcpdecoder.GwUtility.isAckOk
import org.apache.mina.core.service.IoHandlerAdapter
import org.apache.mina.core.session.IoSession
import org.slf4j.LoggerFactory

class MessageReceiveHandler(
    private val boundaries: Boundaries,
    private val receiveProcessor: ReceiveCommandProcessor,
    private val sendProcessor: SendCommandProcessor,
    private val onOpen: (session: IoSession) -> Unit = {},
    private val onCreated: (session: IoSession) -> Unit = {},
    private val onClosed: (session: IoSession) -> Unit = {}

) : IoHandlerAdapter() {
    private val log = LoggerFactory.getLogger(MessageReceiveHandler::class.java)

    private var messageGnt: String = ""
    private var acceptedSession: IoSession? = null
        set(value) {
            sendProcessor.session = value
            field = value
        }

    override fun sessionClosed(session: IoSession) {
        log.info("session Closed ${session.id}")
        onClosed(session)
        if (acceptedSession?.id == session.id) {
            acceptedSession = null
        }
        super.sessionClosed(session)
    }

    override fun sessionCreated(session: IoSession) {
        log.info("session Created ${session.id}")
        onCreated(session)
        if (acceptedSession == null) {
            acceptedSession = session
        } else {
            session.closeNow()
        }

        super.sessionCreated(session)
    }

    override fun exceptionCaught(session: IoSession?, cause: Throwable?) {
        log.error("session Error ${session?.id}", cause)
    }

    override fun sessionOpened(session: IoSession) {
        log.info("session Opened ${session.id}")
        onOpen(session)
        super.sessionOpened(session)
    }

    override fun messageReceived(session: IoSession, message: Any) {
        var str: String = message.toString()
        //tampung pesan ke processor
        if (str.contains(boundaries.ACK)) {
            //send next command
            log.info("received ACK")
            str = str.replace(boundaries.ACK, "")
            isAckOk?.let {
                if (!it.isDone) {
                    it.complete(true)
                }
            }
        } else if (str.contains(boundaries.NAK)) {
            //send again
            str = str.replace(boundaries.NAK, "")
            log.info("received NAK Gateway")
            isAckOk?.let {
                if (!it.isDone) {
                    it.complete(false)
                }
            }
            //sesProcessing.put(session, false);
        }
        //process command
        messageGnt = messageProcessor(messageGnt + str, session, true)
    }

    private fun messageProcessor(msg: String, session: IoSession, checkStx: Boolean): String {
        var str = msg
        if (str.contains(boundaries.STX) && checkStx) {
            val stxIdx: Int = str.indexOf(boundaries.STX)
            str = str.substring(stxIdx)
            return messageProcessor(str, session, false)
        }
        if (str.contains(boundaries.ETX) && !checkStx) {
            session.write(boundaries.ACK)
            val etxIdx: Int = str.indexOf(boundaries.ETX)
            val comm = str.substring(0, etxIdx + 1).replace(boundaries.STX, "").replace(boundaries.ETX, "")
            str = str.substring(etxIdx + 1)
            receiveProcessor.add(comm)

            return messageProcessor(str, session, true)
        }
        return str
    }
}


