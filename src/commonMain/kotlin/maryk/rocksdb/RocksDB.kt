package maryk.rocksdb

expect val defaultColumnFamily: ByteArray
expect val rocksDBNotFound: Int

/**
 * A RocksDB is a persistent ordered map from keys to values.  It is safe for
 * concurrent access from multiple threads without any external synchronization.
 * All methods of this class could potentially throw RocksDBException, which
 * indicates something wrong at the RocksDB library side and the call failed.
 */
expect open class RocksDB : RocksObject {
    /**
     * This is similar to {@link #close()} except that it
     * throws an exception if any error occurs.
     *
     * This will not fsync the WAL files.
     * If syncing is required, the caller must first call {@link #syncWal()}
     * or {@link #write(WriteOptions, WriteBatch)} using an empty write batch
     * with {@link WriteOptions#setSync(boolean)} set to true.
     *
     * See also {@link #close()}.
     *
     * @throws RocksDBException if an error occurs whilst closing.
     */
    fun closeE()

    /**
     * This is similar to {@link #closeE()} except that it
     * silently ignores any errors.
     *
     * This will not fsync the WAL files.
     * If syncing is required, the caller must first call {@link #syncWal()}
     * or {@link #write(WriteOptions, WriteBatch)} using an empty write batch
     * with {@link WriteOptions#setSync(boolean)} set to true.
     *
     * See also {@link #close()}.
     */
    override fun close()

    /**
     * Creates a new column family with the name columnFamilyName and
     * allocates a ColumnFamilyHandle within an internal structure.
     * The ColumnFamilyHandle is automatically disposed with DB disposal.
     *
     * @param columnFamilyDescriptor column family to be created.
     * @return [ColumnFamilyHandle] instance.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun createColumnFamily(
        columnFamilyDescriptor: ColumnFamilyDescriptor
    ): ColumnFamilyHandle

    /**
     * Bulk create column families with the same column family options.
     *
     * @param columnFamilyOptions the options for the column families.
     * @param columnFamilyNames the names of the column families.
     * @return the handles to the newly created column families.
     */
    fun createColumnFamilies(
        columnFamilyOptions: ColumnFamilyOptions,
        columnFamilyNames: List<ByteArray>
    ): List<ColumnFamilyHandle>

    /**
     * Bulk create column families with the same column family options.
     * @param columnFamilyDescriptors the descriptions of the column families.
     * @return the handles to the newly created column families.
     */
    fun createColumnFamilies(
        columnFamilyDescriptors: List<ColumnFamilyDescriptor>
    ): List<ColumnFamilyHandle>

    /**
     * Drops the column family specified by `columnFamilyHandle`. This call
     * only records a drop record in the manifest and prevents the column
     * family from flushing and compacting.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun dropColumnFamily(columnFamilyHandle: ColumnFamilyHandle)

    /**
     * Bulk drop column families. This call only records drop records in the
     * manifest and prevents the column families from flushing and compacting.
     * In case of error, the request may succeed partially. User may call
     * ListColumnFamilies to check the result.
     */
    fun dropColumnFamilies(columnFamilies: List<ColumnFamilyHandle>)

    /**
     * Set the database entry for "key" to "value".
     *
     * @param key the specified key to be inserted.
     * @param value the value associated with the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun put(key: ByteArray, value: ByteArray)

    /**
     * Set the database entry for "key" to "value".
     *
     * @param key The specified key to be inserted
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param value the value associated with the specified key
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, must be
     * non-negative and no larger than ("value".length -  offset)
     *
     * @throws RocksDBException thrown if errors happens in underlying native
     * library.
     * @throws IndexOutOfBoundsException if an offset or length is out of bounds
     */
    fun put(
        key: ByteArray, offset: Int, len: Int,
        value: ByteArray, vOffset: Int, vLen: Int
    )

    /**
     * Set the database entry for "key" to "value" in the specified
     * column family.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param key the specified key to be inserted.
     * @param value the value associated with the specified key.
     *
     * throws IllegalArgumentException if column family is not present
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray, value: ByteArray
    )

    /**
     * Set the database entry for "key" to "value" in the specified
     * column family.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param key The specified key to be inserted
     * @param offset the offset of the "key" array to be used, must
     * be non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param value the value associated with the specified key
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, must be
     * non-negative and no larger than ("value".length - offset)
     *
     * @throws RocksDBException thrown if errors happens in underlying native
     * library.
     * @throws IndexOutOfBoundsException if an offset or length is out of bounds
     */
    fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray, offset: Int, len: Int,
        value: ByteArray, vOffset: Int, vLen: Int
    )

    /**
     * Set the database entry for "key" to "value".
     *
     * @param writeOpts [WriteOptions] instance.
     * @param key the specified key to be inserted.
     * @param value the value associated with the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun put(
        writeOpts: WriteOptions, key: ByteArray,
        value: ByteArray
    )

    /**
     * Set the database entry for "key" to "value".
     *
     * @param writeOpts [WriteOptions] instance.
     * @param key The specified key to be inserted
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param value the value associated with the specified key
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, must be
     * non-negative and no larger than ("value".length -  offset)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     * @throws IndexOutOfBoundsException if an offset or length is out of bounds
     */
    fun put(
        writeOpts: WriteOptions,
        key: ByteArray, offset: Int, len: Int,
        value: ByteArray, vOffset: Int, vLen: Int
    )

    /**
     * Set the database entry for "key" to "value" for the specified
     * column family.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param writeOpts [WriteOptions] instance.
     * @param key the specified key to be inserted.
     * @param value the value associated with the specified key.
     *
     * throws IllegalArgumentException if column family is not present
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     * @see IllegalArgumentException
     */
    fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpts: WriteOptions, key: ByteArray,
        value: ByteArray
    )

    /**
     * Set the database entry for "key" to "value" for the specified
     * column family.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param writeOpts [WriteOptions] instance.
     * @param key The specified key to be inserted
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param value the value associated with the specified key
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, must be
     * non-negative and no larger than ("value".length -  offset)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     * @throws IndexOutOfBoundsException if an offset or length is out of bounds
     */
    fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpts: WriteOptions,
        key: ByteArray, offset: Int, len: Int,
        value: ByteArray, vOffset: Int, vLen: Int
    )

    /**
     * Delete the database entry (if any) for "key".  Returns OK on
     * success, and a non-OK status on error.  It is not an error if "key"
     * did not exist in the database.
     *
     * @param key Key to delete within database
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun delete(key: ByteArray)

    /**
     * Delete the database entry (if any) for "key".  Returns OK on
     * success, and a non-OK status on error.  It is not an error if "key"
     * did not exist in the database.
     *
     * @param key Key to delete within database
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be
     * non-negative and no larger than ("key".length - offset)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun delete(key: ByteArray, offset: Int, len: Int)

    /**
     * Delete the database entry (if any) for "key".  Returns OK on
     * success, and a non-OK status on error.  It is not an error if "key"
     * did not exist in the database.
     *
     * @param columnFamilyHandle [maryk.rocksdb.ColumnFamilyHandle]
     * instance
     * @param key Key to delete within database
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun delete(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray
    )

    /**
     * Delete the database entry (if any) for "key".  Returns OK on
     * success, and a non-OK status on error.  It is not an error if "key"
     * did not exist in the database.
     *
     * @param columnFamilyHandle [maryk.rocksdb.ColumnFamilyHandle]
     * instance
     * @param key Key to delete within database
     * @param offset the offset of the "key" array to be used,
     * must be non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("value".length - offset)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun delete(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray, offset: Int, len: Int
    )

    /**
     * Delete the database entry (if any) for "key".  Returns OK on
     * success, and a non-OK status on error.  It is not an error if "key"
     * did not exist in the database.
     *
     * @param writeOpt WriteOptions to be used with delete operation
     * @param key Key to delete within database
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun delete(writeOpt: WriteOptions, key: ByteArray)

    /**
     * Delete the database entry (if any) for "key".  Returns OK on
     * success, and a non-OK status on error.  It is not an error if "key"
     * did not exist in the database.
     *
     * @param writeOpt WriteOptions to be used with delete operation
     * @param key Key to delete within database
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be
     * non-negative and no larger than ("key".length -  offset)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun delete(
        writeOpt: WriteOptions, key: ByteArray,
        offset: Int, len: Int
    )

    /**
     * Delete the database entry (if any) for "key".  Returns OK on
     * success, and a non-OK status on error.  It is not an error if "key"
     * did not exist in the database.
     *
     * @param columnFamilyHandle [maryk.rocksdb.ColumnFamilyHandle]
     * instance
     * @param writeOpt WriteOptions to be used with delete operation
     * @param key Key to delete within database
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun delete(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpt: WriteOptions, key: ByteArray
    )

    /**
     * Delete the database entry (if any) for "key".  Returns OK on
     * success, and a non-OK status on error.  It is not an error if "key"
     * did not exist in the database.
     *
     * @param columnFamilyHandle [maryk.rocksdb.ColumnFamilyHandle]
     * instance
     * @param writeOpt WriteOptions to be used with delete operation
     * @param key Key to delete within database
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be
     * non-negative and no larger than ("key".length -  offset)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun delete(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpt: WriteOptions, key: ByteArray, offset: Int,
        len: Int
    )

    /**
     * Removes the database entries in the range ["beginKey", "endKey"), i.e.,
     * including "beginKey" and excluding "endKey". a non-OK status on error. It
     * is not an error if no keys exist in the range ["beginKey", "endKey").
     *
     * Delete the database entry (if any) for "key". Returns OK on success, and a
     * non-OK status on error. It is not an error if "key" did not exist in the
     * database.
     *
     * @param beginKey First key to delete within database (inclusive)
     * @param endKey Last key to delete within database (exclusive)
     *
     * @throws RocksDBException thrown if error happens in underlying native
     * library.
     */
    fun deleteRange(beginKey: ByteArray, endKey: ByteArray)

    /**
     * Removes the database entries in the range ["beginKey", "endKey"), i.e.,
     * including "beginKey" and excluding "endKey". a non-OK status on error. It
     * is not an error if no keys exist in the range ["beginKey", "endKey").
     *
     * Delete the database entry (if any) for "key". Returns OK on success, and a
     * non-OK status on error. It is not an error if "key" did not exist in the
     * database.
     *
     * @param columnFamilyHandle [maryk.rocksdb.ColumnFamilyHandle] instance
     * @param beginKey First key to delete within database (inclusive)
     * @param endKey Last key to delete within database (exclusive)
     *
     * @throws RocksDBException thrown if error happens in underlying native
     * library.
     */
    fun deleteRange(
        columnFamilyHandle: ColumnFamilyHandle,
        beginKey: ByteArray, endKey: ByteArray
    )

    /**
     * Removes the database entries in the range ["beginKey", "endKey"), i.e.,
     * including "beginKey" and excluding "endKey". a non-OK status on error. It
     * is not an error if no keys exist in the range ["beginKey", "endKey").
     *
     * Delete the database entry (if any) for "key". Returns OK on success, and a
     * non-OK status on error. It is not an error if "key" did not exist in the
     * database.
     *
     * @param writeOpt WriteOptions to be used with delete operation
     * @param beginKey First key to delete within database (inclusive)
     * @param endKey Last key to delete within database (exclusive)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun deleteRange(
        writeOpt: WriteOptions, beginKey: ByteArray,
        endKey: ByteArray
    )

    /**
     * Removes the database entries in the range ["beginKey", "endKey"), i.e.,
     * including "beginKey" and excluding "endKey". a non-OK status on error. It
     * is not an error if no keys exist in the range ["beginKey", "endKey").
     *
     * Delete the database entry (if any) for "key". Returns OK on success, and a
     * non-OK status on error. It is not an error if "key" did not exist in the
     * database.
     *
     * @param columnFamilyHandle [org.rocksdb.ColumnFamilyHandle] instance
     * @param writeOpt WriteOptions to be used with delete operation
     * @param beginKey First key to delete within database (included)
     * @param endKey Last key to delete within database (excluded)
     *
     * @throws RocksDBException thrown if error happens in underlying native
     * library.
     */
    fun deleteRange(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpt: WriteOptions, beginKey: ByteArray, endKey: ByteArray
    )

    /**
     * Add merge operand for key/value pair.
     *
     * @param key the specified key to be merged.
     * @param value the value to be merged with the current value for the
     * specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun merge(key: ByteArray, value: ByteArray)

    /**
     * Add merge operand for key/value pair.
     *
     * @param key the specified key to be merged.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param value the value to be merged with the current value for the
     * specified key.
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, must be
     * non-negative and must be non-negative and no larger than
     * ("value".length -  offset)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     * @throws IndexOutOfBoundsException if an offset or length is out of bounds
     */
    fun merge(
        key: ByteArray, offset: Int, len: Int, value: ByteArray,
        vOffset: Int, vLen: Int
    )

    /**
     * Add merge operand for key/value pair in a ColumnFamily.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle] instance
     * @param key the specified key to be merged.
     * @param value the value to be merged with the current value for
     * the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray, value: ByteArray
    )

    /**
     * Add merge operand for key/value pair in a ColumnFamily.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle] instance
     * @param key the specified key to be merged.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param value the value to be merged with the current value for
     * the specified key.
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, must be
     * must be non-negative and no larger than ("value".length -  offset)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     * @throws IndexOutOfBoundsException if an offset or length is out of bounds
     */
    fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray, offset: Int, len: Int, value: ByteArray,
        vOffset: Int, vLen: Int
    )

    /**
     * Add merge operand for key/value pair.
     *
     * @param writeOpts [WriteOptions] for this write.
     * @param key the specified key to be merged.
     * @param value the value to be merged with the current value for
     * the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun merge(
        writeOpts: WriteOptions, key: ByteArray,
        value: ByteArray
    )

    /**
     * Add merge operand for key/value pair.
     *
     * @param writeOpts [WriteOptions] for this write.
     * @param key the specified key to be merged.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("value".length -  offset)
     * @param value the value to be merged with the current value for
     * the specified key.
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, must be
     * non-negative and no larger than ("value".length -  offset)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     * @throws IndexOutOfBoundsException if an offset or length is out of bounds
     */
    fun merge(
        writeOpts: WriteOptions,
        key: ByteArray, offset: Int, len: Int,
        value: ByteArray, vOffset: Int, vLen: Int
    )

    /**
     * Add merge operand for key/value pair.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle] instance
     * @param writeOpts [WriteOptions] for this write.
     * @param key the specified key to be merged.
     * @param value the value to be merged with the current value for the
     * specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpts: WriteOptions, key: ByteArray, value: ByteArray
    )

    /**
     * Add merge operand for key/value pair.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle] instance
     * @param writeOpts [WriteOptions] for this write.
     * @param key the specified key to be merged.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param value the value to be merged with the current value for
     * the specified key.
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, must be
     * non-negative and no larger than ("value".length -  offset)
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     * @throws IndexOutOfBoundsException if an offset or length is out of bounds
     */
    fun merge(
        columnFamilyHandle: ColumnFamilyHandle, writeOpts: WriteOptions,
        key: ByteArray, offset: Int, len: Int,
        value: ByteArray, vOffset: Int, vLen: Int
    )

    /**
     * Apply the specified updates to the database.
     *
     * @param writeOpts WriteOptions instance
     * @param updates WriteBatch instance
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun write(writeOpts: WriteOptions, updates: WriteBatch)

    /**
     * Get the value associated with the specified key within column family*
     *
     * @param key the key to retrieve the value.
     * @param value the out-value to receive the retrieved value.
     *
     * @return The size of the actual value that matches the specified
     * `key` in byte.  If the return value is greater than the
     * length of `value`, then it indicates that the size of the
     * input buffer `value` is insufficient and partial result will
     * be returned.  RocksDB.NOT_FOUND will be returned if the value not
     * found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(key: ByteArray, value: ByteArray): Int

    /**
     * Get the value associated with the specified key within column family*
     *
     * @param key the key to retrieve the value.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param value the out-value to receive the retrieved value.
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "value".length
     * @param vLen the length of the "value" array to be used, must be
     * non-negative and and no larger than ("value".length -  offset)
     *
     * @return The size of the actual value that matches the specified
     * `key` in byte.  If the return value is greater than the
     * length of `value`, then it indicates that the size of the
     * input buffer `value` is insufficient and partial result will
     * be returned.  RocksDB.NOT_FOUND will be returned if the value not
     * found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        key: ByteArray, offset: Int, len: Int,
        value: ByteArray, vOffset: Int, vLen: Int
    ): Int

    /**
     * Get the value associated with the specified key within column family.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param key the key to retrieve the value.
     * @param value the out-value to receive the retrieved value.
     * @return The size of the actual value that matches the specified
     * `key` in byte.  If the return value is greater than the
     * length of `value`, then it indicates that the size of the
     * input buffer `value` is insufficient and partial result will
     * be returned.  RocksDB.NOT_FOUND will be returned if the value not
     * found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        columnFamilyHandle: ColumnFamilyHandle, key: ByteArray,
        value: ByteArray
    ): Int

    /**
     * Get the value associated with the specified key within column family.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param key the key to retrieve the value.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * an no larger than ("key".length -  offset)
     * @param value the out-value to receive the retrieved value.
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, must be
     * non-negative and no larger than ("value".length -  offset)
     *
     * @return The size of the actual value that matches the specified
     * `key` in byte.  If the return value is greater than the
     * length of `value`, then it indicates that the size of the
     * input buffer `value` is insufficient and partial result will
     * be returned.  RocksDB.NOT_FOUND will be returned if the value not
     * found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        columnFamilyHandle: ColumnFamilyHandle, key: ByteArray,
        offset: Int, len: Int, value: ByteArray, vOffset: Int,
        vLen: Int
    ): Int

    /**
     * Get the value associated with the specified key.
     *
     * @param opt [ReadOptions] instance.
     * @param key the key to retrieve the value.
     * @param value the out-value to receive the retrieved value.
     * @return The size of the actual value that matches the specified
     * `key` in byte.  If the return value is greater than the
     * length of `value`, then it indicates that the size of the
     * input buffer `value` is insufficient and partial result will
     * be returned.  RocksDB.NOT_FOUND will be returned if the value not
     * found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        opt: ReadOptions, key: ByteArray,
        value: ByteArray
    ): Int

    /**
     * Get the value associated with the specified key.
     *
     * @param opt [ReadOptions] instance.
     * @param key the key to retrieve the value.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param value the out-value to receive the retrieved value.
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, must be
     * non-negative and no larger than ("value".length -  offset)
     * @return The size of the actual value that matches the specified
     * `key` in byte.  If the return value is greater than the
     * length of `value`, then it indicates that the size of the
     * input buffer `value` is insufficient and partial result will
     * be returned.  RocksDB.NOT_FOUND will be returned if the value not
     * found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        opt: ReadOptions, key: ByteArray, offset: Int,
        len: Int, value: ByteArray, vOffset: Int, vLen: Int
    ): Int

    /**
     * Get the value associated with the specified key within column family.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param opt [ReadOptions] instance.
     * @param key the key to retrieve the value.
     * @param value the out-value to receive the retrieved value.
     * @return The size of the actual value that matches the specified
     * `key` in byte.  If the return value is greater than the
     * length of `value`, then it indicates that the size of the
     * input buffer `value` is insufficient and partial result will
     * be returned.  RocksDB.NOT_FOUND will be returned if the value not
     * found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        opt: ReadOptions, key: ByteArray, value: ByteArray
    ): Int

    /**
     * Get the value associated with the specified key within column family.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param opt [ReadOptions] instance.
     * @param key the key to retrieve the value.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be
     * non-negative and and no larger than ("key".length -  offset)
     * @param value the out-value to receive the retrieved value.
     * @param vOffset the offset of the "value" array to be used, must be
     * non-negative and no longer than "key".length
     * @param vLen the length of the "value" array to be used, and must be
     * non-negative and no larger than ("value".length -  offset)
     * @return The size of the actual value that matches the specified
     * `key` in byte.  If the return value is greater than the
     * length of `value`, then it indicates that the size of the
     * input buffer `value` is insufficient and partial result will
     * be returned.  RocksDB.NOT_FOUND will be returned if the value not
     * found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        opt: ReadOptions, key: ByteArray, offset: Int, len: Int,
        value: ByteArray, vOffset: Int, vLen: Int
    ): Int

    /**
     * The simplified version of get which returns a new byte array storing
     * the value associated with the specified input key if any.  null will be
     * returned if the specified key is not found.
     *
     * @param key the key retrieve the value.
     * @return a byte array storing the value associated with the input key if
     * any. null if it does not find the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    operator fun get(key: ByteArray): ByteArray?

    /**
     * The simplified version of get which returns a new byte array storing
     * the value associated with the specified input key if any.  null will be
     * returned if the specified key is not found.
     *
     * @param key the key retrieve the value.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @return a byte array storing the value associated with the input key if
     * any. null if it does not find the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        key: ByteArray, offset: Int,
        len: Int
    ): ByteArray?

    /**
     * The simplified version of get which returns a new byte array storing
     * the value associated with the specified input key if any.  null will be
     * returned if the specified key is not found.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param key the key retrieve the value.
     * @return a byte array storing the value associated with the input key if
     * any.  null if it does not find the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray
    ): ByteArray?

    /**
     * The simplified version of get which returns a new byte array storing
     * the value associated with the specified input key if any.  null will be
     * returned if the specified key is not found.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param key the key retrieve the value.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @return a byte array storing the value associated with the input key if
     * any. null if it does not find the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray, offset: Int, len: Int
    ): ByteArray?

    /**
     * The simplified version of get which returns a new byte array storing
     * the value associated with the specified input key if any.  null will be
     * returned if the specified key is not found.
     *
     * @param key the key retrieve the value.
     * @param opt Read options.
     * @return a byte array storing the value associated with the input key if
     * any.  null if it does not find the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(opt: ReadOptions, key: ByteArray): ByteArray?

    /**
     * The simplified version of get which returns a new byte array storing
     * the value associated with the specified input key if any.  null will be
     * returned if the specified key is not found.
     *
     * @param key the key retrieve the value.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param opt Read options.
     * @return a byte array storing the value associated with the input key if
     * any. null if it does not find the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        opt: ReadOptions, key: ByteArray, offset: Int,
        len: Int
    ): ByteArray?

    /**
     * The simplified version of get which returns a new byte array storing
     * the value associated with the specified input key if any.  null will be
     * returned if the specified key is not found.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param key the key retrieve the value.
     * @param opt Read options.
     * @return a byte array storing the value associated with the input key if
     * any. null if it does not find the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        opt: ReadOptions, key: ByteArray
    ): ByteArray?

    /**
     * The simplified version of get which returns a new byte array storing
     * the value associated with the specified input key if any.  null will be
     * returned if the specified key is not found.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle]
     * instance
     * @param key the key retrieve the value.
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than ("key".length -  offset)
     * @param opt Read options.
     * @return a byte array storing the value associated with the input key if
     * any. null if it does not find the specified key.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        opt: ReadOptions, key: ByteArray, offset: Int, len: Int
    ): ByteArray?


    /**
     * Takes a list of keys, and returns a list of values for the given list of
     * keys. List will contain null for keys which could not be found.
     *
     * @param keys List of keys for which values need to be retrieved.
     * @return List of values for the given list of keys. List will contain
     * null for keys which could not be found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun multiGetAsList(keys: List<ByteArray>): List<ByteArray?>

    /**
     * Returns a list of values for the given list of keys. List will contain
     * null for keys which could not be found.
     *
     * Note: Every key needs to have a related column family name in
     * `columnFamilyHandleList`.
     *
     * @param columnFamilyHandleList [List] containing
     * [maryk.rocksdb.ColumnFamilyHandle] instances.
     * @param keys List of keys for which values need to be retrieved.
     * @return List of values for the given list of keys. List will contain
     * null for keys which could not be found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     * @throws IllegalArgumentException thrown if the size of passed keys is not
     * equal to the amount of passed column family handles.
     */
    fun multiGetAsList(
        columnFamilyHandleList: List<ColumnFamilyHandle>,
        keys: List<ByteArray>
    ): List<ByteArray?>

    /**
     * Returns a list of values for the given list of keys. List will contain
     * null for keys which could not be found.
     *
     * @param opt Read options.
     * @param keys of keys for which values need to be retrieved.
     * @return List of values for the given list of keys. List will contain
     * null for keys which could not be found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun multiGetAsList(
        opt: ReadOptions,
        keys: List<ByteArray>
    ): List<ByteArray?>

    /**
     * Returns a list of values for the given list of keys. List will contain
     * null for keys which could not be found.
     *
     * Note: Every key needs to have a related column family name in
     * `columnFamilyHandleList`.
     *
     * @param opt Read options.
     * @param columnFamilyHandleList [List] containing
     * [maryk.rocksdb.ColumnFamilyHandle] instances.
     * @param keys of keys for which values need to be retrieved.
     * @return List of values for the given list of keys. List will contain
     * null for keys which could not be found.
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     * @throws IllegalArgumentException thrown if the size of passed keys is not
     * equal to the amount of passed column family handles.
     */
    fun multiGetAsList(
        opt: ReadOptions,
        columnFamilyHandleList: List<ColumnFamilyHandle>,
        keys: List<ByteArray>
    ): List<ByteArray?>

    /**
     * If the [key] definitely does not exist in the database, then this method
     * returns false, else true.
     *
     * This check is potentially lighter-weight than invoking DB::Get(). One way
     * to make this lighter weight is to avoid doing any IOs.
     *
     * @param key byte array of a key to search for
     * @param valueHolder non-null to retrieve the value if it is found, or null
     *     if the value is not needed. If non-null, upon return of the function,
     *     the {@code value} will be set if it could be retrieved.
     * @return boolean value indicating if key does not exist or might exist.
     */
    fun keyMayExist(key: ByteArray, valueHolder: Holder<ByteArray>?): Boolean

    /**
     * If the key definitely does not exist in the database, then this method
     * returns false, else true.
     *
     * This check is potentially lighter-weight than invoking DB::Get(). One way
     * to make this lighter weight is to avoid doing any IOs.
     *
     * @param key byte array of a key to search for
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than "key".length
     * @param valueHolder non-null to retrieve the value if it is found, or null
     *     if the value is not needed. If non-null, upon return of the function,
     *     the {@code value} will be set if it could be retrieved.
     *
     * @return boolean value indicating if key does not exist or might exist.
     */
    fun keyMayExist(
        key: ByteArray, offset: Int, len: Int,
        valueHolder: Holder<ByteArray>?
    ): Boolean

    /**
     * If the key definitely does not exist in the database, then this method
     * returns false, else true.
     *
     * This check is potentially lighter-weight than invoking DB::Get(). One way
     * to make this lighter weight is to avoid doing any IOs.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle] instance
     * @param key byte array of a key to search for
     * @param valueHolder non-null to retrieve the value if it is found, or null
     *     if the value is not needed. If non-null, upon return of the function,
     *     the {@code value} will be set if it could be retrieved.
     * @return boolean value indicating if key does not exist or might exist.
     */
    fun keyMayExist(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray, valueHolder: Holder<ByteArray>?
    ): Boolean

    /**
     * If the key definitely does not exist in the database, then this method
     * returns false, else true.
     *
     * This check is potentially lighter-weight than invoking DB::Get(). One way
     * to make this lighter weight is to avoid doing any IOs.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle] instance
     * @param key byte array of a key to search for
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than "key".length
     * @param valueHolder non-null to retrieve the value if it is found, or null
     *     if the value is not needed. If non-null, upon return of the function,
     *     the {@code value} will be set if it could be retrieved.
     * @return boolean value indicating if key does not exist or might exist.
     */
    fun keyMayExist(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray, offset: Int, len: Int, valueHolder: Holder<ByteArray>?
    ): Boolean

    /**
     * If the key definitely does not exist in the database, then this method
     * returns false, else true.
     *
     * This check is potentially lighter-weight than invoking DB::Get(). One way
     * to make this lighter weight is to avoid doing any IOs.
     *
     * @param readOptions [ReadOptions] instance
     * @param key byte array of a key to search for
     * @param valueHolder non-null to retrieve the value if it is found, or null
     *     if the value is not needed. If non-null, upon return of the function,
     *     the {@code value} will be set if it could be retrieved.
     * @return boolean value indicating if key does not exist or might exist.
     */
    fun keyMayExist(
        readOptions: ReadOptions,
        key: ByteArray, valueHolder: Holder<ByteArray>?
    ): Boolean

    /**
     * If the key definitely does not exist in the database, then this method
     * returns false, else true.
     *
     * This check is potentially lighter-weight than invoking DB::Get(). One way
     * to make this lighter weight is to avoid doing any IOs.
     *
     * @param readOptions [ReadOptions] instance
     * @param key byte array of a key to search for
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than "key".length
     * @param valueHolder non-null to retrieve the value if it is found, or null
     *     if the value is not needed. If non-null, upon return of the function,
     *     the {@code value} will be set if it could be retrieved.
     * @return boolean value indicating if key does not exist or might exist.
     */
    fun keyMayExist(
        readOptions: ReadOptions,
        key: ByteArray, offset: Int, len: Int,
        valueHolder: Holder<ByteArray>?
    ): Boolean

    /**
     * If the key definitely does not exist in the database, then this method
     * returns false, else true.
     *
     * This check is potentially lighter-weight than invoking DB::Get(). One way
     * to make this lighter weight is to avoid doing any IOs.
     *
     * @param columnFamilyHandle [ColumnFamilyHandle] instance
     * @param readOptions [ReadOptions] instance
     * @param key byte array of a key to search for
     * @param valueHolder non-null to retrieve the value if it is found, or null
     *     if the value is not needed. If non-null, upon return of the function,
     *     the {@code value} will be set if it could be retrieved.
     * @return boolean value indicating if key does not exist or might exist.
     */
    fun keyMayExist(
        columnFamilyHandle: ColumnFamilyHandle,
        readOptions: ReadOptions,
        key: ByteArray,
        valueHolder: Holder<ByteArray>?
    ): Boolean

    /**
     * If the key definitely does not exist in the database, then this method
     * returns false, else true.
     *
     * This check is potentially lighter-weight than invoking DB::Get(). One way
     * to make this lighter weight is to avoid doing any IOs.
     *
     * @param readOptions [ReadOptions] instance
     * @param columnFamilyHandle [ColumnFamilyHandle] instance
     * @param key byte array of a key to search for
     * @param offset the offset of the "key" array to be used, must be
     * non-negative and no larger than "key".length
     * @param len the length of the "key" array to be used, must be non-negative
     * and no larger than "key".length
     * @param valueHolder non-null to retrieve the value if it is found, or null
     *     if the value is not needed. If non-null, upon return of the function,
     *     the {@code value} will be set if it could be retrieved.
     * @return boolean value indicating if key does not exist or might exist.
     */
    fun keyMayExist(
        columnFamilyHandle: ColumnFamilyHandle,
        readOptions: ReadOptions,
        key: ByteArray,
        offset: Int, len: Int, valueHolder: Holder<ByteArray>?
    ): Boolean

    /**
     * Return a heap-allocated iterator over the contents of the
     * database. The result of newIterator() is initially invalid
     * (caller must call one of the Seek methods on the iterator
     * before using it).
     *
     * Caller should close the iterator when it is no longer needed.
     * The returned iterator should be closed before this db is closed.
     *
     * @return instance of iterator object.
     */
    fun newIterator(): RocksIterator

    /**
     * Return a heap-allocated iterator over the contents of the
     * database. The result of newIterator() is initially invalid
     * (caller must call one of the Seek methods on the iterator
     * before using it).
     *
     * Caller should close the iterator when it is no longer needed.
     * The returned iterator should be closed before this db is closed.
     *
     * @param readOptions [ReadOptions] instance.
     * @return instance of iterator object.
     */
    fun newIterator(readOptions: ReadOptions): RocksIterator

    /**
     * Return a heap-allocated iterator over the contents of the
     * database. The result of newIterator() is initially invalid
     * (caller must call one of the Seek methods on the iterator
     * before using it).
     *
     * Caller should close the iterator when it is no longer needed.
     * The returned iterator should be closed before this db is closed.
     *
     * @param columnFamilyHandle [maryk.rocksdb.ColumnFamilyHandle]
     * instance
     * @return instance of iterator object.
     */
    fun newIterator(
        columnFamilyHandle: ColumnFamilyHandle
    ): RocksIterator

    /**
     * Return a heap-allocated iterator over the contents of the
     * database. The result of newIterator() is initially invalid
     * (caller must call one of the Seek methods on the iterator
     * before using it).
     *
     * Caller should close the iterator when it is no longer needed.
     * The returned iterator should be closed before this db is closed.
     *
     * @param columnFamilyHandle [maryk.rocksdb.ColumnFamilyHandle]
     * instance
     * @param readOptions [ReadOptions] instance.
     * @return instance of iterator object.
     */
    fun newIterator(
        columnFamilyHandle: ColumnFamilyHandle,
        readOptions: ReadOptions
    ): RocksIterator

    /**
     * Returns iterators from a consistent database state across multiple
     * column families. Iterators are heap allocated and need to be deleted
     * before the db is deleted
     *
     * @param columnFamilyHandleList [java.util.List] containing
     * [org.rocksdb.ColumnFamilyHandle] instances.
     * @return [java.util.List] containing [org.rocksdb.RocksIterator]
     * instances
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun newIterators(
        columnFamilyHandleList: List<ColumnFamilyHandle>
    ): List<RocksIterator>

    /**
     * Returns iterators from a consistent database state across multiple
     * column families. Iterators are heap allocated and need to be deleted
     * before the db is deleted
     *
     * @param columnFamilyHandleList [java.util.List] containing
     * [org.rocksdb.ColumnFamilyHandle] instances.
     * @param readOptions [ReadOptions] instance.
     * @return [java.util.List] containing [org.rocksdb.RocksIterator]
     * instances
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun newIterators(
        columnFamilyHandleList: List<ColumnFamilyHandle>,
        readOptions: ReadOptions
    ): List<RocksIterator>

    /**
     * Return a handle to the current DB state. Iterators created with
     * this handle will all observe a stable snapshot of the current DB
     * state. The caller must call ReleaseSnapshot(result) when the
     * snapshot is no longer needed.
     *
     * nullptr will be returned if the DB fails to take a snapshot or does
     * not support snapshot.
     *
     * @return Snapshot [Snapshot] instance
     */
    fun getSnapshot(): Snapshot?

    /**
     * Release a previously acquired snapshot.
     *
     * The caller must not use "snapshot" after this call.
     *
     * @param snapshot [Snapshot] instance
     */
    fun releaseSnapshot(snapshot: Snapshot)

    /**
     * DB implements can export properties about their state
     * via this method on a per column family level.
     *
     * If `property` is a valid property understood by this DB
     * implementation, fills `value` with its current value and
     * returns true. Otherwise returns false.
     *
     * Valid property names include:
     *
     *  - "rocksdb.num-files-at-level&lt;N&gt;" - return the number of files at
     *    level &lt;N&gt;, where &lt;N&gt; is an ASCII representation of a level
     *    number (e.g. "0").
     *  - "rocksdb.stats" - returns a multi-line string that describes statistics
     *    about the internal operation of the DB.
     *  - "rocksdb.sstables" - returns a multi-line string that describes all
     *    of the sstables that make up the db contents.
     *
     * @param columnFamilyHandle [org.rocksdb.ColumnFamilyHandle]
     * instance, or null for the default column family.
     * @param property to be fetched. See above for examples
     * @return property value
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun getProperty(
        columnFamilyHandle: ColumnFamilyHandle,
        property: String
    ): String?

    /**
     * DB implementations can export properties about their state
     * via this method.  If "property" is a valid property understood by this
     * DB implementation, fills "*value" with its current value and returns
     * true.  Otherwise returns false.
     *
     * Valid property names include:
     *
     *  - "rocksdb.num-files-at-level<N>" - return the number of files at
     *    level <N>, where <N> is an ASCII representation of a level
     *    number (e.g. "0").
     *  - "rocksdb.stats" - returns a multi-line string that describes statistics
     *    about the internal operation of the DB.
     *  - "rocksdb.sstables" - returns a multi-line string that describes all
     *    of the sstables that make up the db contents.
     *
     * @param property to be fetched. See above for examples
     * @return property value
     *
     * @throws RocksDBException thrown if error happens in underlying
     * native library.
     */
    fun getProperty(property: String): String?

    /**
     * Gets a property map.
     *
     * @param property to be fetched.
     *
     * @return the property map
     *
     * @throws RocksDBException if an error happens in the underlying native code.
     */
    fun getMapProperty(property: String): Map<String, String>

    /**
     * Gets a property map.
     *
     * @param columnFamilyHandle [maryk.rocksdb.ColumnFamilyHandle]
     * instance, or null for the default column family.
     * @param property to be fetched.
     *
     * @return the property map
     *
     * @throws RocksDBException if an error happens in the underlying native code.
     */
    fun getMapProperty(
        columnFamilyHandle: ColumnFamilyHandle,
        property: String
    ): Map<String, String>

    /**
     * Similar to GetProperty(), but only works for a subset of properties
     * whose return value is a numerical value. Return the value as long.
     *
     * The value should be treated as unsigned long using provided methods
     * of type [Long].
     *
     * @param property to be fetched.
     *
     * @return numerical property value.
     *
     * @throws RocksDBException if an error happens in the underlying native code.
     */
    fun getLongProperty(property: String): Long

    /**
     * Similar to GetProperty(), but only works for a subset of properties
     * whose return value is a numerical value. Return the value as long.
     *
     * The value should be treated as unsigned long using provided methods
     * of type [Long].
     *
     * @param columnFamilyHandle [maryk.rocksdb.ColumnFamilyHandle]
     * instance, or null for the default column family
     * @param property to be fetched.
     *
     * @return numerical property value
     *
     * @throws RocksDBException if an error happens in the underlying native code.
     */
    fun getLongProperty(
        columnFamilyHandle: ColumnFamilyHandle,
        property: String
    ) : Long

    /**
     * Reset internal stats for DB and all column families.
     *
     * Note this doesn't reset [Options.statistics] as it is not
     * owned by DB.
     */
    fun resetStats()

    /**
     * Range compaction of database.
     *
     * **Note**: After the database has been compacted,
     * all data will have been pushed down to the last level containing
     * any data.
     *
     * **See also**
     *  - [.compactRange]
     *
     * @throws RocksDBException thrown if an error occurs within the native
     * part of the library.
     */
    fun compactRange()

    /**
     * Range compaction of column family.
     *
     * **Note**: After the database has been compacted,
     * all data will have been pushed down to the last level containing
     * any data.
     *
     *
     * **See also**
     * - [.compactRange]
     *
     * @param columnFamilyHandle [maryk.rocksdb.ColumnFamilyHandle]
     * instance, or null for the default column family.
     *
     * @throws RocksDBException thrown if an error occurs within the native
     * part of the library.
     */
    fun compactRange(
        columnFamilyHandle: ColumnFamilyHandle
    )

    /**
     * Range compaction of database.
     *
     * **Note**: After the database has been compacted,
     * all data will have been pushed down to the last level containing
     * any data.
     *
     * **See also**
     *  - [.compactRange]
     *
     * @param begin start of key range (included in range)
     * @param end end of key range (excluded from range)
     *
     * @throws RocksDBException thrown if an error occurs within the native
     * part of the library.
     */
    fun compactRange(begin: ByteArray, end: ByteArray)

    /**
     * Range compaction of column family.
     *
     * **Note**: After the database has been compacted,
     * all data will have been pushed down to the last level containing
     * any data.
     *
     * **See also**
     * [.compactRange]
     *
     * @param columnFamilyHandle [org.rocksdb.ColumnFamilyHandle]
     * instance, or null for the default column family.
     * @param begin start of key range (included in range)
     * @param end end of key range (excluded from range)
     *
     * @throws RocksDBException thrown if an error occurs within the native
     * part of the library.
     */
    fun compactRange(
        columnFamilyHandle: ColumnFamilyHandle,
        begin: ByteArray, end: ByteArray
    )

    /**
     *
     * Range compaction of column family.
     *
     * **Note**: After the database has been compacted,
     * all data will have been pushed down to the last level containing
     * any data.
     *
     * @param columnFamilyHandle [org.rocksdb.ColumnFamilyHandle] instance.
     * @param begin start of key range (included in range)
     * @param end end of key range (excluded from range)
     * @param compactRangeOptions options for the compaction
     *
     * @throws RocksDBException thrown if an error occurs within the native
     * part of the library.
     */
    fun compactRange(
        columnFamilyHandle: ColumnFamilyHandle,
        begin: ByteArray, end: ByteArray,
        compactRangeOptions: CompactRangeOptions
    )

    /**
     * This function will wait until all currently running background processes
     * finish. After it returns, no background process will be run until
     * [.continueBackgroundWork] is called
     *
     * @throws RocksDBException If an error occurs when pausing background work
     */
    fun pauseBackgroundWork()

    /**
     * Resumes background work which was suspended by
     * previously calling [.pauseBackgroundWork]
     *
     * @throws RocksDBException If an error occurs when resuming background work
     */
    fun continueBackgroundWork()

    /**
     * Enable automatic compactions for the given column
     * families if they were previously disabled.
     *
     * The function will first set the
     * [ColumnFamilyOptions.disableAutoCompactions] option for each
     * column family to false, after which it will schedule a flush/compaction.
     *
     * NOTE: Setting disableAutoCompactions to 'false' through
     * [.setOptions]
     * does NOT schedule a flush/compaction afterwards, and only changes the
     * parameter itself within the column family option.
     *
     * @param columnFamilyHandles the column family handles
     *
     * @throws RocksDBException thrown if an error occurs within the native
     * part of the library.
     */
    fun enableAutoCompaction(
        columnFamilyHandles: List<ColumnFamilyHandle>
    )

    /**
     * Number of levels used for this DB.
     *
     * @return the number of levels
     */
    fun numberLevels(): Int

    /**
     * Number of levels used for a column family in this DB.
     *
     * @param columnFamilyHandle the column family handle, or null
     * for the default column family
     *
     * @return the number of levels
     */
    fun numberLevels(columnFamilyHandle: ColumnFamilyHandle): Int

    /**
     * Maximum level to which a new compacted memtable is pushed if it
     * does not create overlap.
     */
    fun maxMemCompactionLevel(): Int

    /**
     * Maximum level to which a new compacted memtable is pushed if it
     * does not create overlap.
     *
     * @param columnFamilyHandle the column family handle
     */
    fun maxMemCompactionLevel(
        columnFamilyHandle: ColumnFamilyHandle
    ): Int

    /**
     * Number of files in level-0 that would stop writes.
     */
    fun level0StopWriteTrigger(): Int

    /**
     * Number of files in level-0 that would stop writes.
     *
     * @param columnFamilyHandle the column family handle
     */
    fun level0StopWriteTrigger(
        columnFamilyHandle: ColumnFamilyHandle
    ): Int

    /**
     * Get the Env object from the DB
     *
     * @return the env
     */
    fun getEnv(): Env

    /**
     * Flush the WAL memory buffer to the file. If `sync` is true,
     * it calls [.syncWal] afterwards.
     *
     * @param sync true to also fsync to disk.
     */
    fun flushWal(sync: Boolean)

    /**
     * Sync the WAL.
     *
     * Note that [.write] followed by
     * [.syncWal] is not exactly the same as
     * [.write] with
     * [WriteOptions.sync] set to true; In the latter case the changes
     * won't be visible until the sync is done.
     *
     * Currently only works if [Options.allowMmapWrites] is set to false.
     */
    fun syncWal()

    /**
     * The sequence number of the most recent transaction.
     *
     * @return sequence number of the most
     * recent transaction.
     */
    fun getLatestSequenceNumber(): Long

    /**
     * Prevent file deletions. Compactions will continue to occur,
     * but no obsolete files will be deleted. Calling this multiple
     * times have the same effect as calling it once.
     *
     * @throws RocksDBException thrown if operation was not performed
     * successfully.
     */
    fun disableFileDeletions()

    /**
     * Allow compactions to delete obsolete files.
     * If force == true, the call to EnableFileDeletions()
     * will guarantee that file deletions are enabled after
     * the call, even if DisableFileDeletions() was called
     * multiple times before.
     *
     * If force == false, EnableFileDeletions will only
     * enable file deletion after it's been called at least
     * as many times as DisableFileDeletions(), enabling
     * the two methods to be called by two threads
     * concurrently without synchronization
     * -- i.e., file deletions will be enabled only after both
     * threads call EnableFileDeletions()
     *
     * @param force boolean value described above.
     *
     * @throws RocksDBException thrown if operation was not performed
     * successfully.
     */
    fun enableFileDeletions(force: Boolean)

    /**
     * Delete the file name from the db directory and update the internal state to
     * reflect that. Supports deletion of sst and log files only. 'name' must be
     * path relative to the db directory. eg. 000001.sst, /archive/000003.log
     *
     * @param name the file name
     */
    fun deleteFile(name: String)

    /**
     * Obtains the meta data of the specified column family of the DB.
     *
     * @param columnFamilyHandle the column family
     *
     * @return the column family metadata
     */
    fun getColumnFamilyMetaData(
        columnFamilyHandle: ColumnFamilyHandle
    ): ColumnFamilyMetaData

    /**
     * Obtains the meta data of the default column family of the DB.
     *
     * @return the column family metadata
     */
    fun getColumnFamilyMetaData(): ColumnFamilyMetaData

    /**
     * Verify checksum
     *
     * @throws RocksDBException if the checksum is not valid
     */
    fun verifyChecksum()

    /**
     * Gets the handle for the default column family
     * @return The handle of the default column family
     */
    fun getDefaultColumnFamily(): ColumnFamilyHandle

    /**
     * Promote L0.
     *
     * @param columnFamilyHandle the column family handle,
     * or null for the default column family.
     */
    fun promoteL0(
        columnFamilyHandle: ColumnFamilyHandle,
        targetLevel: Int
    )

    /** Promote L0 for the default column family. */
    fun promoteL0(targetLevel: Int)
}

/**
 * Static method to destroy the contents of the specified database.
 * Be very careful using this method.
 *
 * @param path the path to the Rocksdb database.
 * @param options [org.rocksdb.Options] instance.
 *
 * @throws RocksDBException thrown if error happens in underlying
 * native library.
 */
expect fun destroyRocksDB(path: String, options: Options)

/**
 * Function to determine all available column families for a
 * RocksDB database identified by path
 *
 * @param options Options for opening the database
 * @param path Absolute path to RocksDB database
 * @return List containing the column family names
 *
 * @throws RocksDBException thrown if error happens in underlying
 * native library.
 */
expect fun listColumnFamilies(
    options: Options,
    path: String
): List<ByteArray>
