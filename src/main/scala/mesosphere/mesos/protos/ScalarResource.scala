package mesosphere.mesos.protos

case class ScalarResource(
  name: String,
  value: Double,
  role: String = "*",
  revocable: Boolean = false) extends Resource
