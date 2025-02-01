package maryk.rocksdb

import kotlinx.cinterop.*

actual class StringAppendOperator actual constructor(
    delim: String
) : MergeOperator() {
    private val delimiter: String = delim

    actual constructor(delim: Char) : this(delim.toString())
    actual constructor() : this(",")

    override fun fullMerge(
        key: CPointer<ByteVarOf<Byte>>?, keyLen: ULong,
        existingValue: CPointer<ByteVarOf<Byte>>?, existingValueLen: ULong,
        operands: CPointer<CPointerVarOf<CPointer<ByteVarOf<Byte>>>>?,
        operandsLengths: CPointer<ULongVarOf<ULong>>?, numOperands: Int
    ): Pair<Boolean, Pair<CPointer<ByteVarOf<Byte>>?, ULong>> {
        val parts = mutableListOf<String>()
        // If an existing value is present, decode it as UTF-8.
        if (existingValue != null && existingValueLen > 0u) {
            val existingBytes = existingValue.readBytes(existingValueLen.toInt())
            parts.add(existingBytes.decodeToString())
        }
        // Decode each operand in order.
        if (operands != null && operandsLengths != null) {
            for (i in 0 until numOperands) {
                val operandPtr = operands[i]
                val operandLen = operandsLengths[i]
                if (operandPtr != null && operandLen > 0u) {
                    val operandBytes = operandPtr.readBytes(operandLen.toInt())
                    parts.add(operandBytes.decodeToString())
                }
            }
        }
        // Join all parts with the delimiter.
        val resultString = parts.joinToString(separator = delimiter)
        val resultBytes = resultString.encodeToByteArray()
        val resultLen = resultBytes.size.toULong()
        val merged = nativeHeap.allocArray<ByteVar>(resultBytes.size)
        for (i in resultBytes.indices) {
            merged[i] = resultBytes[i]
        }
        return Pair(true, Pair(merged, resultLen))
    }

    override fun partialMerge(
        key: CPointer<ByteVarOf<Byte>>?, keyLen: ULong,
        operands: CPointer<CPointerVarOf<CPointer<ByteVarOf<Byte>>>>?,
        operandsLengths: CPointer<ULongVarOf<ULong>>?, numOperands: Int
    ): Pair<Boolean, Pair<CPointer<ByteVarOf<Byte>>?, ULong>> {
        val parts = mutableListOf<String>()
        if (operands != null && operandsLengths != null) {
            for (i in 0 until numOperands) {
                val operandPtr = operands[i]
                val operandLen = operandsLengths[i]
                if (operandPtr != null && operandLen > 0u) {
                    val operandBytes = operandPtr.readBytes(operandLen.toInt())
                    parts.add(operandBytes.decodeToString())
                }
            }
        }
        if (parts.isEmpty()) {
            return Pair(false, Pair(null, 0uL))
        }
        val resultString = parts.joinToString(separator = delimiter)
        val resultBytes = resultString.encodeToByteArray()
        val resultLen = resultBytes.size.toULong()
        val merged = nativeHeap.allocArray<ByteVar>(resultBytes.size)

        for (i in resultBytes.indices) {
            merged[i] = resultBytes[i]
        }
        return Pair(true, Pair(merged, resultLen))
    }

    override fun name(): String {
        return "StringAppendOperator($delimiter)"
    }
}
