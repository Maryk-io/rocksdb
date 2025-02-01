package maryk.rocksdb

actual class TransactionDBOptions actual constructor(): RocksObject() {
    val native = rocksdb.rocksdb_transactiondb_options_create()

    actual fun getMaxNumLocks(): Long {
        return rocksdb.rocksdb_transactiondb_options_get_max_num_locks(native)
    }

    actual fun setMaxNumLocks(maxNumLocks: Long): TransactionDBOptions {
        rocksdb.rocksdb_transactiondb_options_set_max_num_locks(native, maxNumLocks)
        return this
    }

    actual fun getNumStripes(): Long = rocksdb.rocksdb_transactiondb_options_get_num_stripes(native).toLong()

    actual fun setNumStripes(numStripes: Long): TransactionDBOptions {
        rocksdb.rocksdb_transactiondb_options_set_num_stripes(native, numStripes.toULong())
        return this
    }

    actual fun getTransactionLockTimeout(): Long = rocksdb.rocksdb_transactiondb_options_get_transaction_lock_timeout(native)

    actual fun setTransactionLockTimeout(transactionLockTimeout: Long): TransactionDBOptions {
        rocksdb.rocksdb_transactiondb_options_set_transaction_lock_timeout(native, transactionLockTimeout)
        return this
    }

    actual fun getDefaultLockTimeout(): Long = rocksdb.rocksdb_transactiondb_options_get_default_lock_timeout(native).toLong()

    actual fun setDefaultLockTimeout(defaultLockTimeout: Long): TransactionDBOptions {
        rocksdb.rocksdb_transactiondb_options_set_default_lock_timeout(native, defaultLockTimeout)
        return this
    }

    actual fun getWritePolicy(): TxnDBWritePolicy {
        return getTxnDBWritePolicy(rocksdb.rocksdb_transactiondb_options_get_write_policy(native).toByte())
    }

    actual fun setWritePolicy(writePolicy: TxnDBWritePolicy): TransactionDBOptions {
        rocksdb.rocksdb_transactiondb_options_set_write_policy(native, writePolicy.getValue().toInt())
        return this
    }

    override fun close() {
        if (isOwningHandle()) {
            rocksdb.rocksdb_transactiondb_options_destroy(native)
            super.close()
        }
    }
}
