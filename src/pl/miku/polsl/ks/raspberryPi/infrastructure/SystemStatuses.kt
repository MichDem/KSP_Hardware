package pl.miku.polsl.ks.raspberryPi.infrastructure

object SystemStatuses {
    val isShuttingDown = SingleUseFlag(false)
    val connectedUsers = BooleanArray(5){ false }
}