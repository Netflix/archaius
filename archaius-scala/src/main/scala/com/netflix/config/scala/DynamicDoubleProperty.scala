package com.netflix.config.scala

/**
 * User: gorzell
 * Date: 8/10/12
 */

class DynamicDoubleProperty(val propertyName: String, val default: Double)
  extends com.netflix.config.DynamicDoubleProperty(propertyName, default) {

  def apply(): Option[Double] = Option(get())
}
