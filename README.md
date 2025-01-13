[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://img.shields.io/maven-central/v/io.maryk.rocksdb/rocksdb-android)](https://central.sonatype.com/artifact/io.maryk.rocksdb/rocksdb-multiplatform)

# Kotlin Multi-platform RocksDB implementation

This project provides a multi-platform Kotlin implementation for RocksDB, a high-performance embedded key-value store for 
storage of data on disk. RocksDB is widely used in many industries for various applications, including database management 
systems, big data systems, and storage engines for other distributed systems.

The aim of this project is to provide a multi-platform RocksDB implementation that can be used across different platforms,
including JVM, Android, and native Linux/macOS/iOS. This allows developers to write applications that can run on different platforms 
without having to rewrite the codebase.

This project is useful for developers who want to build multi-platform applications that require high-performance disk-based
storage. By using this implementation of RocksDB in their codebase, developers can ensure that their application is portable 
across different platforms while maintaining a high level of performance and reliability.

## RocksDB API Support

The project supports the full RocksDB Java interfaces on the JVM and Android platforms. 
This common library includes most common operations, but if you need additional features, you can request 
them by creating an issue or submitting a merge request.

## Native platforms
Currently, only Linux, iOS and macOS (+arm64 simulators) are supported for native compilations. 

Since the implementation is based on the RocksDB C API,
other native platforms should be able to be added in the future if RocksDB can be compiled to that platform.
Unfortunately tvOS and watchOS cannot be supported. No tvOS because it blocks certain calls RocksDB needs, and no watchOS
because it needs specific arm64_32 support.

Kotlin Native targets:
- macosArm64
- macosX64
- iosArm64
- iosSimulatorArm64
- linuxX64
- linuxArm64

## Reference

You can refer to the [API reference](src/commonMain/kotlin/maryk/rocksdb) or the official [RocksDB website](https://rocksdb.org) for more information.

## Gradle Dependency

The dependency is published in Maven Central, so you can easily add it to your project:

```kotlin
implementation("io.maryk.rocksdb:rocksdb-multiplatform:9.6.2")
```

## Usage Example

Here's an example of how to open a RocksDB database, write a key-value pair and retrieve the value by key:
```kotlin
openRocksDB("path_to_store_on_disk").use { db ->
    val key = "test".encodeToByteArray()
    db.put(key, "value".encodeToByteArray())
    
    val value = db.get("test".encodeToByteArray())
    
    println(value?.decodeToString())
}
```

Check out the [tests](src/commonTest/kotlin/maryk/rocksdb) for more examples on how to use this library.

## Contributing

We welcome contributions to the project! If you find a bug or want to suggest a new feature, please submit an issue or 
submit a pull request.

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE file](LICENSE) for details.
