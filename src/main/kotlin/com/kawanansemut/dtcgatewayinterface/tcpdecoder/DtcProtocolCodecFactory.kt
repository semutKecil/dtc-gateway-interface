package com.kawanansemut.dtcgatewayinterface.tcpdecoder

import org.apache.mina.core.buffer.IoBuffer
import org.apache.mina.core.session.IoSession
import org.apache.mina.filter.codec.*
import org.apache.mina.filter.codec.textline.TextLineEncoder
import org.slf4j.LoggerFactory


class DtcProtocolCodecFactory(
    private val boundaries: Boundaries
) : TextLineEncoder(), ProtocolCodecFactory {
    private val log = LoggerFactory.getLogger(DtcProtocolCodecFactory::class.java)

    private fun buffToStr(
        ioBuffer: IoBuffer,
        from: Int = ioBuffer.position(),
        to: Int = ioBuffer.limit()
    ): String {
        val chrList = mutableListOf<Char>()
        for (i in from..<to) {
            val chr = try {
                Char(ioBuffer[i].toInt())
            } catch (e: Exception) {
                log.error("invalid char", e)
                null
            }
            chr?.let { chrList.add(it) }
        }

        return String(chrList.toCharArray())
    }

    private fun searchInLoop(pos: Int, src: String, ioBuffer: IoBuffer): Boolean {
        try {
            val srcLn = src.length
            if (srcLn == 1) {
                return (Char(ioBuffer[pos].toInt()).toString() == src)
            }
            if (pos >= srcLn - 1) {
                val ackChr = CharArray(srcLn)
                var x = srcLn - 1
                for (j in 0..<srcLn) {
                    ackChr[x] = Char(ioBuffer[pos - j].toInt())
                    x--
                }
                return src == String(ackChr)
            }
        } catch (e: Exception) {
            log.error("invalid char",e)
        }
        return false
    }


    override fun getEncoder(ioSession: IoSession?): ProtocolEncoder {
        return object : ProtocolEncoderAdapter(), ProtocolEncoder {
            override fun encode(ioSession: IoSession, o: Any, protocolDecoderOutput: ProtocolEncoderOutput) {
                val msg = o.toString()
                val ioBuffer = IoBuffer.allocate(3, false)
                ioBuffer.isAutoExpand = true
                ioBuffer.isAutoShrink = true
                val responseByteArr = msg.toByteArray()
                ioBuffer.put(responseByteArr)
                ioBuffer.flip() //Flip it or there will be nothing to send
                protocolDecoderOutput.write(ioBuffer)
//                protocolDecoderOutput.flush()
            }
        }
    }

    override fun getDecoder(ioSession: IoSession?): ProtocolDecoder {
        return object : ProtocolDecoder, CumulativeProtocolDecoder() {
            override fun doDecode(
                ioSession: IoSession,
                ioBuffer: IoBuffer,
                protocolDecoderOutput: ProtocolDecoderOutput
            ): Boolean {
                ioBuffer.isAutoExpand = true
                ioBuffer.isAutoShrink = true
                val start = ioBuffer.position()
                for (i in start..<ioBuffer.limit()) {
                    if (searchInLoop(i, boundaries.ACK, ioBuffer) || searchInLoop(
                            i,
                            boundaries.NAK,
                            ioBuffer
                        ) || searchInLoop(
                            i,
                            boundaries.STX,
                            ioBuffer
                        ) || searchInLoop(
                            i,
                            boundaries.ETX, ioBuffer
                        )
                    ) {
                        val outputString = buffToStr(ioBuffer, ioBuffer.position(), i + 1)
                        ioBuffer.position(i + 1)
                        protocolDecoderOutput.write(outputString)
                        return true
                    }
                }
                return false
            }
        }
    }
}