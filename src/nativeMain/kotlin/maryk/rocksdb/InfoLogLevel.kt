package maryk.rocksdb

actual enum class InfoLogLevel(
    internal val value: UByte
) {
    DEBUG_LEVEL(0u),
    INFO_LEVEL(1u),
    WARN_LEVEL(2u),
    ERROR_LEVEL(3u),
    FATAL_LEVEL(4u),
    HEADER_LEVEL(5u),
    NUM_INFO_LOG_LEVELS(6u);
}

/**
 * Get InfoLogLevel by byte value.
 *
 * @param value byte representation of InfoLogLevel.
 *
 * @return [InfoLogLevel] instance.
 * @throws IllegalArgumentException if an invalid
 * value is provided.
 */
fun getInfoLogLevel(value: UByte): InfoLogLevel {
    for (infoLogLevel in InfoLogLevel.values()) {
        if (infoLogLevel.value == value) {
            return infoLogLevel
        }
    }
    throw IllegalArgumentException("Illegal value provided for InfoLogLevel.")
}
