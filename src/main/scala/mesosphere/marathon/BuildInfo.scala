package mesosphere.marathon

import java.util.jar.{ Attributes, Manifest }
import scala.Predef._
import scala.util.Try
import scala.util.control.NonFatal

case object BuildInfo {

  lazy val manifest: Option[Manifest] = Try {
    val mf = new Manifest()
    mf.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"))
    mf
  }.toOption

  lazy val attributes: Option[Attributes] = manifest.map(_.getMainAttributes())

  def getAttribute(name: String): String = attributes.map { attrs =>
    try {
      attrs.getValue(name) match {
        case null => "unknown"
        case v => v
      }
    } catch {
      case NonFatal(_) => "unknown"
    }
  }.getOrElse("unknown")

  lazy val name: String = getAttribute("Implementation-Title")

  lazy val version: String = getAttribute("Implementation-Version")

  lazy val scalaVersion: String = getAttribute("Scala-Version")

  lazy val buildref: String = getAttribute("Git-Commit")

  override val toString: String = {
    "name: %s, version: %s, scalaVersion: %s, buildref: %s" format (
      name, version, scalaVersion, buildref
    )
  }
}
