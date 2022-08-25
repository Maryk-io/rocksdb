package maryk.rocksdb

import maryk.assertContains
import maryk.assertContentEquals
import maryk.rocksdb.CompressionType.BZLIB2_COMPRESSION
import maryk.rocksdb.util.createTestDBFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ColumnFamilyTest {
    private fun createTestFolder() = createTestDBFolder("ColumnFamilyTest")

    @Test
    fun columnFamilyDescriptorName() {
        val cfName = "some_name".encodeToByteArray()

        ColumnFamilyOptions().use { cfOptions ->
            val cfDescriptor = ColumnFamilyDescriptor(cfName, cfOptions)
            assertContentEquals(cfName, cfDescriptor.getName())
        }
    }

    @Test
    fun columnFamilyDescriptorOptions() {
        val cfName = "some_name".encodeToByteArray()

        ColumnFamilyOptions()
            .setCompressionType(BZLIB2_COMPRESSION).use { cfOptions ->
                val cfDescriptor = ColumnFamilyDescriptor(cfName, cfOptions)

                assertEquals(BZLIB2_COMPRESSION, cfDescriptor.getOptions().compressionType())
            }
    }

    @Test
    fun elistColumnFamilies() {
        val testFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(
                options,
                testFolder
            ).use {
                // Test listColumnFamilies
                val columnFamilyNames = listColumnFamilies(
                    options,
                    testFolder
                )
                assertNotNull(columnFamilyNames)
                assertEquals(1, columnFamilyNames.size)
                assertContentEquals("default".encodeToByteArray(), columnFamilyNames[0])
            }
        }
    }

    @Test
    fun defaultColumnFamily() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(
                options,
                createTestFolder()
            ).use { db ->
                val cfh = db.getDefaultColumnFamily()
                try {
                    assertNotNull(cfh)

                    assertContentEquals("default".encodeToByteArray(), cfh.getName())
                    assertEquals(0, cfh.getID())

                    val key = "key".encodeToByteArray()
                    val value = "value".encodeToByteArray()

                    db.put(cfh, key, value)

                    val actualValue = db.get(cfh, key)

                    assertNotNull(cfh)
                    assertContentEquals(value, actualValue)
                } finally {
                    cfh.close()
                }
            }
        }
    }

    @Test
    fun createColumnFamily() {
        val cfName = "new_cf".encodeToByteArray()
        val cfDescriptor = ColumnFamilyDescriptor(
            cfName,
            ColumnFamilyOptions()
        )

        Options().setCreateIfMissing(true).use { options ->
            val testFolder = createTestFolder()

            openRocksDB(
                options,
                testFolder
            ).use { db ->
                val columnFamilyHandle = db.createColumnFamily(cfDescriptor)

                try {
                    assertContentEquals(cfName, columnFamilyHandle.getName())
                    assertEquals(1, columnFamilyHandle.getID())

                    val columnFamilyNames = listColumnFamilies(
                        options, testFolder
                    )
                    assertNotNull(columnFamilyNames)
                    assertEquals(2, columnFamilyNames.size)
                    assertContentEquals("default".encodeToByteArray(), columnFamilyNames[0])
                    assertContentEquals("new_cf".encodeToByteArray(), columnFamilyNames[1])
                } finally {
                    columnFamilyHandle.close()
                }
            }
        }
    }

    @Test
    fun openWithColumnFamilies() {
        val cfNames = listOf(
            ColumnFamilyDescriptor(defaultColumnFamily),
            ColumnFamilyDescriptor("new_cf".encodeToByteArray())
        )

        val columnFamilyHandleList = mutableListOf<ColumnFamilyHandle>()

        // Test open database with column family names
        DBOptions()
            .setCreateIfMissing(true)
            .setCreateMissingColumnFamilies(true).use { options ->
                openRocksDB(
                    options,
                    createTestFolder(), cfNames,
                    columnFamilyHandleList
                ).use { db ->
                    try {
                        assertEquals(2, columnFamilyHandleList.size)
                        db.put("dfkey1".encodeToByteArray(), "dfvalue".encodeToByteArray())
                        db.put(
                            columnFamilyHandleList[0], "dfkey2".encodeToByteArray(),
                            "dfvalue".encodeToByteArray()
                        )
                        db.put(
                            columnFamilyHandleList[1], "newcfkey1".encodeToByteArray(),
                            "newcfvalue".encodeToByteArray()
                        )

                        val retVal = db.get(
                            columnFamilyHandleList[1],
                            "newcfkey1".encodeToByteArray()
                        )
                        assertContentEquals("newcfvalue".encodeToByteArray(), retVal)
                        assertNull(
                            (db.get(
                                columnFamilyHandleList[1],
                                "dfkey1".encodeToByteArray()
                            ))
                        )
                        db.delete(columnFamilyHandleList[1], "newcfkey1".encodeToByteArray())
                        assertNull(
                            (db.get(
                                columnFamilyHandleList[1],
                                "newcfkey1".encodeToByteArray()
                            ))
                        )
                        db.delete(
                            columnFamilyHandleList[0], WriteOptions(),
                            "dfkey2".encodeToByteArray()
                        )
                        assertNull(
                            db.get(
                                columnFamilyHandleList[0], ReadOptions(),
                                "dfkey2".encodeToByteArray()
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
    fun openWithByteArrayColumnFamilies() {
        val cfNames = listOf(
            ColumnFamilyDescriptor(defaultColumnFamily),
            ColumnFamilyDescriptor(byteArrayOf(0, 1)),
            ColumnFamilyDescriptor(byteArrayOf(0, 2))
        )

        val columnFamilyHandleList = mutableListOf<ColumnFamilyHandle>()

        // Test open database with column family names
        DBOptions()
            .setCreateIfMissing(true)
            .setCreateMissingColumnFamilies(true).use { options ->
                openRocksDB(
                    options,
                    createTestFolder(), cfNames,
                    columnFamilyHandleList
                ).use {
                    try {
                        assertEquals(3, columnFamilyHandleList.size)

                        assertContentEquals(cfNames[0].getName(), columnFamilyHandleList[0].getName())
                        assertContentEquals(cfNames[1].getName(), columnFamilyHandleList[1].getName())
                        assertContentEquals(cfNames[2].getName(), columnFamilyHandleList[2].getName())
                    } finally {
                        for (columnFamilyHandle in columnFamilyHandleList) {
                            columnFamilyHandle.close()
                        }
                    }
                }
            }
    }

    @Test
    fun getWithOutValueAndCf() {
        val cfDescriptors = listOf(
            ColumnFamilyDescriptor(defaultColumnFamily)
        )
        val columnFamilyHandleList = mutableListOf<ColumnFamilyHandle>()

        // Test open database with column family names
        DBOptions().apply {
            setCreateIfMissing(true)
            setCreateMissingColumnFamilies(true)
        }.use { options ->
            openRocksDB(
                options,
                createTestFolder(), cfDescriptors,
                columnFamilyHandleList
            ).use { db ->
                try {
                    db.put(
                        columnFamilyHandleList[0], WriteOptions(),
                        "key1".encodeToByteArray(), "value".encodeToByteArray()
                    )
                    db.put("key2".encodeToByteArray(), "12345678".encodeToByteArray())
                    val outValue = ByteArray(5)
                    // not found value
                    var getResult = db.get("keyNotFound".encodeToByteArray(), outValue)
                    assertEquals(rocksDBNotFound, getResult)
                    // found value which fits in outValue
                    getResult = db.get(
                        columnFamilyHandleList[0], "key1".encodeToByteArray(),
                        outValue
                    )
                    assertNotEquals(rocksDBNotFound, getResult)
                    assertContentEquals("value".encodeToByteArray(), outValue)
                    // found value which fits partially
                    getResult = db.get(
                        columnFamilyHandleList[0], ReadOptions(),
                        "key2".encodeToByteArray(), outValue
                    )
                    assertNotEquals(rocksDBNotFound, getResult)
                    assertContentEquals("12345".encodeToByteArray(), outValue)
                } finally {
                    for (columnFamilyHandle in columnFamilyHandleList) {
                        columnFamilyHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun createWriteDropColumnFamily() {
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
                createTestFolder(), cfDescriptors,
                columnFamilyHandleList
            ).use { db ->
                var tmpColumnFamilyHandle: ColumnFamilyHandle? = null
                try {
                    tmpColumnFamilyHandle = db.createColumnFamily(
                        ColumnFamilyDescriptor(
                            "tmpCF".encodeToByteArray(),
                            ColumnFamilyOptions()
                        )
                    )
                    db.put(tmpColumnFamilyHandle, "key".encodeToByteArray(), "value".encodeToByteArray())
                    db.dropColumnFamily(tmpColumnFamilyHandle)
                } finally {
                    tmpColumnFamilyHandle?.close()
                    for (columnFamilyHandle in columnFamilyHandleList) {
                        columnFamilyHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun createWriteDropColumnFamilies() {
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
                createTestFolder(), cfDescriptors,
                columnFamilyHandleList
            ).use { db ->
                var tmpColumnFamilyHandle: ColumnFamilyHandle? = null
                var tmpColumnFamilyHandle2: ColumnFamilyHandle? = null
                try {
                    tmpColumnFamilyHandle = db.createColumnFamily(
                        ColumnFamilyDescriptor(
                            "tmpCF".encodeToByteArray(),
                            ColumnFamilyOptions()
                        )
                    )
                    tmpColumnFamilyHandle2 = db.createColumnFamily(
                        ColumnFamilyDescriptor(
                            "tmpCF2".encodeToByteArray(),
                            ColumnFamilyOptions()
                        )
                    )
                    db.put(tmpColumnFamilyHandle, "key".encodeToByteArray(), "value".encodeToByteArray())
                    db.put(tmpColumnFamilyHandle2, "key".encodeToByteArray(), "value".encodeToByteArray())
                    db.dropColumnFamilies(listOf(tmpColumnFamilyHandle, tmpColumnFamilyHandle2))
                } finally {
                    tmpColumnFamilyHandle?.close()
                    tmpColumnFamilyHandle2?.close()
                    for (columnFamilyHandle in columnFamilyHandleList) {
                        columnFamilyHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun iteratorOnColumnFamily() {
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
                    db.put(
                        columnFamilyHandleList[1], "newcfkey".encodeToByteArray(),
                        "value".encodeToByteArray()
                    )
                    db.put(
                        columnFamilyHandleList[1], "newcfkey2".encodeToByteArray(),
                        "value2".encodeToByteArray()
                    )
                    db.newIterator(columnFamilyHandleList[1]).use { rocksIterator ->
                        rocksIterator.seekToFirst()
                        val refMap = HashMap<String, String>()
                        refMap["newcfkey"] = "value"
                        refMap["newcfkey2"] = "value2"
                        var i = 0
                        while (rocksIterator.isValid()) {
                            i++
                            assertEquals(
                                rocksIterator.value().decodeToString(),
                                refMap[rocksIterator.key().decodeToString()]
                            )
                            rocksIterator.next()
                        }
                        assertEquals(2, i)
                    }
                } finally {
                    for (columnFamilyHandle in columnFamilyHandleList) {
                        columnFamilyHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun multiGetAsList() {
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
                    db.put(
                        columnFamilyHandleList[0], "key".encodeToByteArray(),
                        "value".encodeToByteArray()
                    )
                    db.put(
                        columnFamilyHandleList[1], "newcfkey".encodeToByteArray(),
                        "value".encodeToByteArray()
                    )

                    val keys = listOf("key".encodeToByteArray(), "newcfkey".encodeToByteArray())
                    var retValues = db.multiGetAsList(
                        columnFamilyHandleList,
                        keys
                    )
                    assertEquals(2, retValues.size)
                    assertContentEquals("value".encodeToByteArray(), retValues[0])
                    assertContentEquals("value".encodeToByteArray(), retValues[1])
                    retValues = db.multiGetAsList(
                        ReadOptions(), columnFamilyHandleList,
                        keys
                    )
                    assertEquals(2, retValues.size)
                    assertContentEquals("value".encodeToByteArray(), retValues[0])
                    assertContentEquals("value".encodeToByteArray(), retValues[1])
                } finally {
                    for (columnFamilyHandle in columnFamilyHandleList) {
                        columnFamilyHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun iterators() {
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
                createTestFolder(), cfDescriptors,
                columnFamilyHandleList
            ).use { db ->
                var iterators: List<RocksIterator>? = null
                try {
                    iterators = db.newIterators(columnFamilyHandleList)
                    assertEquals(2, iterators.size)
                    var iter = iterators[0]
                    iter.seekToFirst()
                    val defRefMap = HashMap<String, String>()
                    defRefMap["dfkey1"] = "dfvalue"
                    defRefMap["key"] = "value"
                    while (iter.isValid()) {
                        assertEquals(iter.value().decodeToString(), defRefMap[iter.key().decodeToString()])
                        iter.next()
                    }
                    // iterate over new_cf key/value pairs
                    val cfRefMap = HashMap<String, String>()
                    cfRefMap["newcfkey"] = "value"
                    cfRefMap["newcfkey2"] = "value2"
                    iter = iterators[1]
                    iter.seekToFirst()
                    while (iter.isValid()) {
                        assertEquals(iter.value().decodeToString(), cfRefMap[iter.key().decodeToString()])
                        iter.next()
                    }
                } finally {
                    if (iterators != null) {
                        for (rocksIterator in iterators) {
                            rocksIterator.close()
                        }
                    }
                    for (columnFamilyHandle in columnFamilyHandleList) {
                        columnFamilyHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun failPutDisposedCF() {
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
                    db.dropColumnFamily(columnFamilyHandleList[1])
                    assertFailsWith<RocksDBException> {
                        db.put(
                            columnFamilyHandleList[1], "key".encodeToByteArray(),
                            "value".encodeToByteArray()
                        )
                    }
                } finally {
                    for (columnFamilyHandle in columnFamilyHandleList) {
                        columnFamilyHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun failRemoveDisposedCF() {
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
                    db.dropColumnFamily(columnFamilyHandleList[1])
                    assertFailsWith<RocksDBException> {
                        db.delete(columnFamilyHandleList[1], "key".encodeToByteArray())
                    }
                } finally {
                    for (columnFamilyHandle in columnFamilyHandleList) {
                        columnFamilyHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun failMultiGetWithoutCorrectNumberOfCF() {
        val cfDescriptors = listOf(
            ColumnFamilyDescriptor(defaultColumnFamily),
            ColumnFamilyDescriptor("new_cf".encodeToByteArray())
        )
        val columnFamilyHandleList = mutableListOf<ColumnFamilyHandle>()
        DBOptions().apply {
            setCreateMissingColumnFamilies(true)
            setCreateIfMissing(true)
        }.use { options ->
            openRocksDB(
                options,
                createTestFolder(),
                cfDescriptors,
                columnFamilyHandleList
            ).use { db ->
                try {
                    val keys = mutableListOf<ByteArray>()
                    keys.add("key".encodeToByteArray())
                    keys.add("newcfkey".encodeToByteArray())
                    val cfCustomList = mutableListOf<ColumnFamilyHandle>()
                    assertFailsWith<IllegalArgumentException> {
                        db.multiGetAsList(cfCustomList, keys)
                    }
                } finally {
                    for (columnFamilyHandle in columnFamilyHandleList) {
                        columnFamilyHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun testByteCreateColumnFamily() {
        val testFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(
                options,
                testFolder
            ).use { db ->
                val b0 = byteArrayOf(0x00.toByte())
                val b1 = byteArrayOf(0x01.toByte())
                val b2 = byteArrayOf(0x02.toByte())
                var cf1: ColumnFamilyHandle? = null
                var cf2: ColumnFamilyHandle? = null
                var cf3: ColumnFamilyHandle? = null
                try {
                    cf1 = db.createColumnFamily(ColumnFamilyDescriptor(b0))
                    cf2 = db.createColumnFamily(ColumnFamilyDescriptor(b1))
                    val families = listColumnFamilies(
                        options,
                        testFolder
                    )
                    assertContains(families, "default".encodeToByteArray(), b0, b1)
                    cf3 = db.createColumnFamily(ColumnFamilyDescriptor(b2))
                } finally {
                    cf1?.close()
                    cf2?.close()
                    cf3?.close()
                }
            }
        }
    }

    @Test
    fun testCFNamesWithZeroBytes() {
        var cf1: ColumnFamilyHandle? = null
        var cf2: ColumnFamilyHandle? = null
        val testFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(
                options,
                testFolder
            ).use { db ->
                try {
                    val b0 = byteArrayOf(0, 0)
                    val b1 = byteArrayOf(0, 1)
                    cf1 = db.createColumnFamily(ColumnFamilyDescriptor(b0))
                    cf2 = db.createColumnFamily(ColumnFamilyDescriptor(b1))
                    val families = listColumnFamilies(
                        options,
                        testFolder
                    )
                    assertContains(families, "default".encodeToByteArray(), b0, b1)
                } finally {
                    cf1?.close()
                    cf2?.close()
                }
            }
        }
    }

    @Test
    fun testCFNameSimplifiedChinese() {
        val testFolder = createTestFolder()
        var columnFamilyHandle: ColumnFamilyHandle? = null
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(
                options,
                testFolder
            ).use { db ->
                try {
                    val simplifiedChinese = "\u7b80\u4f53\u5b57"
                    columnFamilyHandle = db.createColumnFamily(
                        ColumnFamilyDescriptor(simplifiedChinese.encodeToByteArray())
                    )

                    val families = listColumnFamilies(
                        options,
                        testFolder
                    )
                    assertContains(
                        families,
                        "default".encodeToByteArray(),
                        simplifiedChinese.encodeToByteArray()
                    )
                } finally {
                    if (columnFamilyHandle != null) {
                        columnFamilyHandle!!.close()
                    }
                }
            }
        }
    }
}
