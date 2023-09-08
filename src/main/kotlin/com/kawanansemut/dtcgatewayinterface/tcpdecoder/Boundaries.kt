package com.kawanansemut.dtcgatewayinterface.tcpdecoder

class Boundaries(
    val STX: String = (2).toChar().toString(),
    val ETX: String = (3).toChar().toString(),
    val ACK: String = (6).toChar().toString(),
    val NAK: String = (21).toChar().toString()
) {
    companion object {
        fun load(stx: Int? = null, etx: Int? = null, ack: Int? = null, nak: Int? = null): Boundaries {
            return Boundaries(
                STX = (stx ?: 2).toChar().toString(),
                ETX = (etx ?: 3).toChar().toString(),
                ACK = (ack ?: 6).toChar().toString(),
                NAK = (nak ?: 21).toChar().toString()
            )
        }
    }

//    fun load(conf: Properties): Boundaries {
//        STX = (conf["tcp.stx"]?.toString()?.toInt() ?: 2).toChar().toString()
//        ETX = (conf["tcp.etx"]?.toString()?.toInt() ?: 3).toChar().toString()
//        ACK = (conf["tcp.ack"]?.toString()?.toInt() ?: 6).toChar().toString()
//        NAK = (conf["tcp.nak"]?.toString()?.toInt() ?: 21).toChar().toString()
//        return this
//    }
}