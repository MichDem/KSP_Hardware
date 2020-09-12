package pl.miku.polsl.ks.raspberryPi.infrastructure

import org.json.JSONObject

class UserStatus(var userId: Int, var value: String){
    companion object{
        fun from(json: JSONObject?): UserStatus? {
            if(json == null) return null
            val res = UserStatus(0, "")
            res.userId = json.getInt("user")
            res.value = json.getString("value")
            return res
        }
    }

    val valueBoolean: Boolean?
    get() {
        return when(value){
            "true" -> true
            "false" -> false
            else -> null
        }
    }

}