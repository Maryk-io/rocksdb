package maryk.rocksdb

actual enum class TxnDBWritePolicy(val value: Byte) {
    WRITE_COMMITTED(0x00),

    /**
     * Write data after the prepare phase of 2pc.
     */
    WRITE_PREPARED(0x1),

    /**
     * Write data before the prepare phase of 2pc.
     */
    WRITE_UNPREPARED(0x2);

    actual fun getValue(): Byte = value
}


fun getTxnDBWritePolicy(value: Byte): TxnDBWritePolicy {
    for (level in TxnDBWritePolicy.entries) {
        if (level.value == value) {
            return level
        }
    }
    throw IllegalArgumentException("Illegal value provided for TxnDBWritePolicy.")
}
