package world.gregs.rs2.file

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import world.gregs.rs2.file.Network.Companion.ACKNOWLEDGE
import world.gregs.rs2.file.Network.Companion.STATUS_LOGGED_OUT
import world.gregs.rs2.file.Network.Companion.SYNCHRONISE
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

class FileClient(
    private val input: ByteReadChannel,
    private val output: ByteWriteChannel
) {

    var job: Job? = null

    fun finish() {
        job?.cancel()
        output.close()
    }

    suspend fun requestArchive(priority: Int, index: Int, archive: Int) {
        output.writePacket {
            writeByte(priority.toByte())
            writeMedium((index shl 16) + archive)
        }
    }

    suspend fun readArchive(): ByteArray? {
        var payload: ByteArray? = null
        input.readAvailable {
            payload = ByteArray(it.remaining())
            it.get(payload)
        }
        return payload
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>): Unit = runBlocking {

            val builder = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
            val address = InetSocketAddress("127.0.0.1", 43594)
            val connections = 2000
            val start = System.currentTimeMillis()
            val clients = (0 until connections).mapNotNull {
                val socket = builder.connect(address)
                connect(socket, 634)
            }
            println("Created $connections in ${System.currentTimeMillis() - start}ms")
            var total = 0L
            try {
                withTimeout(60000L) {
                    clients.map { client ->
                        async {
                            var counter = 0
                            try {
                                while (true) {
                                    client.requestArchive(0, 5, counter % 4000)
                                    client.readArchive()
                                    counter++
                                }
                            } finally {
                                total += counter
                            }
                        }
                    }.forEach {
                        it.await()
                    }
                }
            } finally {
                println("Requests successfully served: $total")
                clients.forEach {
                    it.finish()
                }
            }
        }

        suspend fun connect(socket: Socket, revision: Int): FileClient? {
            try {
                val input = socket.openReadChannel()
                val output = socket.openWriteChannel(autoFlush = true)
                output.writePacket {
                    writeByte(SYNCHRONISE.toByte())
                    writeInt(revision)
                }
                input.readByte()
                repeat(27) {
                    input.readInt()
                }
                output.writePacket {
                    writeByte(ACKNOWLEDGE.toByte())
                    writeMedium(Network.ACKNOWLEDGE_ID)
                    writeByte(STATUS_LOGGED_OUT.toByte())
                    writeMedium(Network.STATUS_ID)
                }
                return FileClient(input, output)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        private fun BytePacketBuilder.writeMedium(value: Int) {
            writeByte((value shr 16 and 0xff).toByte())
            writeByte((value shr 8 and 0xff).toByte())
            writeByte((value and 0xff).toByte())
        }
    }
}