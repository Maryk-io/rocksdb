package maryk

import kotlin.test.assertTrue
import kotlin.test.fail

fun assertContains(results: Collection<ByteArray?>, toMatch: ByteArray?) {
    for (result in results) {
        if (result == null) {
            if (toMatch == null) return else break
        } else {
            if (toMatch != null && result.contentEquals(toMatch)) return
        }
    }
    fail("Results do not contain ${toMatch?.decodeToString()}.")
}

fun assertContains(results: Collection<ByteArray?>, vararg toMatch: ByteArray?) {
    results@for (match in toMatch) {
        assertContains(results, match)
    }
}

fun assertContainsExactly(results: Collection<ByteArray?>, vararg toMatch: ByteArray?) {
    if (results.size != toMatch.size) fail("Results do not match exactly $toMatch")
    assertContains(results, *toMatch)
}

fun <T> assertContainsExactly(results: Collection<T?>, vararg toMatch: T?) {
    if (results.size != toMatch.size) fail("Results do not match exactly: $results and $toMatch")
    assertTrue("Results do not match exactly: $results and $toMatch") {
        results.containsAll(toMatch.toSet())
    }
}
