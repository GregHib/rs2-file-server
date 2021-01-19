package world.gregs.rs2.file

import com.displee.cache.CacheLibrary
import com.displee.cache.index.Index
import com.displee.cache.index.Index255
import com.displee.cache.index.archive.ArchiveSector
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PrefetchKeysKtTest {

    @Test
    fun `Get length of single index archive`() {
        val cache: CacheLibrary = mockk()
        val index: Index = mockk()
        val sector: ArchiveSector = mockk()
        val name = "hhgttg"
        every { cache.index(11) } returns index
        every { index.archiveId(name) } returns 7
        every { index.readArchiveSector(7) } returns sector
        every { sector.size } returns 44

        val size = file(cache, 11, name)

        assertEquals(42, size)
    }

    @Test
    fun `Get length of missing index archive`() {
        val cache: CacheLibrary = mockk()
        val index: Index = mockk()
        val name = "x-files"
        every { cache.index(11) } returns index
        every { index.archiveId(name) } returns -1

        val size = file(cache, 11, name)

        assertEquals(0, size)
    }

    @Test
    fun `Get length of missing index archive sector`() {
        val cache: CacheLibrary = mockk()
        val index: Index = mockk()
        val name = "hal"
        every { cache.index(3) } returns index
        every { index.archiveId(name) } returns 2001
        every { index.readArchiveSector(2001) } returns null

        val size = file(cache, 3, name)

        assertEquals(0, size)
    }

    @Test
    fun `Get length of multiple index archives`() {
        val cache: CacheLibrary = mockk()
        val index: Index = mockk()
        val index255: Index255 = mockk()
        every { cache.index(4) } returns index
        every { cache.index255 } returns index255

        every { index.archiveIds() } returns intArrayOf(0, 1, 3, 5)

        // Regular id
        val sector0: ArchiveSector = mockk()
        every { sector0.size } returns 24
        every { index.readArchiveSector(0) } returns sector0

        // Regular id
        val sector1: ArchiveSector = mockk()
        every { sector1.size } returns 7
        every { index.readArchiveSector(1) } returns sector1

        // Ignored id
        val sector2: ArchiveSector = mockk()
        every { sector2.size } returns 3
        every { index.readArchiveSector(2) } returns sector2

        // Non sequential id
        val sector3: ArchiveSector = mockk()
        every { sector3.size } returns 6
        every { index.readArchiveSector(3) } returns sector3

        // Missing id
        every { index.readArchiveSector(5) } returns null

        // Extra index value
        val sector4: ArchiveSector = mockk()
        every { sector4.size } returns 1
        every { index255.readArchiveSector(4) } returns sector4

        val size = archive(cache, 4)

        assertEquals(38, size)
        verifyOrder {
            index.archiveIds()
            index.readArchiveSector(0)
            index.readArchiveSector(1)
            index.readArchiveSector(2)?.wasNot(Called)
            index.readArchiveSector(3)
            index.readArchiveSector(5)
        }
    }

    @Test
    fun `Get length of native archive`() {
        val cache: CacheLibrary = mockk()
        val index: Index = mockk()
        val sector: ArchiveSector = mockk()
        every { cache.index(30) } returns index
        every { index.archiveId("windows/x86/vulkan.dll") } returns 0
        every { index.readArchiveSector(0) } returns sector
        every { sector.size } returns 3

        val size = native(cache, "vulkan")

        assertEquals(1, size)
    }
}