package pl.miku.polsl.ks.raspberryPi.infrastructure

class SingleUseFlag(private val initialState: Boolean) {
    var state: Boolean = initialState; set(value) {
        if(field == initialState)
            field = value
    }

}