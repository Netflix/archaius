import com.netflix.config.PropertyWrapper
import com.netflix.config.scala._

/**
 * User: gorzell
 * Date: 8/6/12
 */

trait DynamicProperties {

  protected def dynamicIntProperty(propertyName: String, default: Int, callback: Runnable): DynamicIntProperty = {
    val prop = new DynamicIntProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicIntProperty(propertyName: String, default: Int): DynamicIntProperty =
    dynamicIntProperty(propertyName, default, null)

  protected def dynamicLongProperty(propertyName: String, default: Long, callback: Runnable): DynamicLongProperty = {
    val prop = new DynamicLongProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicLongProperty(propertyName: String, default: Long): DynamicLongProperty =
    dynamicLongProperty(propertyName, default, null)

  protected def dynamicFloatProperty(propertyName: String, default: Float, callback: Runnable): DynamicFloatProperty = {
    val prop = new DynamicFloatProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicFloatProperty(propertyName: String, default: Float): DynamicFloatProperty =
    dynamicFloatProperty(propertyName, default, null)

  protected def dynamicDoubleProperty(propertyName: String, default: Double, callback: Runnable): DynamicDoubleProperty = {
    val prop = new DynamicDoubleProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicDoubleProperty(propertyName: String, default: Double): DynamicDoubleProperty =
    dynamicDoubleProperty(propertyName, default, null)

  protected def dynamicBooleanProperty(propertyName: String, default: Boolean, callback: Runnable): DynamicBooleanProperty = {
    val prop = new DynamicBooleanProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicBooleanProperty(propertyName: String, default: Boolean): DynamicBooleanProperty =
    dynamicBooleanProperty(propertyName, default, null)

  protected def dynamicStringProperty(propertyName: String, default: String, callback: Runnable): DynamicStringProperty = {
    val prop = new DynamicStringProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicStringProperty(propertyName: String, default: String): DynamicStringProperty =
    dynamicStringProperty(propertyName, default, null)

  private def addCallback(callback: Runnable, property: PropertyWrapper[_]) {
    if (callback != null) property.addCallback(callback)
  }
}