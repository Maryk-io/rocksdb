package maryk.rocksdb

/**
 * Opens a `TransactionDB` instance with the specified options and database path.
 *
 * This method is analogous to [openRocksDB], but includes transaction-specific options.
 *
 * @param options An instance of [Options] to configure the database.
 * @param transactionDbOptions An instance of [TransactionDBOptions] to configure transaction behavior.
 * @param path The filesystem path where the database should be opened or created.
 *
 * @return A `TransactionDB` instance if the database is successfully opened; otherwise, `null`.
 *
 * @throws RocksDBException If an error occurs while opening the database.
 */
@Throws(RocksDBException::class)
expect fun openTransactionDB(options: Options, transactionDbOptions: TransactionDBOptions, path: String): TransactionDB

@Throws(RocksDBException::class)
expect fun openTransactionDB(
    dbOptions: DBOptions,
    transactionDbOptions: TransactionDBOptions,
    path: String,
    columnFamilyDescriptors: List<ColumnFamilyDescriptor>,
    columnFamilyHandles: MutableList<ColumnFamilyHandle>
): TransactionDB
