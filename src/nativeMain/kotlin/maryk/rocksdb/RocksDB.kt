@file:OptIn(ExperimentalNativeApi::class)

package maryk.rocksdb

import kotlinx.cinterop.ptr
import cnames.structs.rocksdb_column_family_handle_t
import cnames.structs.rocksdb_column_family_metadata_t
import cnames.structs.rocksdb_t
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import maryk.byteArrayToCPointer
import maryk.toBoolean
import maryk.toByteArray
import maryk.toUByte
import maryk.wrapWithErrorThrower
import maryk.wrapWithMultiErrorThrower
import maryk.wrapWithNullErrorThrower2
import platform.posix.size_tVar
import platform.posix.uint64_tVar
import rocksdb.rocksdb_close
import rocksdb.rocksdb_column_family_metadata_get_level_count
import rocksdb.rocksdb_column_family_metadata_get_level_metadata
import rocksdb.rocksdb_compact_range
import rocksdb.rocksdb_compact_range_cf
import rocksdb.rocksdb_compact_range_cf_opt
import rocksdb.rocksdb_create_column_family
import rocksdb.rocksdb_create_iterator
import rocksdb.rocksdb_create_iterator_cf
import rocksdb.rocksdb_delete
import rocksdb.rocksdb_delete_cf
import rocksdb.rocksdb_delete_file
import rocksdb.rocksdb_delete_range_cf
import rocksdb.rocksdb_destroy_db
import rocksdb.rocksdb_disable_file_deletions
import rocksdb.rocksdb_drop_column_family
import rocksdb.rocksdb_enable_file_deletions
import rocksdb.rocksdb_flush_wal
import rocksdb.rocksdb_get
import rocksdb.rocksdb_get_cf
import rocksdb.rocksdb_get_column_family_metadata
import rocksdb.rocksdb_get_default_column_family_handle
import rocksdb.rocksdb_get_latest_sequence_number
import rocksdb.rocksdb_key_may_exist
import rocksdb.rocksdb_key_may_exist_cf
import rocksdb.rocksdb_level_metadata_destroy
import rocksdb.rocksdb_level_metadata_get_file_count
import rocksdb.rocksdb_level_metadata_get_level
import rocksdb.rocksdb_level_metadata_get_size
import rocksdb.rocksdb_level_metadata_get_sst_file_metadata
import rocksdb.rocksdb_list_column_families
import rocksdb.rocksdb_list_column_families_destroy
import rocksdb.rocksdb_merge
import rocksdb.rocksdb_merge_cf
import rocksdb.rocksdb_multi_get
import rocksdb.rocksdb_multi_get_cf
import rocksdb.rocksdb_property_int
import rocksdb.rocksdb_property_int_cf
import rocksdb.rocksdb_put
import rocksdb.rocksdb_put_cf
import rocksdb.rocksdb_sst_file_metadata_destroy
import rocksdb.rocksdb_sst_file_metadata_get_directory
import rocksdb.rocksdb_sst_file_metadata_get_relative_filename
import rocksdb.rocksdb_sst_file_metadata_get_size
import rocksdb.rocksdb_sst_file_metadata_get_smallestkey
import rocksdb.rocksdb_write
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.min

actual val defaultColumnFamily = "default".encodeToByteArray()
actual val rocksDBNotFound = -1

