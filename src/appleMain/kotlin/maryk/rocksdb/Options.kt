package maryk.rocksdb

import cnames.structs.rocksdb_options_t
import kotlinx.cinterop.CPointer
import maryk.rocksdb.util.BytewiseComparator
import maryk.rocksdb.util.ReverseBytewiseComparator
import maryk.toBoolean
import maryk.toUByte
import rocksdb.rocksdb_options_create
import rocksdb.rocksdb_options_destroy
import rocksdb.rocksdb_options_get_WAL_size_limit_MB
import rocksdb.rocksdb_options_get_bloom_locality
import rocksdb.rocksdb_options_get_bytes_per_sync
import rocksdb.rocksdb_options_get_compaction_style
import rocksdb.rocksdb_options_get_compression
import rocksdb.rocksdb_options_get_create_if_missing
import rocksdb.rocksdb_options_get_create_missing_column_families
import rocksdb.rocksdb_options_get_disable_auto_compactions
import rocksdb.rocksdb_options_get_error_if_exists
import rocksdb.rocksdb_options_get_info_log_level
import rocksdb.rocksdb_options_get_keep_log_file_num
import rocksdb.rocksdb_options_get_level0_file_num_compaction_trigger
import rocksdb.rocksdb_options_get_level0_slowdown_writes_trigger
import rocksdb.rocksdb_options_get_level0_stop_writes_trigger
import rocksdb.rocksdb_options_get_log_file_time_to_roll
import rocksdb.rocksdb_options_get_max_bytes_for_level_base
import rocksdb.rocksdb_options_get_max_bytes_for_level_multiplier
import rocksdb.rocksdb_options_get_max_log_file_size
import rocksdb.rocksdb_options_get_max_open_files
import rocksdb.rocksdb_options_get_max_write_buffer_number
import rocksdb.rocksdb_options_get_min_write_buffer_number_to_merge
import rocksdb.rocksdb_options_get_num_levels
import rocksdb.rocksdb_options_get_paranoid_checks
import rocksdb.rocksdb_options_get_target_file_size_base
import rocksdb.rocksdb_options_get_target_file_size_multiplier
import rocksdb.rocksdb_options_get_use_fsync
import rocksdb.rocksdb_options_get_write_buffer_size
import rocksdb.rocksdb_options_set_WAL_size_limit_MB
import rocksdb.rocksdb_options_set_bloom_locality
import rocksdb.rocksdb_options_set_bytes_per_sync
import rocksdb.rocksdb_options_set_compaction_style
import rocksdb.rocksdb_options_set_comparator
import rocksdb.rocksdb_options_set_compression
import rocksdb.rocksdb_options_set_create_if_missing
import rocksdb.rocksdb_options_set_create_missing_column_families
import rocksdb.rocksdb_options_set_disable_auto_compactions
import rocksdb.rocksdb_options_set_error_if_exists
import rocksdb.rocksdb_options_set_info_log_level
import rocksdb.rocksdb_options_set_keep_log_file_num
import rocksdb.rocksdb_options_set_level0_file_num_compaction_trigger
import rocksdb.rocksdb_options_set_level0_slowdown_writes_trigger
import rocksdb.rocksdb_options_set_level0_stop_writes_trigger
import rocksdb.rocksdb_options_set_log_file_time_to_roll
import rocksdb.rocksdb_options_set_max_bytes_for_level_base
import rocksdb.rocksdb_options_set_max_bytes_for_level_multiplier
import rocksdb.rocksdb_options_set_max_log_file_size
import rocksdb.rocksdb_options_set_max_open_files
import rocksdb.rocksdb_options_set_max_write_buffer_number
import rocksdb.rocksdb_options_set_min_write_buffer_number_to_merge
import rocksdb.rocksdb_options_set_num_levels
import rocksdb.rocksdb_options_set_paranoid_checks
import rocksdb.rocksdb_options_set_prefix_extractor
import rocksdb.rocksdb_options_set_target_file_size_base
import rocksdb.rocksdb_options_set_target_file_size_multiplier
import rocksdb.rocksdb_options_set_use_fsync
import rocksdb.rocksdb_options_set_write_buffer_size
import rocksdb.rocksdb_slicetransform_create_fixed_prefix

