package com.netflix.config.scala

import com.netflix.config.{DynamicBooleanProperty => JDynamicBooleanProperty}

/**
 * User: gorzell
 * Date: 8/10/12
 */

class DynamicBooleanProperty(val propertyName: String, val default: Boolean) {

  private val prop = new JDynamicBooleanProperty(propertyName, default)

  def apply(): Option[Boolean] = Option(get())

  def get(): Boolean = prop.get()

  def addCallback(callback: Runnable) {
    prop.addCallback(callback)
  }
}
