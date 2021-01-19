package world.gregs.rs2.file

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.util.concurrent.Executors

class Network(
    private val cache: CacheLibrary,
    private val versionTable: ByteArray,
    private val prefetchKeys: IntArray,
    private val revision: Int
) {
    private val logger = InlineLogger()

    private val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
        logger.warn(throwable) { "Exception in context: $context" }
    }

    private lateinit var dispatcher: ExecutorCoroutineDispatcher
    private var running = false

    fun start(port: Int, threads: Int) = runBlocking {
        val executor = if (threads == 0) Executors.newCachedThreadPool() else Executors.newFixedThreadPool(threads)
        dispatcher = executor.asCoroutineDispatcher()
        val selector = ActorSelectorManager(dispatcher)
        val supervisor = SupervisorJob()
        val scope = CoroutineScope(coroutineContext + supervisor + exceptionHandler)
        with(scope) {
            val server = aSocket(selector).tcp().bind(port = port)
            running = true
            logger.info { "Listening for requests on port ${port}..." }
            while (running) {
                val socket = server.accept()
                logger.trace { "New connection accepted $socket" }
                launch(Dispatchers.IO) {
                    connect(socket)
                }
            }
        }
    }

    suspend fun connect(socket: Socket) {
        val read = socket.openReadChannel()
        val write = socket.openWriteChannel(autoFlush = true)
        synchronise(read, write)
        if (acknowledge(read, write)) {
            logger.trace { "Client synchronisation complete: $socket" }
            readRequests(read, write)
        }
    }

    suspend fun synchronise(read: ByteReadChannel, write: ByteWriteChannel) {
        val opcode = read.readByte().toInt()
        if (opcode != SYNCHRONISE) {
            logger.trace { "Invalid sync session id: $opcode" }
            write.writeByte(REJECT_SESSION)
            write.close()
            return
        }

        val revision = read.readInt()
        if (revision != this.revision) {
            logger.trace { "Invalid game revision: $revision" }
            write.writeByte(GAME_UPDATED)
            write.close()
            return
        }

        write.writeByte(0)
        prefetchKeys.forEach { key ->
            write.writeInt(key)
        }
    }

    suspend fun acknowledge(read: ByteReadChannel, write: ByteWriteChannel): Boolean {
        val opcode = read.readByte().toInt()
        if (opcode != ACKNOWLEDGE) {
            logger.trace { "Invalid ack opcode: $opcode" }
            write.writeByte(REJECT_SESSION)
            write.close()
            return false
        }

        return verify(read, write, ACKNOWLEDGE_ID)
    }

    suspend fun verify(read: ByteReadChannel, write: ByteWriteChannel, expected: Int): Boolean {
        val id = read.readMedium()
        if (id != expected) {
            logger.trace { "Invalid session id expected: $expected actual: $id" }
            write.writeByte(BAD_SESSION_ID)
            write.close()
            return false
        }
        return true
    }

    suspend fun readRequests(read: ByteReadChannel, write: ByteWriteChannel) = coroutineScope {
        try {
            while (isActive) {
                readRequest(read, write)
            }
        } finally {
            logger.trace { "Client disconnected: $read" }
        }
    }

    suspend fun readRequest(read: ByteReadChannel, write: ByteWriteChannel) {
        val opcode = read.readByte().toInt()
        logger.trace { "Request received $opcode." }
        when (opcode) {
            STATUS_LOGGED_OUT, STATUS_LOGGED_IN -> verify(read, write, STATUS_ID)
            PRIORITY_REQUEST, PREFETCH_REQUEST -> fulfill(read, write, opcode == PREFETCH_REQUEST)
            else -> {
                logger.warn { "Unknown request $opcode." }
                write.close()
            }
        }
    }

    suspend fun fulfill(read: ByteReadChannel, write: ByteWriteChannel, prefetch: Boolean) {
        val value = read.readUMedium()
        val index = value shr 16
        val archive = value and 0xffff
        val data = getData(index, archive) ?: return logger.trace { "Unable to fulfill request $index $archive $prefetch." }
        serve(write, index, archive, data)
    }

    fun getData(indexId: Int, archiveId: Int): ByteArray? {
        if (indexId == 255 && archiveId == 255) {
            return versionTable
        }
        val index = if (indexId == 255) cache.index255 else cache.index(indexId)
        return index?.readArchiveSector(archiveId)?.data
    }

    suspend fun serve(write: ByteWriteChannel, index: Int, archive: Int, data: ByteArray) {
        TODO("Not yet implemented")
    }

    fun stop() {
        running = false
        dispatcher.close()
    }

    companion object {
        // Session ids
        private const val ACKNOWLEDGE_ID = 3
        private const val STATUS_ID = 0

        // Opcodes
        private const val PREFETCH_REQUEST = 0
        private const val PRIORITY_REQUEST = 1
        private const val SYNCHRONISE = 15
        private const val STATUS_LOGGED_IN = 2
        private const val STATUS_LOGGED_OUT = 3
        private const val ACKNOWLEDGE = 6

        // Response codes
        private const val GAME_UPDATED = 6
        private const val BAD_SESSION_ID = 10
        private const val REJECT_SESSION = 11
    }
}