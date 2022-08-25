package maryk.rocksdb

import cnames.structs.rocksdb_iterator_t
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.value
import maryk.toBoolean
import maryk.wrapWithErrorThrower2
import platform.posix.size_tVar
import rocksdb.rocksdb_iter_destroy
import rocksdb.rocksdb_iter_get_error
import rocksdb.rocksdb_iter_key
import rocksdb.rocksdb_iter_next
import rocksdb.rocksdb_iter_prev
import rocksdb.rocksdb_iter_seek
import rocksdb.rocksdb_iter_seek_for_prev
import rocksdb.rocksdb_iter_seek_to_first
import rocksdb.rocksdb_iter_seek_to_last
import rocksdb.rocksdb_iter_valid
import rocksdb.rocksdb_iter_value

actual class RocksIterator internal constructor(
    internal val native: CPointer<rocksdb_iterator_t>
) : AbstractRocksIterator<RocksDB>() {
    override fun close() {
        if (isOwningHandle()) {
            rocksdb_iter_destroy(native)
            super.close()
        }
    }

    actual fun key(): ByteArray = memScoped {
        val length = alloc<size_tVar>()
        rocksdb_iter_key(native, length.ptr)!!.toByteArray(length.value)
    }

    actual fun value(): ByteArray = memScoped {
        val length = alloc<size_tVar>()
        rocksdb_iter_value(native, length.ptr)!!.toByteArray(length.value)
    }

    override fun isValid(): Boolean =
        rocksdb_iter_valid(native).toBoolean()

    override fun seekToFirst() {
        rocksdb_iter_seek_to_first(native)
    }

    override fun seekToLast() {
        rocksdb_iter_seek_to_last(native)
    }

    override fun seek(target: ByteArray) {
        rocksdb_iter_seek(native, target.toCValues(), target.size.toULong())
    }

    override fun seekForPrev(target: ByteArray) {
        rocksdb_iter_seek_for_prev(native, target.toCValues(), target.size.toULong())
    }

    override fun next() {
        rocksdb_iter_next(native)
    }

    override fun prev() {
        rocksdb_iter_prev(native)
    }

    override fun status() {
        wrapWithErrorThrower2 { error ->
            rocksdb_iter_get_error(native, error)
        }
    }
}
