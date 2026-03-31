package at.hannibal2.skyhanni.features.bingo.bingobrewers.official

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.event.bingo.BingoBrewersConfig
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.PlayerUtils.getName
import at.hannibal2.skyhanni.utils.PlayerUtils.getUuid
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.minlog.Log
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.math.min

object ServerConnection : Listener(), Runnable {
    val isConnected: Boolean
        get() = client?.isConnected ?: false
    val thread = Thread(this, "Bingo Brewers Server Connection Thread")
    init {
        thread.start()
    }

    override fun run() {
        val client = Client(16384, 16384)
        this.client = client
        try {
            connection()
        } catch (e: Exception) {
            println("catch reconnect")
            println("BB Server Connection Error: " + e.message)
            if (!reconnect) {
                reconnect()
            }
        }
    }

    @Throws(Exception::class)
    private fun connection() {
        Log.set(Log.LEVEL_ERROR)
        val client = client!!
        KryoNetwork.register(client)
        client.addListener(
            object : Listener() {
                override fun received(connection: Connection?, `object`: Any?) {
                    PacketProcessing.processPacket(connection, `object`)
                }

                override fun disconnected(connection: Connection?) {
                    println("disconnected")
                    reconnect()
                }
            },
        )

        client.start()

        connectionsThisSession++

        client.connect(8000, "bingobrewers.com", 8080, 7070)

        println("Connected to server.")
    }


    @Synchronized
    fun sendPlayerCount(count: KryoNetwork.PlayerCount?) {
        if (!config.showSplashes) return
        val currentClient: Client? = client
        if (currentClient == null) {
            println("Client is null")
            return
        }
        currentClient.sendTCP(count)
    }

    fun reconnect() {
        val client = client ?: return
        client.close()
        client.removeListener(this)
        var waitTime = 0f

        waitTime = (5000 * Math.random() + 2000).toInt().toFloat()

        println("Disconnected from server.")
        reconnect = true
        while (reconnect) {
            println("Reconnecting to Bingo Brewers server.")
            try {
                this.client = Client(16384, 16384)
                connection() // there's a built in reconnect method idk that's not used but this works
                reconnect = false
            } catch (e: Exception) {
                e.printStackTrace()
                println("[Bingo Brewers] Server Connection Error: " + e.message)
                client.close()
                client.removeListener(this)

                try {
                    println("Reconnect failed. Trying again in " + waitTime + " milliseconds.")
                    Thread.sleep(waitTime.toInt().toLong())
                } catch (ex: InterruptedException) {
                    throw RuntimeException(ex)
                }

                waitTime = min(waitTime * 1.5f, 60000f)

                if (waitTime == 60000f) {
                    waitTime = (60000 - (5000 * Math.random() + 1000).toInt()).toFloat() // slightly vary time
                }
            }
        }
    }

        // The server sends it's public key to the client, which checks it based on this. If they don't match the connection is refused.
        // This would require all users to update in the event of the key being changed but simpler than CA.
        const val SERVER_PUBLIC_KEY: String =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqgPvRC780jqwtXV4/39jjZvlXSnXRGEpD63y3Iptq8YO9sZic7Qno+vHKeoW50Ct5XWmNk13JjUwUdXmWBN4186FUo/b0Z+AtpLNVrkvk7dwkJQgAHa56fok52NK9QN8mTy+Saw1flmX4rdz7TflXpOwPzIYMYC33gqWe4/hMniuU7m+D/07fgzu5Ua5yFz27sNwrbqNuJOr1ReDScLykIazILHzfTa7RFAZn+4nWM3vdtdysKo1YSYQ++05uMR1S51ABtPkJdNLKzEf0sC6H2q1JPOcIAz/9EX2doWHROTfWoYifi0HDHEu+c0Cc20SfhfmY5NjofmLEc0XmuyqewIDAQAB"
        var waitTime: Int = 0
        var reconnect: Boolean = false // controls the loop for reconnecting the client
        var connectionsThisSession: Int = 0 // easy visual indicator server side if connections are struggling

        // if new ch items are added, they will be in this list
        var ign: String = getName()
        var uuid: String = getUuid()
        var symmetricKey: SecretKey? = null

        var client: Client? = null

        private val config get() = SkyHanniMod.feature.event.bingo.bingoNetworks

