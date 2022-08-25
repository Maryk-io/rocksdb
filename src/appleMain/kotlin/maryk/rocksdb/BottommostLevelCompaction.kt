package maryk.rocksdb

actual enum class BottommostLevelCompaction(
    internal val value: UByte
) {
    kSkip(0u),
    kIfHaveCompactionFilter(1u),
    kForce(2u),
}

fun bottommostLevelCompactionFromByte(bottommostLevelCompaction: UByte): BottommostLevelCompaction? {
    return when (bottommostLevelCompaction) {
        BottommostLevelCompaction.kSkip.value -> BottommostLevelCompaction.kSkip
        BottommostLevelCompaction.kIfHaveCompactionFilter.value -> BottommostLevelCompaction.kIfHaveCompactionFilter
        BottommostLevelCompaction.kForce.value -> BottommostLevelCompaction.kForce
        else -> null
    }
}
