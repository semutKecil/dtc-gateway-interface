package com.kawanansemut.dtcgatewayinterface.tcpdecoder

class GWParser(gwCommand: String) {
    private val listStr = mutableListOf<String>()

    init {
        listStr.addAll(gwCommand.split("|").filter { it != "" })
    }

    fun get(prev: String): String? {
        return listStr.firstOrNull { it.startsWith(prev) }?.substring(prev.length)
    }
}