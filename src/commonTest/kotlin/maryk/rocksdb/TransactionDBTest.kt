package maryk.rocksdb

import maryk.rocksdb.util.createTestDBFolder
import kotlin.test.*

class TransactionDBTest {

    private fun createTestFolder() = createTestDBFolder("TransactionDBTest")

    @Test
    fun open() {
        val tempFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            TransactionDBOptions().use { txnDbOptions ->
                openTransactionDB(options, txnDbOptions, tempFolder).use { tdb ->
                    assertNotNull(tdb, "TransactionDB should be opened successfully")
                }
            }
        }
    }

    @Test
    fun open_columnFamilies() {
        val tempFolder = createTestFolder()
        DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true).use { dbOptions ->
            ColumnFamilyOptions().use { cfOpts ->
                val columnFamilyDescriptors = mutableListOf(
                    ColumnFamilyDescriptor(defaultColumnFamily),
                    ColumnFamilyDescriptor("myCf".encodeToByteArray(), cfOpts)
                )

                val columnFamilyHandles = mutableListOf<ColumnFamilyHandle>()

                TransactionDBOptions().use { txnDbOptions ->
                    openTransactionDB(
                        dbOptions,
                        txnDbOptions,
                        tempFolder,
                        columnFamilyDescriptors,
                        columnFamilyHandles
                    ).use { tdb ->
                        assertNotNull(tdb, "TransactionDB with column families should be opened successfully")

                        // Ensure all handles are closed
                        columnFamilyHandles.forEach { it.close() }
                    }
                }
            }
        }
    }

    @Test
    fun open_columnFamilies_no_default() {
        val tempFolder = createTestFolder()
        DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true).use { dbOptions ->
            ColumnFamilyOptions().use { cfOpts ->
                val columnFamilyDescriptors = mutableListOf(
                    ColumnFamilyDescriptor("myCf".encodeToByteArray(), cfOpts)
                )

                val columnFamilyHandles = mutableListOf<ColumnFamilyHandle>()

                TransactionDBOptions().use { txnDbOptions ->
                    val exception = assertFailsWith<IllegalArgumentException> {
                        openTransactionDB(
                            dbOptions,
                            txnDbOptions,
                            tempFolder,
                            columnFamilyDescriptors,
                            columnFamilyHandles
                        )
                    }
                    assertNotNull(exception, "Opening without default column family should throw IllegalArgumentException")
                }
            }
        }
    }

    @Test
    fun beginTransaction() {
        val tempFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            TransactionDBOptions().use { txnDbOptions ->
                openTransactionDB(options, txnDbOptions, tempFolder).use { tdb ->
                    WriteOptions().use { writeOptions ->
                        tdb.beginTransaction(writeOptions).use { txn ->
                            assertNotNull(txn, "Transaction should be created successfully")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun beginTransaction_transactionOptions() {
        val tempFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            TransactionDBOptions().use { txnDbOptions ->
                openTransactionDB(options, txnDbOptions, tempFolder).use { tdb ->
                    WriteOptions().use { writeOptions ->
                        TransactionOptions().use { txnOptions ->
                            tdb.beginTransaction(writeOptions, txnOptions).use { txn ->
                                assertNotNull(txn, "Transaction with TransactionOptions should be created successfully")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun beginTransaction_withOld() {
        val tempFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            TransactionDBOptions().use { txnDbOptions ->
                openTransactionDB(options, txnDbOptions, tempFolder).use { tdb ->
                    WriteOptions().use { writeOptions ->
                        tdb.beginTransaction(writeOptions).use { txn ->
                            val txnReused = tdb.beginTransaction(writeOptions, txn)
                            assertSame(txn, txnReused, "Reusing transaction should return the same instance")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun beginTransaction_withOld_transactionOptions() {
        val tempFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            TransactionDBOptions().use { txnDbOptions ->
                openTransactionDB(options, txnDbOptions, tempFolder).use { tdb ->
                    WriteOptions().use { writeOptions ->
                        TransactionOptions().use { txnOptions ->
                            tdb.beginTransaction(writeOptions).use { txn ->
                                val txnReused = tdb.beginTransaction(writeOptions, txnOptions, txn)
                                assertSame(txn, txnReused, "Reusing transaction with TransactionOptions should return the same instance")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun lockStatusData() {
        val tempFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            TransactionDBOptions().use { txnDbOptions ->
                openTransactionDB(options, txnDbOptions, tempFolder).use { tdb ->
                    WriteOptions().use { writeOptions ->
                        ReadOptions().use { readOptions ->
                            tdb.beginTransaction(writeOptions).use { txn ->
                                val key = "key".encodeToByteArray()
                                val value = "value".encodeToByteArray()

                                txn.put(key, value)
                                val retrievedValue = txn.getForUpdate(readOptions, key, true)
                                assertNotNull(retrievedValue, "Retrieved value should not be null")
                                assertContentEquals(value, retrievedValue, "Retrieved value should match the inserted value")

                                val lockStatus = tdb.getLockStatusData()
                                assertEquals(1, lockStatus.size, "There should be one lock status entry")

                                val entry = lockStatus.entries.first()
                                val columnFamilyId = entry.key
                                val keyLockInfo: KeyLockInfo = entry.value

                                assertEquals(0L, columnFamilyId, "ColumnFamilyId should be 0 (default)")
                                assertEquals("key", keyLockInfo.getKey(), "Locked key should match")
                                assertEquals(1, keyLockInfo.getTransactionIDs().size, "There should be one transaction ID")
                                assertEquals(txn.getId(), keyLockInfo.getTransactionIDs()[0], "Transaction ID should match")
                                assertTrue(keyLockInfo.isExclusive(), "Lock should be exclusive")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun deadlockInfoBuffer() {
        val tempFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            TransactionDBOptions().use { txnDbOptions ->
                openTransactionDB(options, txnDbOptions, tempFolder).use { tdb ->
                    val deadlockInfo = tdb.getDeadlockInfoBuffer()
                    assertTrue(deadlockInfo.isEmpty(), "Deadlock info buffer should be empty initially")
                }
            }
        }
    }

    @Test
    fun setDeadlockInfoBufferSize() {
        val tempFolder = createTestFolder()
        Options().setCreateIfMissing(true).use { options ->
            TransactionDBOptions().use { txnDbOptions ->
                openTransactionDB(options, txnDbOptions, tempFolder).use { tdb ->
                    tdb.setDeadlockInfoBufferSize(123)
                    // Assuming there's a method to get the buffer size for verification
                    // If not, this test ensures no exception is thrown
                }
            }
        }
    }

    @Test
    fun tdbSimpleIterator() {
        val tempFolder = createTestFolder()
        Options().setCreateIfMissing(true).setMaxCompactionBytes(0).use { options ->
            TransactionDBOptions().use { txnDbOptions ->
                openTransactionDB(options, txnDbOptions, tempFolder).use { tdb ->
                    tdb.put("keyI".encodeToByteArray(), "valueI".encodeToByteArray())
                    tdb.newIterator().use { iterator ->
                        iterator.seekToFirst()
                        assertTrue(iterator.isValid(), "Iterator should be valid after seeking to first")
                        assertContentEquals("keyI".encodeToByteArray(), iterator.key(), "Iterator key should match")
                        assertContentEquals("valueI".encodeToByteArray(), iterator.value(), "Iterator value should match")
                        iterator.next()
                        assertFalse(iterator.isValid(), "Iterator should be invalid after reaching the end")
                    }
                }
            }
        }
    }
}
