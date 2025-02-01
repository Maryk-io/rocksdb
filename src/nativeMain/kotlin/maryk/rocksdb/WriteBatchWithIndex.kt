package maryk.rocksdb

import cnames.structs.rocksdb_writebatch_t
import kotlinx.cinterop.CPointer
import rocksdb.rocksdb_writebatch_create

actual class WriteBatchWithIndex(
    internal val native: CPointer<rocksdb_writebatch_t>
) : AbstractWriteBatch(native) {
    actual constructor() : this(rocksdb_writebatch_create()!!)

    actual constructor(overwriteKey: Boolean) : this()

    actual constructor(fallbackIndexComparator: AbstractComparator, reservedBytes: Int, overwriteKey: Boolean) : this()

    actual fun newIterator(columnFamilyHandle: ColumnFamilyHandle): WBWIRocksIterator {
        TODO("Not yet implemented")
    }

    actual fun newIterator(): WBWIRocksIterator {
        TODO("Not yet implemented")
    }

    actual fun newIteratorWithBase(columnFamilyHandle: ColumnFamilyHandle, baseIterator: RocksIterator): RocksIterator {
        TODO("Not yet implemented")
    }

    actual fun newIteratorWithBase(columnFamilyHandle: ColumnFamilyHandle, baseIterator: RocksIterator, readOptions: ReadOptions?): RocksIterator {
        TODO("Not yet implemented")
    }

    actual fun newIteratorWithBase(baseIterator: RocksIterator): RocksIterator {
        TODO("Not yet implemented")
    }

    actual fun newIteratorWithBase(baseIterator: RocksIterator, readOptions: ReadOptions?): RocksIterator {
        TODO("Not yet implemented")
    }

    actual fun getFromBatch(columnFamilyHandle: ColumnFamilyHandle, options: DBOptions, key: ByteArray): ByteArray? {
        TODO("Not yet implemented")
    }

    actual fun getFromBatch(options: DBOptions, key: ByteArray): ByteArray? {
        TODO("Not yet implemented")
    }

    actual fun getFromBatchAndDB(db: RocksDB, columnFamilyHandle: ColumnFamilyHandle, options: ReadOptions, key: ByteArray): ByteArray? {
        TODO("Not yet implemented")
    }

    actual fun getFromBatchAndDB(db: RocksDB, options: ReadOptions, key: ByteArray): ByteArray? {
        TODO("Not yet implemented")
    }
}