actual open class RocksDB
internal constructor(
    internal val native: CPointer<rocksdb_t>,
)
    : RocksObject() {
    private val defaultReadOptions = ReadOptions()
    private val defaultWriteOptions = WriteOptions()

    actual fun getName(): String {
        return "IMPLEMENT"
    }

    actual override fun close() {
        if (isOwningHandle()) {
            defaultReadOptions.close()
            defaultWriteOptions.close()

            rocksdb_close(native)
            super.close()
        }
    }

    actual fun closeE() {
        if (isOwningHandle()) {
            rocksdb_close(native)
            super.close()
        }
    }

    actual fun createColumnFamily(columnFamilyDescriptor: ColumnFamilyDescriptor): ColumnFamilyHandle =
        wrapWithErrorThrower { error ->
            ColumnFamilyHandle(
                rocksdb_create_column_family(native, columnFamilyDescriptor.getOptions().native, columnFamilyDescriptor.getName().toCValues(), error)!!,
            )
        }

    actual fun createColumnFamilies(
        columnFamilyOptions: ColumnFamilyOptions,
        columnFamilyNames: List<ByteArray>
    ): List<ColumnFamilyHandle> = wrapWithErrorThrower { error ->
        buildList {
            for (name in columnFamilyNames) {
                this += ColumnFamilyHandle(
                    rocksdb_create_column_family(native, columnFamilyOptions.native, name.toCValues(), error)!!,
                )
            }
        }
    }

    actual fun createColumnFamilies(columnFamilyDescriptors: List<ColumnFamilyDescriptor>): List<ColumnFamilyHandle> =
        wrapWithErrorThrower { error ->
            buildList {
                for (descriptor in columnFamilyDescriptors) {
                    rocksdb_create_column_family(
                        native,
                        descriptor.getOptions().native,
                        descriptor.getName().toCValues(),
                        error
                    )?.let(::ColumnFamilyHandle)?.let(::add)
                }
            }
        }

    actual fun dropColumnFamily(columnFamilyHandle: ColumnFamilyHandle) {
        wrapWithErrorThrower { error ->
            rocksdb_drop_column_family(native, columnFamilyHandle.native, error)
        }
    }

    actual fun dropColumnFamilies(columnFamilies: List<ColumnFamilyHandle>) {
//        wrapWithErrorThrower { error ->
//            native.dropColumnFamilies(columnFamilies.map { it.native }, error)
//        }
    }

    actual fun put(key: ByteArray, value: ByteArray) {
        wrapWithErrorThrower { error ->
            rocksdb_put(native, defaultWriteOptions.native, key.toCValues(), key.size.toULong(), value.toCValues(), value.size.toULong(), error)
        }
    }

    actual fun put(
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ) {
        wrapWithErrorThrower { error ->
            memScoped {
                rocksdb_put(
                    native,
                    defaultWriteOptions.native,
                    byteArrayToCPointer(key, offset, len),
                    len.toULong(),
                    byteArrayToCPointer(value, vOffset, vLen),
                    vLen.toULong(),
                    error
                )
            }
        }
    }

    actual fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray
    ) {
        wrapWithErrorThrower { error ->
            rocksdb_put_cf(
                native,
                defaultWriteOptions.native,
                columnFamilyHandle.native,
                key.toCValues(),
                key.size.toULong(),
                value.toCValues(),
                value.size.toULong(),
                error
            )
        }
    }

    actual fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ) {
        put(columnFamilyHandle, defaultWriteOptions, key, offset, len, value, vOffset, vLen)
    }

    actual fun put(writeOpts: WriteOptions, key: ByteArray, value: ByteArray) {
        wrapWithErrorThrower { error ->
            rocksdb_put(
                native,
                writeOpts.native,
                key.toCValues(),
                key.size.toULong(),
                value.toCValues(),
                value.size.toULong(),
                error
            )
        }
    }

    actual fun put(
        writeOpts: WriteOptions,
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ) {
        wrapWithErrorThrower { error ->
            memScoped {
                rocksdb_put(
                    native,
                    defaultWriteOptions.native,
                    byteArrayToCPointer(key, offset, len),
                    len.toULong(),
                    byteArrayToCPointer(value, vOffset, vLen),
                    vLen.toULong(),
                    error
                )
            }
        }
    }

    actual fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpts: WriteOptions,
        key: ByteArray,
        value: ByteArray
    ) {
        wrapWithErrorThrower { error ->
            rocksdb_put_cf(
                native,
                writeOpts.native,
                columnFamilyHandle.native,
                key.toCValues(),
                key.size.toULong(),
                value.toCValues(),
                value.size.toULong(),
                error
            )
        }
    }

    actual fun put(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpts: WriteOptions,
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ) {
        wrapWithErrorThrower { error ->
            memScoped {
                rocksdb_put_cf(
                    native,
                    writeOpts.native,
                    columnFamilyHandle.native,
                    byteArrayToCPointer(key, offset, len),
                    len.toULong(),
                    byteArrayToCPointer(value, vOffset, vLen),
                    vLen.toULong(),
                    error
                )
            }
        }
    }

    actual fun delete(key: ByteArray) {
        delete(defaultWriteOptions, key)
    }

    actual fun delete(key: ByteArray, offset: Int, len: Int) {
        delete(defaultWriteOptions, key, offset, len)
    }

    actual fun delete(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray) {
        delete(columnFamilyHandle, defaultWriteOptions, key)
    }

    actual fun delete(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        offset: Int,
        len: Int
    ) {
        delete(columnFamilyHandle, defaultWriteOptions, key, offset, len)
    }

    actual fun delete(writeOpt: WriteOptions, key: ByteArray) {
        wrapWithErrorThrower { error ->
            rocksdb_delete(
                native,
                writeOpt.native,
                key.toCValues(),
                key.size.toULong(),
                error
            )
        }
    }

    actual fun delete(
        writeOpt: WriteOptions,
        key: ByteArray,
        offset: Int,
        len: Int
    ) {
        wrapWithErrorThrower { error ->
            memScoped {
                rocksdb_delete(
                    native,
                    writeOpt.native,
                    byteArrayToCPointer(key, offset, len),
                    len.toULong(),
                    error
                )
            }
        }
    }

    actual fun delete(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpt: WriteOptions,
        key: ByteArray
    ) {
        wrapWithErrorThrower { error ->
            rocksdb_delete_cf(
                native,
                writeOpt.native,
                columnFamilyHandle.native,
                key.toCValues(),
                key.size.toULong(),
                error
            )
        }
    }

    actual fun delete(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpt: WriteOptions,
        key: ByteArray,
        offset: Int,
        len: Int
    ) {
        wrapWithErrorThrower { error ->
            memScoped {
                rocksdb_delete_cf(
                    native,
                    writeOpt.native,
                    columnFamilyHandle.native,
                    byteArrayToCPointer(key, offset, len),
                    len.toULong(),
                    error
                )
            }
        }
    }

    actual fun deleteRange(beginKey: ByteArray, endKey: ByteArray) {
        deleteRange(defaultWriteOptions, beginKey, endKey)
    }

    actual fun deleteRange(
        columnFamilyHandle: ColumnFamilyHandle,
        beginKey: ByteArray,
        endKey: ByteArray
    ) {
        deleteRange(columnFamilyHandle, defaultWriteOptions, beginKey, endKey)
    }

    actual fun deleteRange(writeOpt: WriteOptions, beginKey: ByteArray, endKey: ByteArray) {
        wrapWithErrorThrower { error ->
            val default = rocksdb_get_default_column_family_handle(native)
            rocksdb_delete_range_cf(
                db = native,
                options = writeOpt.native,
                column_family = default,
                start_key = beginKey.toCValues(),
                start_key_len = beginKey.size.toULong(),
                end_key = endKey.toCValues(),
                end_key_len = endKey.size.toULong(),
                errptr = error,
            )
            rocksdb.rocksdb_column_family_handle_destroy(default)
        }
    }

    actual fun deleteRange(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpt: WriteOptions,
        beginKey: ByteArray,
        endKey: ByteArray
    ) {
        wrapWithErrorThrower { error ->
            rocksdb_delete_range_cf(
                db = native,
                options = writeOpt.native,
                column_family = columnFamilyHandle.native,
                start_key = beginKey.toCValues(),
                start_key_len = beginKey.size.toULong(),
                end_key = endKey.toCValues(),
                end_key_len = endKey.size.toULong(),
                errptr = error,
            )
        }
    }

    actual fun merge(key: ByteArray, value: ByteArray) {
        merge(defaultWriteOptions, key, value)
    }

    actual fun merge(
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ) {
        merge(defaultWriteOptions, key, offset, len, value, vOffset, vLen)
    }

    actual fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray
    ) {
        merge(columnFamilyHandle, defaultWriteOptions, key, value)
    }

    actual fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ) {
        merge(columnFamilyHandle, defaultWriteOptions, key, offset, len, value, vOffset, vLen)
    }

    actual fun merge(writeOpts: WriteOptions, key: ByteArray, value: ByteArray) {
        wrapWithErrorThrower { error ->
            rocksdb_merge(
                native,
                writeOpts.native,
                key.toCValues(),
                key.size.toULong(),
                value.toCValues(),
                value.size.toULong(),
                error,
            )
        }
    }

    actual fun merge(
        writeOpts: WriteOptions,
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ) {
        memScoped {
            wrapWithErrorThrower { error ->
                rocksdb_merge(
                    native,
                    writeOpts.native,
                    byteArrayToCPointer(key, offset, len),
                    len.toULong(),
                    byteArrayToCPointer(value, vOffset, vLen),
                    vLen.toULong(),
                    error,
                )
            }
        }
    }

    actual fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpts: WriteOptions,
        key: ByteArray,
        value: ByteArray
    ) {
        wrapWithErrorThrower { error ->
            rocksdb_merge_cf(
                native,
                writeOpts.native,
                columnFamilyHandle.native,
                key.toCValues(),
                key.size.toULong(),
                value.toCValues(),
                value.size.toULong(),
                error,
            )
        }
    }

    actual fun merge(
        columnFamilyHandle: ColumnFamilyHandle,
        writeOpts: WriteOptions,
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ) {
        memScoped {
            wrapWithErrorThrower { error ->
                rocksdb_merge_cf(
                    native,
                    defaultWriteOptions.native,
                    columnFamilyHandle.native,
                    byteArrayToCPointer(key, offset, len),
                    len.toULong(),
                    byteArrayToCPointer(value, vOffset, vLen),
                    vLen.toULong(),
                    error,
                )
            }
        }
    }

    actual fun write(writeOpts: WriteOptions, updates: WriteBatch) {
        wrapWithErrorThrower { error ->
            rocksdb_write(native, writeOpts.native, updates.native, error)
        }
    }

    actual fun get(key: ByteArray, value: ByteArray): Int = wrapWithNullErrorThrower2 { error ->
        memScoped {
            val valueLength = alloc<size_tVar>()
            val result = rocksdb_get(native, defaultReadOptions.native, key.toCValues(), key.size.toULong(), valueLength.ptr, error)

            val length = valueLength.value.toInt()

            result?.let {
                for (index in 0 until min(length, value.size)) {
                    value[index] = result[index]
                }
                length
            }.also {
                rocksdb.rocksdb_free(result)
            }
        }
    } ?: rocksDBNotFound

    actual fun get(
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ): Int {
        memScoped {
            val valueLength = alloc<size_tVar>()
            return wrapWithNullErrorThrower2 { error ->
                rocksdb_get(
                    native,
                    defaultReadOptions.native,
                    byteArrayToCPointer(key, offset, len),
                    len.toULong(),
                    valueLength.ptr,
                    error
                ).also {
                    rocksdb.rocksdb_free(it)
                }
                valueLength.value.toInt()
            } ?: rocksDBNotFound
        }
    }

    actual fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        value: ByteArray
    ): Int =
        get(columnFamilyHandle, defaultReadOptions, key, value)

    actual fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ): Int =
        get(columnFamilyHandle, defaultReadOptions, key, offset, len, value, vOffset, vLen)

    actual fun get(opt: ReadOptions, key: ByteArray, value: ByteArray): Int = wrapWithNullErrorThrower2 { error ->
        memScoped {
            val valueLength = alloc<size_tVar>()
            val result = rocksdb_get(native, opt.native, key.toCValues(), key.size.toULong(), valueLength.ptr, error)

            val length = valueLength.value.toInt()

            result?.let {
                for (index in 0 until min(length, value.size)) {
                    value[index] = result[index]
                }
                length
            }
        }
    } ?: rocksDBNotFound

    actual fun get(
        opt: ReadOptions,
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ): Int {
        memScoped {
            val valueLength = alloc<size_tVar>()
            return wrapWithNullErrorThrower2 { error ->
                val retrieved = rocksdb_get(
                    native,
                    opt.native,
                    byteArrayToCPointer(key, offset, len),
                    key.size.toULong(),
                    valueLength.ptr,
                    error
                )
                if (retrieved != null) {
                    val length = valueLength.value.toInt()

                    for (index in 0 until min(length, vLen)) {
                        value[index + vOffset] = retrieved[index]
                    }

                    length
                } else rocksDBNotFound
            } ?: rocksDBNotFound
        }
    }

    actual fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        opt: ReadOptions,
        key: ByteArray,
        value: ByteArray
    ): Int {
        memScoped {
            val valueLength = alloc<size_tVar>()
            return wrapWithNullErrorThrower2 { error ->
                val retrieved = rocksdb_get_cf(
                    native,
                    defaultReadOptions.native,
                    columnFamilyHandle.native,
                    key.toCValues(),
                    key.size.toULong(),
                    valueLength.ptr,
                    error
                )
                if (retrieved != null) {
                    val length = valueLength.value.toInt()

                    for (index in 0 until min(length, value.size)) {
                        value[index] = retrieved[index]
                    }

                    rocksdb.rocksdb_free(retrieved)

                    length
                } else rocksDBNotFound
            } ?: rocksDBNotFound
        }
    }

    actual fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        opt: ReadOptions,
        key: ByteArray,
        offset: Int,
        len: Int,
        value: ByteArray,
        vOffset: Int,
        vLen: Int
    ): Int {
        memScoped {
            val valueLength = alloc<size_tVar>()
            return wrapWithNullErrorThrower2 { error ->
                rocksdb_get_cf(
                    native,
                    opt.native,
                    columnFamilyHandle.native,
                    key.toCValues(),
                    key.size.toULong(),
                    valueLength.ptr,
                    error
                ).also {
                    rocksdb.rocksdb_free(it)
                }
                valueLength.value.toInt()
            } ?: rocksDBNotFound
        }
    }

    actual operator fun get(key: ByteArray): ByteArray? =
        get(defaultReadOptions, key)

    actual fun get(key: ByteArray, offset: Int, len: Int): ByteArray? =
        get(defaultReadOptions, key, offset, len)

    actual fun get(columnFamilyHandle: ColumnFamilyHandle, key: ByteArray): ByteArray? =
        get(columnFamilyHandle, defaultReadOptions, key)

    actual fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        offset: Int,
        len: Int
    ): ByteArray? = get(columnFamilyHandle, defaultReadOptions, key, offset, len)

    actual fun get(opt: ReadOptions, key: ByteArray): ByteArray? =
        wrapWithNullErrorThrower2 { error ->
            memScoped {
                val valueLength = alloc<size_tVar>()
                val result = rocksdb_get(native, opt.native, key.toCValues(), key.size.toULong(), valueLength.ptr, error)

                result?.toByteArray(valueLength.value).also {
                    rocksdb.rocksdb_free(result)
                }
            }
        }

    actual fun get(
        opt: ReadOptions,
        key: ByteArray,
        offset: Int,
        len: Int
    ): ByteArray? = wrapWithNullErrorThrower2 { error ->
        memScoped {
            val valueLength = alloc<size_tVar>()
            val result = rocksdb_get(
                native,
                opt.native,
                byteArrayToCPointer(key, offset, len),
                len.toULong(),
                valueLength.ptr,
                error
            )

            result?.toByteArray(valueLength.value).also {
                rocksdb.rocksdb_free(result)
            }
        }
    }

    actual fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        opt: ReadOptions,
        key: ByteArray
    ): ByteArray? = wrapWithNullErrorThrower2 { error ->
        memScoped {
            val valueLength = alloc<size_tVar>()
            val result = rocksdb_get_cf(native, opt.native, columnFamilyHandle.native, key.toCValues(), key.size.toULong(), valueLength.ptr, error)

            result?.toByteArray(valueLength.value).also {
                rocksdb.rocksdb_free(result)
            }
        }
    }

    actual fun get(
        columnFamilyHandle: ColumnFamilyHandle,
        opt: ReadOptions,
        key: ByteArray,
        offset: Int,
        len: Int
    ): ByteArray? {
        return wrapWithNullErrorThrower2 { error ->
            memScoped {
                val valueLength = alloc<size_tVar>()
                val result = rocksdb_get_cf(
                    native,
                    opt.native,
                    columnFamilyHandle.native,
                    byteArrayToCPointer(key, offset, len),
                    len.toULong(),
                    valueLength.ptr,
                    error
                )

                result?.toByteArray(valueLength.value).also {
                    rocksdb.rocksdb_free(result)
                }
            }
        }
    }

    actual fun multiGetAsList(keys: List<ByteArray>): List<ByteArray?> =
        multiGetAsList(defaultReadOptions, keys)

    actual fun multiGetAsList(
        columnFamilyHandleList: List<ColumnFamilyHandle>,
        keys: List<ByteArray>
    ): List<ByteArray?> = multiGetAsList(defaultReadOptions, columnFamilyHandleList, keys)

    actual fun multiGetAsList(
        opt: ReadOptions,
        keys: List<ByteArray>
    ): List<ByteArray?> {
        assert(keys.isNotEmpty())

        return wrapWithMultiErrorThrower(keys.size) { error ->
            memScoped {
                val keyList = allocArray<CPointerVar<ByteVar>>(keys.size)
                val keyListSizes = allocArray<size_tVar>(keys.size)

                keys.forEachIndexed { index, bytes ->
                    keyList[index] = bytes.toCValues().ptr
                    keyListSizes[index] = bytes.size.toULong()
                }

                val valueList = allocArray<CPointerVar<ByteVar>>(keys.size)
                val valueListSizes = allocArray<size_tVar>(keys.size)

                rocksdb_multi_get(
                    db = native,
                    options = opt.native,
                    num_keys = keys.size.toULong(),
                    keys_list = keyList,
                    keys_list_sizes = keyListSizes,
                    values_list = valueList,
                    values_list_sizes = valueListSizes,
                    errs = error,
                )

                buildList(keys.size) {
                    keys.indices.forEach { index ->
                        this += valueList[index]?.toByteArray(valueListSizes[index])
                    }
                }
            }
        } ?: emptyList()
    }

    @OptIn(ExperimentalStdlibApi::class)
    actual fun multiGetAsList(
        opt: ReadOptions,
        columnFamilyHandleList: List<ColumnFamilyHandle>,
        keys: List<ByteArray>
    ): List<ByteArray?> {
        if (columnFamilyHandleList.isEmpty()) {
            throw IllegalArgumentException("columnFamilyHandleList is empty")
        }
        assert(keys.isNotEmpty())

        return wrapWithMultiErrorThrower(keys.size) { error ->
            memScoped {
                val columnFamilies = allocArray<CPointerVar<rocksdb_column_family_handle_t>>(columnFamilyHandleList.size)
                columnFamilyHandleList.forEachIndexed { i, handle ->
                    columnFamilies[i] = handle.native
                }

                val keyList = allocArray<CPointerVar<ByteVar>>(keys.size)
                val keyListSizes = allocArray<size_tVar>(keys.size)

                keys.forEachIndexed { index, bytes ->
                    keyList[index] = bytes.toCValues().ptr
                    keyListSizes[index] = bytes.size.toULong()
                }

                val valueList = allocArray<CPointerVar<ByteVar>>(keys.size)
                val valueListSizes = allocArray<size_tVar>(keys.size)

                rocksdb_multi_get_cf(
                    db = native,
                    options = opt.native,
                    column_families = columnFamilies,
                    num_keys = keys.size.toULong(),
                    keys_list = keyList,
                    keys_list_sizes = keyListSizes,
                    values_list = valueList,
                    values_list_sizes = valueListSizes,
                    errs = error,
                )

                buildList(keys.size) {
                    keys.indices.forEach { index ->
                        this += valueList[index]?.toByteArray(valueListSizes[index])
                    }
                }
            }
        } ?: emptyList()
    }

    actual fun keyMayExist(key: ByteArray, valueHolder: Holder<ByteArray>?): Boolean =
        keyMayExist(defaultReadOptions, key, valueHolder)

    actual fun keyMayExist(
        key: ByteArray,
        offset: Int,
        len: Int,
        valueHolder: Holder<ByteArray>?
    ): Boolean =
        keyMayExist(defaultReadOptions, key, offset, len, valueHolder)

    actual fun keyMayExist(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        valueHolder: Holder<ByteArray>?
    ): Boolean =
        keyMayExist(columnFamilyHandle, defaultReadOptions, key, valueHolder)

    actual fun keyMayExist(
        columnFamilyHandle: ColumnFamilyHandle,
        key: ByteArray,
        offset: Int,
        len: Int,
        valueHolder: Holder<ByteArray>?
    ): Boolean =
        keyMayExist(columnFamilyHandle, defaultReadOptions, key, offset, len, valueHolder)

    actual fun keyMayExist(
        readOptions: ReadOptions,
        key: ByteArray,
        valueHolder: Holder<ByteArray>?
    ): Boolean {
        memScoped {
            val value = alloc<CPointerVar<ByteVar>>()
            val valueLength = alloc<size_tVar>()
            val timestamp = alloc<ByteVar>()
            val timestampLength = alloc<size_tVar>()
            val valueFound = alloc<UByteVar>()
            rocksdb_key_may_exist(
                native,
                readOptions.native,
                key.toCValues(),
                key.size.toULong(),
                value.ptr,
                valueLength.ptr,
                timestamp.ptr,
                timestampLength.value,
                valueFound.ptr,
            )
            valueHolder?.setValue(value.value?.toByteArray(valueLength.value))
            return valueFound.value.toBoolean()
        }
    }

    actual fun keyMayExist(
        readOptions: ReadOptions,
        key: ByteArray,
        offset: Int,
        len: Int,
        valueHolder: Holder<ByteArray>?
    ): Boolean {
        memScoped {
            val value = alloc<CPointerVar<ByteVar>>()
            val valueLength = alloc<size_tVar>()
            val timestamp = alloc<ByteVar>()
            val timestampLength = alloc<size_tVar>()
            val valueFound = alloc<UByteVar>()
            rocksdb_key_may_exist(
                native,
                readOptions.native,
                byteArrayToCPointer(key, offset, len),
                len.toULong(),
                value.ptr,
                valueLength.ptr,
                timestamp.ptr,
                timestampLength.value,
                valueFound.ptr,
            )
            valueHolder?.setValue(value.value?.toByteArray(valueLength.value))
            return valueFound.value.toBoolean()
        }
    }

    actual fun keyMayExist(
        columnFamilyHandle: ColumnFamilyHandle,
        readOptions: ReadOptions,
        key: ByteArray,
        valueHolder: Holder<ByteArray>?
    ): Boolean {
        memScoped {
            val value = alloc<CPointerVar<ByteVar>>()
            val valueLength = alloc<size_tVar>()
            val timestamp = alloc<ByteVar>()
            val timestampLength = alloc<size_tVar>()
            val valueFound = alloc<UByteVar>()
            rocksdb_key_may_exist_cf(
                native,
                readOptions.native,
                columnFamilyHandle.native,
                key.toCValues(),
                key.size.toULong(),
                value.ptr,
                valueLength.ptr,
                timestamp.ptr,
                timestampLength.value,
                valueFound.ptr,
            )
            valueHolder?.setValue(value.value?.toByteArray(valueLength.value))
            // valueFound is unreliable value so using valueLength
            return valueLength.value > 0uL
        }
    }

    actual fun keyMayExist(
        columnFamilyHandle: ColumnFamilyHandle,
        readOptions: ReadOptions,
        key: ByteArray,
        offset: Int,
        len: Int,
        valueHolder: Holder<ByteArray>?
    ): Boolean {
        memScoped {
            val cKey = allocArray<ByteVar>(len)
            for (i in (0 until len)) {
                cKey[i] = key[i + offset]
            }

            val value = alloc<CPointerVar<ByteVar>>()
            val valueLength = alloc<size_tVar>()
            val timestamp = alloc<ByteVar>()
            val timestampLength = alloc<size_tVar>()
            val valueFound = alloc<UByteVar>()
            rocksdb_key_may_exist_cf(
                native,
                readOptions.native,
                columnFamilyHandle.native,
                cKey,
                len.toULong(),
                value.ptr,
                valueLength.ptr,
                timestamp.ptr,
                timestampLength.value,
                valueFound.ptr,
            )
            valueHolder?.setValue(value.value?.toByteArray(valueLength.value))
            // valueFound is unreliable value so using valueLength
            return valueLength.value > 0uL
        }
    }

    actual fun newIterator(): RocksIterator = newIterator(defaultReadOptions)

    actual fun newIterator(readOptions: ReadOptions): RocksIterator =
        RocksIterator(
            rocksdb_create_iterator(native, defaultReadOptions.native)!!,
        )

    actual fun newIterator(columnFamilyHandle: ColumnFamilyHandle): RocksIterator =
        newIterator(columnFamilyHandle, defaultReadOptions)

    actual fun newIterator(
        columnFamilyHandle: ColumnFamilyHandle,
        readOptions: ReadOptions
    ): RocksIterator = RocksIterator(
        rocksdb_create_iterator_cf(native, readOptions.native, columnFamilyHandle.native)!!,
    )

    actual fun newIterators(columnFamilyHandleList: List<ColumnFamilyHandle>): List<RocksIterator> {
        return wrapWithErrorThrower { error ->
            columnFamilyHandleList.map { handle ->
                this.newIterator(handle)
            }
        }
    }

    actual fun newIterators(
        columnFamilyHandleList: List<ColumnFamilyHandle>,
        readOptions: ReadOptions
    ): List<RocksIterator> {
        return wrapWithErrorThrower { error ->
            columnFamilyHandleList.map { handle ->
                this.newIterator(handle, readOptions)
            }
        }
    }

    actual fun getSnapshot(): Snapshot? {
        throw NotImplementedError("DO SOMETHING")
//        return Snapshot(native.snapshot())
    }

    actual fun releaseSnapshot(snapshot: Snapshot) {
        throw NotImplementedError("DO SOMETHING")
//        snapshot.native.close()
    }

    actual fun getProperty(
        columnFamilyHandle: ColumnFamilyHandle,
        property: String
    ): String? {
        throw NotImplementedError("DO SOMETHING")
//        return native.valueForProperty(property, columnFamilyHandle.native)
    }

    actual fun getProperty(property: String): String? {
        throw NotImplementedError("DO SOMETHING")
//        return native.valueForProperty(property)
    }

    actual fun getMapProperty(property: String): Map<String, String> {
//        @Suppress("UNCHECKED_CAST")
//        return native.valueForMapProperty(property) as Map<String, String>
        throw NotImplementedError("DO SOMETHING")
    }

    actual fun getMapProperty(
        columnFamilyHandle: ColumnFamilyHandle,
        property: String
    ): Map<String, String> {
        throw NotImplementedError("DO SOMETHING")
//        @Suppress("UNCHECKED_CAST")
//        return native.valueForMapProperty(property, columnFamilyHandle.native) as Map<String, String>
    }

    actual fun getLongProperty(property: String): Long {
        memScoped {
            val outValue = alloc<uint64_tVar>()
            rocksdb_property_int(
                native,
                property,
                outValue.ptr,
            )

            return outValue.value.toLong()
        }
    }

    actual fun getLongProperty(
        columnFamilyHandle: ColumnFamilyHandle,
        property: String
    ): Long {
        memScoped {
            val outValue = alloc<uint64_tVar>()
            rocksdb_property_int_cf(
                native,
                columnFamilyHandle.native,
                property,
                outValue.ptr,
            )
            return outValue.value.toLong()
        }
    }

    actual fun resetStats() {
        throw NotImplementedError("DO SOMETHING")
//        wrapWithErrorThrower { error ->
//            native.resetStats(error)
//        }
    }

    actual fun compactRange() {
        rocksdb_compact_range(
            native,
            null,
            0u,
            null,
            0u,
        )
    }

    actual fun compactRange(columnFamilyHandle: ColumnFamilyHandle) {
        rocksdb_compact_range_cf(
            native,
            columnFamilyHandle.native,
            null,
            0u,
            null,
            0u,
        )
    }

    actual fun compactRange(begin: ByteArray, end: ByteArray) {
        rocksdb_compact_range(
            native,
            begin.toCValues(),
            begin.size.toULong(),
            end.toCValues(),
            end.size.toULong(),
        )
    }

    actual fun compactRange(
        columnFamilyHandle: ColumnFamilyHandle,
        begin: ByteArray,
        end: ByteArray
    ) {
        rocksdb_compact_range_cf(
            native,
            columnFamilyHandle.native,
            begin.toCValues(),
            begin.size.toULong(),
            end.toCValues(),
            end.size.toULong(),
        )
    }

    actual fun compactRange(
        columnFamilyHandle: ColumnFamilyHandle,
        begin: ByteArray,
        end: ByteArray,
        compactRangeOptions: CompactRangeOptions
    ) {
        rocksdb_compact_range_cf_opt(
            native,
            columnFamilyHandle.native,
            compactRangeOptions.native,
            begin.toCValues(),
            begin.size.toULong(),
            end.toCValues(),
            end.size.toULong(),
        )
    }

    actual fun pauseBackgroundWork() {
        throw NotImplementedError("DO SOMETHING")
//        wrapWithErrorThrower { error ->
//            native.pauseBackgroundWork(error)
//        }
    }

    actual fun continueBackgroundWork() {
        throw NotImplementedError("DO SOMETHING")
//        wrapWithErrorThrower { error ->
//            native.continueBackgroundWork(error)
//        }
    }

    actual fun enableAutoCompaction(columnFamilyHandles: List<ColumnFamilyHandle>) {
        throw NotImplementedError("DO SOMETHING")
//        wrapWithErrorThrower { error ->
//            native.enableAutoCompaction(columnFamilyHandles.map { it.native }, error)
//        }
    }

    actual fun numberLevels(): Int {
        throw NotImplementedError("DO SOMETHING")
//        return native.numberLevels()
    }

    actual fun numberLevels(columnFamilyHandle: ColumnFamilyHandle): Int {
        throw NotImplementedError("DO SOMETHING")
//        return native.numberLevelsInColumnFamily(columnFamilyHandle.native)
    }

    actual fun maxMemCompactionLevel(): Int {
        throw NotImplementedError("DO SOMETHING")
//        return native.maxMemCompactionLevel()
    }

    actual fun maxMemCompactionLevel(columnFamilyHandle: ColumnFamilyHandle): Int {
        throw NotImplementedError("DO SOMETHING")
//        return native.maxMemCompactionLevelInColumnFamily(columnFamilyHandle.native)
    }

    actual fun level0StopWriteTrigger(): Int {
        throw NotImplementedError("DO SOMETHING")
//        return native.level0StopWriteTrigger()
    }

    actual fun level0StopWriteTrigger(columnFamilyHandle: ColumnFamilyHandle): Int {
        throw NotImplementedError("DO SOMETHING")
//        return native.level0StopWriteTriggerInColumnFamily(columnFamilyHandle.native)
    }

    actual fun getEnv(): Env {
        return getDefaultEnv()
    }

    actual fun flushWal(sync: Boolean) {
        wrapWithErrorThrower { error ->
            rocksdb_flush_wal(native, sync.toUByte(), error)
        }
    }

    actual fun syncWal() {
        throw NotImplementedError("DO SOMETHING")
//        wrapWithErrorThrower { error ->
//            native.syncWal(error)
//        }
    }

    actual fun getLatestSequenceNumber(): Long =
        rocksdb_get_latest_sequence_number(native).toLong()

    actual fun disableFileDeletions() {
        wrapWithErrorThrower { error ->
            rocksdb_disable_file_deletions(native, error)
        }
    }

    actual fun enableFileDeletions() {
        wrapWithErrorThrower { error ->
            rocksdb_enable_file_deletions(native, error)
        }
    }

    actual fun deleteFile(name: String) {
        rocksdb_delete_file(native, name)
    }

    internal fun processColumnFamilyMetaData(metaData: CPointer<rocksdb_column_family_metadata_t>?): ColumnFamilyMetaData {
        val levelCount = rocksdb_column_family_metadata_get_level_count(metaData)

        val levels = memScoped {
            buildList<LevelMetaData> {
                for (i in 0uL until levelCount) {
                    val levelData = rocksdb_column_family_metadata_get_level_metadata(metaData, i)!!
                    val count = rocksdb_level_metadata_get_file_count(levelData)

                    val files =
                        buildList<SstFileMetaData> {
                            for (i in 0uL until count) {
                                val sstMetaData = rocksdb_level_metadata_get_sst_file_metadata(levelData, i)!!
                                val smallestKeyLength = this@memScoped.alloc<uint64_tVar>()
                                val largestKeyLength = this@memScoped.alloc<uint64_tVar>()

                                val fileName = rocksdb_sst_file_metadata_get_relative_filename(sstMetaData)
                                val directory = rocksdb_sst_file_metadata_get_directory(sstMetaData)
                                val smallestKey = rocksdb_sst_file_metadata_get_smallestkey(sstMetaData, smallestKeyLength.ptr)
                                val largestKey = rocksdb_sst_file_metadata_get_smallestkey(sstMetaData, largestKeyLength.ptr)
                                add(
                                    SstFileMetaData(
                                        fileName = fileName!!.toKString(),
                                        path = directory!!.toKString(),
                                        size = rocksdb_sst_file_metadata_get_size(sstMetaData),
                                        smallestKey = smallestKey!!.toByteArray(smallestKeyLength.value),
                                        largestKey = largestKey!!.toByteArray(largestKeyLength.value),
                                    )
                                )
                                rocksdb.rocksdb_free(fileName)
                                rocksdb.rocksdb_free(directory)
                                rocksdb.rocksdb_free(smallestKey)
                                rocksdb.rocksdb_free(largestKey)
                                rocksdb_sst_file_metadata_destroy(sstMetaData)
                            }
                        }
                    add(
                        LevelMetaData(
                            level = rocksdb_level_metadata_get_level(levelData),
                            size = rocksdb_level_metadata_get_size(levelData),
                            files = files,
                        )
                    )

                    rocksdb_level_metadata_destroy(levelData)
                }
            }
        }

        val name = rocksdb.rocksdb_column_family_metadata_get_name(metaData)

        return ColumnFamilyMetaData(
            size = rocksdb.rocksdb_column_family_metadata_get_size(metaData),
            fileCount = rocksdb.rocksdb_column_family_metadata_get_file_count(metaData),
            name = name!!.toKString(),
            levels = levels
        ).also {
            rocksdb.rocksdb_free(name)
            rocksdb.rocksdb_column_family_metadata_destroy(metaData)
        }
    }

    actual fun getColumnFamilyMetaData(columnFamilyHandle: ColumnFamilyHandle): ColumnFamilyMetaData {
        val metaData = rocksdb.rocksdb_get_column_family_metadata_cf(native, columnFamilyHandle.native)

        return processColumnFamilyMetaData(metaData)
    }

    actual fun getColumnFamilyMetaData(): ColumnFamilyMetaData {
        val metaData = rocksdb_get_column_family_metadata(native)

        return processColumnFamilyMetaData(metaData)
    }

    actual fun verifyChecksum() {
        throw NotImplementedError("DO SOMETHING")
//        wrapWithErrorThrower { error ->
//            native.verifyChecksum(error)
//        }
    }

    actual fun getDefaultColumnFamily() = ColumnFamilyHandle(
        rocksdb_get_default_column_family_handle(native)!!
    )

    actual fun promoteL0(columnFamilyHandle: ColumnFamilyHandle, targetLevel: Int) {
        throw NotImplementedError("DO SOMETHING")
//        wrapWithErrorThrower { error ->
//            native.promoteL0(targetLevel, columnFamilyHandle.native, error)
//        }
    }

    actual fun promoteL0(targetLevel: Int) {
        throw NotImplementedError("DO SOMETHING")
//        wrapWithErrorThrower { error ->
//            native.promoteL0(targetLevel, error)
//        }
    }
}

actual fun destroyRocksDB(path: String, options: Options) {
    Unit.wrapWithErrorThrower { error ->
        rocksdb_destroy_db(options.native, path, error)
    }
}

actual fun listColumnFamilies(
    options: Options,
    path: String
): List<ByteArray> {
    return Unit.wrapWithErrorThrower { error ->
        memScoped {
            val cfCount = alloc<size_tVar>()
            val values = rocksdb_list_column_families(options.native, path, cfCount.ptr, error)!!

            buildList {
                for (i in 0uL until cfCount.value) {
                    values[i.toInt()]?.toKString()?.encodeToByteArray()?.let(::add)
                }
            }.also {
                rocksdb_list_column_families_destroy(values, cfCount.value)
            }
        }
    }
}
