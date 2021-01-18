package world.gregs.rs2.file

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger

class Network(
    private val cache: CacheLibrary,
    private val versionTable: ByteArray,
    private val revision: Int
) {
    private val logger = InlineLogger()

    fun start(port: Int) {
        logger.info { "Listening for requests on port ${port}..." }
    }

}