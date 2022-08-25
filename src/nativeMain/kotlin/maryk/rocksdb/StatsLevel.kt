package maryk.rocksdb

actual enum class StatsLevel(
    internal val value: UByte
) {
    DISABLE_ALL(0u),
    EXCEPT_TICKERS(1u),
    EXCEPT_HISTOGRAM_OR_TIMERS(2u),
    EXCEPT_TIMERS(3u),
    EXCEPT_DETAILED_TIMERS(4u),
    EXCEPT_TIME_FOR_MUTEX(5u),
    ALL(6u);
}