actual class Options private constructor(val native: CPointer<rocksdb_options_t>) : RocksObject() {
    private var statistics: Statistics? = null

    actual constructor() : this(rocksdb_options_create()!!)

    override fun close() {
        if (isOwningHandle()) {
            rocksdb_options_destroy(native)
            super.close()
        }
    }

    actual fun setMaxOpenFiles(maxOpenFiles: Int): Options {
        rocksdb_options_set_max_open_files(native, maxOpenFiles)
        return this
    }

    actual fun maxOpenFiles(): Int =
        rocksdb_options_get_max_open_files(native)

    actual fun setBytesPerSync(bytesPerSync: Long): Options {
        rocksdb_options_set_bytes_per_sync(native, bytesPerSync.toULong())
        return this
    }

    actual fun bytesPerSync() =
        rocksdb_options_get_bytes_per_sync(native).toLong()

    actual fun setCreateIfMissing(flag: Boolean): Options {
        rocksdb_options_set_create_if_missing(native, flag.toUByte())
        return this
    }

    actual fun maxWriteBufferNumber(): Int =
        rocksdb_options_get_max_write_buffer_number(native)

    actual fun minWriteBufferNumberToMerge(): Int =
        rocksdb_options_get_min_write_buffer_number_to_merge(native)

    actual fun setBloomLocality(bloomLocality: Int): Options {
        rocksdb_options_set_bloom_locality(native, bloomLocality.toUInt())
        return this
    }

    actual fun bloomLocality(): Int =
        rocksdb_options_get_bloom_locality(native).toInt()

    actual fun setNumLevels(numLevels: Int): Options {
        rocksdb_options_set_num_levels(native, numLevels)
        return this
    }

    actual fun numLevels() = rocksdb_options_get_num_levels(native)

    actual fun setCompactionStyle(compactionStyle: CompactionStyle): Options {
        rocksdb_options_set_compaction_style(native, compactionStyle.value.toInt())
        return this
    }

    actual fun compactionStyle() = getCompactionStyle(
        rocksdb_options_get_compaction_style(native).toByte()
    )

    actual fun setWriteBufferSize(writeBufferSize: Long): Options {
        rocksdb_options_set_write_buffer_size(native, writeBufferSize.toULong())
        return this
    }

    actual fun writeBufferSize(): Long =
        rocksdb_options_get_write_buffer_size(native).toLong()

    actual fun setDisableAutoCompactions(disableAutoCompactions: Boolean): Options {
        rocksdb_options_set_disable_auto_compactions(native, if (disableAutoCompactions) 1 else 0)
        return this
    }

    actual fun disableAutoCompactions(): Boolean =
        rocksdb_options_get_disable_auto_compactions(native).toBoolean()

    actual fun setLevel0FileNumCompactionTrigger(level0FileNumCompactionTrigger: Int): Options {
        rocksdb_options_set_level0_file_num_compaction_trigger(native, level0FileNumCompactionTrigger)
        return this
    }

    actual fun level0FileNumCompactionTrigger(): Int =
        rocksdb_options_get_level0_file_num_compaction_trigger(native)

    actual fun setMaxBytesForLevelBase(maxBytesForLevelBase: Long): Options {
        rocksdb_options_set_max_bytes_for_level_base(native, maxBytesForLevelBase.toULong())
        return this
    }

    actual fun maxBytesForLevelBase(): Long =
        rocksdb_options_get_max_bytes_for_level_base(native).toLong()

    actual fun setCompressionType(compressionType: CompressionType): Options {
        rocksdb_options_set_compression(native, compressionType.value.toInt())
        return this
    }

    actual fun compressionType() = getCompressionType(
        rocksdb_options_get_compression(native).toByte()
    )

    actual fun setComparator(builtinComparator: BuiltinComparator): Options {
        val comparator = when (builtinComparator) {
            BuiltinComparator.BYTEWISE_COMPARATOR -> BytewiseComparator(null)
            BuiltinComparator.REVERSE_BYTEWISE_COMPARATOR -> ReverseBytewiseComparator(null)
            else -> throw Exception("Unknown comparator")
        }
        rocksdb_options_set_comparator(native, comparator.native)
        return this
    }

    actual fun setComparator(comparator: AbstractComparator): Options {
        rocksdb_options_set_comparator(native, comparator.native)
        return this
    }

    actual fun useFixedLengthPrefixExtractor(n: Int): Options {
        assert(isOwningHandle())
        val prefix = rocksdb_slicetransform_create_fixed_prefix(n.toULong())
        rocksdb_options_set_prefix_extractor(native, prefix)
        // Should it be destroyed?
//        rocksdb_slicetransform_destroy(prefix)
        return this
    }

    actual fun setMaxBytesForLevelMultiplier(multiplier: Double): Options {
        rocksdb_options_set_max_bytes_for_level_multiplier(native, multiplier)
        return this
    }

    actual fun maxBytesForLevelMultiplier(): Double =
        rocksdb_options_get_max_bytes_for_level_multiplier(native)

    actual fun setLevel0SlowdownWritesTrigger(level0SlowdownWritesTrigger: Int): Options {
        rocksdb_options_set_level0_slowdown_writes_trigger(native, level0SlowdownWritesTrigger)
        return this
    }

    actual fun level0SlowdownWritesTrigger(): Int =
        rocksdb_options_get_level0_slowdown_writes_trigger(native)

    actual fun setLevel0StopWritesTrigger(level0StopWritesTrigger: Int): Options {
        rocksdb_options_set_level0_stop_writes_trigger(native, level0StopWritesTrigger)
        return this
    }

    actual fun level0StopWritesTrigger(): Int =
        rocksdb_options_get_level0_stop_writes_trigger(native)

    actual fun setTargetFileSizeBase(targetFileSizeBase: Long): Options {
        rocksdb_options_set_target_file_size_base(native, targetFileSizeBase.toULong())
        return this
    }

    actual fun targetFileSizeBase(): Long =
        rocksdb_options_get_target_file_size_base(native).toLong()

    actual fun setTargetFileSizeMultiplier(multiplier: Int): Options {
        rocksdb_options_set_target_file_size_multiplier(native, multiplier)
        return this
    }

    actual fun targetFileSizeMultiplier(): Int =
        rocksdb_options_get_target_file_size_multiplier(native)

    actual fun createIfMissing() =
        rocksdb_options_get_create_if_missing(native).toBoolean()

    actual fun setMaxWriteBufferNumber(maxWriteBufferNumber: Int): Options {
        rocksdb_options_set_max_write_buffer_number(native, maxWriteBufferNumber)
        return this
    }

    actual fun setMinWriteBufferNumberToMerge(minWriteBufferNumberToMerge: Int): Options {
        rocksdb_options_set_min_write_buffer_number_to_merge(native, minWriteBufferNumberToMerge)
        return this
    }

    actual fun setCreateMissingColumnFamilies(flag: Boolean): Options {
        rocksdb_options_set_create_missing_column_families(native, flag.toUByte())
        return this
    }

    actual fun createMissingColumnFamilies(): Boolean =
        rocksdb_options_get_create_missing_column_families(native).toBoolean()

    actual fun setErrorIfExists(errorIfExists: Boolean): Options {
        rocksdb_options_set_error_if_exists(native, errorIfExists.toUByte())
        return this
    }

    actual fun errorIfExists(): Boolean =
        rocksdb_options_get_error_if_exists(native).toBoolean()

    actual fun setParanoidChecks(paranoidChecks: Boolean): Options {
        rocksdb_options_set_paranoid_checks(native, paranoidChecks.toUByte())
        return this
    }

    actual fun paranoidChecks(): Boolean =
        rocksdb_options_get_paranoid_checks(native).toBoolean()

    actual fun setInfoLogLevel(infoLogLevel: InfoLogLevel): Options {
        rocksdb_options_set_info_log_level(native, infoLogLevel.value.toInt())
        return this
    }

    actual fun infoLogLevel(): InfoLogLevel =
        getInfoLogLevel(rocksdb_options_get_info_log_level(native).toUByte())

    actual fun setStatistics(statistics: Statistics): Options {
        throw NotImplementedError("DO SOMETHING")
//        this.statistics = statistics
//        native.statistics = statistics.native
//        return this
    }

    actual fun statistics(): Statistics? {
        throw NotImplementedError("DO SOMETHING")
//        return this.statistics ?:
//            native.statistics?.let { Statistics(it) }
    }

    actual fun setUseFsync(useFsync: Boolean): Options {
        rocksdb_options_set_use_fsync(native, if (useFsync) 1 else 0)
        return this
    }

    actual fun useFsync(): Boolean =
        rocksdb_options_get_use_fsync(native) == 1

    actual fun setMaxLogFileSize(maxLogFileSize: Long): Options {
        rocksdb_options_set_max_log_file_size(native, maxLogFileSize.toULong())
        return this
    }

    actual fun maxLogFileSize() =
        rocksdb_options_get_max_log_file_size(native).toLong()

    actual fun setLogFileTimeToRoll(logFileTimeToRoll: Long): Options {
        rocksdb_options_set_log_file_time_to_roll(native, logFileTimeToRoll.toULong())
        return this
    }

    actual fun logFileTimeToRoll() =
        rocksdb_options_get_log_file_time_to_roll(native).toLong()

    actual fun setKeepLogFileNum(keepLogFileNum: Long): Options {
        rocksdb_options_set_keep_log_file_num(native, keepLogFileNum.toULong())
        return this
    }

    actual fun keepLogFileNum(): Long {
        return rocksdb_options_get_keep_log_file_num(native).toLong()
    }

    actual fun setWalSizeLimitMB(sizeLimitMB: Long): Options {
        rocksdb_options_set_WAL_size_limit_MB(native, sizeLimitMB.toULong())
        return this
    }

    actual fun walSizeLimitMB(): Long {
        return rocksdb_options_get_WAL_size_limit_MB(native).toLong()
    }

    actual fun setEnv(env: Env): Options {
        throw NotImplementedError("DO SOMETHING")
//        native.setEnv(env.native)
//        return this
    }

    actual fun getEnv(): Env {
        throw NotImplementedError("DO SOMETHING")
//        return RocksEnv(native.env())
    }
}
