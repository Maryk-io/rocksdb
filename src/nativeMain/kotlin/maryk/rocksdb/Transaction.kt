package maryk.rocksdb

import cnames.structs.rocksdb_transaction_t
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import maryk.ByteBuffer
import maryk.byteArrayToCPointer
import maryk.toByteArray
import maryk.wrapWithErrorThrower

actual class Transaction(
    internal val native: CPointer<rocksdb_transaction_t>,
): RocksObject() {
    private val defaultReadOptions = ReadOptions()

    override fun close() {
        if (isOwningHandle()) {
            rocksdb.rocksdb_transaction_destroy(native)
            defaultReadOptions.close()
            super.close()
        }
    }

    actual fun setSnapshot() {
//        wrapWithErrorThrower { error ->
//            rocksdb.rocksdb_transaction_set_snapshot(native, error)
//        }
        TODO()
    }

    actual fun setSnapshotOnNextOperation() {
//        wrapWithErrorThrower { error ->
//            rocksdb.rocksdb_transaction_set_snapshot_on_next_operation(native, error)
//        }
        TODO()
    }

    actual fun setSnapshotOnNextOperation(transactionNotifier: AbstractTransactionNotifier) {
        // There is no direct C API for a transaction notifier.
        // Either implement notifier support in your native layer or throw an error.
        throw NotImplementedError("Transaction notifier support is not available in the C API.")
    }

    actual fun getSnapshot(): Snapshot? {
        val snapshotPtr = rocksdb.rocksdb_transaction_get_snapshot(native)
        return snapshotPtr?.let { Snapshot(it) }
    }

    actual fun clearSnapshot() {
//        wrapWithErrorThrower { error ->
//            rocksdb.rocksdb_transaction_clear_snapshot(native, error)
//        }
        TODO()
    }

    actual fun prepare() {
        wrapWithErrorThrower { error ->
            rocksdb.rocksdb_transaction_prepare(native, error)
        }
    }

    @Throws(RocksDBException::class)
    actual fun commit() {
        wrapWithErrorThrower { error ->
            rocksdb.rocksdb_transaction_commit(native, error)
        }
    }

    @Throws(RocksDBException::class)
    actual fun rollback() {
        wrapWithErrorThrower { error ->
            rocksdb.rocksdb_transaction_rollback(native, error)
        }
    }

    @Throws(RocksDBException::class)
    actual fun setSavePoint() {
        rocksdb.rocksdb_transaction_set_savepoint(native)
    }

    @Throws(RocksDBException::class)
    actual fun rollbackToSavePoint() {
        wrapWithErrorThrower { error ->
            rocksdb.rocksdb_transaction_rollback_to_savepoint(native, error)
        }
    }

    actual fun get(readOptions: ReadOptions, columnFamilyHandle: ColumnFamilyHandle, key: ByteArray): ByteArray? =
        memScoped {
            val errPtr = allocPointerTo<ByteVar>()
            val valueLen = alloc<ULongVar>()
            val valuePtr = rocksdb.rocksdb_transaction_get_cf(
                native,
                readOptions.native,
                columnFamilyHandle.native,
                byteArrayToCPointer(key, 0, key.size),
                key.size.convert(),
                valueLen.ptr,
                errPtr.ptr
            )
            if (errPtr.value != null) {
                throw RocksDBException(errPtr.value!!.toKString())
            }
            if (valuePtr == null) null else valuePtr.readBytes(valueLen.value.toInt())
        }

    actual fun get(readOptions: ReadOptions, key: ByteArray): ByteArray? =
        memScoped {
            val errPtr = allocPointerTo<ByteVar>()
            val valueLen = alloc<ULongVar>()
            val valuePtr = rocksdb.rocksdb_transaction_get(
                native,
                readOptions.native,
                byteArrayToCPointer(key, 0, key.size),
                key.size.convert(),
                valueLen.ptr,
                errPtr.ptr
            )
            if (errPtr.value != null) {
                throw RocksDBException(errPtr.value!!.toKString())
            }
            if (valuePtr == null) null else valuePtr.readBytes(valueLen.value.toInt())
        }

    actual fun get(opt: ReadOptions, key: ByteArray, value: ByteArray): GetStatus = memScoped {
        val result = get(opt, key)
        if (result == null) {
            // Not found: return status with NotFound and no required size.
            GetStatus(Status(StatusCode.NotFound, StatusSubCode.None, null), 0)
        } else {
            // Copy as many bytes as possible into value.
            val copyLength = minOf(result.size, value.size)
            result.copyInto(value, 0, 0, copyLength)
            // Return full length as required size along with Ok status.
            GetStatus(Status(StatusCode.Ok, StatusSubCode.None, null), result.size)
        }
    }

    actual fun get(
        opt: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray
    ): GetStatus =
        memScoped {
            val result = get(opt, columnFamilyHandle, key)
            if (result == null) {
                GetStatus(Status(StatusCode.NotFound, StatusSubCode.None, null), 0)
            } else {
                val copyLength = minOf(result.size, value.size)
                result.copyInto(value, 0, 0, copyLength)
                GetStatus(Status(StatusCode.Ok, StatusSubCode.None, null), result.size)
            }
        }

    actual fun get(
        opt: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer
    ): GetStatus {
        // Extract key into a temporary array.
        val keyArray = ByteArray(key.remaining())
        key.get(keyArray)
        val result = get(opt, columnFamilyHandle, keyArray)
        return if (result == null) {
            GetStatus(Status(StatusCode.NotFound, StatusSubCode.None, null), 0)
        } else {
            // Copy the full result into the destination ByteBuffer.
            value.put(result)
            GetStatus(Status(StatusCode.Ok, StatusSubCode.None, null), result.size)
        }
    }

    actual fun get(opt: ReadOptions, key: ByteBuffer, value: ByteBuffer): GetStatus {
        val keyArray = ByteArray(key.remaining())
        key.get(keyArray)
        val result = get(opt, keyArray)
        return if (result == null) {
            GetStatus(Status(StatusCode.NotFound, StatusSubCode.None, "NotFound"), 0)
        } else {
            value.put(result)
            GetStatus(Status(StatusCode.Ok, StatusSubCode.None, null), result.size)
        }
    }

    actual fun multiGetAsList(
        readOptions: ReadOptions,
        columnFamilyHandles: List<ColumnFamilyHandle>,
        keys: List<ByteArray>
    ): List<ByteArray?> =
        keys.mapIndexed { index, key ->
            get(readOptions, columnFamilyHandles[index], key)
        }

    actual fun multiGetAsList(readOptions: ReadOptions, keys: List<ByteArray>): List<ByteArray?> =
        keys.map { key -> get(readOptions, key) }

    actual fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        exclusive: Boolean,
    ) = getForUpdate(readOptions, columnFamilyHandle, key, exclusive, doValidate = true)

    actual fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        exclusive: Boolean,
        doValidate: Boolean,
    ): ByteArray? = memScoped {
        // Allocate error pointer and length holder.
        val errPtr = allocPointerTo<ByteVar>()
        val valueLen = alloc<ULongVar>()
        // Call the native function using the correct parameter ordering:
        // txn, options, column_family, key, klen, vlen, exclusive, errptr
        val valuePtr = rocksdb.rocksdb_transaction_get_for_update_cf(
            native,
            readOptions.native,
            columnFamilyHandle.native,
            byteArrayToCPointer(key, 0, key.size),
            key.size.convert(),
            valueLen.ptr,
            if (exclusive) 1.toUByte() else 0.toUByte(),
            errPtr.ptr
        )
        if (errPtr.value != null) throw RocksDBException(errPtr.value!!.toKString())
        if (valuePtr == null) null else valuePtr.readBytes(valueLen.value.toInt())
    }

    actual fun getForUpdate(
        readOptions: ReadOptions,
        key: ByteArray,
        exclusive: Boolean
    ): ByteArray? = memScoped {
        val errPtr = allocPointerTo<ByteVar>()
        val valueLen = alloc<ULongVar>()
        // Call the native function using the correct parameter ordering:
        // txn, options, key, klen, vlen, exclusive, errptr
        val valuePtr = rocksdb.rocksdb_transaction_get_for_update(
            native,
            readOptions.native,
            byteArrayToCPointer(key, 0, key.size),
            key.size.convert(),
            valueLen.ptr,
            if (exclusive) 1.toUByte() else 0.toUByte(),
            errPtr.ptr
        )
        if (errPtr.value != null) throw RocksDBException(errPtr.value!!.toKString())
        if (valuePtr == null) null else valuePtr.readBytes(valueLen.value.toInt())
    }

    actual fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray,
        exclusive: Boolean
    ): GetStatus = memScoped {
        val result = getForUpdate(readOptions, columnFamilyHandle, key, exclusive, doValidate = false)
        if (result == null)
            GetStatus(Status(StatusCode.NotFound, StatusSubCode.None, null), 0)
        else {
            val copyLength = minOf(result.size, value.size)
            result.copyInto(value, 0, 0, copyLength)
            GetStatus(Status(StatusCode.Ok, StatusSubCode.None, null), result.size)
        }
    }

    actual fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray,
        exclusive: Boolean,
        doValidate: Boolean
    ): GetStatus = memScoped {
        val result = getForUpdate(readOptions, columnFamilyHandle, key, exclusive, doValidate)
        if (result == null)
            GetStatus(Status(StatusCode.NotFound, StatusSubCode.None, null), 0)
        else {
            val copyLength = minOf(result.size, value.size)
            result.copyInto(value, 0, 0, copyLength)
            GetStatus(Status(StatusCode.Ok, StatusSubCode.None, null), result.size)
        }
    }

    actual fun getForUpdate(
        readOptions: ReadOptions,
        key: ByteArray,
        value: ByteArray,
        exclusive: Boolean
    ): GetStatus = memScoped {
        val result = getForUpdate(readOptions, key, exclusive)
        if (result == null)
            GetStatus(Status(StatusCode.NotFound, StatusSubCode.None, null), 0)
        else {
            val copyLength = minOf(result.size, value.size)
            result.copyInto(value, 0, 0, copyLength)
            GetStatus(Status(StatusCode.Ok, StatusSubCode.None, null), result.size)
        }
    }

    actual fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer,
        exclusive: Boolean,
    ): GetStatus = getForUpdate(readOptions, columnFamilyHandle, key, value, exclusive, doValidate = false)

    actual fun getForUpdate(
        readOptions: ReadOptions,
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer,
        exclusive: Boolean,
        doValidate: Boolean
    ): GetStatus {
        val keyArray = ByteArray(key.remaining())
        key.get(keyArray)
        val result = getForUpdate(readOptions, columnFamilyHandle, keyArray, exclusive, doValidate)
        return if (result == null)
            GetStatus(Status(StatusCode.NotFound, StatusSubCode.None, null), 0)
        else {
            value.put(result)
            GetStatus(Status(StatusCode.Ok, StatusSubCode.None, null), result.size)
        }
    }

    actual fun getForUpdate(
        readOptions: ReadOptions,
        key: ByteBuffer,
        value: ByteBuffer,
        exclusive: Boolean
    ): GetStatus {
        val keyArray = ByteArray(key.remaining())
        key.get(keyArray)
        val result = getForUpdate(readOptions, keyArray, exclusive)
        return if (result == null)
            GetStatus(Status(StatusCode.NotFound, StatusSubCode.None, null), 0)
        else {
            value.put(result)
            GetStatus(Status(StatusCode.Ok, StatusSubCode.None, null), result.size)
        }
    }

    actual fun multiGetForUpdateAsList(
        readOptions: ReadOptions,
        columnFamilyHandles: List<ColumnFamilyHandle>,
        keys: List<ByteArray>
    ): List<ByteArray?> =
        keys.mapIndexed { index, key ->
            getForUpdate(readOptions, columnFamilyHandles[index], key, exclusive = false)
        }

    actual fun multiGetForUpdateAsList(readOptions: ReadOptions, keys: List<ByteArray>): List<ByteArray?> =
        keys.map { key ->
            getForUpdate(readOptions, key, exclusive = false)
        }

    actual fun getIterator(): RocksIterator {
        return RocksIterator(rocksdb.rocksdb_transaction_create_iterator(native, defaultReadOptions.native)!!)
    }

    actual fun getIterator(readOptions: ReadOptions): RocksIterator {
        return RocksIterator(rocksdb.rocksdb_transaction_create_iterator(native, readOptions.native)!!)
    }

    actual fun getIterator(readOptions: ReadOptions, columnFamilyHandle: ColumnFamilyHandle): RocksIterator {
        return RocksIterator(rocksdb.rocksdb_transaction_create_iterator_cf(native, readOptions.native, columnFamilyHandle.native)!!)
    }

    actual fun getIterator(columnFamilyHandle: ColumnFamilyHandle): RocksIterator {
        return RocksIterator(rocksdb.rocksdb_transaction_create_iterator_cf(native, defaultReadOptions.native, columnFamilyHandle.native)!!)
    }

    actual fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray,
        assumeTracked: Boolean
    ) {
        wrapWithErrorThrower { error ->
            memScoped {
                rocksdb.rocksdb_transaction_put_cf(
                    native,
                    columnFamilyHandle.native,
                    byteArrayToCPointer(key, 0, key.size),
                    key.size.convert(),
                    byteArrayToCPointer(value, 0, value.size),
                    value.size.convert(),
                    error
                )
            }
        }
    }

    actual fun put(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray, value: ByteArray) {
        put(columnFamilyHandle, key, value, assumeTracked = true)
    }

    actual fun put(key: ByteArray, value: ByteArray) {
        wrapWithErrorThrower { error ->
            memScoped {
                rocksdb.rocksdb_transaction_put(
                    native,
                    byteArrayToCPointer(key, 0, key.size),
                    key.size.convert(),
                    byteArrayToCPointer(value, 0, value.size),
                    value.size.convert(),
                    error
                )
            }
        }
    }

    actual fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        keyParts: Array<ByteArray>,
        valueParts: Array<ByteArray>,
        assumeTracked: Boolean
    ) {
        val key = keyParts.reduce { acc, bytes -> acc + bytes }
        val value = valueParts.reduce { acc, bytes -> acc + bytes }
        put(columnFamilyHandle, key, value, assumeTracked)
    }

    actual fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        keyParts: Array<ByteArray>,
        valueParts: Array<ByteArray>
    ) {
        put(columnFamilyHandle, keyParts, valueParts, assumeTracked = true)
    }

    actual fun put(keyParts: Array<ByteArray>, valueParts: Array<ByteArray>) {
        val key = keyParts.reduce { acc, bytes -> acc + bytes }
        val value = valueParts.reduce { acc, bytes -> acc + bytes }
        put(key, value)
    }

    actual fun put(key: ByteBuffer, value: ByteBuffer) {
        val keyArray = ByteArray(key.remaining())
        key.get(keyArray)
        val valueArray = ByteArray(value.remaining())
        value.get(valueArray)
        put(keyArray, valueArray)
    }

    actual fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer,
        assumeTracked: Boolean
    ) {
        val keyArray = ByteArray(key.remaining())
        key.get(keyArray)
        val valueArray = ByteArray(value.remaining())
        value.get(valueArray)
        put(columnFamilyHandle, keyArray, valueArray, assumeTracked)
    }

    actual fun put(columnFamilyHandle: ColumnFamilyHandle, key: ByteBuffer, value: ByteBuffer) {
        put(columnFamilyHandle, key, value, assumeTracked = true)
    }

    actual fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray,
        assumeTracked: Boolean
    ) {
        wrapWithErrorThrower { error ->
            memScoped {
                rocksdb.rocksdb_transaction_merge_cf(
                    native,
                    columnFamilyHandle.native,
                    byteArrayToCPointer(key, 0, key.size),
                    key.size.convert(),
                    byteArrayToCPointer(value, 0, value.size),
                    value.size.convert(),
                    error
                )
            }
        }
    }

    actual fun merge(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray, value: ByteArray) {
        merge(columnFamilyHandle, key, value, assumeTracked = true)
    }

    actual fun merge(key: ByteArray, value: ByteArray) {
        wrapWithErrorThrower { error ->
            memScoped {
                rocksdb.rocksdb_transaction_merge(
                    native,
                    byteArrayToCPointer(key, 0, key.size),
                    key.size.convert(),
                    byteArrayToCPointer(value, 0, value.size),
                    value.size.convert(),
                    error
                )
            }
        }
    }

    actual fun merge(key: ByteBuffer, value: ByteBuffer) {
        val keyArray = ByteArray(key.remaining())
        key.get(keyArray)
        val valueArray = ByteArray(value.remaining())
        value.get(valueArray)
        merge(keyArray, valueArray)
    }

    actual fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteBuffer,
        value: ByteBuffer,
        assumeTracked: Boolean
    ) {
        val keyArray = ByteArray(key.remaining())
        key.get(keyArray)
        val valueArray = ByteArray(value.remaining())
        value.get(valueArray)
        merge(columnFamilyHandle, keyArray, valueArray, assumeTracked)
    }

    actual fun merge(columnFamilyHandle: ColumnFamilyHandle, key: ByteBuffer, value: ByteBuffer) {
        merge(columnFamilyHandle, key, value, assumeTracked = true)
    }

    actual fun delete(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray, assumeTracked: Boolean) {
        // Here we use the same native function as for tracked deletion.
        delete(columnFamilyHandle, key)
    }

    actual fun delete(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray) {
        wrapWithErrorThrower { error ->
            memScoped {
               rocksdb.rocksdb_transaction_delete_cf(
                    native,
                    columnFamilyHandle.native,
                    byteArrayToCPointer(key, 0, key.size),
                    key.size.convert(),
                    error
                )
            }
        }
    }

    actual fun delete(key: ByteArray) {
        wrapWithErrorThrower { error ->
            memScoped {
                rocksdb.rocksdb_transaction_delete(
                    native,
                    byteArrayToCPointer(key, 0, key.size),
                    key.size.convert(),
                    error
                )
            }
        }
    }

    actual fun putUntracked(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray, value: ByteArray) {
//        wrapWithErrorThrower { error ->
//            memScoped {
//                rocksdb.rocksdb_transaction_put_untracked_cf(
//                    native,
//                    columnFamilyHandle.native,
//                    byteArrayToCPointer(key, 0, key.size),
//                    key.size.convert(),
//                    byteArrayToCPointer(value, 0, value.size),
//                    value.size.convert(),
//                    error
//                )
//            }
//        }
        TODO()
    }

    actual fun putUntracked(key: ByteArray, value: ByteArray) {
//        wrapWithErrorThrower { error ->
//            memScoped {
//                rocksdb_transaction_put_untracked(
//                    native,
//                    byteArrayToCPointer(key, 0, key.size),
//                    key.size.convert(),
//                    byteArrayToCPointer(value, 0, value.size),
//                    value.size.convert(),
//                    error
//                )
//            }
//        }
        TODO()
    }

    actual fun putUntracked(
        columnFamilyHandle: ColumnFamilyHandle,
        keyParts: Array<ByteArray>,
        valueParts: Array<ByteArray>
    ) {
        val key = keyParts.reduce { acc, bytes -> acc + bytes }
        val value = valueParts.reduce { acc, bytes -> acc + bytes }
        putUntracked(columnFamilyHandle, key, value)
    }

    actual fun putUntracked(keyParts: Array<ByteArray>, valueParts: Array<ByteArray>) {
        val key = keyParts.reduce { acc, bytes -> acc + bytes }
        val value = valueParts.reduce { acc, bytes -> acc + bytes }
        putUntracked(key, value)
    }

    actual fun mergeUntracked(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray, value: ByteArray) {
//        wrapWithErrorThrower { error ->
//            memScoped {
//                rocksdb_transaction_merge_untracked_cf(
//                    native,
//                    columnFamilyHandle.native,
//                    byteArrayToCPointer(key, 0, key.size),
//                    key.size.convert(),
//                    byteArrayToCPointer(value, 0, value.size),
//                    value.size.convert(),
//                    error
//                )
//            }
//        }
        TODO()
    }

    actual fun mergeUntracked(key: ByteArray, value: ByteArray) {
//        wrapWithErrorThrower { error ->
//            memScoped {
//                rocksdb_transaction_merge_untracked(
//                    native,
//                    byteArrayToCPointer(key, 0, key.size),
//                    key.size.convert(),
//                    byteArrayToCPointer(value, 0, value.size),
//                    value.size.convert(),
//                    error
//                )
//            }
//        }
        TODO()
    }

    actual fun mergeUntracked(columnFamilyHandle: ColumnFamilyHandle, key: ByteBuffer, value: ByteBuffer) {
        val keyArray = ByteArray(key.remaining())
        key.get(keyArray)
        val valueArray = ByteArray(value.remaining())
        value.get(valueArray)
        mergeUntracked(columnFamilyHandle, keyArray, valueArray)
    }

    actual fun mergeUntracked(key: ByteBuffer, value: ByteBuffer) {
        val keyArray = ByteArray(key.remaining())
        key.get(keyArray)
        val valueArray = ByteArray(value.remaining())
        value.get(valueArray)
        mergeUntracked(keyArray, valueArray)
    }

    actual fun deleteUntracked(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray) {
//        wrapWithErrorThrower { error ->
//            memScoped {
//                rocksdb_transaction_delete_untracked_cf(
//                    native,
//                    columnFamilyHandle.native,
//                    byteArrayToCPointer(key, 0, key.size),
//                    key.size.convert(),
//                    error
//                )
//            }
//        }
        TODO()
    }

    actual fun deleteUntracked(key: ByteArray) {
//        wrapWithErrorThrower { error ->
//            memScoped {
//                rocksdb_transaction_delete_untracked(
//                    native,
//                    byteArrayToCPointer(key, 0, key.size),
//                    key.size.convert(),
//                    error
//                )
//            }
//        }
        TODO()
    }

    actual fun putLogData(logData: ByteArray) {
//        wrapWithErrorThrower { error ->
//            memScoped {
//                rocksdb.rocksdb_transaction_put_log_data(
//                    native,
//                    byteArrayToCPointer(logData, 0, logData.size),
//                    logData.size.convert(),
//                    error
//                )
//            }
//        }
        TODO()
    }

    actual fun disableIndexing() {
//        wrapWithErrorThrower { error ->
//            rocksdb.rocksdb_transaction_disable_indexing(native, error)
//        }
        TODO()
    }

    actual fun enableIndexing() {
//        wrapWithErrorThrower { error ->
//            rocksdb.rocksdb_transaction_enable_indexing(native, error)
//        }
        TODO()
    }

    actual fun getNumKeys(): Long {
//        return rocksdb.rocksdb_transaction_get_num_keys(native)
        TODO()
    }

    actual fun getNumPuts(): Long {
//        return rocksdb.rocksdb_transaction_get_num_puts(native)
        TODO()
    }

    actual fun getNumDeletes(): Long {
//        return rocksdb.rocksdb_transaction_get_num_deletes(native)
        TODO()
    }

    actual fun getNumMerges(): Long {
//        return rocksdb.rocksdb_transaction_get_num_merges(native)
        TODO()
    }

    actual fun getElapsedTime(): Long {
//        return rocksdb.rocksdb_transaction_get_elapsed_time(native)
        TODO()
    }

    actual fun getWriteBatch(): WriteBatchWithIndex {
//        val wbPtr = rocksdb.rocksdb_transaction_get_write_batch(native)
//        return WriteBatchWithIndex(wbPtr)
        TODO()
    }

    actual fun setLockTimeout(lockTimeout: Long) {
//        wrapWithErrorThrower { error ->
//            rocksdb.rocksdb_transaction_set_lock_timeout(native, lockTimeout.convert(), error)
//        }
        TODO()
    }

    actual fun getWriteOptions(): WriteOptions {
//        val optionsPtr = rocksdb.rocksdb_transaction_get_write_options(native)
//        return WriteOptions(optionsPtr)
        TODO()
    }

    actual fun setWriteOptions(writeOptions: WriteOptions) {
//        wrapWithErrorThrower { error ->
//            rocksdb.rocksdb_transaction_set_write_options(native, writeOptions.native, error)
//        }
        TODO()
    }

    actual fun undoGetForUpdate(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray) {
//        wrapWithErrorThrower { error ->
//            memScoped {
//                rocksdb.rocksdb_transaction_undo_get_for_update_cf(
//                    native,
//                    columnFamilyHandle.native,
//                    byteArrayToCPointer(key, 0, key.size),
//                    key.size.convert(),
//                    error
//                )
//            }
//        }
        TODO()
    }

    actual fun undoGetForUpdate(key: ByteArray) {
//        wrapWithErrorThrower { error ->
//            memScoped {
//                rocksdb.rocksdb_transaction_undo_get_for_update(
//                    native,
//                    byteArrayToCPointer(key, 0, key.size),
//                    key.size.convert(),
//                    error
//                )
//            }
//        }
        TODO()
    }

    actual fun rebuildFromWriteBatch(writeBatch: WriteBatch) {
//        wrapWithErrorThrower { error ->
//            rocksdb.rocksdb_transaction_rebuild_from_write_batch(native, writeBatch.native, error)
//        }
        TODO()
    }

    actual fun getCommitTimeWriteBatch(): WriteBatch {
//        val wbPtr = rocksdb.rocksdb_transaction_get_commit_time_write_batch(native)
//        return WriteBatch(wbPtr)
        TODO()
    }

    actual fun setLogNumber(logNumber: Long) {
//        wrapWithErrorThrower { error ->
//            rocksdb.rocksdb_transaction_set_log_number(native, logNumber.convert(), error)
//        }
        TODO()
    }

    actual fun getLogNumber(): Long {
//        return rocksdb.rocksdb_transaction_get_log_number(native)
        TODO()
    }

    actual fun setName(transactionName: String) {
        wrapWithErrorThrower { error ->
            transactionName.cstr.usePinned { pinned ->
                rocksdb.rocksdb_transaction_set_name(native, transactionName, transactionName.length.toULong(), error)
            }
        }
    }

    @ExperimentalForeignApi
    actual fun getName(): String = memScoped {
        val nameLenVar = alloc<ULongVar>()
        val namePtr = rocksdb.rocksdb_transaction_get_name(native, nameLenVar.ptr)
        namePtr?.toByteArray(nameLenVar.value)?.decodeToString() ?: ""
    }

    actual fun getID(): Long {
//        return rocksdb.rocksdb_transaction_get_id(native)
        TODO()
    }

    actual fun getId(): Long {
//        return getID()
        TODO()
    }

    actual fun isDeadlockDetect(): Boolean {
//        return rocksdb.rocksdb_transaction_is_deadlock_detect(native) != 0.toUByte()
        TODO()
    }

    actual fun getWaitingTxns(): WaitingTransactions {
//        val waitingPtr = rocksdb.rocksdb_transaction_get_waiting_txns(native)
//        return WaitingTransactions(waitingPtr)
        TODO()
    }

    actual fun getState(): TransactionState {
//        val state = rocksdb.rocksdb_transaction_get_state(native)
//        return TransactionState.fromInt(state)
        TODO()
    }
}
