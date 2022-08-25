package maryk.rocksdb

import maryk.rocksdb.CompressionType.NO_COMPRESSION

actual enum class CompressionType(
    internal val value: Byte,
    private val libraryName: String?
) {
    NO_COMPRESSION(0x0, null),
    SNAPPY_COMPRESSION(0x1, "snappy"),
    ZLIB_COMPRESSION(0x2, "z"),
    BZLIB2_COMPRESSION(0x3, "bzip2"),
    LZ4_COMPRESSION(0x4, "lz4"),
    LZ4HC_COMPRESSION(0x5, "lz4hc"),
    XPRESS_COMPRESSION(0x6, "xpress"),
    ZSTD_COMPRESSION(0x7, "zstd"),
    DISABLE_COMPRESSION_OPTION(0x7f, null);

    actual fun getLibraryName() = libraryName
}

actual fun getCompressionType(libraryName: String?): CompressionType {
    if (libraryName != null) {
        for (compressionType in CompressionType.values()) {
            if (compressionType.getLibraryName() != null && compressionType.getLibraryName() == libraryName) {
                return compressionType
            }
        }
    }
    return NO_COMPRESSION
}

actual fun getCompressionType(byteIdentifier: Byte): CompressionType {
    for (compressionType in CompressionType.values()) {
        if (compressionType.value == byteIdentifier) {
            return compressionType
        }
    }

    throw IllegalArgumentException("Illegal value provided for CompressionType.")
}
