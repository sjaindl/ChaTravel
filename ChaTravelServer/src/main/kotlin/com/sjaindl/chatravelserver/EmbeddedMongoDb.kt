package com.sjaindl.chatravelserver

import com.mongodb.ConnectionString
import de.flapdoodle.embed.mongo.commands.MongodArguments
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.transitions.Mongod
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess
import de.flapdoodle.net.Net
import de.flapdoodle.reverse.TransitionWalker
import de.flapdoodle.reverse.transitions.Start
import java.io.Closeable
import de.flapdoodle.embed.mongo.config.Net as NetConfig

class EmbeddedMongoDB(
  version: Version.Main,
  port: Int,
  ip: String,
) : Closeable {
  private val mongod: TransitionWalker.ReachedState<RunningMongodProcess>

  val connectionString: ConnectionString

  init {
    val mongodArgs = MongodArguments.builder().build()
    val net = NetConfig.of(ip, port, Net.localhostIsIPv6())

    mongod = Mongod.builder()
      .net(Start.to(NetConfig::class.java).initializedWith(net))
      .mongodArguments(Start.to(MongodArguments::class.java).initializedWith(mongodArgs))
      .build()
      .start(version)
    connectionString = ConnectionString("mongodb://$ip:$port")
  }

  override fun close() {
    mongod.close()
  }
}