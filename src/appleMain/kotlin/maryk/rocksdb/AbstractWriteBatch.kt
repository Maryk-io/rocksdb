package maryk.rocksdb

import cnames.structs.rocksdb_writebatch_t
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.toCValues
import maryk.wrapWithNullErrorThrower2
import rocksdb.rocksdb_writebatch_clear
import rocksdb.rocksdb_writebatch_count
import rocksdb.rocksdb_writebatch_delete
import rocksdb.rocksdb_writebatch_delete_cf
import rocksdb.rocksdb_writebatch_delete_range
import rocksdb.rocksdb_writebatch_delete_range_cf
import rocksdb.rocksdb_writebatch_destroy
import rocksdb.rocksdb_writebatch_merge
import rocksdb.rocksdb_writebatch_merge_cf
import rocksdb.rocksdb_writebatch_pop_save_point
import rocksdb.rocksdb_writebatch_put
import rocksdb.rocksdb_writebatch_put_cf
import rocksdb.rocksdb_writebatch_put_log_data
import rocksdb.rocksdb_writebatch_rollback_to_save_point
import rocksdb.rocksdb_writebatch_set_save_point

actual abstract class AbstractWriteBatch internal constructor(
    private val nativeBase: CPointer<rocksdb_writebatch_t>
) : RocksObject(), WriteBatchInterface {
    override fun close() {
        if (isOwningHandle()) {
            rocksdb_writebatch_destroy(nativeBase)
            super.close()
        }
    }

    override fun singleDelete(key: ByteArray) {
        rocksdb_writebatch_delete(nativeBase, key.toCValues(), key.size.toULong())
    }

    override fun singleDelete(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray) {
        rocksdb_writebatch_delete_cf(nativeBase, columnFamilyHandle.native, key.toCValues(), key.size.toULong())
    }

    override fun count(): Int =
        rocksdb_writebatch_count(nativeBase)

    override fun put(key: ByteArray, value: ByteArray) {
        rocksdb_writebatch_put(nativeBase, key.toCValues(), key.size.toULong(), value.toCValues(), value.size.toULong())
    }

    override fun put(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray, value: ByteArray) {
        rocksdb_writebatch_put_cf(nativeBase, columnFamilyHandle.native, key.toCValues(), key.size.toULong(), value.toCValues(), value.size.toULong())
    }

    override fun merge(key: ByteArray, value: ByteArray) {
        rocksdb_writebatch_merge(nativeBase, key.toCValues(), key.size.toULong(), value.toCValues(), value.size.toULong())
    }

    override fun merge(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray, value: ByteArray) {
        rocksdb_writebatch_merge_cf(nativeBase, columnFamilyHandle.native, key.toCValues(), key.size.toULong(), value.toCValues(), value.size.toULong())
    }

    override fun delete(key: ByteArray) {
        rocksdb_writebatch_delete(nativeBase, key.toCValues(), key.size.toULong())
    }

    override fun delete(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray) {
        rocksdb_writebatch_delete_cf(nativeBase, columnFamilyHandle.native, key.toCValues(), key.size.toULong())
    }

    override fun deleteRange(beginKey: ByteArray, endKey: ByteArray) {
        rocksdb_writebatch_delete_range(nativeBase, beginKey.toCValues(), beginKey.size.toULong(), endKey.toCValues(), endKey.size.toULong())
    }

    override fun deleteRange(columnFamilyHandle: ColumnFamilyHandle, beginKey: ByteArray, endKey: ByteArray) {
        rocksdb_writebatch_delete_range_cf(nativeBase, columnFamilyHandle.native, beginKey.toCValues(), beginKey.size.toULong(), endKey.toCValues(), endKey.size.toULong())
    }

    override fun putLogData(blob: ByteArray) {
        rocksdb_writebatch_put_log_data(nativeBase, blob.toCValues(), blob.size.toULong())
    }

    override fun clear() {
        rocksdb_writebatch_clear(nativeBase)
    }

    override fun setSavePoint() {
        rocksdb_writebatch_set_save_point(nativeBase)
    }

    override fun rollbackToSavePoint() {
        wrapWithNullErrorThrower2 { error ->
            rocksdb_writebatch_rollback_to_save_point(nativeBase, error)
        }
    }

    override fun popSavePoint() {
        wrapWithNullErrorThrower2 { error ->
            rocksdb_writebatch_pop_save_point(nativeBase, error)
        }
    }

    override fun setMaxBytes(maxBytes: Long) {
        throw NotImplementedError("DO SOMETHING")
    }

    override fun getWriteBatch(): WriteBatch {
        return WriteBatch(nativeBase)
    }
}
