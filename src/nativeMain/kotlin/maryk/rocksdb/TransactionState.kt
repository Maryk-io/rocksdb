package maryk.rocksdb

actual enum class TransactionState(
    val value: Byte
) {
    STARTED(0),
    AWAITING_PREPARE(1),
    PREPARED(2),
    AWAITING_COMMIT(3),
    COMMITTED(4),
    AWAITING_ROLLBACK(5),
    ROLLEDBACK(6),
    LOCKS_STOLEN(7);
}

fun getTransactionState(value: Byte): TransactionState {
    return TransactionState.entries.firstOrNull { it.value == value }
        ?: throw IllegalArgumentException("Illegal value provided for TransactionState.")
}
