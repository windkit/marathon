package mesosphere.marathon
package core.base

/**
  * Simple value container which is used to help things know if Marathon is on it's way down
  */
trait ShutdownState {
  def isShuttingDown: Boolean
}

object ShutdownState {
  object WatchingJVM extends ShutdownState {
    private[this] var shuttingDown = false

    /* Note - each shutdown hook is run in it's own thread, so this won't have to wait until some other shutdownHook
     * finishes before the boolean can be set */
    sys.addShutdownHook {
      shuttingDown = true
    }

    override def isShuttingDown: Boolean = shuttingDown
  }

  object Ignore extends ShutdownState {
    override val isShuttingDown: Boolean = false
  }
}
