package maryk.rocksdb

actual class WaitingTransactions(
    val columnFamilyId: Long,
    val key: String,
    val transactionIds: LongArray,
){

    actual fun getColumnFamilyId(): Long = columnFamilyId
    actual fun getKey(): String = key
    actual fun getTransactionIds(): LongArray = transactionIds
}
