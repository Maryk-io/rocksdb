package maryk.rocksdb

import cnames.structs.rocksdb_options_t
import kotlinx.cinterop.CPointer
import maryk.toBoolean
import maryk.toUByte
import rocksdb.rocksdb_options_create
import rocksdb.rocksdb_options_destroy
import rocksdb.rocksdb_options_get_WAL_size_limit_MB
import rocksdb.rocksdb_options_get_create_if_missing
import rocksdb.rocksdb_options_get_create_missing_column_families
import rocksdb.rocksdb_options_get_error_if_exists
import rocksdb.rocksdb_options_get_info_log_level
import rocksdb.rocksdb_options_get_keep_log_file_num
import rocksdb.rocksdb_options_get_log_file_time_to_roll
import rocksdb.rocksdb_options_get_max_log_file_size
import rocksdb.rocksdb_options_get_paranoid_checks
import rocksdb.rocksdb_options_get_use_fsync
import rocksdb.rocksdb_options_set_WAL_size_limit_MB
import rocksdb.rocksdb_options_set_create_if_missing
import rocksdb.rocksdb_options_set_create_missing_column_families
import rocksdb.rocksdb_options_set_error_if_exists
import rocksdb.rocksdb_options_set_info_log_level
import rocksdb.rocksdb_options_set_keep_log_file_num
import rocksdb.rocksdb_options_set_log_file_time_to_roll
import rocksdb.rocksdb_options_set_max_log_file_size
import rocksdb.rocksdb_options_set_paranoid_checks
import rocksdb.rocksdb_options_set_use_fsync

actual class DBOptions internal constructor(
    internal val native: CPointer<rocksdb_options_t>
) : RocksObject() {
    private var statistics: Statistics? = null

    actual constructor() : this(rocksdb_options_create()!!)

    override fun close() {
        if (isOwningHandle()) {
            rocksdb_options_destroy(native)
            super.close()
        }
    }

    actual fun setCreateIfMissing(flag: Boolean): DBOptions {
        rocksdb_options_set_create_if_missing(native, flag.toUByte())
        return this
    }

    actual fun createIfMissing(): Boolean =
        rocksdb_options_get_create_if_missing(native).toBoolean()

    actual fun setCreateMissingColumnFamilies(flag: Boolean): DBOptions {
        rocksdb_options_set_create_missing_column_families(native, flag.toUByte())
        return this
    }

    actual fun createMissingColumnFamilies() =
        rocksdb_options_get_create_missing_column_families(native).toBoolean()

    actual fun setErrorIfExists(errorIfExists: Boolean): DBOptions {
        rocksdb_options_set_error_if_exists(native, errorIfExists.toUByte())
        return this
    }

    actual fun errorIfExists(): Boolean =
        rocksdb_options_get_error_if_exists(native).toBoolean()

    actual fun setParanoidChecks(paranoidChecks: Boolean): DBOptions {
        rocksdb_options_set_paranoid_checks(native, paranoidChecks.toUByte())
        return this
    }

    actual fun paranoidChecks(): Boolean =
        rocksdb_options_get_paranoid_checks(native).toBoolean()

    actual fun setInfoLogLevel(infoLogLevel: InfoLogLevel): DBOptions {
        rocksdb_options_set_info_log_level(native, infoLogLevel.value.toInt())
        return this
    }

    actual fun infoLogLevel() = getInfoLogLevel(
        rocksdb_options_get_info_log_level(native).toUByte()
    )

    actual fun setStatistics(statistics: Statistics): DBOptions {
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

    actual fun setUseFsync(useFsync: Boolean): DBOptions {
        rocksdb_options_set_use_fsync(native, if (useFsync) 1 else 0)
        return this
    }

    actual fun useFsync() = rocksdb_options_get_use_fsync(native) == 1

    actual fun setMaxLogFileSize(maxLogFileSize: Long): DBOptions {
        rocksdb_options_set_max_log_file_size(native, maxLogFileSize.toULong())
        return this
    }

    actual fun maxLogFileSize(): Long =
        rocksdb_options_get_max_log_file_size(native).toLong()

    actual fun setLogFileTimeToRoll(logFileTimeToRoll: Long): DBOptions {
        rocksdb_options_set_log_file_time_to_roll(native, logFileTimeToRoll.toULong())
        return this
    }

    actual fun logFileTimeToRoll(): Long =
        rocksdb_options_get_log_file_time_to_roll(native).toLong()

    actual fun setKeepLogFileNum(keepLogFileNum: Long): DBOptions {
        rocksdb_options_set_keep_log_file_num(native, keepLogFileNum.toULong())
        return this
    }

    actual fun keepLogFileNum(): Long =
        rocksdb_options_get_keep_log_file_num(native).toLong()

    actual fun setWalSizeLimitMB(sizeLimitMB: Long): DBOptions {
        rocksdb_options_set_WAL_size_limit_MB(native, sizeLimitMB.toULong())
        return this
    }

    actual fun walSizeLimitMB(): Long =
        rocksdb_options_get_WAL_size_limit_MB(native).toLong()
}
