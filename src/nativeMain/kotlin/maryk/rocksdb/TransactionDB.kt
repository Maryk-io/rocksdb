@file:OptIn(ExperimentalNativeApi::class)

package maryk.rocksdb

import cnames.structs.rocksdb_transactiondb_t
import kotlinx.cinterop.CPointer
import kotlin.experimental.ExperimentalNativeApi

actual open class TransactionDB
internal constructor(
    internal val tnative: CPointer<rocksdb_transactiondb_t>,
) : RocksDB(rocksdb.rocksdb_transactiondb_get_base_db(tnative)!!) {
    val defaultTransactionOptions: TransactionOptions = TransactionOptions()

    override fun close() {
        if (isOwningHandle()) {
            defaultTransactionOptions.close()
            super.close()
        }
    }

    actual fun beginTransaction(writeOptions: WriteOptions): Transaction {
        return rocksdb.rocksdb_transaction_begin(tnative, writeOptions.native, defaultTransactionOptions.native, null)!!.let(::Transaction)
    }

    actual fun beginTransaction(writeOptions: WriteOptions, transactionOptions: TransactionOptions): Transaction {
        return rocksdb.rocksdb_transaction_begin(tnative, writeOptions.native, transactionOptions.native, null)!!.let(::Transaction)
    }

    actual fun beginTransaction(writeOptions: WriteOptions, oldTransaction: Transaction): Transaction {
        rocksdb.rocksdb_transaction_begin(tnative, writeOptions.native, defaultTransactionOptions.native, oldTransaction.native)
        return oldTransaction
    }

    actual fun beginTransaction(writeOptions: WriteOptions, transactionOptions: TransactionOptions, oldTransaction: Transaction): Transaction {
        rocksdb.rocksdb_transaction_begin(tnative, writeOptions.native, transactionOptions.native, oldTransaction.native)
        return oldTransaction
    }

    actual fun getTransactionByName(transactionName: String): Transaction? {
        TODO()
    }

    actual fun getAllPreparedTransactions(): List<Transaction> {
        TODO()
    }

    actual fun getLockStatusData(): Map<Long, KeyLockInfo> {
        TODO()
    }

    actual fun getDeadlockInfoBuffer(): Array<DeadlockPath> {
        TODO()
    }

    actual fun setDeadlockInfoBufferSize(targetSize: Int) {
        TODO()
    }
}

actual class KeyLockInfo actual constructor(
    val key: String,
    val transactionIDs: LongArray,
    val exclusive: Boolean
) {
    actual fun getKey(): String = key
    actual fun getTransactionIDs(): LongArray = transactionIDs
    actual fun isExclusive(): Boolean = exclusive
}

/**
 * Represents information about a deadlock involving transactions.
 *
 * This data class contains details about the transactions involved in a deadlock,
 * including the transaction IDs, column family IDs, waiting keys, and lock statuses.
 */
actual class DeadlockInfo(
    val transactionID: Long,
    val columnFamilyId: Long,
    val waitingKey: String,
    val isExclusive: Boolean,
) {
    actual fun getTransactionID(): Long = transactionID
    actual fun getColumnFamilyId(): Long = columnFamilyId
    actual fun getWaitingKey(): String = waitingKey
    actual fun isExclusive(): Boolean = isExclusive
}

/**
 * Represents a path of transactions involved in a deadlock.
 *
 * This data class contains an array of [DeadlockInfo] instances representing the
 * sequence of transactions and their dependencies, as well as a flag indicating
 * whether the deadlock detection limit was exceeded.
 */
actual class DeadlockPath(
    val isEmpty: Boolean,
) {
    /**
     * Checks if the deadlock path is empty and the limit has not been exceeded.
     *
     * @return `true` if the path is empty and the limit is not exceeded; otherwise, `false`.
     */
    actual fun isEmpty(): Boolean = isEmpty
}
