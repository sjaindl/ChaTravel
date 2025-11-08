package com.sjaindl.chatravelserver

import com.mongodb.ConnectionString
import de.flapdoodle.embed.mongo.commands.MongodArguments
import de.flapdoodle.embed.mongo.config.Storage
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.net.Net
import de.flapdoodle.reverse.TransitionWalker
import de.flapdoodle.reverse.transitions.Start
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import de.flapdoodle.embed.mongo.config.Net as NetConfig
import de.flapdoodle.embed.mongo.types.DatabaseDir

class EmbeddedMongoDB(
    version: Version.Main,
    port: Int,
    ip: String,
    dataDir: Path = Paths.get(System.getProperty("user.home"), ".chatravel", "mongo-data"),
) : Closeable {
  private val mongod: TransitionWalker.ReachedState<RunningMongodProcess>

  val connectionString: ConnectionString

  init {
    Files.createDirectories(dataDir)

    val mongodArgs = MongodArguments.builder().build()
    val net = NetConfig.of(ip, port, Net.localhostIsIPv6())

      val dbDir = DatabaseDir.of(dataDir)

    mongod = Mongod.builder()
      .net(Start.to(NetConfig::class.java).initializedWith(net))
      .mongodArguments(Start.to(MongodArguments::class.java).initializedWith(mongodArgs))
        .databaseDir(Start.to(DatabaseDir::class.java).initializedWith(dbDir))
        .build()
      .start(version)
    connectionString = ConnectionString("mongodb://$ip:$port")
  }

  override fun close() {
    mongod.close()
  }
}
