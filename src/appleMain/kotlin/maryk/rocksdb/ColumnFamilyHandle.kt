package maryk.rocksdb

import cnames.structs.rocksdb_column_family_handle_t
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.posix.uint64_tVar
import rocksdb.rocksdb_column_family_handle_destroy
import rocksdb.rocksdb_column_family_handle_get_id
import rocksdb.rocksdb_column_family_handle_get_name

actual class ColumnFamilyHandle
    internal constructor(
        val native: CPointer<rocksdb_column_family_handle_t>
    )
: RocksObject() {
    override fun close() {
        if (isOwningHandle()) {
            rocksdb_column_family_handle_destroy(native)
            super.close()
        }
    }

    actual fun getName(): ByteArray = memScoped {
        val length = alloc<uint64_tVar>()
        rocksdb_column_family_handle_get_name(native, length.ptr)?.toByteArray(length.value)
            ?: throw RocksDBException("Missing Column Family Name")
    }

    actual fun getID(): Int =
        rocksdb_column_family_handle_get_id(native).toInt()
}
