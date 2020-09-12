package pl.miku.polsl.ks.raspberryPi.hardware

import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList

class DS18B20(private var id: String) {
    companion object {
        val path = "/sys/bus/w1/devices/"

        fun getAllThermometers(): ArrayList<DS18B20>{
            val result = ArrayList<DS18B20>()
            File(path).listFiles().forEach {
                if(it.name.startsWith("28-"))
                    result.add(DS18B20(it.name))
            }
            return result
        }
    }

    val tempSufix = "/w1_slave"
    var tempFile = File(path + id + tempSufix)

    //DEBUGING:
    var actuallReads = 0; private set

    @Volatile
    private var isReading = false
    private val latchList = ArrayList<CountDownLatch>()
    private var lastTemp: Double = 85.0

    private fun checkCRC(crc: String): Boolean = crc.endsWith("YES")

    fun readTemp(): Double{
        return if(isReading)
            awaitTemp()
        else
            actuallyReadTemp()
    }

    private fun awaitTemp(): Double{
        val latch = CountDownLatch(1)
        synchronized(DS18B20){
            if(!isReading)
                return actuallyReadTemp()
            latchList += latch
        }
        latch.await()
        return lastTemp
    }

    private fun actuallyReadTemp(): Double {
        isReading = true
        val text = tempFile.readText()
        val crc = text.split("\n")[0]
        isReading = false
        val result: Double
        synchronized(DS18B20){
            result = processOutput(crc, text)
            lastTemp = result
            latchList.forEach { it.countDown() }
        }
        actuallReads++
        return result
    }

    private fun processOutput(crc: String, text: String): Double {
        return if (checkCRC(crc)) {
            val temp = text.split("\n")[1]
            Integer.parseInt(temp.substring(temp.indexOf("t=") + 2)) / 1000.0
        } else {
            85.0
        }
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other !is DS18B20) return false
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    override fun toString(): String {
        return "DS18B20{id=${id}}"
    }


}