package maryk.rocksdb

@Throws(RocksDBException::class)
actual fun openTransactionDB(
    dbOptions: DBOptions,
    transactionDbOptions: TransactionDBOptions,
    path: String,
    columnFamilyDescriptors: List<ColumnFamilyDescriptor>,
    columnFamilyHandles: MutableList<ColumnFamilyHandle>
): TransactionDB =
    org.rocksdb.TransactionDB.open(dbOptions, transactionDbOptions, path, columnFamilyDescriptors, columnFamilyHandles)

@Throws(RocksDBException::class)
actual fun openTransactionDB(
    options: Options,
    transactionDbOptions: TransactionDBOptions,
    path: String
): TransactionDB =
    org.rocksdb.TransactionDB.open(options, transactionDbOptions, path)
