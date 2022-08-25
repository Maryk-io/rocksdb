package maryk.rocksdb

import maryk.rocksdb.StatsLevel.ALL
import maryk.rocksdb.StatsLevel.DISABLE_ALL
import maryk.rocksdb.StatsLevel.EXCEPT_DETAILED_TIMERS
import maryk.rocksdb.StatsLevel.EXCEPT_HISTOGRAM_OR_TIMERS
import maryk.rocksdb.StatsLevel.EXCEPT_TICKERS
import maryk.rocksdb.StatsLevel.EXCEPT_TIMERS
import maryk.rocksdb.StatsLevel.EXCEPT_TIME_FOR_MUTEX
import maryk.wrapWithErrorThrower
import rocksdb.RocksDBStatistics
import rocksdb.RocksDBStatsLevel.RocksDBStatsLevelAll
import rocksdb.RocksDBStatsLevel.RocksDBStatsLevelDisableAll
import rocksdb.RocksDBStatsLevel.RocksDBStatsLevelExceptDetailedTimers
import rocksdb.RocksDBStatsLevel.RocksDBStatsLevelExceptHistogramOrTimers
import rocksdb.RocksDBStatsLevel.RocksDBStatsLevelExceptTickers
import rocksdb.RocksDBStatsLevel.RocksDBStatsLevelExceptTimeForMutex
import rocksdb.RocksDBStatsLevel.RocksDBStatsLevelExceptTimers

actual class Statistics internal constructor(
    internal val native: RocksDBStatistics = RocksDBStatistics()
) : RocksObject() {
    actual constructor() : this(RocksDBStatistics())

    actual fun statsLevel() = when (native.statsLevel) {
        RocksDBStatsLevelDisableAll -> DISABLE_ALL
        RocksDBStatsLevelExceptTickers -> EXCEPT_TICKERS
        RocksDBStatsLevelExceptHistogramOrTimers -> EXCEPT_HISTOGRAM_OR_TIMERS
        RocksDBStatsLevelExceptTimers -> EXCEPT_TIMERS
        RocksDBStatsLevelExceptDetailedTimers -> EXCEPT_DETAILED_TIMERS
        RocksDBStatsLevelExceptTimeForMutex -> EXCEPT_TIME_FOR_MUTEX
        RocksDBStatsLevelAll -> ALL
        else -> throw NotImplementedError()
    }

    actual fun setStatsLevel(statsLevel: StatsLevel) {
        throw NotImplementedError("DO SOMETHING")
//        native.statsLevel = statsLevel.value
    }

    actual fun getTickerCount(tickerType: TickerType): Long {
        throw NotImplementedError("DO SOMETHING")
//        return native.countForTicker(tickerType.value).toLong()
    }

    actual fun getAndResetTickerCount(tickerType: TickerType): Long {
        throw NotImplementedError("DO SOMETHING")
//        return native.countForTickerAndReset(tickerType.value).toLong()
    }

    actual fun getHistogramData(histogramType: HistogramType) :HistogramData {
        throw NotImplementedError("DO SOMETHING")
//        native.histogramDataForType(histogramType.value)
    }

    actual fun getHistogramString(histogramType: HistogramType): String {
        throw NotImplementedError("DO SOMETHING")
//        native.histogramStringForType(histogramType.value)
    }

    actual fun reset() {
        wrapWithErrorThrower { error ->
            native.reset(error)
        }
    }
}
