package com.netflix.config.scala

/**
 * User: gorzell
 * Date: 8/10/12
 */

class DynamicFloatProperty(val propertyName: String, val default: Float)
  extends com.netflix.config.DynamicFloatProperty(propertyName, default) {

  def apply(): Option[Float] = Option(get())
}
