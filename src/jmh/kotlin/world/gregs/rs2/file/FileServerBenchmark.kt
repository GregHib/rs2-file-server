package world.gregs.rs2.file

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import org.openjdk.jmh.annotations.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Timeout(time = 10, timeUnit = TimeUnit.SECONDS)
class FileServerBenchmark {
    @Param("1")//, "50000", "100000")
    var concurrentConnections = 0

    @Param("1")//, "50000", "100000")
    var requests = 0

    lateinit var server: Network

    lateinit var clientBuilder: TcpSocketBuilder
    private val address = InetSocketAddress("127.0.0.1", 50016)

    lateinit var job: Job

    lateinit var client: FileClient

    @Setup
    fun setup() {
        clientBuilder = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp()
        server = Network(FileServer(provider, byteArrayOf()), intArrayOf(1, 2, 3, 4), 667)
        job = GlobalScope.launch {
            server.start(address.port, 0)
        }
        runBlocking {
            client = FileClient.connect(clientBuilder.connect(address), 667)!!
        }
    }

    @TearDown
    fun teardown() {
        server.stop()
        job.cancel()
        client.finish()
    }

    /*@Benchmark
    fun benchmarkSmallPriorityRequest(): ByteArray = runBlocking {
        client.requestArchive(1, 0, 0)
        client.readArchive()
    }

    @Benchmark
    fun benchmarkMediumPriorityRequest(): ByteArray = runBlocking {
        client.requestArchive(1, 0, 1)
        client.readArchive()
    }

    @Benchmark
    fun benchmarkLargePriorityRequest(): ByteArray = runBlocking {
        client.requestArchive(1, 0, 2)
        client.readArchive()
    }*/

    @Benchmark
    fun benchmarkSmallRequest(): ByteArray? = runBlocking {
        client.requestArchive(0, 0, 0)
        client.readArchive()
    }

    @Benchmark
    fun benchmarkMediumRequest(): ByteArray? = runBlocking {
        client.requestArchive(0, 0, 1)
        client.readArchive()
    }

    @Benchmark
    fun benchmarkLargeRequest(): ByteArray? = runBlocking {
        client.requestArchive(0, 0, 2)
        client.readArchive()
    }

    companion object {
        private val provider = object : DataProvider {
            override fun data(index: Int, archive: Int): ByteArray? = when (archive.rem(3)) {
                0 -> smallFile
                1 -> mediumFile
                2 -> largeFile
                else -> null
            }
        }
        private val smallFile = ByteBuffer.allocate(16).apply {
            put(0)
            putInt(11)
            repeat(11) {
                put(it.toByte())
            }
        }.array()
        private val mediumFile = ByteBuffer.allocate(512).apply {
            put(0)
            putInt(507)
            repeat(507) {
                put(it.rem(100).toByte())
            }
        }.array()
        private val largeFile = ByteBuffer.allocate(4096).apply {
            put(0)
            putInt(4091)
            repeat(4091) {
                put(it.rem(100).toByte())
            }
        }.array()
    }
}