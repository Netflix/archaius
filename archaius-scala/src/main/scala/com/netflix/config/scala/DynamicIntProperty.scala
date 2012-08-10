import scala.None

/**
 * User: gorzell
 * Date: 8/6/12
 */

class DynamicIntProperty(val property: String, val default: Int)
  extends com.netflix.config.DynamicIntProperty(property, default) {

  def apply(): Option[Int] = Option(get())
}