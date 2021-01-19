package world.gregs.rs2.file

import com.displee.cache.CacheLibrary
import com.displee.cache.index.Index
import com.displee.cache.index.Index255
import com.displee.cache.index.archive.ArchiveSector
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FileServerTest {

    @Test
    fun `Get cache version table`() {
        val versionTable = byteArrayOf(124, -10)
        val server = FileServer(mockk(), versionTable)
        val data = server.getData(255, 255)
        assertArrayEquals(versionTable, data)
    }

    @Test
    fun `Get cache index255`() {
        val cache: CacheLibrary = mockk()
        val server = FileServer(cache, byteArrayOf())
        val index: Index255 = mockk()
        every { cache.index255 } returns index
        val sector: ArchiveSector = mockk()
        every { index.readArchiveSector(10) } returns sector
        val payload = byteArrayOf(1, 2, 3)
        every { sector.data } returns payload
        val data = server.getData(255, 10)
        assertArrayEquals(payload, data)
    }

    @Test
    fun `Get cache index`() {
        val cache: CacheLibrary = mockk()
        val server = FileServer(cache, byteArrayOf())
        val index: Index = mockk()
        every { cache.index(11) } returns index
        val sector: ArchiveSector = mockk()
        every { index.readArchiveSector(0) } returns sector
        val payload = byteArrayOf(1, 2, 3)
        every { sector.data } returns payload
        val data = server.getData(11, 0)
        assertArrayEquals(payload, data)
    }

    @Test
    fun `Get invalid cache archive`() {
        val cache: CacheLibrary = mockk()
        val server = FileServer(cache, byteArrayOf())
        val index: Index = mockk()
        every { cache.index(11) } returns index
        every { index.readArchiveSector(0) } returns null
        val data = server.getData(11, 0)
        assertNull(data)
    }
}