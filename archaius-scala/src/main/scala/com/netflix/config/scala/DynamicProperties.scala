import com.netflix.config.DynamicPropertyFactory

/**
 * User: gorzell
 * Date: 8/6/12
 */

trait DynamicProperties {

  protected def dynamicIntProperty(propertyName: String, default: Int) {
    new DynamicIntProperty(propertyName, default)
  }

  protected def dynamicStringProperty(propertyName: String, default: String) {
    new DynamicStringProperty(propertyName, default)
  }
}