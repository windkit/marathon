package mesosphere.marathon
package core.task.termination.impl

import com.typesafe.scalalogging.StrictLogging
import mesosphere.marathon.core.condition.Condition

/**
  * Responsible for resolving the relevant [[KillAction]] for an instance that should be killed.
  */
private[termination] object KillActionResolver extends StrictLogging {

  /* returns whether or not we can expect the task to report a terminal state after sending a kill signal */
  private val wontRespondToKill: Condition => Boolean = {
    import Condition._
    Set(
      Unknown, Unreachable, UnreachableInactive,
      // TODO: it should be safe to remove these from this list, because
      // 1) all taskId's should be removed at this point, because Gone & Dropped are terminal.
      // 2) Killing a Gone / Dropped task will cause it to be in a terminal state.
      // 3) Killing a Gone / Dropped task may result in no status change at all.
      // 4) Either way, we end up in a terminal state.
      // However, we didn't want to risk changing behavior in a point release. So they remain here.
      Dropped, Gone
    )
  }

  /**
    * Computes the [[KillAction]] based on the instance's state.
    *
    * if the instance can't be reached, issuing a kill request won't cause the instance to progress towards a terminal
    * state; Mesos will simply re-send the current state. Our current behavior is to simply delete any knowledge that
    * the instance might be running, such that if it is reported by Mesos later we will kill it. (that could be
    * improved).
    *
    * If the instance is lost _and_ has reservations, we want to keep the reservation info and therefore transition to
    * reserved.
    *
    * any other case -> issue a kill request
    */
  def computeAction(toKill: ToKill): KillAction = {
    val instanceId = toKill.instanceId
    val taskIds = toKill.taskIdsToKill
    val hasReservations = toKill.maybeInstance.fold(false)(_.hasReservation)

    // TODO(PODS): align this with other Terminal/Unreachable/whatever extractors
    val maybeCondition = toKill.maybeInstance.map(_.state.condition)
    val isUnkillable = maybeCondition.fold(false)(wontRespondToKill)

    // An instance will be expunged once all tasks are terminal. Therefore, this case is
    // highly unlikely. Should it ever occur, this will still expunge the instance to clean up.
    val allTerminal: Boolean = taskIds.isEmpty

    if (isUnkillable || allTerminal) {
      val msg = if (isUnkillable)
        s"it is ${maybeCondition.fold("unknown")(_.toString)}"
      else
        "all its tasks are terminal"
      if (hasReservations) {
        logger.info(s"Transitioning ${instanceId} to Reserved because it has reservations and ${msg}")
        // we will eventually be notified of a taskStatusUpdate after the instance has been updated
        KillAction.TransitionToReserved
      } else {
        logger.warn(s"Expunging ${instanceId} from state because ${msg}")
        // we will eventually be notified of a taskStatusUpdate after the instance has been expunged
        KillAction.ExpungeFromState
      }
    } else {
      val knownOrNot = if (toKill.maybeInstance.isDefined) "known" else "unknown"
      logger.warn("Killing {} {} of instance {}", knownOrNot, taskIds.mkString(","), instanceId)
      KillAction.IssueKillRequest
    }
  }
}