        @Synchronized
        fun requestLiveUpdates(unrequest: Boolean) {
            val request: KryoNetwork.RequestLiveUpdatesForServerInfo = KryoNetwork.RequestLiveUpdatesForServerInfo()
            request.unrequest = unrequest
            client?.sendTCP(request)
        }

        @JvmStatic
        fun sendTCP(packet: Any?) {
            client?.sendTCP(packet)
        }


        fun loadPublicKeyFromBase64(base64Key: String?): PublicKey? {
            val decodedKey = Base64.getDecoder().decode(base64Key)
            val spec = X509EncodedKeySpec(decodedKey)
            var keyFactory: KeyFactory? = null
            try {
                keyFactory = KeyFactory.getInstance("RSA")
                return keyFactory.generatePublic(spec)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            } catch (e: InvalidKeySpecException) {
                throw RuntimeException(e)
            }
        }

        @Throws(NoSuchAlgorithmException::class)
        fun generateAESKey(keySize: Int): SecretKey {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(keySize) // keySize can be 128, 192, or 256 bits
            return keyGenerator.generateKey()
        }

        fun encodeKeyToBase64(key: SecretKey): String? {
            return Base64.getEncoder().encodeToString(key.getEncoded())
        }

        fun encryptObjectPublicKey(obj: SecretKey, publicKey: PublicKey?): String? {
            // Get the byte array of the serialized object
            val objectBytes = obj.getEncoded()

            // Encrypt the byte array using RSA
            var cipher: Cipher? = null
            try {
                cipher = Cipher.getInstance("RSA")

                cipher.init(Cipher.ENCRYPT_MODE, publicKey)
                val encryptedBytes = cipher.doFinal(objectBytes)

                // Encode the encrypted bytes to a Base64 string
                return Base64.getEncoder().encodeToString(encryptedBytes)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            } catch (e: NoSuchPaddingException) {
                throw RuntimeException(e)
            } catch (e: IllegalBlockSizeException) {
                throw RuntimeException(e)
            } catch (e: BadPaddingException) {
                throw RuntimeException(e)
            } catch (e: InvalidKeyException) {
                throw RuntimeException(e)
            }
        }

        fun generateIV(): ByteArray {
            val iv = ByteArray(16) // AES block size is 16 bytes
            val random = SecureRandom()
            random.nextBytes(iv)
            return iv
        }

        fun encryptString(obj: String): KryoNetwork.EncryptedString {
            val iv: ByteArray = generateIV()
            val aesKey: SecretKey? = symmetricKey

            // Get the byte array of the string object
            val objectBytes = obj.toByteArray()

            var cipher: Cipher? = null
            try {
                cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val ivSpec = IvParameterSpec(iv)
                cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec)
                val encryptedBytes = cipher.doFinal(objectBytes)
                val encryptedString: KryoNetwork.EncryptedString = KryoNetwork.EncryptedString()
                encryptedString.string = Base64.getEncoder().encodeToString(encryptedBytes)
                encryptedString.iv = iv
                return encryptedString
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            } catch (e: NoSuchPaddingException) {
                throw RuntimeException(e)
            } catch (e: InvalidAlgorithmParameterException) {
                throw RuntimeException(e)
            } catch (e: IllegalBlockSizeException) {
                throw RuntimeException(e)
            } catch (e: BadPaddingException) {
                throw RuntimeException(e)
            } catch (e: InvalidKeyException) {
                throw RuntimeException(e)
            }
        }

        fun decryptString(encryptedString: KryoNetwork.EncryptedString): String {
            val aesKey: SecretKey? = symmetricKey

            try {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val ivSpec = IvParameterSpec(encryptedString.iv)
                cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec)
                val encryptedData: String? = encryptedString.string
                val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData))
                return String(decryptedBytes)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            } catch (e: NoSuchPaddingException) {
                throw RuntimeException(e)
            } catch (e: InvalidAlgorithmParameterException) {
                throw RuntimeException(e)
            } catch (e: IllegalBlockSizeException) {
                throw RuntimeException(e)
            } catch (e: BadPaddingException) {
                throw RuntimeException(e)
            } catch (e: InvalidKeyException) {
                throw RuntimeException(e)
            }
        }

    fun close() {
        client?.removeListener(this)
        client?.close()
        client = null
        thread.interrupt()
    }

    fun init() {
        //Do nothing, connection is initialized in the thread
    }

    fun connect() {
        if (client == null || !client!!.isConnected) {
            Thread(this).start()
        }
    }
}
