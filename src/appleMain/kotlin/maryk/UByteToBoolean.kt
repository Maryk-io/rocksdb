package maryk

private val TRUE = 1u.toUByte()
internal fun UByte.toBoolean() = when (this) {
    TRUE -> true
    else -> false
}
