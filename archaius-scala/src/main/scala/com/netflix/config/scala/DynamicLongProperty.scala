package com.netflix.config.scala

/**
 * User: gorzell
 * Date: 8/10/12
 */

class DynamicLongProperty(val propertyName: String, val default: Long)
  extends com.netflix.config.DynamicLongProperty(propertyName, default) {

  def apply(): Option[Long] = Option(get())
}
