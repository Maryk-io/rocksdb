@file:OptIn(ExperimentalNativeApi::class)

package maryk.rocksdb

import cnames.structs.rocksdb_mergeoperator_t
import kotlinx.cinterop.*
import platform.posix.memcpy
import kotlin.experimental.ExperimentalNativeApi

/**
 * Base (actual) merge operator implementation.
 *
 * This implementation now uses a persistent pinned name similar to AbstractComparator.
 */
actual abstract class MergeOperator : RocksObject() {
    // Allocate and persist the operator's name on the native heap.
    internal val pinnedName: CPointer<ByteVar> by lazy {
        val actualName = name()
        // Append a null terminator.
        val nameBytes = (actualName + "\u0000").encodeToByteArray()
        val mem = nativeHeap.allocArray<ByteVar>(nameBytes.size)
        nameBytes.usePinned { pinned ->
            memcpy(mem, pinned.addressOf(0), nameBytes.size.convert())
        }
        mem
    }

    val native: CPointer<rocksdb_mergeoperator_t>

    init {
        // Create a stable reference to this instance so that C callbacks can find it.
        val stableRef = StableRef.create(this)
        native = rocksdb.rocksdb_mergeoperator_create(
            state = stableRef.asCPointer(),
            destructor = staticCFunction(::mergeOperatorDestructor),
            full_merge = staticCFunction(::fullMergeCallback),
            partial_merge = staticCFunction(::partialMergeCallback),
            delete_value = staticCFunction(::deleteValueCallback),
            name = staticCFunction(::mergeOperatorNameCallback)
        ) ?: error("Failed to create merge operator")
    }

    // Override close() to free our pinned name and destroy the native object.
    override fun close() {
        if (isOwningHandle()) {
            // Free the memory allocated for the merge operator's name.
            nativeHeap.free(pinnedName.rawValue)
            // Destroy the native merge operator. Adjust this function call if needed.
            rocksdb.rocksdb_mergeoperator_destroy(native)
            super.close()
        }
    }

    /**
     * Full merge: by default returns a copy of the last operand (or the existing value).
     */
    open fun fullMerge(
        key: CPointer<ByteVarOf<Byte>>?, keyLen: ULong,
        existingValue: CPointer<ByteVarOf<Byte>>?, existingValueLen: ULong,
        operands: CPointer<CPointerVarOf<CPointer<ByteVarOf<Byte>>>>?,
        operandsLengths: CPointer<ULongVarOf<ULong>>?, numOperands: Int
    ): Pair<Boolean, Pair<CPointer<ByteVarOf<Byte>>?, ULong>> {
        if (numOperands > 0 && operands != null && operandsLengths != null) {
            val lastIndex = numOperands - 1
            val operandPtr = (operands + lastIndex)!!.pointed.value
            val operandLen = (operandsLengths + lastIndex)!!.pointed.value
            val merged = nativeHeap.allocArray<ByteVar>(operandLen.toInt())
            memcpy(merged, operandPtr, operandLen)
            return Pair(true, Pair(merged, operandLen))
        } else if (existingValue != null) {
            val merged = nativeHeap.allocArray<ByteVar>(existingValueLen.toInt())
            memcpy(merged, existingValue, existingValueLen)
            return Pair(true, Pair(merged, existingValueLen))
        } else {
            return Pair(false, Pair(null, 0uL))
        }
    }

    /**
     * Partial merge: by default returns a copy of the last operand.
     */
    open fun partialMerge(
        key: CPointer<ByteVarOf<Byte>>?, keyLen: ULong,
        operands: CPointer<CPointerVarOf<CPointer<ByteVarOf<Byte>>>>?,
        operandsLengths: CPointer<ULongVarOf<ULong>>?, numOperands: Int
    ): Pair<Boolean, Pair<CPointer<ByteVarOf<Byte>>?, ULong>> {
        if (numOperands > 0 && operands != null && operandsLengths != null) {
            val lastIndex = numOperands - 1
            val operandPtr = (operands + lastIndex)!!.pointed.value
            val operandLen = (operandsLengths + lastIndex)!!.pointed.value
            val merged = nativeHeap.allocArray<ByteVar>(operandLen.toInt())
            memcpy(merged, operandPtr, operandLen)
            return Pair(true, Pair(merged, operandLen))
        } else {
            return Pair(false, Pair(null, 0uL))
        }
    }

    /**
     * Returns the name of the merge operator.
     */
    open fun name(): String = "DefaultMergeOperator"

    /**
     * Called when the merge operator is being destroyed.
     * Default implementation does nothing.
     */
    open fun destructor() {
        // No instance state to clean up.
    }

    /**
     * Called to delete a previously-merged value.
     * Default implementation frees the native heap allocation.
     */
    open fun deleteValue(value: CPointer<ByteVarOf<Byte>>?, valueLen: ULong) {
        if (value != null) {
            nativeHeap.free(value)
        }
    }
}

// --- C Callback Implementations ---

private fun mergeOperatorDestructor(state: CPointer<out CPointed>?) {
    state?.asStableRef<MergeOperator>()?.get()?.destructor()
    state?.asStableRef<MergeOperator>()?.dispose()
}

private fun fullMergeCallback(
    state: CPointer<out CPointed>?,
    key: CPointer<ByteVarOf<Byte>>?, keyLen: ULong,
    existingValue: CPointer<ByteVarOf<Byte>>?, existingValueLen: ULong,
    operands: CPointer<CPointerVarOf<CPointer<ByteVarOf<Byte>>>>?,
    operandsLengths: CPointer<ULongVarOf<ULong>>?, numOperands: Int,
    success: CPointer<UByteVarOf<UByte>>?, newValueLength: CPointer<ULongVarOf<ULong>>?
): CPointer<ByteVarOf<Byte>>? {
    val instance = state?.asStableRef<MergeOperator>()?.get() ?: return null
    val (succeeded, resultPair) = instance.fullMerge(key, keyLen, existingValue, existingValueLen, operands, operandsLengths, numOperands)
    success?.pointed?.value = if (succeeded) 1u else 0u
    if (resultPair.first != null) {
        newValueLength?.pointed?.value = resultPair.second
    }
    return resultPair.first
}

private fun partialMergeCallback(
    state: CPointer<out CPointed>?,
    key: CPointer<ByteVarOf<Byte>>?, keyLen: ULong,
    operands: CPointer<CPointerVarOf<CPointer<ByteVarOf<Byte>>>>?,
    operandsLengths: CPointer<ULongVarOf<ULong>>?, numOperands: Int,
    success: CPointer<UByteVarOf<UByte>>?, newValueLength: CPointer<ULongVarOf<ULong>>?
): CPointer<ByteVarOf<Byte>>? {
    val instance = state?.asStableRef<MergeOperator>()?.get() ?: return null
    val (succeeded, resultPair) = instance.partialMerge(key, keyLen, operands, operandsLengths, numOperands)
    success?.pointed?.value = if (succeeded) 1u else 0u
    if (resultPair.first != null) {
        newValueLength?.pointed?.value = resultPair.second
    }
    return resultPair.first
}

// Updated callback: now simply returns the persistent pinnedName.
private fun mergeOperatorNameCallback(state: CPointer<out CPointed>?): CPointer<ByteVarOf<Byte>>? {
    val instance = state?.asStableRef<MergeOperator>()?.get()
    return instance?.pinnedName
}

private fun deleteValueCallback(
    state: CPointer<out CPointed>?,
    value: CPointer<ByteVarOf<Byte>>?,
    valueLen: ULong
) {
    state?.asStableRef<MergeOperator>()?.get()?.deleteValue(value, valueLen)
}
