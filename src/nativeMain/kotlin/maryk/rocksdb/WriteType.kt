package maryk.rocksdb

import maryk.rocksdb.WriteType.DELETE
import maryk.rocksdb.WriteType.DELETE_RANGE
import maryk.rocksdb.WriteType.LOG
import maryk.rocksdb.WriteType.MERGE
import maryk.rocksdb.WriteType.PUT
import maryk.rocksdb.WriteType.SINGLE_DELETE
import maryk.rocksdb.WriteType.XID

actual enum class WriteType(
    internal val value: UByte
) {
    PUT(0u),
    MERGE(1u),
    DELETE(2u),
    SINGLE_DELETE(3u),
    DELETE_RANGE(4u),
    LOG(5u),
    XID(6u);
}

internal fun getWriteTypeByValue(value: UByte): WriteType {
    return when (value) {
        PUT.value -> PUT
        MERGE.value -> MERGE
        DELETE.value -> DELETE
        SINGLE_DELETE.value -> SINGLE_DELETE
        DELETE_RANGE.value -> DELETE_RANGE
        LOG.value -> LOG
        XID.value -> XID
        else -> throw NotImplementedError()
    }
}
