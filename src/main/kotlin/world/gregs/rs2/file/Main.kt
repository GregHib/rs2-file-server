package world.gregs.rs2.file

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import java.io.File
import java.math.BigInteger
import kotlin.concurrent.thread

fun main() {
    val logger = InlineLogger()
    val start = System.currentTimeMillis()
    logger.info { "Start up..." }
    val file = File("./file-server.properties")
    if (!file.exists()) {
        logger.error { "Unable to find server properties file." }
        return
    }

    var revision = 0
    var port = 0
    var threads = 0
    lateinit var cachePath: String
    lateinit var modulus: BigInteger
    lateinit var exponent: BigInteger
    var prefetchKeys: IntArray = intArrayOf()
    file.forEachLine { line ->
        val (key, value) = line.split("=")
        when (key) {
            "revision" -> revision = value.toInt()
            "port" -> port = value.toInt()
            "threads" -> threads = value.toInt()
            "cachePath" -> cachePath = value
            "rsaModulus" -> modulus = BigInteger(value, 16)
            "rsaPrivate" -> exponent = BigInteger(value, 16)
            "prefetchKeys" -> prefetchKeys = value.split(",").map { it.toInt() }.toIntArray()
        }
    }
    logger.info { "Settings loaded." }

    val cache = CacheLibrary(cachePath)
    val versionTable = cache.generateNewUkeys(exponent, modulus)
    logger.debug { "Version table generated: ${versionTable.contentToString()}" }

    if (prefetchKeys.isEmpty()) {
        prefetchKeys = generatePrefetchKeys(cache)
        logger.debug { "Prefetch keys generated: ${prefetchKeys.contentToString()}" }
    }
    logger.info { "Cache loaded." }

    val fileServer = FileServer(cache, versionTable)
    val network = Network(fileServer, prefetchKeys, revision)
    logger.info { "Loading complete [${System.currentTimeMillis() - start}ms]" }
    val runtime = Runtime.getRuntime()
    runtime.addShutdownHook(thread(start = false) { network.stop() })
    network.start(port, threads)
}