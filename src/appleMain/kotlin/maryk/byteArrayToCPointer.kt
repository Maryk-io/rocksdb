package maryk

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.set

fun MemScope.byteArrayToCPointer(
    key: ByteArray,
    offset: Int,
    len: Int
): CArrayPointer<ByteVar> {
    val cKey = allocArray<ByteVar>(len)
    for (i in (0 until len)) {
        cKey[i] = key[i + offset]
    }
    return cKey
}
