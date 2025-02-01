package maryk.rocksdb

import maryk.toBoolean
import maryk.toUByte

actual class TransactionOptions actual constructor(): RocksObject() {
    val native = rocksdb.rocksdb_transaction_options_create()


    actual fun isSetSnapshot(): Boolean {
        return rocksdb.rocksdb_transaction_options_get_set_snapshot(native).toBoolean()
    }

    actual fun setSetSnapshot(setSnapshot: Boolean): TransactionOptions {
        rocksdb.rocksdb_transaction_options_set_set_snapshot(native, setSnapshot.toUByte())
        return this
    }

    actual fun isDeadlockDetect(): Boolean {
        return rocksdb.rocksdb_transaction_options_get_deadlock_detect(native).toBoolean()
    }

    actual fun setDeadlockDetect(deadlockDetect: Boolean): TransactionOptions {
        rocksdb.rocksdb_transaction_options_set_deadlock_detect(native, deadlockDetect.toUByte())
        return this
    }

    actual fun getLockTimeout(): Long {
        return rocksdb.rocksdb_transaction_options_get_lock_timeout(native).toLong()
    }

    actual fun setLockTimeout(lockTimeout: Long): TransactionOptions {
        rocksdb.rocksdb_transaction_options_set_lock_timeout(native, lockTimeout)
        return this
    }

    actual fun getExpiration(): Long {
        return rocksdb.rocksdb_transaction_options_get_expiration(native).toLong()
    }

    actual fun setExpiration(expiration: Long): TransactionOptions {
        rocksdb.rocksdb_transaction_options_set_expiration(native, expiration)
        return this
    }

    actual fun getDeadlockDetectDepth(): Long {
        return rocksdb.rocksdb_transaction_options_get_deadlock_detect_depth(native).toLong()
    }

    actual fun setDeadlockDetectDepth(deadlockDetectDepth: Long): TransactionOptions {
        rocksdb.rocksdb_transaction_options_set_deadlock_detect_depth(native, deadlockDetectDepth)
        return this
    }

    actual fun getMaxWriteBatchSize(): Long {
        return rocksdb.rocksdb_transaction_options_get_max_write_batch_size(native).toLong()
    }

    actual fun setMaxWriteBatchSize(maxWriteBatchSize: Long): TransactionOptions {
        rocksdb.rocksdb_transaction_options_set_max_write_batch_size(native, maxWriteBatchSize.toULong())
        return this
    }

    override fun close() {
        if (isOwningHandle()) {
            rocksdb.rocksdb_transaction_options_destroy(native)
            super.close()
        }
    }
}
