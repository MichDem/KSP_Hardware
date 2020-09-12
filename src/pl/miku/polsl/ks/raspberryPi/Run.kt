package pl.miku.polsl.ks.raspberryPi

import com.pi4j.io.gpio.*
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import pl.miku.polsl.ks.raspberryPi.hardware.DS18B20
import pl.miku.polsl.ks.raspberryPi.infrastructure.SystemStatuses
import pl.miku.polsl.ks.raspberryPi.infrastructure.UserStatus
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object Run {

    private val ledsPins: ArrayList<GpioPinDigitalOutput> by lazy {
        val gpio = GpioFactory.getInstance()
        val p = ArrayList<GpioPinDigitalOutput>()

        p += gpio.provisionDigitalOutputPin(RaspiPin.GPIO_25)
        p += gpio.provisionDigitalOutputPin(RaspiPin.GPIO_24)
        p += gpio.provisionDigitalOutputPin(RaspiPin.GPIO_23)
        p += gpio.provisionDigitalOutputPin(RaspiPin.GPIO_22)
        p += gpio.provisionDigitalOutputPin(RaspiPin.GPIO_21)
        p
    }

    @JvmStatic
    fun main(args: Array<String>) {
        registerShutdownButton()
        startServer()
    }

    private fun startServer() {
        val socket = IO.socket("https://ksp-project.azurewebsites.net/")
        socket.on(Socket.EVENT_CONNECT) {
//            sendTemperature(socket)
//            blinkDiodes()
            thread {
                while (true) {
                    sendTemperature(socket)
                    blinkDiodes()
                    Thread.sleep(1_000)
                }
            }
        }.on("userConfirmation") { p0 ->
            updateConnectedUsers(p0)
        }
        socket.connect()
    }

    private fun blinkDiodes() = thread {
        SystemStatuses.connectedUsers.forEachIndexed { index, connected ->
            if (connected)
                ledsPins[index].high()
        }
        Thread.sleep(200)
        shutDownAllDiodes(ledsPins)
    }

    private fun updateConnectedUsers(p0: Array<Any>) = with(SystemStatuses) {
        val userStatus = UserStatus.from(p0.firstOrNull() as? JSONObject)
        if (userStatus != null) {
            if (userStatus.userId in connectedUsers.indices) {
                if (userStatus.valueBoolean != null)
                    connectedUsers[userStatus.userId] = userStatus.valueBoolean!!
                else
                    connectedUsers[userStatus.userId] =
                        connectedUsers[userStatus.userId].not()
            }
        }
    }

    private fun sendTemperature(socket: Socket) {
        val temp = getTemperature()
        println("Sending temperature: $temp")
        socket.emit("currentValue", temp)
    }

    private fun getTemperature(): String {
        val allThermometers = DS18B20.getAllThermometers()
        return if (allThermometers.isEmpty())
            "No thermometers"
        else
            allThermometers.first().readTemp().toString()
    }

    private fun registerShutdownButton() {
        val gpio = GpioFactory.getInstance()
        val button = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN)
        val statusLed = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03)
        statusLed.high()
        button.setShutdownOptions(true)
        button.addListener(object : GpioPinListenerDigital {
            override fun handleGpioPinDigitalStateChangeEvent(p0: GpioPinDigitalStateChangeEvent?) {
                if (p0?.state == PinState.LOW) {
                    SystemStatuses.isShuttingDown.state = true
                    val runtime = Runtime.getRuntime()
                    runtime.exec("/sbin/shutdown -P -t 5")
                    println("System will be shut down soon")
                    statusLed.low()
                    Thread.sleep(1_000)
                    statusLed.high()
                    exitProcess(0)
                }
            }
        })
    }

    private fun shutDownAllDiodes(diodes: ArrayList<GpioPinDigitalOutput>) {
        diodes.forEach {
            it.low()
        }
    }
}