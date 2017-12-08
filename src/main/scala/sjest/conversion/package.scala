package sjest

import sjest.support.TypeClass

package object conversion extends PackageCommon {

  @TypeClass
  private[conversion] trait Convertible[T, R] {
    def convert(t: T): R
  }

  private[conversion] implicit class TreeConversionOps
  [T <: JsTestTree](t: T)
                   (implicit ev: Convertible[T, Seq[String] => String]) {
    def toJsTest(upperPath: Seq[String] = Seq.empty): String = ev.convert(t).apply(upperPath)
  }
}
