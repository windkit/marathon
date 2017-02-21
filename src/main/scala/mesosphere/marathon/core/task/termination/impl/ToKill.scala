package mesosphere.marathon
package core.task.termination.impl

import mesosphere.marathon.Seq
import mesosphere.marathon.core.instance.Instance
import mesosphere.marathon.core.task.Task
import mesosphere.marathon.state.Timestamp

/**
  * Metadata used to track which instances to kill and how many attempts have been made
  *
  * @param instanceId id of the instance to kill
  * @param taskIdsToKill ids of the tasks to kill
  * @param maybeInstance the instance, if available
  * @param attempts the number of kill attempts
  * @param issued the time of the last issued kill request
  */
private[termination] case class ToKill(
  instanceId: Instance.Id,
  taskIdsToKill: Seq[Task.Id],
  maybeInstance: Option[Instance],
  attempts: Int,
  issued: Timestamp = Timestamp.zero)
