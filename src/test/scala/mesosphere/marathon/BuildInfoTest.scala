package mesosphere.marathon

import mesosphere.UnitTest

class BuildInfoTest extends UnitTest {

  "BuildInfo" should {
    "return a default versions" in {
      BuildInfo.scalaVersion should be("0.0.0-SNAPSHOT")
      BuildInfo.version should be("0.0.0-SNAPSHOT")
    }
  }
}
