package maryk.rocksdb

import maryk.assertContentEquals
import maryk.rocksdb.util.createTestDBFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KeyMayExistTest {
    init {
        loadRocksDBLibrary()
    }

    private fun createTestFolder() = createTestDBFolder("KeyMayExistTest")

    @Test
    fun keyMayExist() {
        val cfDescriptors = listOf(
            ColumnFamilyDescriptor(defaultColumnFamily),
            ColumnFamilyDescriptor("new_cf".encodeToByteArray())
        )

        val columnFamilyHandleList = mutableListOf<ColumnFamilyHandle>()
        DBOptions().apply {
            setCreateIfMissing(true)
            setCreateMissingColumnFamilies(true)
        }.use { options ->
            openRocksDB(
                options,
                createTestFolder(),
                cfDescriptors, columnFamilyHandleList
            ).use { db ->
                try {
                    assertEquals(2, columnFamilyHandleList.size)
                    db.put("key".encodeToByteArray(), "value".encodeToByteArray())
                    // Test without column family
                    var holder = Holder<ByteArray>()
                    var exists = db.keyMayExist("key".encodeToByteArray(), holder)
                    assertTrue(exists)
                    assertNotNull(holder.getValue())
                    assertEquals("value", holder.getValue()?.decodeToString())

                    // Slice key
                    val builder = StringBuilder("prefix")
                    val offset = builder.toString().length
                    builder.append("slice key 0")
                    val len = builder.toString().length - offset
                    builder.append("suffix")

                    val sliceKey = builder.toString().encodeToByteArray()
                    val sliceValue = "slice value 0".encodeToByteArray()
                    db.put(sliceKey, offset, len, sliceValue, 0, sliceValue.size)

                    exists = db.keyMayExist(sliceKey, offset, len, holder)
                    assertTrue(exists)
                    assertContentEquals(sliceValue, holder.getValue())

                    // Test without column family but with readOptions
                    ReadOptions().use { readOptions ->
                        exists = db.keyMayExist(readOptions, "key".encodeToByteArray(), holder)
                        assertTrue(exists)
                        assertNotNull(holder.getValue())
                        assertEquals("value", holder.getValue()?.decodeToString())

                        exists = db.keyMayExist(readOptions, sliceKey, offset, len, holder)
                        assertTrue(exists)
                        assertNotNull(holder.getValue())
                        assertContentEquals(sliceValue, holder.getValue())
                    }

                    // Test with column family
                    exists = db.keyMayExist(
                        columnFamilyHandleList[0], "key".encodeToByteArray(),
                        holder
                    )
                    assertTrue(exists)
                    assertNotNull(holder.getValue())
                    assertEquals("value", holder.getValue()?.decodeToString())

                    // Test slice sky with column family
                    holder = Holder()
                    exists = db.keyMayExist(
                        columnFamilyHandleList[0], sliceKey, offset, len,
                        holder
                    )
                    assertTrue(exists)
                    assertContentEquals(sliceValue, holder.getValue())

                    // Test with column family and readOptions
                    ReadOptions().use { readOptions ->
                        exists = db.keyMayExist(
                            columnFamilyHandleList[0], readOptions,
                            "key".encodeToByteArray(),
                            holder
                        )
                        assertTrue(exists)
                        assertEquals("value", holder.getValue()?.decodeToString())

                        // Test slice key with column family and read options
                        exists = db.keyMayExist(
                            columnFamilyHandleList[0], readOptions,
                            sliceKey, offset, len,
                            holder
                        )
                        assertTrue(exists)
                        assertContentEquals(sliceValue, holder.getValue())
                    }

                    // KeyMayExist in CF1 must return false
                    assertFalse(
                        db.keyMayExist(
                            columnFamilyHandleList[1],
                            "key".encodeToByteArray(), holder
                        )
                    )

                    // slice key
                    assertFalse(
                        db.keyMayExist(
                            columnFamilyHandleList[1],
                            sliceKey, 1, 3, holder
                        )
                    )
                } finally {
                    for (columnFamilyHandle in columnFamilyHandleList) {
                        columnFamilyHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun keyMayExistNonUnicodeString() {
        Options().apply {
            setCreateIfMissing(true)
            setCreateMissingColumnFamilies(true)
        }.use { options ->
            openRocksDB(
                options,
                createTestFolder()
            ).use { db ->
                val key = "key".encodeToByteArray()
                val value = byteArrayOf(0x80.toByte()) // invalid unicode code-point

                db.put(key, value)

                val buf = ByteArray(10)
                val read = db.get(key, buf)
                assertEquals(1, read)
                assertTrue { buf[0] == 0x80.toByte() }

                val holder = Holder<ByteArray>()
                var exists = db.keyMayExist(key, holder)
                assertTrue(exists)
                assertNotNull(holder.getValue())
                assertContentEquals(value, holder.getValue())
                exists = db.keyMayExist(key, null)
                assertTrue(exists)
            }
        }
    }
}
