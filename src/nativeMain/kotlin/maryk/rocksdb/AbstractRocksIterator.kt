package maryk.rocksdb

import cnames.structs.rocksdb_iterator_t
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.toCValues
import maryk.toBoolean
import maryk.wrapWithErrorThrower
import rocksdb.rocksdb_iter_destroy
import rocksdb.rocksdb_iter_get_error
import rocksdb.rocksdb_iter_next
import rocksdb.rocksdb_iter_prev
import rocksdb.rocksdb_iter_seek
import rocksdb.rocksdb_iter_seek_for_prev
import rocksdb.rocksdb_iter_seek_to_first
import rocksdb.rocksdb_iter_seek_to_last
import rocksdb.rocksdb_iter_valid

actual abstract class AbstractRocksIterator<P : RocksObject>
    protected constructor(
        internal val native: CPointer<rocksdb_iterator_t>
    )
: RocksObject(), RocksIteratorInterface {
    /**
     * An iterator is either positioned at an entry, or
     * not valid.  This method returns true if the iterator is valid.
     *
     * @return true if iterator is valid.
     */
    actual override fun isValid(): Boolean =
        rocksdb_iter_valid(native).toBoolean()

    /**
     * Position at the first entry in the source.  The iterator is Valid()
     * after this call if the source is not empty.
     */
    actual override fun seekToFirst() {
        rocksdb_iter_seek_to_first(native)
    }

    /**
     * Position at the last entry in the source.  The iterator is
     * valid after this call if the source is not empty.
     */
    actual override fun seekToLast() {
        rocksdb_iter_seek_to_last(native)
    }

    /**
     * Position at the first entry in the source whose key is that or
     * past target.
     *
     * The iterator is valid after this call if the source contains
     * a key that comes at or past target.
     *
     * @param target byte array describing a key or a
     * key prefix to seek for.
     */
    actual override fun seek(target: ByteArray) {
        rocksdb_iter_seek(native, target.toCValues(), target.size.toULong())
    }

    /**
     * Position at the first entry in the source whose key is that or
     * before target.
     *
     * The iterator is valid after this call if the source contains
     * a key that comes at or before target.
     *
     * @param target byte array describing a key or a
     * key prefix to seek for.
     */
    actual override fun seekForPrev(target: ByteArray) {
        rocksdb_iter_seek_for_prev(native, target.toCValues(), target.size.toULong())
    }

    /**
     * Moves to the next entry in the source.  After this call, Valid() is
     * true if the iterator was not positioned at the last entry in the source.
     *
     * REQUIRES: [.isValid]
     */
    actual override fun next() {
        rocksdb_iter_next(native)
    }

    /**
     * Moves to the previous entry in the source.  After this call, Valid() is
     * true if the iterator was not positioned at the first entry in source.
     *
     * REQUIRES: [.isValid]
     */
    actual override fun prev() {
        rocksdb_iter_prev(native)
    }

    /**
     * If an error has occurred, return it.  Else return an ok status.
     * If non-blocking IO is requested and this operation cannot be
     * satisfied without doing some IO, then this returns Status::Incomplete().
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    actual override fun status() {
        wrapWithErrorThrower { error ->
            rocksdb_iter_get_error(native, error)
        }
    }

    override fun close() {
        if (isOwningHandle()) {
            rocksdb_iter_destroy(native)
            super.close()
        }
    }
}
