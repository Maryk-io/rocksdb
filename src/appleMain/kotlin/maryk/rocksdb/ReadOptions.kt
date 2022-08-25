package maryk.rocksdb

import cnames.structs.rocksdb_readoptions_t
import kotlinx.cinterop.CPointer
import maryk.toBoolean
import maryk.toUByte
import rocksdb.rocksdb_readoptions_create
import rocksdb.rocksdb_readoptions_destroy
import rocksdb.rocksdb_readoptions_get_fill_cache
import rocksdb.rocksdb_readoptions_get_prefix_same_as_start
import rocksdb.rocksdb_readoptions_get_verify_checksums
import rocksdb.rocksdb_readoptions_set_fill_cache
import rocksdb.rocksdb_readoptions_set_prefix_same_as_start
import rocksdb.rocksdb_readoptions_set_verify_checksums

actual class ReadOptions private constructor(val native: CPointer<rocksdb_readoptions_t>?) : RocksObject() {
    actual constructor() : this(rocksdb_readoptions_create())

    override fun close() {
        if (isOwningHandle()) {
            rocksdb_readoptions_destroy(native)
            super.close()
        }
    }

    actual fun verifyChecksums(): Boolean {
        assert(isOwningHandle())
        return rocksdb_readoptions_get_verify_checksums(native).toBoolean()
    }

    actual fun setVerifyChecksums(verifyChecksums: Boolean): ReadOptions {
        assert(isOwningHandle())
        rocksdb_readoptions_set_verify_checksums(native, verifyChecksums.toUByte())
        return this
    }

    actual fun fillCache(): Boolean {
        assert(isOwningHandle())
        return rocksdb_readoptions_get_fill_cache(native).toBoolean()
    }

    actual fun setFillCache(fillCache: Boolean): ReadOptions {
        assert(isOwningHandle())
        rocksdb_readoptions_set_fill_cache(native, fillCache.toUByte())
        return this
    }

    actual fun prefixSameAsStart(): Boolean {
        assert(isOwningHandle())
        return rocksdb_readoptions_get_prefix_same_as_start(native).toBoolean()
    }

    actual fun setPrefixSameAsStart(prefixSameAsStart: Boolean): ReadOptions {
        assert(isOwningHandle())
        rocksdb_readoptions_set_prefix_same_as_start(native, prefixSameAsStart.toUByte())
        return this;
    }
}
