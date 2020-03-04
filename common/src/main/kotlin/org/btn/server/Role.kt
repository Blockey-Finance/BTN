package org.btn.server



const val ADMIN = 1
const val NORMAL_USER = 0

open class User {
    var role: Int = 0
        get() {return role}
}
open class Role {
    companion object {
        fun canExecute(user: User?, httpApi: HttpApi): Boolean {
            return true
        }
    }
}
