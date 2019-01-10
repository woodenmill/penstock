
object Test {
  def doIt(): Test = new Test("do it")
  private def apply(a: String): Test = new Test(a)
}
class Test private (a: String)

 Test.doIt()