@file:Suppress("unused")

package maryk.rocksdb

import maryk.ByteBuffer

/**
 * Provides `BEGIN`/`COMMIT`/`ROLLBACK` transactions.
 *
 * To use transactions, you must first create either an
 * [`OptimisticTransactionDB`](#OptimisticTransactionDB) or a [`TransactionDB`](#TransactionDB).
 *
 * To create a transaction, use
 * [`OptimisticTransactionDB.beginTransaction`](#OptimisticTransactionDB.beginTransaction) or
 * [`TransactionDB.beginTransaction`](#TransactionDB.beginTransaction).
 *
 * It is up to the caller to synchronize access to this object.
 *
 * See [`OptimisticTransactionSample.java`](#OptimisticTransactionSample.java) and
 * [`TransactionSample.java`](#TransactionSample.java) for some simple
 * examples.
 */
expect class Transaction : RocksObject {

    /**
     * If a transaction has a snapshot set, the transaction will ensure that
     * any keys successfully written (or fetched via [`getForUpdate`](#getForUpdate)) have
     * not been modified outside of this transaction since the time the snapshot
     * was set.
     *
     * If a snapshot has not been set, the transaction guarantees that keys have
     * not been modified since the time each key was first written (or fetched via
     * [`getForUpdate`](#getForUpdate)).
     *
     * Using `setSnapshot()` will provide stricter isolation guarantees
     * at the expense of potentially more transaction failures due to conflicts
     * with other writes.
     *
     * Calling `setSnapshot()` has no effect on keys written before this
     * function has been called.
     *
     * `setSnapshot()` may be called multiple times if you would like to
     * change the snapshot used for different operations in this transaction.
     *
     * Calling `setSnapshot()` will not affect the version of Data returned
     * by `get(...)` methods. See [`get`](#get) for more details.
     */
    fun setSnapshot()

    /**
     * Similar to [`setSnapshot()`](#setSnapshot), but will not change the current snapshot
     * until `put`/`merge`/`delete`/`getForUpdate`/`multiGetForUpdate` is called.
     * By calling this function, the transaction will essentially call
     * [`setSnapshot()`](#setSnapshot) for you right before performing the next
     * `write`/`getForUpdate`.
     *
     * Calling `setSnapshotOnNextOperation()` will not affect what
     * snapshot is returned by [`getSnapshot`](#getSnapshot) until the next
     * `write`/`getForUpdate` is executed.
     *
     * When the snapshot is created, the notifier's `snapshotCreated` method will
     * be called so that the caller can get access to the snapshot.
     *
     * This is an optimization to reduce the likelihood of conflicts that
     * could occur in between the time [`setSnapshot()`](#setSnapshot) is called and the
     * first `write`/`getForUpdate` operation. For example, this prevents the following
     * race condition:
     *
     * ```
     * txn1.setSnapshot()
     * txn2.put("A", ...)
     * txn2.commit()
     * txn1.getForUpdate(opts, "A", ...)  // FAIL!
     * ```
     */
    fun setSnapshotOnNextOperation()

    /**
     * Similar to [`setSnapshot()`](#setSnapshot), but will not change the current snapshot
     * until `put`/`merge`/`delete`/`getForUpdate`/`multiGetForUpdate` is called.
     * By calling this function, the transaction will essentially call
     * [`setSnapshot()`](#setSnapshot) for you right before performing the next
     * `write`/`getForUpdate`.
     *
     * Calling [`setSnapshotOnNextOperation`](#setSnapshotOnNextOperation) will not affect what
     * snapshot is returned by [`getSnapshot`](#getSnapshot) until the next
     * `write`/`getForUpdate` is executed.
     *
     * When the snapshot is created, the
     * [`AbstractTransactionNotifier.snapshotCreated`](#AbstractTransactionNotifier.snapshotCreated) method will
     * be called so that the caller can get access to the snapshot.
     *
     * This is an optimization to reduce the likelihood of conflicts that
     * could occur in between the time [`setSnapshot()`](#setSnapshot) is called and the
     * first `write`/`getForUpdate` operation. For example, this prevents the following
     * race condition:
     *
     * ```
     * txn1.setSnapshot()
     * txn2.put("A", ...)
     * txn2.commit()
     * txn1.getForUpdate(opts, "A", ...)  // FAIL!
     * ```
     *
     * @param transactionNotifier A handler for receiving snapshot notifications
     * for the transaction
     */
    fun setSnapshotOnNextOperation(transactionNotifier: AbstractTransactionNotifier)

    /**
     * Returns the Snapshot created by the last call to [`setSnapshot()`](#setSnapshot).
     *
     * **REQUIRED:** The returned Snapshot is only valid up until the next time
     * [`setSnapshot()`](#setSnapshot)/[`setSnapshotOnNextOperation()`](#setSnapshotOnNextOperation) is called,
     * [`clearSnapshot()`](#clearSnapshot) is called, or the Transaction is deleted.
     *
     * @return The snapshot or `null` if there is no snapshot
     */
    fun getSnapshot(): Snapshot?

    /**
     * Clears the current snapshot (i.e., no snapshot will be 'set').
     *
     * This removes any snapshot that currently exists or is set to be created
     * on the next update operation (`setSnapshotOnNextOperation()`).
     *
     * Calling `clearSnapshot()` has no effect on keys written before this
     * function has been called.
     *
     * If a reference to a snapshot was retrieved via [`getSnapshot()`](#getSnapshot), it
     * will no longer be valid and should be discarded after a call to
     * `clearSnapshot()`.
     */
    fun clearSnapshot()

    /**
     * Prepares the current transaction for two-phase commit (2PC).
     *
     * @throws RocksDBException if an error occurs during preparation
     */
    fun prepare()

    /**
     * Writes all batched keys to the database atomically.
     *
     * Returns `OK` on success.
     *
     * May return any error status that could be returned by `DB:Write()`.
     *
     * If this transaction was created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * `Status::Busy()` may be returned if the transaction could not guarantee
     * that there are no write conflicts. `Status::TryAgain()` may be returned
     * if the memtable history size is not large enough
     * (See `max_write_buffer_number_to_maintain`).
     *
     * If this transaction was created by a [`TransactionDB`](#TransactionDB),
     * `Status::Expired()` may be returned if this transaction has lived for
     * longer than [`TransactionOptions.getExpiration()`](#TransactionOptions.getExpiration).
     *
     * @throws RocksDBException if an error occurs when committing the transaction
     */
    @Throws(RocksDBException::class)
    fun commit()

    /**
     * Discards all batched writes in this transaction.
     *
     * @throws RocksDBException if an error occurs when rolling back the transaction
     */
    @Throws(RocksDBException::class)
    fun rollback()

    /**
     * Records the state of the transaction for future calls to
     * [`rollbackToSavePoint()`](#rollbackToSavePoint).
     *
     * May be called multiple times to set multiple save points.
     *
     * @throws RocksDBException if an error occurs while setting a save point
     */
    @Throws(RocksDBException::class)
    fun setSavePoint()

    /**
     * Undoes all operations in this transaction (`put`, `merge`, `delete`, `putLogData`)
     * since the most recent call to [`setSavePoint()`](#setSavePoint) and removes the most
     * recent save point.
     *
     * If there is no previous call to [`setSavePoint()`](#setSavePoint),
     * returns `Status::NotFound()`.
     *
     * @throws RocksDBException if an error occurs when rolling back to a save point
     */
    @Throws(RocksDBException::class)
    fun rollbackToSavePoint()

    /**
     * This function is similar to
     * [`RocksDB.get(ColumnFamilyHandle, ReadOptions, ByteArray)`](#RocksDB.get) except it will
     * also read pending changes in this transaction.
     * Currently, this function will return `Status::MergeInProgress` if the most
     * recent write to the queried key in this batch is a Merge.
     *
     * If [`ReadOptions.snapshot`](#ReadOptions.snapshot) is not set, the current version of the
     * key will be read. Calling [`setSnapshot()`](#setSnapshot) does not affect the
     * version of the data returned.
     *
     * Note that setting [`ReadOptions.setSnapshot`](#ReadOptions.setSnapshot) will affect
     * what is read from the DB but will **not** change which keys are read from this
     * transaction (the keys in this transaction do not yet belong to any snapshot
     * and will be fetched regardless).
     *
     * @param readOptions Read options.
     * @param columnFamilyHandle [`ColumnFamilyHandle`](#ColumnFamilyHandle) instance
     * @param key the key to retrieve the value for.
     *
     * @return A byte array storing the value associated with the input key if
     *     any, or `null` if it does not find the specified key.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying native
     *     library.
     */
    fun get(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray
    ): ByteArray?

    /**
     * This function is similar to
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get) except it will
     * also read pending changes in this transaction.
     * Currently, this function will return `Status::MergeInProgress` if the most
     * recent write to the queried key in this batch is a Merge.
     *
     * If [`ReadOptions.snapshot`](#ReadOptions.snapshot) is not set, the current version of the
     * key will be read. Calling [`setSnapshot()`](#setSnapshot) does not affect the
     * version of the data returned.
     *
     * Note that setting [`ReadOptions.setSnapshot`](#ReadOptions.setSnapshot) will affect
     * what is read from the DB but will **not** change which keys are read from this
     * transaction (the keys in this transaction do not yet belong to any snapshot
     * and will be fetched regardless).
     *
     * @param readOptions Read options.
     * @param key the key to retrieve the value for.
     *
     * @return A byte array storing the value associated with the input key if
     *     any, or `null` if it does not find the specified key.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying native
     *     library.
     */
    fun get(
        readOptions: ReadOptions,
        key: ByteArray
    ): ByteArray?

    /**
     * Gets the value associated with the specified key in the default column family.
     *
     * @param opt [`ReadOptions`](#ReadOptions) instance.
     * @param key the key to retrieve the value.
     * @param value the out-value to receive the retrieved value.
     * @return A [`GetStatus`](#GetStatus) wrapping the result status and the return value size.
     *     If [`GetStatus.status`](#GetStatus.status) is `Ok` then [`GetStatus.requiredSize`](#GetStatus.requiredSize) contains
     *     the size of the actual value that matches the specified
     *     `key` in bytes. If [`GetStatus.requiredSize`](#GetStatus.requiredSize) is greater than the
     *     length of `value`, then it indicates that the size of the
     *     input buffer `value` is insufficient and a partial result was
     *     returned. If [`GetStatus.status`](#GetStatus.status) is `NotFound` this indicates that
     *     the value was not found.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun get(
        opt: ReadOptions,
        key: ByteArray,
        value: ByteArray
    ): GetStatus

    /**
     * Gets the value associated with the specified key in a specified column family.
     *
     * @param opt [`ReadOptions`](#ReadOptions) instance.
     * @param columnFamilyHandle the column family to find the key in
     * @param key the key to retrieve the value.
     * @param value the out-value to receive the retrieved value.
     * @return A [`GetStatus`](#GetStatus) wrapping the result status and the return value size.
     *     If [`GetStatus.status`](#GetStatus.status) is `Ok` then [`GetStatus.requiredSize`](#GetStatus.requiredSize) contains
     *     the size of the actual value that matches the specified
     *     `key` in bytes. If [`GetStatus.requiredSize`](#GetStatus.requiredSize) is greater than the
     *     length of `value`, then it indicates that the size of the
     *     input buffer `value` is insufficient and a partial result was
     *     returned. If [`GetStatus.status`](#GetStatus.status) is `NotFound` this indicates that
     *     the value was not found.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun get(
        opt: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray
    ): GetStatus

    /**
     * Gets the value associated with the specified key within the specified column family.
     *
     * @param opt [`ReadOptions`](#ReadOptions) instance.
     * @param columnFamilyHandle the column family in which to find the key.
     * @param key the key to retrieve the value. It is using position and limit.
     *     Supports direct buffer only.
     * @param value the out-value to receive the retrieved value.
     *     It is using position and limit. Limit is set according to value size.
     *     Supports direct buffer only.
     * @return A [`GetStatus`](#GetStatus) wrapping the result status and the return value size.
     *     If [`GetStatus.status`](#GetStatus.status) is `Ok` then [`GetStatus.requiredSize`](#GetStatus.requiredSize) contains
     *     the size of the actual value that matches the specified
     *     `key` in bytes. If [`GetStatus.requiredSize`](#GetStatus.requiredSize) is greater than the
     *     length of `value`, then it indicates that the size of the
     *     input buffer `value` is insufficient and a partial result was
     *     returned. If [`GetStatus.status`](#GetStatus.status) is `NotFound` this indicates that
     *     the value was not found.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun get(
        opt: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer
    ): GetStatus

    /**
     * Gets the value associated with the specified key within the default column family.
     *
     * @param opt [`ReadOptions`](#ReadOptions) instance.
     * @param key the key to retrieve the value. It is using position and limit.
     *     Supports direct buffer only.
     * @param value the out-value to receive the retrieved value.
     *     It is using position and limit. Limit is set according to value size.
     *     Supports direct buffer only.
     * @return A [`GetStatus`](#GetStatus) wrapping the result status and the return value size.
     *     If [`GetStatus.status`](#GetStatus.status) is `Ok` then [`GetStatus.requiredSize`](#GetStatus.requiredSize) contains
     *     the size of the actual value that matches the specified
     *     `key` in bytes. If [`GetStatus.requiredSize`](#GetStatus.requiredSize) is greater than the
     *     length of `value`, then it indicates that the size of the
     *     input buffer `value` is insufficient and a partial result was
     *     returned. If [`GetStatus.status`](#GetStatus.status) is `NotFound` this indicates that
     *     the value was not found.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun get(
        opt: ReadOptions,
        key: ByteBuffer,
        value: ByteBuffer
    ): GetStatus

    /**
     * This function is similar to
     * [`RocksDB.multiGetAsList`](#RocksDB.multiGetAsList) except it will
     * also read pending changes in this transaction.
     * Currently, this function will return `Status::MergeInProgress` if the most
     * recent write to the queried key in this batch is a Merge.
     *
     * If [`ReadOptions.snapshot`](#ReadOptions.snapshot) is not set, the current version of the
     * key will be read. Calling [`setSnapshot()`](#setSnapshot) does not affect the
     * version of the data returned.
     *
     * Note that setting [`ReadOptions.setSnapshot`](#ReadOptions.setSnapshot) will affect
     * what is read from the DB but will **not** change which keys are read from this
     * transaction (the keys in this transaction do not yet belong to any snapshot
     * and will be fetched regardless).
     *
     * @param readOptions Read options.
     * @param columnFamilyHandles A list containing
     *     [`ColumnFamilyHandle`](#ColumnFamilyHandle) instances.
     * @param keys The keys for which values need to be retrieved.
     *
     * @return A list of values, one for each key.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     * @throws IllegalArgumentException thrown if the size of passed keys is not
     *    equal to the number of passed column family handles.
     */
    fun multiGetAsList(
        readOptions: ReadOptions,
        columnFamilyHandles: List<ColumnFamilyHandle>,
        keys: List<ByteArray>
    ): List<ByteArray?>

    /**
     * This function is similar to
     * [`RocksDB.multiGetAsList`](#RocksDB.multiGetAsList) except it will
     * also read pending changes in this transaction.
     * Currently, this function will return `Status::MergeInProgress` if the most
     * recent write to the queried key in this batch is a Merge.
     *
     * If [`ReadOptions.snapshot`](#ReadOptions.snapshot) is not set, the current version of the
     * key will be read. Calling [`setSnapshot()`](#setSnapshot) does not affect the
     * version of the data returned.
     *
     * Note that setting [`ReadOptions.setSnapshot`](#ReadOptions.setSnapshot) will affect
     * what is read from the DB but will **not** change which keys are read from this
     * transaction (the keys in this transaction do not yet belong to any snapshot
     * and will be fetched regardless).
     *
     * @param readOptions Read options.
     * @param keys The keys for which values need to be retrieved.
     *
     * @return A list of values, one for each key.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun multiGetAsList(
        readOptions: ReadOptions,
        keys: List<ByteArray>
    ): List<ByteArray?>

    /**
     * Reads this key and ensures that this transaction will only
     * be able to be committed if this key is not written outside this
     * transaction after it has first been read (or after the snapshot if a
     * snapshot is set in this transaction). The transaction behavior is the
     * same regardless of whether the key exists or not.
     *
     * **Note:** Currently, this function will return `Status::MergeInProgress`
     * if the most recent write to the queried key in this batch is a Merge.
     *
     * The values returned by this function are similar to
     * [`RocksDB.get(ColumnFamilyHandle, ReadOptions, ByteArray)`](#RocksDB.get).
     * If `value` is `null`, then this function will not read any data, but will
     * still ensure that this key cannot be written to by outside of this
     * transaction.
     *
     * If this transaction was created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * [`commit()`](#commit) could fail. Otherwise, it could return any error
     * that could be returned by
     * [`RocksDB.get(ColumnFamilyHandle, ReadOptions, ByteArray)`](#RocksDB.get).
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     * - `StatusCode.MergeInProgress` if merge operations cannot be
     *   resolved.
     *
     * @param readOptions Read options.
     * @param columnFamilyHandle [`ColumnFamilyHandle`](#ColumnFamilyHandle) instance
     * @param key the key to retrieve the value for.
     * @param exclusive `true` if the transaction should have exclusive access to
     *     the key, otherwise `false` for shared access.
     * @param doValidate `true` if it should validate the snapshot before doing the read
     *
     * @return A byte array storing the value associated with the input key if
     *     any, or `null` if it does not find the specified key.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        exclusive: Boolean,
        doValidate: Boolean
    ): ByteArray?

    /**
     * Same as [`getForUpdate`](#getForUpdate) with `doValidate=true`.
     *
     * @param readOptions Read options.
     * @param columnFamilyHandle [`ColumnFamilyHandle`](#ColumnFamilyHandle) instance
     * @param key the key to retrieve the value for.
     * @param exclusive `true` if the transaction should have exclusive access to
     *     the key, otherwise `false` for shared access.
     *
     * @return A byte array storing the value associated with the input key if
     *     any, or `null` if it does not find the specified key.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        exclusive: Boolean
    ): ByteArray?

    /**
     * Reads this key and ensures that this transaction will only
     * be able to be committed if this key is not written outside this
     * transaction after it has first been read (or after the snapshot if a
     * snapshot is set in this transaction). The transaction behavior is the
     * same regardless of whether the key exists or not.
     *
     * **Note:** Currently, this function will return `Status::MergeInProgress`
     * if the most recent write to the queried key in this batch is a Merge.
     *
     * The values returned by this function are similar to
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     * If `value` is `null`, then this function will not read any data, but will
     * still ensure that this key cannot be written to by outside of this
     * transaction.
     *
     * If this transaction was created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * [`commit()`](#commit) could fail. Otherwise, it could return any error
     * that could be returned by
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param readOptions Read options.
     * @param key the key to retrieve the value for.
     * @param exclusive `true` if the transaction should have exclusive access to
     *     the key, otherwise `false` for shared access.
     *
     * @return A byte array storing the value associated with the input key if
     *     any, or `null` if it does not find the specified key.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun getForUpdate(
        readOptions: ReadOptions,
        key: ByteArray,
        exclusive: Boolean
    ): ByteArray?

    /**
     * Reads this key and ensures that this transaction will only
     * be able to be committed if this key is not written outside this
     * transaction after it has first been read (or after the snapshot if a
     * snapshot is set in this transaction). The transaction behavior is the
     * same regardless of whether the key exists or not.
     *
     * **Note:** Currently, this function will return `Status::MergeInProgress`
     * if the most recent write to the queried key in this batch is a Merge.
     *
     * The values returned by this function are similar to
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     * If `value` is `null`, then this function will not read any data, but will
     * still ensure that this key cannot be written to by outside of this
     * transaction.
     *
     * If this transaction was created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * [`commit()`](#commit) could fail. Otherwise, it could return any error
     * that could be returned by
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param readOptions Read options.
     * @param columnFamilyHandle the column family in which to find the key/value
     * @param key the key to retrieve the value for.
     * @param value the value associated with the input key if
     *     any. The result is undefined if no value is associated with the key
     * @param exclusive `true` if the transaction should have exclusive access to
     *     the key, otherwise `false` for shared access.
     *
     * @return A [`GetStatus`](#GetStatus) containing
     * - `Status.OK` if the requested value was read
     * - `Status.NotFound` if the requested value does not exist
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray,
        exclusive: Boolean
    ): GetStatus

    /**
     * Reads this key and ensures that this transaction will only
     * be able to be committed if this key is not written outside this
     * transaction after it has first been read (or after the snapshot if a
     * snapshot is set in this transaction). The transaction behavior is the
     * same regardless of whether the key exists or not.
     *
     * **Note:** Currently, this function will return `Status::MergeInProgress`
     * if the most recent write to the queried key in this batch is a Merge.
     *
     * The values returned by this function are similar to
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     * If `value` is `null`, then this function will not read any data, but will
     * still ensure that this key cannot be written to by outside of this
     * transaction.
     *
     * If this transaction was created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * [`commit()`](#commit) could fail. Otherwise, it could return any error
     * that could be returned by
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param readOptions Read options.
     * @param columnFamilyHandle the column family in which to find the key/value
     * @param key the key to retrieve the value for.
     * @param value the value associated with the input key if
     *     any. The result is undefined if no value is associated with the key
     * @param exclusive `true` if the transaction should have exclusive access to
     *     the key, otherwise `false` for shared access.
     * @param doValidate `true` if the transaction should validate the snapshot before doing the read
     *
     * @return A [`GetStatus`](#GetStatus) containing
     * - `Status.OK` if the requested value was read
     * - `Status.NotFound` if the requested value does not exist
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray,
        exclusive: Boolean,
        doValidate: Boolean
    ): GetStatus

    /**
     * Reads this key and ensures that this transaction will only
     * be able to be committed if this key is not written outside this
     * transaction after it has first been read (or after the snapshot if a
     * snapshot is set in this transaction). The transaction behavior is the
     * same regardless of whether the key exists or not.
     *
     * **Note:** Currently, this function will return `Status::MergeInProgress`
     * if the most recent write to the queried key in this batch is a Merge.
     *
     * The values returned by this function are similar to
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     * If `value` is `null`, then this function will not read any data, but will
     * still ensure that this key cannot be written to by outside of this
     * transaction.
     *
     * If this transaction was created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * [`commit()`](#commit) could fail. Otherwise, it could return any error
     * that could be returned by
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param readOptions Read options.
     * @param key the key to retrieve the value for.
     * @param value the value associated with the input key if
     *     any. The result is undefined if no value is associated with the key
     * @param exclusive `true` if the transaction should have exclusive access to
     *     the key, otherwise `false` for shared access.
     *
     * @return A [`GetStatus`](#GetStatus) containing
     * - `Status.OK` if the requested value was read
     * - `Status.NotFound` if the requested value does not exist
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun getForUpdate(
        readOptions: ReadOptions,
        key: ByteArray,
        value: ByteArray,
        exclusive: Boolean
    ): GetStatus

    /**
     * Reads this key and ensures that this transaction will only
     * be able to be committed if this key is not written outside this
     * transaction after it has first been read (or after the snapshot if a
     * snapshot is set in this transaction). The transaction behavior is the
     * same regardless of whether the key exists or not.
     *
     * **Note:** Currently, this function will return `Status::MergeInProgress`
     * if the most recent write to the queried key in this batch is a Merge.
     *
     * The values returned by this function are similar to
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     * If `value` is `null`, then this function will not read any data, but will
     * still ensure that this key cannot be written to by outside of this
     * transaction.
     *
     * If this transaction was created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * [`commit()`](#commit) could fail. Otherwise, it could return any error
     * that could be returned by
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param readOptions Read options.
     * @param columnFamilyHandle the column family in which to find the key/value
     * @param key the key to retrieve the value for.
     * @param value the value associated with the input key if
     *     any. The result is undefined if no value is associated with the key
     * @param exclusive `true` if the transaction should have exclusive access to
     *     the key, otherwise `false` for shared access.
     * @param doValidate `true` if the transaction should validate the snapshot before doing the read
     *
     * @return A [`GetStatus`](#GetStatus) containing
     * - `Status.OK` if the requested value was read
     * - `Status.NotFound` if the requested value does not exist
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer,
        exclusive: Boolean,
        doValidate: Boolean
    ): GetStatus

    /**
     * Reads this key and ensures that this transaction will only
     * be able to be committed if this key is not written outside this
     * transaction after it has first been read (or after the snapshot if a
     * snapshot is set in this transaction). The transaction behavior is the
     * same regardless of whether the key exists or not.
     *
     * **Note:** Currently, this function will return `Status::MergeInProgress`
     * if the most recent write to the queried key in this batch is a Merge.
     *
     * The values returned by this function are similar to
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     * If `value` is `null`, then this function will not read any data, but will
     * still ensure that this key cannot be written to by outside of this
     * transaction.
     *
     * If this transaction was created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * [`commit()`](#commit) could fail. Otherwise, it could return any error
     * that could be returned by
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param readOptions Read options.
     * @param columnFamilyHandle the column family in which to find the key/value
     * @param key the key to retrieve the value for.
     * @param value the value associated with the input key if
     *     any. The result is undefined if no value is associated with the key
     * @param exclusive `true` if the transaction should have exclusive access to
     *     the key, otherwise `false` for shared access.
     *
     * @return A [`GetStatus`](#GetStatus) containing
     * - `Status.OK` if the requested value was read
     * - `Status.NotFound` if the requested value does not exist
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer,
        exclusive: Boolean
    ): GetStatus

    /**
     * Reads this key and ensures that this transaction will only
     * be able to be committed if this key is not written outside this
     * transaction after it has first been read (or after the snapshot if a
     * snapshot is set in this transaction). The transaction behavior is the
     * same regardless of whether the key exists or not.
     *
     * **Note:** Currently, this function will return `Status::MergeInProgress`
     * if the most recent write to the queried key in this batch is a Merge.
     *
     * The values returned by this function are similar to
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     * If `value` is `null`, then this function will not read any data, but will
     * still ensure that this key cannot be written to by outside of this
     * transaction.
     *
     * If this transaction was created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * [`commit()`](#commit) could fail. Otherwise, it could return any error
     * that could be returned by
     * [`RocksDB.get(ReadOptions, ByteArray)`](#RocksDB.get).
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param readOptions Read options.
     * @param key the key to retrieve the value for.
     * @param value the value associated with the input key if
     *     any. The result is undefined if no value is associated with the key
     * @param exclusive `true` if the transaction should have exclusive access to
     *     the key, otherwise `false` for shared access.
     *
     * @return A [`GetStatus`](#GetStatus) containing
     * - `Status.OK` if the requested value was read
     * - `Status.NotFound` if the requested value does not exist
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun getForUpdate(
        readOptions: ReadOptions,
        key: ByteBuffer,
        value: ByteBuffer,
        exclusive: Boolean
    ): GetStatus

    /**
     * A multi-key version of
     * [`getForUpdate`](#getForUpdate).
     *
     * @param readOptions Read options.
     * @param columnFamilyHandles A list containing
     *     [`ColumnFamilyHandle`](#ColumnFamilyHandle) instances.
     * @param keys The keys for which values need to be retrieved.
     *
     * @return A list of values, one for each key.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun multiGetForUpdateAsList(
        readOptions: ReadOptions,
        columnFamilyHandles: List<ColumnFamilyHandle>,
        keys: List<ByteArray>
    ): List<ByteArray?>

    /**
     * A multi-key version of
     * [`getForUpdate`](#getForUpdate).
     *
     * @param readOptions Read options.
     * @param keys The keys for which values need to be retrieved.
     *
     * @return A list of values, one for each key.
     *
     * @throws RocksDBException thrown if an error occurs in the underlying
     *    native library.
     */
    fun multiGetForUpdateAsList(
        readOptions: ReadOptions,
        keys: List<ByteArray>
    ): List<ByteArray?>

    /**
     * Returns an iterator that will iterate over all keys in the default
     * column family including both keys in the DB and uncommitted keys in this
     * transaction.
     *
     * The caller is responsible for deleting the returned `Iterator`.
     *
     * The returned iterator is only valid until [`commit()`](#commit),
     * [`rollback()`](#rollback), or [`rollbackToSavePoint()`](#rollbackToSavePoint) is called.
     *
     * @return An instance of `RocksIterator`.
     */
    fun getIterator(): RocksIterator

    /**
     * Returns an iterator that will iterate over all keys in the default
     * column family including both keys in the DB and uncommitted keys in this
     * transaction.
     *
     * Setting [`ReadOptions.setSnapshot`](#ReadOptions.setSnapshot) will affect what is read
     * from the DB but will **not** change which keys are read from this transaction
     * (the keys in this transaction do not yet belong to any snapshot and will be
     * fetched regardless).
     *
     * The caller is responsible for deleting the returned `Iterator`.
     *
     * The returned iterator is only valid until [`commit()`](#commit),
     * [`rollback()`](#rollback), or [`rollbackToSavePoint()`](#rollbackToSavePoint) is called.
     *
     * @param readOptions Read options.
     *
     * @return An instance of `RocksIterator`.
     */
    fun getIterator(readOptions: ReadOptions): RocksIterator

    /**
     * Returns an iterator that will iterate over all keys in the specified
     * column family including both keys in the DB and uncommitted keys in this
     * transaction.
     *
     * Setting [`ReadOptions.setSnapshot`](#ReadOptions.setSnapshot) will affect what is read
     * from the DB but will **not** change which keys are read from this transaction
     * (the keys in this transaction do not yet belong to any snapshot and will be
     * fetched regardless).
     *
     * The caller is responsible for calling [`RocksIterator.close()`](#RocksIterator.close) on
     * the returned `Iterator`.
     *
     * The returned iterator is only valid until [`commit()`](#commit),
     * [`rollback()`](#rollback), or [`rollbackToSavePoint()`](#rollbackToSavePoint) is called.
     *
     * @param readOptions Read options.
     * @param columnFamilyHandle [`ColumnFamilyHandle`](#ColumnFamilyHandle) instance
     *
     * @return An instance of `RocksIterator`.
     */
    fun getIterator(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle
    ): RocksIterator

    /**
     * Returns an iterator that will iterate over all keys in the specified
     * column family including both keys in the DB and uncommitted keys in this
     * transaction.
     *
     * Setting [`ReadOptions.setSnapshot`](#ReadOptions.setSnapshot) will affect what is read
     * from the DB but will **not** change which keys are read from this transaction
     * (the keys in this transaction do not yet belong to any snapshot and will be
     * fetched regardless).
     *
     * The caller is responsible for calling [`RocksIterator.close()`](#RocksIterator.close) on
     * the returned `Iterator`.
     *
     * The returned iterator is only valid until [`commit()`](#commit),
     * [`rollback()`](#rollback), or [`rollbackToSavePoint()`](#rollbackToSavePoint) is called.
     *
     * @param columnFamilyHandle [`ColumnFamilyHandle`](#ColumnFamilyHandle) instance
     *
     * @return An instance of `RocksIterator`.
     */
    fun getIterator(
        columnFamilyHandle: ColumnFamilyHandle
    ): RocksIterator

    /**
     * Similar to [`RocksDB.put(ColumnFamilyHandle, ByteArray, ByteArray)`](#RocksDB.put), but
     * will also perform conflict checking on the keys to be written.
     *
     * If this transaction was created on an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * these functions should always succeed.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param columnFamilyHandle The column family to put the key/value into
     * @param key The specified key to be inserted.
     * @param value The value associated with the specified key.
     * @param assumeTracked `true` when it is expected that the key is already
     *     tracked. More specifically, it means the key was previously tracked
     *     in the same savepoint, with the same exclusive flag, and at a lower
     *     sequence number. If valid, it skips snapshot validation; throws an error otherwise.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray,
        assumeTracked: Boolean
    )

    /**
     * Similar to [`put`](#put) but with `assumeTracked = false`.
     *
     * Will also perform conflict checking on the keys to be written.
     *
     * If this transaction was created on an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * these functions should always succeed.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param columnFamilyHandle The column family to put the key/value into
     * @param key The specified key to be inserted.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray
    )

    /**
     * Similar to [`RocksDB.put(ByteArray, ByteArray)`](#RocksDB.put), but
     * will also perform conflict checking on the keys to be written.
     *
     * If this transaction was created on an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * these functions should always succeed.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param key The specified key to be inserted.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun put(
        key: ByteArray,
        value: ByteArray
    )

    /**
     * Similar to [`put`](#put) but allows
     * you to specify the key and value in several parts that will be
     * concatenated together.
     *
     * @param columnFamilyHandle The column family to put the key/value into
     * @param keyParts The specified key to be inserted, split into parts.
     * @param valueParts The value associated with the specified key, split into parts.
     * @param assumeTracked `true` when it is expected that the key is already
     *     tracked. More specifically, it means the key was previously tracked
     *     in the same savepoint, with the same exclusive flag, and at a lower
     *     sequence number. If valid, it skips snapshot validation; throws an error otherwise.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        keyParts: Array<ByteArray>,
        valueParts: Array<ByteArray>,
        assumeTracked: Boolean
    )

    /**
     * Similar to [`put`](#put) but with `assumeTracked = false`.
     *
     * Allows you to specify the key and value in several parts that will be
     * concatenated together.
     *
     * @param columnFamilyHandle The column family to put the key/value into
     * @param keyParts The specified key to be inserted, split into parts.
     * @param valueParts The value associated with the specified key, split into parts.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        keyParts: Array<ByteArray>,
        valueParts: Array<ByteArray>
    )

    /**
     * Similar to [`RocksDB.put(ByteArray, ByteArray)`](#RocksDB.put) but allows
     * you to specify the key and value in several parts that will be
     * concatenated together.
     *
     * @param keyParts The specified key to be inserted, split into parts.
     * @param valueParts The value associated with the specified key, split into parts.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun put(
        keyParts: Array<ByteArray>,
        valueParts: Array<ByteArray>
    )

    /**
     * Similar to `RocksDB.put(byte[], byte[])`, but
     * will also perform conflict checking on the keys being written.
     *
     * If this transaction was created on an `OptimisticTransactionDB`,
     * these functions should always succeed.
     *
     * If this transaction was created on a `TransactionDB`, a `RocksDBException`
     * may be thrown with an accompanying `Status` when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See `ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`
     *
     * @param key the specified key to be inserted.
     * @param value the value associated with the specified key.
     *
     * @throws RocksDBException when one of the `TransactionDB` conditions
     * described above occurs, or in the case of an unexpected error.
     */
    fun put(key: ByteBuffer, value: ByteBuffer)

    /**
     * Similar to `RocksDB.put(byte[], byte[])`, but
     * will also perform conflict checking on the keys being written.
     *
     * If this transaction was created on an `OptimisticTransactionDB`,
     * these functions should always succeed.
     *
     * If this transaction was created on a `TransactionDB`, a `RocksDBException`
     * may be thrown with an accompanying `Status` when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See `ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`
     *
     * @param columnFamilyHandle the column family to put the key/value into.
     * @param key the specified key to be inserted.
     * @param value the value associated with the specified key.
     * @param assumeTracked true when it is expected that the key is already
     * tracked. If valid, skips snapshot validation; throws an error otherwise.
     *
     * @throws RocksDBException when one of the `TransactionDB` conditions
     * described above occurs, or in the case of an unexpected error.
     */
    fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer,
        assumeTracked: Boolean
    )

    /**
     * Similar to `put(columnFamilyHandle, key, value, assumeTracked)` but assumes
     * `assumeTracked = false`.
     *
     * @param columnFamilyHandle the column family to put the key/value into.
     * @param key the specified key to be inserted.
     * @param value the value associated with the specified key.
     *
     * @throws RocksDBException when one of the `TransactionDB` conditions
     * described above occurs, or in the case of an unexpected error.
     */
    fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer
    )

    /**
     * Similar to [`RocksDB.merge(ColumnFamilyHandle, ByteArray, ByteArray)`](#RocksDB.merge), but
     * will also perform conflict checking on the keys to be written.
     *
     * If this transaction was created on an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * these functions should always succeed.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param columnFamilyHandle The column family to merge the key/value into
     * @param key The specified key to be merged.
     * @param value The value associated with the specified key.
     * @param assumeTracked `true` when it is expected that the key is already
     *     tracked. More specifically, it means the key was previously tracked
     *     in the same savepoint, with the same exclusive flag, and at a lower
     *     sequence number. If valid, it skips snapshot validation; throws an error otherwise.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray,
        assumeTracked: Boolean
    )

    /**
     * Similar to [`merge`](#merge) but with `assumeTracked = false`.
     *
     * Will also perform conflict checking on the keys to be written.
     *
     * If this transaction was created on an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * these functions should always succeed.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param columnFamilyHandle The column family to merge the key/value into
     * @param key The specified key to be merged.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray
    )

    /**
     * Similar to `RocksDB.merge(byte[], byte[])`, but
     * will also perform conflict checking on the keys being written.
     *
     * If this transaction was created on an `OptimisticTransactionDB`,
     * these functions should always succeed.
     *
     * If this transaction was created on a `TransactionDB`, a `RocksDBException`
     * may be thrown with an accompanying `Status` when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See `ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`
     *
     * @param key The specified key to be merged.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException when one of the `TransactionDB` conditions
     * described above occurs, or in the case of an unexpected error.
     */
    fun merge(key: ByteArray, value: ByteArray)

    /**
     * Similar to `RocksDB.merge(byte[], byte[])`, but
     * will also perform conflict checking on the keys being written.
     *
     * If this transaction was created on an `OptimisticTransactionDB`,
     * these functions should always succeed.
     *
     * If this transaction was created on a `TransactionDB`, a `RocksDBException`
     * may be thrown with an accompanying `Status` when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See `ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`
     *
     * @param key The specified key to be merged.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException when one of the `TransactionDB` conditions
     * described above occurs, or in the case of an unexpected error.
     */
    fun merge(key: ByteBuffer, value: ByteBuffer)

    /**
     * Similar to `RocksDB.merge(byte[], byte[])`, but
     * will also perform conflict checking on the keys being written.
     *
     * If this transaction was created on an `OptimisticTransactionDB`,
     * these functions should always succeed.
     *
     * If this transaction was created on a `TransactionDB`, a `RocksDBException`
     * may be thrown with an accompanying `Status` when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See `ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`
     *
     * @param columnFamilyHandle The column family in which to apply the merge.
     * @param key The specified key to be merged.
     * @param value The value associated with the specified key.
     * @param assumeTracked Expects the key to already be tracked.
     *
     * @throws RocksDBException when one of the `TransactionDB` conditions
     * described above occurs, or in the case of an unexpected error.
     */
    fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer,
        assumeTracked: Boolean
    )

    /**
     * Similar to `merge(columnFamilyHandle, key, value, assumeTracked)` but assumes
     * `assumeTracked = false`.
     *
     * @param columnFamilyHandle The column family in which to apply the merge.
     * @param key The specified key to be merged.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException when one of the `TransactionDB` conditions
     * described above occurs, or in the case of an unexpected error.
     */
    fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer
    )

    /**
     * Similar to [`RocksDB.delete(ColumnFamilyHandle, ByteArray)`](#RocksDB.delete), but
     * will also perform conflict checking on the keys to be deleted.
     *
     * If this transaction was created on an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * these functions should always succeed.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param columnFamilyHandle The column family to delete the key from
     * @param key The specified key to be deleted.
     * @param assumeTracked `true` when it is expected that the key is already
     *     tracked. More specifically, it means the key was previously tracked
     *     in the same savepoint, with the same exclusive flag, and at a lower
     *     sequence number. If valid, it skips snapshot validation; throws an error otherwise.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun delete(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        assumeTracked: Boolean
    )

    /**
     * Similar to [`delete`](#delete) but with `assumeTracked = false`.
     *
     * Will also perform conflict checking on the keys to be deleted.
     *
     * If this transaction was created on an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * these functions should always succeed.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param columnFamilyHandle The column family to delete the key from
     * @param key The specified key to be deleted.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun delete(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray
    )

    /**
     * Similar to [`delete`](#delete) but with `assumeTracked = false`.
     *
     * Will also perform conflict checking on the keys to be deleted.
     *
     * If this transaction was created on an [`OptimisticTransactionDB`](#OptimisticTransactionDB),
     * these functions should always succeed.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), an
     * [`RocksDBException`](#RocksDBException) may be thrown with an accompanying [`Status`](#Status)
     * when:
     * - `StatusCode.Busy` if there is a write conflict,
     * - `StatusCode.TimedOut` if a lock could not be acquired,
     * - `StatusCode.TryAgain` if the memtable history size is not large
     *   enough. See
     *   [`ColumnFamilyOptions.maxWriteBufferNumberToMaintain()`](#ColumnFamilyOptions.maxWriteBufferNumberToMaintain)
     *
     * @param key The specified key to be deleted.
     *
     * @throws RocksDBException when one of the TransactionDB conditions
     *     described above occurs, or in the case of an unexpected error
     */
    fun delete(
        key: ByteArray
    )

    /**
     * Similar to [`put(ColumnFamilyHandle, ByteArray, ByteArray)`](#put), but operates on the transaction's write batch. This write will only happen
     * if this transaction gets committed successfully.
     *
     * Unlike [`put(ColumnFamilyHandle, ByteArray, ByteArray)`](#put), no conflict
     * checking will be performed for this key.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), this function
     * will still acquire locks necessary to ensure this write doesn't cause
     * conflicts in other transactions. This may throw a [`RocksDBException`](#RocksDBException)
     * with an associated [`StatusCode.Busy`](#StatusCode.Busy).
     *
     * @param columnFamilyHandle The column family to put the key/value into
     * @param key The specified key to be inserted.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException When one of the TransactionalDB conditions
     *     described above occurs, or in the case of an unexpected error.
     */
    fun putUntracked(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray
    )

    /**
     * Similar to [`put(ByteArray, ByteArray)`](#put), but operates on the transaction's write batch. This write will only happen
     * if this transaction gets committed successfully.
     *
     * Unlike [`put(ByteArray, ByteArray)`](#put), no conflict
     * checking will be performed for this key.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), this function
     * will still acquire locks necessary to ensure this write doesn't cause
     * conflicts in other transactions. This may throw a [`RocksDBException`](#RocksDBException)
     * with an associated [`StatusCode.Busy`](#StatusCode.Busy).
     *
     * @param key The specified key to be inserted.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException When one of the TransactionalDB conditions
     *     described above occurs, or in the case of an unexpected error.
     */
    fun putUntracked(
        key: ByteArray,
        value: ByteArray
    )

    /**
     * Similar to [`put(ColumnFamilyHandle, Array<ByteArray>, Array<ByteArray>, boolean)`](#put), but with `assumeTracked = false`.
     *
     * Allows you to specify the key and value in several parts that will be
     * concatenated together.
     *
     * @param columnFamilyHandle The column family to put the key/value into
     * @param keyParts The specified key to be inserted, split into parts.
     * @param valueParts The value associated with the specified key, split into parts.
     *
     * @throws RocksDBException When one of the TransactionalDB conditions
     *     described above occurs, or in the case of an unexpected error.
     */
    fun putUntracked(
        columnFamilyHandle: ColumnFamilyHandle,
        keyParts: Array<ByteArray>,
        valueParts: Array<ByteArray>
    )

    /**
     * Similar to [`put(ByteArray, ByteArray)`](#put), but allows you to specify the key and value in several parts that will be
     * concatenated together.
     *
     * @param keyParts The specified key to be inserted, split into parts.
     * @param valueParts The value associated with the specified key, split into parts.
     *
     * @throws RocksDBException When one of the TransactionalDB conditions
     *     described above occurs, or in the case of an unexpected error.
     */
    fun putUntracked(
        keyParts: Array<ByteArray>,
        valueParts: Array<ByteArray>
    )

    /**
     * Similar to [`merge(ColumnFamilyHandle, ByteArray, ByteArray)`](#merge), but operates on the transaction's write batch. This merge will only happen
     * if this transaction gets committed successfully.
     *
     * Unlike [`merge(ColumnFamilyHandle, ByteArray, ByteArray)`](#merge), no conflict
     * checking will be performed for this key.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), this function
     * will still acquire locks necessary to ensure this merge doesn't cause
     * conflicts in other transactions. This may throw a [`RocksDBException`](#RocksDBException)
     * with an associated [`StatusCode.Busy`](#StatusCode.Busy).
     *
     * @param columnFamilyHandle The column family to merge the key/value into
     * @param key The specified key to be merged.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException When one of the TransactionalDB conditions
     *     described above occurs, or in the case of an unexpected error.
     */
    fun mergeUntracked(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray
    )

    /**
     * Similar to [`merge(ByteArray, ByteArray)`](#merge), but operates on the transaction's write batch. This merge will only happen
     * if this transaction gets committed successfully.
     *
     * Unlike [`merge(ByteArray, ByteArray)`](#merge), no conflict
     * checking will be performed for this key.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), this function
     * will still acquire locks necessary to ensure this merge doesn't cause
     * conflicts in other transactions. This may throw a [`RocksDBException`](#RocksDBException)
     * with an associated [`StatusCode.Busy`](#StatusCode.Busy).
     *
     * @param key The specified key to be merged.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException When one of the TransactionalDB conditions
     *     described above occurs, or in the case of an unexpected error.
     */
    fun mergeUntracked(
        key: ByteArray,
        value: ByteArray
    )

    /**
     * Similar to [`merge(ColumnFamilyHandle, ByteBuffer, ByteBuffer)`](#merge), but operates on the transaction's write batch. This merge will only happen
     * if this transaction gets committed successfully.
     *
     * Unlike [`merge(ColumnFamilyHandle, ByteBuffer, ByteBuffer)`](#merge), no conflict
     * checking will be performed for this key.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), this function
     * will still acquire locks necessary to ensure this merge doesn't cause
     * conflicts in other transactions. This may throw a [`RocksDBException`](#RocksDBException)
     * with an associated [`StatusCode.Busy`](#StatusCode.Busy).
     *
     * @param columnFamilyHandle The column family to merge the key/ value into
     * @param key The specified key to be merged.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException When one of the TransactionalDB conditions
     *     described above occurs, or in the case of an unexpected error.
     */
    fun mergeUntracked(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer
    )

    /**
     * Similar to [`merge(ByteBuffer, ByteBuffer)`](#merge), but operates on the transaction's write batch. This merge will only happen
     * if this transaction gets committed successfully.
     *
     * Unlike [`merge(ByteBuffer, ByteBuffer)`](#merge), no conflict
     * checking will be performed for this key.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), this function
     * will still acquire locks necessary to ensure this merge doesn't cause
     * conflicts in other transactions. This may throw a [`RocksDBException`](#RocksDBException)
     * with an associated [`StatusCode.Busy`](#StatusCode.Busy).
     *
     * @param key The specified key to be merged.
     * @param value The value associated with the specified key.
     *
     * @throws RocksDBException When one of the TransactionalDB conditions
     *     described above occurs, or in the case of an unexpected error.
     */
    fun mergeUntracked(
        key: ByteBuffer,
        value: ByteBuffer
    )

    /**
     * Similar to [`RocksDB.delete(ColumnFamilyHandle, ByteArray)`](#delete), but operates on the transaction's write batch.
     * This delete will only happen if this transaction gets committed successfully.
     *
     * Unlike [`delete(ColumnFamilyHandle, ByteArray)`](#delete), no conflict
     * checking will be performed for this key.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), this function
     * will still acquire locks necessary to ensure this delete doesn't cause
     * conflicts in other transactions. This may throw a [`RocksDBException`](#RocksDBException)
     * with an associated [`StatusCode.Busy`](#StatusCode.Busy).
     *
     * @param columnFamilyHandle The column family to delete the key/value from.
     * @param key The specified key to be deleted.
     *
     * @throws RocksDBException When one of the TransactionalDB conditions
     *     described above occurs, or in the case of an unexpected error.
     */
    fun deleteUntracked(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray
    )

    /**
     * Similar to [`RocksDB.delete(ByteArray)`](#delete), but operates on the transaction's write batch.
     * This delete will only happen if this transaction gets committed successfully.
     *
     * Unlike [`delete(ByteArray)`](#delete), no conflict
     * checking will be performed for this key.
     *
     * If this transaction was created on a [`TransactionDB`](#TransactionDB), this function
     * will still acquire locks necessary to ensure this delete doesn't cause
     * conflicts in other transactions. This may throw a [`RocksDBException`](#RocksDBException)
     * with an associated [`StatusCode.Busy`](#StatusCode.Busy).
     *
     * @param key The specified key to be deleted.
     *
     * @throws RocksDBException When one of the TransactionalDB conditions
     *     described above occurs, or in the case of an unexpected error.
     */
    fun deleteUntracked(
        key: ByteArray
    )

    /**
     * Adds log data to the transaction.
     *
     * @param logData The data to add to the transaction log.
     *
     * @throws RocksDBException Thrown if an error occurs in the underlying native library.
     */
    fun putLogData(logData: ByteArray)

    /**
     * Disables indexing for all future put/merge/delete operations in this transaction.
     * This can be used as a performance optimization if the caller does not need to fetch the keys about to be written.
     *
     * Indexing will remain disabled until [`enableIndexing()`](#enableIndexing) is called.
     */
    fun disableIndexing()

    /**
     * Re-enables indexing after a previous call to [`disableIndexing()`](#disableIndexing).
     */
    fun enableIndexing()

    /**
     * Returns the number of distinct keys being tracked by this transaction.
     * - If created by a [`TransactionDB`](#TransactionDB), this is the number of keys currently locked by this transaction.
     * - If created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB), this is the number of keys that need to be checked for conflicts at commit time.
     *
     * @return The number of distinct keys being tracked by this transaction.
     */
    fun getNumKeys(): Long

    /**
     * Returns the number of put operations that have been applied to this transaction so far.
     *
     * @return The number of put operations applied.
     */
    fun getNumPuts(): Long

    /**
     * Returns the number of delete operations that have been applied to this transaction so far.
     *
     * @return The number of delete operations applied.
     */
    fun getNumDeletes(): Long

    /**
     * Returns the number of merge operations that have been applied to this transaction so far.
     *
     * @return The number of merge operations applied.
     */
    fun getNumMerges(): Long

    /**
     * Returns the elapsed time in milliseconds since this transaction began.
     *
     * @return The elapsed time in milliseconds.
     */
    fun getElapsedTime(): Long

    /**
     * Fetches the underlying write batch that contains all pending changes to be committed.
     *
     * **Note:** You should not write or delete anything from the batch directly. Use the functions in the [`Transaction`](#Transaction) class to modify the transaction.
     *
     * @return The write batch.
     */
    fun getWriteBatch(): WriteBatchWithIndex

    /**
     * Changes the lock timeout value (in milliseconds) for this transaction.
     * This has no effect on optimistic transactions.
     *
     * @param lockTimeout The timeout (in milliseconds) for locks used by this transaction.
     */
    fun setLockTimeout(lockTimeout: Long)

    /**
     * Returns the [`WriteOptions`](#WriteOptions) that will be used during [`commit()`](#commit).
     *
     * @return The current write options.
     */
    fun getWriteOptions(): WriteOptions

    /**
     * Sets the [`WriteOptions`](#WriteOptions) that will be used during [`commit()`](#commit).
     *
     * @param writeOptions The new write options.
     */
    fun setWriteOptions(writeOptions: WriteOptions)

    /**
     * If this key was previously fetched in this transaction using
     * [`getForUpdate(ReadOptions, ColumnFamilyHandle, ByteArray, Boolean)`](#getForUpdate) or
     * [`multiGetForUpdate(ReadOptions, List, Array<ByteArray>)`](#multiGetForUpdate),
     * calling this function will tell the transaction that it no longer needs to perform conflict checking for this key.
     *
     * - If a key has been fetched N times, this function must be called N times to fully undo the fetch.
     * - If the key has been written to in this transaction, this function has no effect.
     * - If [`setSavePoint()`](#setSavePoint) has been called after fetching the key, this function has no effect.
     *
     * For transactions created by a [`TransactionDB`](#TransactionDB):
     * - This may release any held locks for this key.
     *
     * For transactions created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB):
     * - This can affect whether the key is conflict-checked at commit time.
     *
     * @param columnFamilyHandle The column family handle.
     * @param key The key for which to undo the get-for-update.
     */
    fun undoGetForUpdate(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray
    )

    /**
     * If this key was previously fetched in this transaction using
     * [`getForUpdate(ReadOptions, ByteArray, Boolean)`](#getForUpdate) or
     * [`multiGetForUpdate(ReadOptions, List, Array<ByteArray>)`](#multiGetForUpdate),
     * calling this function will tell the transaction that it no longer needs to perform conflict checking for this key.
     *
     * - If a key has been fetched N times, this function must be called N times to fully undo the fetch.
     * - If the key has been written to in this transaction, this function has no effect.
     * - If [`setSavePoint()`](#setSavePoint) has been called after fetching the key, this function has no effect.
     *
     * For transactions created by a [`TransactionDB`](#TransactionDB):
     * - This may release any held locks for this key.
     *
     * For transactions created by an [`OptimisticTransactionDB`](#OptimisticTransactionDB):
     * - This can affect whether the key is conflict-checked at commit time.
     *
     * @param key The key for which to undo the get-for-update.
     */
    fun undoGetForUpdate(
        key: ByteArray
    )

    /**
     * Adds the keys from the provided [`WriteBatch`](#WriteBatch) to this transaction.
     *
     * @param writeBatch The write batch to read from.
     *
     * @throws RocksDBException If an error occurs while rebuilding from the write batch.
     */
    fun rebuildFromWriteBatch(writeBatch: WriteBatch)

    /**
     * Retrieves the commit-time write batch.
     *
     * @return The commit-time write batch.
     */
    fun getCommitTimeWriteBatch(): WriteBatch

    /**
     * Sets the log number for this transaction.
     *
     * @param logNumber The log number to set.
     */
    fun setLogNumber(logNumber: Long)

    /**
     * Retrieves the log number for this transaction.
     *
     * @return The log number.
     */
    fun getLogNumber(): Long

    /**
     * Sets the name of the transaction.
     *
     * @param transactionName The name to assign to this transaction.
     *
     * @throws RocksDBException If an error occurs when setting the transaction name.
     */
    fun setName(transactionName: String)

    /**
     * Retrieves the name of the transaction.
     *
     * @return The name of the transaction.
     */
    fun getName(): String

    /**
     * Retrieves the ID of the transaction.
     *
     * @return The transaction ID.
     */
    fun getID(): Long

    /**
     * The globally unique id with which the transaction is identified. This id
     * might or might not be set depending on the implementation. Similarly the
     * implementation decides the point in lifetime of a transaction at which it
     * assigns the id. Although currently it is the case, the id is not guaranteed
     * to remain the same across restarts.
     *
     * EXPERIMENTAL
     *
     * @return the transaction id.
     */
    fun getId(): Long

    /**
     * Determines if a deadlock has been detected for this transaction.
     *
     * @return `true` if a deadlock has been detected; otherwise, `false`.
     */
    fun isDeadlockDetect(): Boolean

    /**
     * Retrieves the list of transactions that are waiting.
     *
     * @return The list of waiting transactions.
     */
    fun getWaitingTxns(): WaitingTransactions

    /**
     * Retrieves the execution status of the transaction.
     *
     * **Note:** The execution status of an Optimistic Transaction never changes.
     * This is only useful for non-optimistic transactions.
     *
     * @return The execution status of the transaction.
     */
    fun getState(): TransactionState
}
