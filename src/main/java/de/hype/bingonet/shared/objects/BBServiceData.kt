package de.hype.bingonet.shared.objects

import de.hype.bingonet.shared.constants.StatusConstants
import de.hype.bingonet.shared.constants.TradeType
import de.hype.bingonet.shared.objects.BNUser
import java.time.Instant

data class BBServiceData(
    val serviceId: Int,
    val description: String,
    val hosterUsername: String,
    val type: TradeType?,
    val price: Int,
    val helpers: List<Helper>,
    val maxUsers: Int,
    val forceModOnline: Boolean,
    val status: StatusConstants,
    val title: String?,
    val participants: List<Participant>,
    val joinLock: Boolean,
    val circulateParticipants: Boolean,
) {


    data class Participant(
        @JvmField val user: BNUser,
        @JvmField val price: Int,
        @JvmField val priority: Boolean = false,
        @JvmField val joinTime: Instant = Instant.now(),
        @JvmField val autoRequeue: Boolean = false,
    ) {
        override fun equals(other: Any?): Boolean {
            if (other is Participant) return other.user == user
            if (other is BNUser) return other == this.user
            return false
        }

        override fun hashCode(): Int {
            return user.hashCode()
        }
    }

    class Helper {
        private val user: BNUser?
        private val username: String?

        constructor(user: BNUser) {
            this.user = user
            this.username = user.mcusername
        }

        constructor(user: BNUser?, username: String) {
            this.user = user
            this.username = username
        }

        override fun equals(other: Any?): Boolean {
            if (other is Helper) return other.username == username
            if (other is String) {
                if (username != null) return other.equals(username, ignoreCase = true)
                else return user!!.mcusername.equals(other, ignoreCase = true)
            }
            if (other is BNUser) {
                if (user != null) return other == user
                else return other.mcusername.equals(username, ignoreCase = true)
            }
            return false
        }

        override fun hashCode(): Int {
            if (username != null) return username.hashCode()
            else return user!!.mcusername.hashCode()
        }

        fun getUserName(): String {
            return username ?: user!!.mcusername
        }

        fun getUser(): BNUser? {
            return user
        }
    }
}
