package world.gregs.rs2.file

import com.displee.cache.CacheLibrary
import com.displee.cache.index.Index
import com.displee.cache.index.Index255
import com.displee.cache.index.archive.ArchiveSector
import io.ktor.utils.io.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
internal class FileServerTest {

    @Test
    fun `Get cache version table`() {
        val versionTable = byteArrayOf(124, -10)
        val server = FileServer(mockk(), versionTable)
        val data = server.data(255, 255)
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
        val data = server.data(255, 10)
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
        val data = server.data(11, 0)
        assertArrayEquals(payload, data)
    }

    @Test
    fun `Get invalid cache archive`() {
        val cache: CacheLibrary = mockk()
        val server = FileServer(cache, byteArrayOf())
        val index: Index = mockk()
        every { cache.index(11) } returns index
        every { index.readArchiveSector(0) } returns null
        val data = server.data(11, 0)
        assertNull(data)
    }

    @Test
    fun `Encode header for prefetch request`() = runBlockingTest {
        val server = spyk(FileServer(mockk(), byteArrayOf()))
        val write: ByteWriteChannel = mockk()
        val file = byteArrayOf(2, 0, 0, -44, 49)

        coEvery { write.writeByte(any()) } just Runs
        coEvery { write.writeShort(any()) } just Runs
        coEvery { write.writeInt(any()) } just Runs
        coEvery { server.serve(write, any(), file, any(), any(), any()) } just Runs

        server.serve(write, 1, 2, file, true)

        coVerifyOrder {
            write.writeByte(1)
            write.writeShort(2)
            write.writeByte(130)
            server.serve(write, 4, file, 1, 54329, 512)
        }
    }

    @Test
    fun `Encode header for priority request`() = runBlockingTest {
        val server = spyk(FileServer(mockk(), byteArrayOf()))
        val write: ByteWriteChannel = mockk()
        val file = byteArrayOf(0, 0, 0, 0, 10)

        coEvery { write.writeByte(any()) } just Runs
        coEvery { write.writeShort(any()) } just Runs
        coEvery { write.writeInt(any()) } just Runs
        coEvery { server.serve(write, any(), file, any(), any(), any()) } just Runs

        server.serve(write, 1, 2, file, false)

        coVerifyOrder {
            write.writeByte(1)
            write.writeShort(2)
            write.writeByte(0)
            server.serve(write, 4, file, 1, 14, 512)
        }
    }

    @Test
    fun `Encode small single sector`() = runBlockingTest {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val server = FileServer(mockk(), byteArrayOf())
        val write: ByteWriteChannel = mockk()
        coEvery { write.writeFully(data, any(), any()) } just Runs

        server.serve(write, 8, data, 1, 4, 14)

        coVerifyOrder {
            write.writeFully(data, 1, 4)
        }
    }

    @Test
    fun `Encode fixed size single sector`() = runBlockingTest {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val server = FileServer(mockk(), byteArrayOf())
        val write: ByteWriteChannel = mockk()
        coEvery { write.writeFully(data, any(), any()) } just Runs

        server.serve(write, 8, data, 1, 4, 12)

        coVerifyOrder {
            write.writeFully(data, 1, 4)
        }
    }

    @Test
    fun `Encode two sectors`() = runBlockingTest {
        val data = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val server = FileServer(mockk(), byteArrayOf())
        val write: ByteWriteChannel = mockk()
        coEvery { write.writeFully(data, any(), any()) } just Runs
        coEvery { write.writeByte(any()) } just Runs

        server.serve(write, 8, data, 2, 5, 12)

        coVerifyOrder {
            write.writeFully(data, 2, 4)
            write.writeByte(255)
            write.writeFully(data, 6, 1)
        }
    }
}