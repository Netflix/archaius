/**
 * User: gorzell
 * Date: 8/6/12
 */

class DynamicStringProperty(val property: String, val default: String)
  extends com.netflix.config.DynamicStringProperty(property, default) {

  def apply(): Option[String] = Option(get())
}