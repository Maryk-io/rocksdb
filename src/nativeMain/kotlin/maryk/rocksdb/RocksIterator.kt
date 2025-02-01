package maryk.rocksdb

import cnames.structs.rocksdb_iterator_t
import kotlinx.cinterop.*
import maryk.toByteArray
import platform.posix.size_tVar
import rocksdb.rocksdb_iter_key
import rocksdb.rocksdb_iter_value

actual class RocksIterator internal constructor(
    native: CPointer<rocksdb_iterator_t>
) : AbstractRocksIterator<RocksDB>(native) {
    actual fun key(): ByteArray = memScoped {
        val length = alloc<size_tVar>()
        rocksdb_iter_key(native, length.ptr)!!.toByteArray(length.value)
    }

    actual fun value(): ByteArray = memScoped {
        val length = alloc<size_tVar>()
        rocksdb_iter_value(native, length.ptr)!!.toByteArray(length.value)
    }
}
