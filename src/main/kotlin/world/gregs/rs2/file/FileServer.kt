package world.gregs.rs2.file

import com.displee.cache.CacheLibrary
import com.github.michaelbull.logging.InlineLogger
import io.ktor.utils.io.*

class FileServer(
    private val cache: CacheLibrary,
    private val versionTable: ByteArray
) {
    private val logger = InlineLogger()

    /**
     * Fulfills a request by sending the requested files data to the requester
     */
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
}