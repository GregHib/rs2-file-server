package world.gregs.rs2.file

import com.displee.cache.CacheLibrary

const val CONFIGS = 2
const val INTERFACES = 3
const val HUFFMAN = 10
const val CLIENT_SCRIPTS = 12
const val FONT_METRICS = 13
const val OBJECTS = 16
const val ENUMS = 17
const val NPCS = 18
const val ITEMS = 19
const val ANIMATIONS = 20
const val GRAPHICS = 21
const val VAR_BIT = 22
const val WORLD_MAP = 23
const val QUICK_CHAT_MESSAGES = 24
const val QUICK_CHAT_MENUS = 25
const val TEXTURE_DEFINITIONS = 26
const val PARTICLES = 27
const val DEFAULTS = 28
const val BILLBOARDS = 29
const val NATIVE_LIBRARIES = 30
const val SHADERS = 31

/**
 * Generates cache prefetch keys used to determine total cache download percentage
 * Note: Can vary between revisions, compare with your client.
 */
fun generatePrefetchKeys(cache: CacheLibrary) = intArrayOf(
    archive(cache, DEFAULTS),
    native(cache, "jaclib"),
    native(cache, "jaggl"),
    native(cache, "jagdx"),
    native(cache, "jagmisc"),
    native(cache, "sw3d"),
    native(cache, "hw3d"),
    native(cache, "jagtheora"),
    archive(cache, SHADERS),
    archive(cache, TEXTURE_DEFINITIONS),
    archive(cache, CONFIGS),
    archive(cache, OBJECTS),
    archive(cache, ENUMS),
    archive(cache, NPCS),
    archive(cache, ITEMS),
    archive(cache, ANIMATIONS),
    archive(cache, GRAPHICS),
    archive(cache, VAR_BIT),
    archive(cache, QUICK_CHAT_MESSAGES),
    archive(cache, QUICK_CHAT_MENUS),
    archive(cache, PARTICLES),
    archive(cache, BILLBOARDS),
    file(cache, HUFFMAN, "huffman"),
    archive(cache, INTERFACES),
    archive(cache, CLIENT_SCRIPTS),
    archive(cache, FONT_METRICS),
    file(cache, WORLD_MAP, "details"),
)

/**
 *  Length of archive with [name] in [index]
 */
fun file(cacheLibrary: CacheLibrary, index: Int, name: String): Int {
    val idx = cacheLibrary.index(index)
    val archive = idx.archiveId(name)
    if (archive == -1) {
        return 0
    }
    return (idx.readArchiveSector(archive)?.size ?: 2) - 2
}

/**
 * Length of all archives in [index]
 */
fun archive(cache: CacheLibrary, index: Int): Int {
    var total = 0
    val idx = cache.index(index)
    idx.archiveIds().forEach { archive ->
        total += idx.readArchiveSector(archive)?.size ?: 0
    }
    total += cache.index255?.readArchiveSector(index)?.size ?: 0
    return total
}

fun native(cache: CacheLibrary, name: String) = file(cache, NATIVE_LIBRARIES, "windows/x86/$name.dll")