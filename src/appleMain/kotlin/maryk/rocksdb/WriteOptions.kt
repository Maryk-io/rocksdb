package maryk.rocksdb

import cnames.structs.rocksdb_writeoptions_t
import kotlinx.cinterop.CPointer
import maryk.toBoolean
import maryk.toUByte
import rocksdb.rocksdb_writeoptions_create
import rocksdb.rocksdb_writeoptions_destroy
import rocksdb.rocksdb_writeoptions_disable_WAL
import rocksdb.rocksdb_writeoptions_get_disable_WAL
import rocksdb.rocksdb_writeoptions_get_ignore_missing_column_families
import rocksdb.rocksdb_writeoptions_get_low_pri
import rocksdb.rocksdb_writeoptions_get_no_slowdown
import rocksdb.rocksdb_writeoptions_get_sync
import rocksdb.rocksdb_writeoptions_set_ignore_missing_column_families
import rocksdb.rocksdb_writeoptions_set_low_pri
import rocksdb.rocksdb_writeoptions_set_no_slowdown
import rocksdb.rocksdb_writeoptions_set_sync

actual class WriteOptions private constructor(
    internal val native: CPointer<rocksdb_writeoptions_t>
) : RocksObject() {
    actual constructor() : this(rocksdb_writeoptions_create()!!)

    override fun close() {
        if (isOwningHandle()) {
            rocksdb_writeoptions_destroy(native)
            super.close()
        }
    }

    actual fun setSync(flag: Boolean): WriteOptions {
        rocksdb_writeoptions_set_sync(native, flag.toUByte())
        return this
    }

    actual fun sync() = rocksdb_writeoptions_get_sync(native).toBoolean()

    actual fun setDisableWAL(flag: Boolean): WriteOptions {
        rocksdb_writeoptions_disable_WAL(native, if (flag) 1 else 0)
        return this
    }

    actual fun disableWAL() = rocksdb_writeoptions_get_disable_WAL(native).toBoolean()

    actual fun setIgnoreMissingColumnFamilies(ignoreMissingColumnFamilies: Boolean): WriteOptions {
        rocksdb_writeoptions_set_ignore_missing_column_families(native, ignoreMissingColumnFamilies.toUByte())
        return this
    }

    actual fun ignoreMissingColumnFamilies() =
        rocksdb_writeoptions_get_ignore_missing_column_families(native).toBoolean()

    actual fun setNoSlowdown(noSlowdown: Boolean): WriteOptions {
        rocksdb_writeoptions_set_no_slowdown(native, noSlowdown.toUByte())
        return this
    }

    actual fun noSlowdown(): Boolean {
        return rocksdb_writeoptions_get_no_slowdown(native).toBoolean()
    }

    actual fun setLowPri(lowPri: Boolean): WriteOptions {
        rocksdb_writeoptions_set_low_pri(native, lowPri.toUByte())
        return this
    }

    actual fun lowPri(): Boolean =
        rocksdb_writeoptions_get_low_pri(native).toBoolean()
}
