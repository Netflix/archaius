package com.netflix.config.scala

/**
 * User: gorzell
 * Date: 8/10/12
 */

class DynamicBooleanProperty(val propertyName: String, val default: Boolean)
  extends com.netflix.config.DynamicBooleanProperty(propertyName, default) {

  def apply(): Option[Boolean] = Option(get())
}
