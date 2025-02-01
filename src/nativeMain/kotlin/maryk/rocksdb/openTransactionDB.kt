package maryk.rocksdb

import cnames.structs.rocksdb_column_family_handle_t
import cnames.structs.rocksdb_options_t
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import kotlinx.cinterop.toCValues
import maryk.wrapWithNullErrorThrower
import kotlin.collections.plusAssign

@Throws(RocksDBException::class)
actual fun openTransactionDB(
    dbOptions: DBOptions,
    transactionDbOptions: TransactionDBOptions,
    path: String,
    columnFamilyDescriptors: List<ColumnFamilyDescriptor>,
    columnFamilyHandles: MutableList<ColumnFamilyHandle>
): TransactionDB {
    if (columnFamilyDescriptors.find { it.getName().contentEquals(defaultColumnFamily) } == null) {
        throw IllegalArgumentException("Default column family descriptor should always be included")
    }

    return Unit.wrapWithNullErrorThrower { error ->
        memScoped {
            val optionsArray = allocArray<CPointerVar<rocksdb_options_t>>(columnFamilyDescriptors.size)
            val namesArray = allocArray<CPointerVar<ByteVar>>(columnFamilyDescriptors.size)

            columnFamilyDescriptors.forEachIndexed { index, cfDesc ->
                namesArray[index] = cfDesc.getName().toCValues().ptr
                optionsArray[index] = cfDesc.getOptions().native
            }

            val handles = allocArray<CPointerVar<rocksdb_column_family_handle_t>>(columnFamilyDescriptors.size)

            rocksdb.rocksdb_transactiondb_open_column_families(
                dbOptions.native,
                transactionDbOptions.native,
                path,
                columnFamilyDescriptors.size,
                namesArray,
                optionsArray,
                handles,
                error,
            )?.let(::TransactionDB).also {
                for (i in columnFamilyDescriptors.indices) {
                    columnFamilyHandles += ColumnFamilyHandle(handles[i]!!)
                }
            }
        }
    } ?: throw RocksDBException("No Database could be opened at $path with given descriptors and handles for column families")
}

@Throws(RocksDBException::class)
actual fun openTransactionDB(
    options: Options,
    transactionDbOptions: TransactionDBOptions,
    path: String
): TransactionDB =
    Unit.wrapWithNullErrorThrower { error ->
        rocksdb.rocksdb_transactiondb_open(options.native, transactionDbOptions.native, path, error)?.let(::TransactionDB)
    } ?: throw RocksDBException("No Database could be opened at $path")
