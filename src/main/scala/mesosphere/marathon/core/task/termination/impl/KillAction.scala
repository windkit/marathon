package mesosphere.marathon
package core.task.termination.impl

/**
  * Possible actions that can be chosen in order to `kill` a given instance.
  * Depending on the instance's state this can be one of
  * - [[KillAction.ExpungeFromState]]
  * - [[KillAction.TransitionToReserved]]
  * - [[KillAction.IssueKillRequest]]
  */
private[termination] sealed trait KillAction

private[termination] object KillAction {
  /**
    * Any normal, reachable and stateless instance will simply be killed via the scheduler driver.
    */
  case object IssueKillRequest extends KillAction

  /**
    * If an instance has associated reservations and persistent volumes, killing it should transition
    * to the Reserved state. Marathon will thus retain knowledge about the reserved resources and will
    * be able to re-use them when trying to launch a new instance.
    */
  case object TransitionToReserved extends KillAction

  /**
    * In case of an instance being Unreachable, killing the related Mesos task is impossible.
    * In order to get rid of the instance, processing this action expunges the metadata from
    * state. If the instance is reported to be non-terminal in the future, it will be killed.
    *
    * Note: stateful instances with associated reservations must be treated using [[TransitionToReserved]].
    */
  case object ExpungeFromState extends KillAction
}
