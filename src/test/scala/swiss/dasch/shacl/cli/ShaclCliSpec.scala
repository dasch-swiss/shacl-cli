package swiss.dasch.shacl.cli

import zio.test.ZIOSpecDefault
import zio.test.assertTrue

object ShaclCliSpec extends ZIOSpecDefault {
  def spec = suite("SomeTest")(
    test("example test") {
      assertTrue(true)
    },
  )
}
