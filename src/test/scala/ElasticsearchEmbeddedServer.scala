package test

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import java.util
import java.util.UUID

import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._

class ElasticsearchEmbeddedServer {

  val httpPort = "19200"

  private val clusterName = s"elasticsearch-${UUID.randomUUID.toString}"
  private val dataDir = Files.createTempDirectory(s"elasticsearch_data_${httpPort}_").toFile
  private val settings = ImmutableSettings.settingsBuilder
    .put("path.data", dataDir.toString)
    .put("cluster.name", clusterName)
    .put("http.enabled", "true")
    .put("http.port", httpPort)
    .put("transport.tcp.port", "19300")
    .build

  private lazy val node = nodeBuilder().local(true).settings(settings).build

  def start(): Unit = node.start()

  def stop(): Unit = {
    node.close()
    try {
      DeleteDir(dataDir.toPath)
    } catch {
      case e: Exception => println(s"Data directory ${dataDir.toPath} cleanup failed")
    }
  }

}

object DeleteDir extends (Path => Unit) {

  override def apply(source: Path): Unit = {

    Files.walkFileTree(source, util.EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
      new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }
        override def postVisitDirectory(dir: Path, e: IOException): FileVisitResult = {
          if (e == null) {
            Files.delete(dir)
            FileVisitResult.CONTINUE
          } else throw e
        }
      }
    )
  }
}
