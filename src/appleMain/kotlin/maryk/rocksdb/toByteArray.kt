package maryk.rocksdb

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.get
import platform.posix.size_t

internal fun CPointer<ByteVar>.toByteArray(value: size_t) =
    ByteArray(value.toInt()) { i ->
        this[i]
    }
