package maryk.rocksdb

/**
 * Represents a RocksDB instance with transaction support.
 *
 * `TransactionDB` extends the functionality of `RocksDB` by providing
 * transactional capabilities, allowing for atomic operations across multiple
 * key-value pairs. It implements the [TransactionalDB] interface, parameterized
 * with [TransactionOptions], to facilitate transaction management.
 *
 * **Example Usage:**
 *
 * ```kotlin
 * val options = Options().setCreateIfMissing(true)
 * val transactionDbOptions = TransactionDBOptions()
 * val transactionDB = TransactionDB.open(options, transactionDbOptions, "/path/to/db")
 *
 * val writeOptions = WriteOptions().setSync(true)
 * val txn = transactionDB.beginTransaction(writeOptions)
 *
 * try {
 *     txn.put("key1", "value1")
 *     txn.put("key2", "value2")
 *     txn.commit()
 * } catch (e: RocksDBException) {
 *     txn.rollback()
 * } finally {
 *     txn.close()
 *     transactionDB.close()
 * }
 * ```
 */
expect class TransactionDB : RocksDB {
    /**
     * Starts a new Transaction.
     *
     * Caller is responsible for calling [Transaction.close] on the returned
     * transaction when it is no longer needed.
     *
     * @param writeOptions Any write options for the transaction.
     * @return A new [Transaction] instance.
     */
    fun beginTransaction(writeOptions: WriteOptions): Transaction

    /**
     * Starts a new Transaction with specified transaction options.
     *
     * Caller is responsible for calling [Transaction.close] on the returned
     * transaction when it is no longer needed.
     *
     * @param writeOptions Any write options for the transaction.
     * @param transactionOptions Any options for the transaction.
     * @return A new [Transaction] instance.
     */
    fun beginTransaction(writeOptions: WriteOptions, transactionOptions: TransactionOptions): Transaction

    /**
     * Starts a new Transaction by reusing an existing transaction.
     *
     * This is an optimization to avoid extra allocations when repeatedly creating transactions.
     * Caller is responsible for calling [Transaction.close] on the returned
     * transaction when it is no longer needed.
     *
     * @param writeOptions Any write options for the transaction.
     * @param oldTransaction The existing [Transaction] to be reused.
     * @return The [oldTransaction] reinitialized as a new transaction.
     */
    fun beginTransaction(writeOptions: WriteOptions, oldTransaction: Transaction): Transaction

    /**
     * Starts a new Transaction with specified transaction options by reusing an existing transaction.
     *
     * This allows for creating a nested transaction with additional configurations,
     * optimizing resource usage by reusing the provided transaction instance.
     * Caller is responsible for calling [Transaction.close] on the returned
     * transaction when it is no longer needed.
     *
     * @param writeOptions Any write options for the transaction.
     * @param transactionOptions Any options for the transaction.
     * @param oldTransaction The existing [Transaction] to be reused.
     * @return The [oldTransaction] reinitialized as a new transaction.
     */
    fun beginTransaction(
        writeOptions: WriteOptions,
        transactionOptions: TransactionOptions,
        oldTransaction: Transaction
    ): Transaction

    /**
     * Retrieves a transaction by its name.
     *
     * @param transactionName The name of the transaction to retrieve.
     *
     * @return A [Transaction] instance if found; otherwise, `null`.
     */
    fun getTransactionByName(transactionName: String): Transaction?

    /**
     * Retrieves all prepared transactions currently held by the database.
     *
     * @return A list of all [Transaction] instances that are in a prepared state.
     */
    fun getAllPreparedTransactions(): List<Transaction>

    /**
     * Retrieves the current status of all locks held by the database.
     *
     * @return A map where each key is a transaction ID, and each value is a [KeyLockInfo] instance detailing the lock information.
     */
    fun getLockStatusData(): Map<Long, KeyLockInfo>

    /**
     * Retrieves the deadlock information buffer.
     *
     * @return An array of [DeadlockPath] instances representing detected deadlocks.
     */
    fun getDeadlockInfoBuffer(): Array<DeadlockPath>

    /**
     * Sets the size of the deadlock information buffer.
     *
     * @param targetSize The desired size of the deadlock information buffer.
     */
    fun setDeadlockInfoBufferSize(targetSize: Int)
}


/**
 * Represents information about a specific key lock.
 *
 * This data class contains details about the key being locked, the transactions
 * holding the lock, and whether the lock is exclusive.
 */
expect class KeyLockInfo(
    key: String,
    transactionIDs: LongArray,
    exclusive: Boolean
) {
    fun getKey(): String
    fun getTransactionIDs(): LongArray
    fun isExclusive(): Boolean
}

/**
 * Represents information about a deadlock involving transactions.
 *
 * This data class contains details about the transactions involved in a deadlock,
 * including the transaction IDs, column family IDs, waiting keys, and lock statuses.
 */
expect class DeadlockInfo {
    fun getTransactionID(): Long
    fun getColumnFamilyId(): Long
    fun getWaitingKey(): String
    fun isExclusive(): Boolean
}

/**
 * Represents a path of transactions involved in a deadlock.
 *
 * This data class contains an array of [DeadlockInfo] instances representing the
 * sequence of transactions and their dependencies, as well as a flag indicating
 * whether the deadlock detection limit was exceeded.
 */
expect class DeadlockPath {
    /**
     * Checks if the deadlock path is empty and the limit has not been exceeded.
     *
     * @return `true` if the path is empty and the limit is not exceeded; otherwise, `false`.
     */
    fun isEmpty(): Boolean
}
