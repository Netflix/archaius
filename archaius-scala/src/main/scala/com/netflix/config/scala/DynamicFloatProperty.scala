package com.netflix.config.scala

import com.netflix.config.{DynamicFloatProperty => JDynamicFloatProperty}

/**
 * User: gorzell
 * Date: 8/10/12
 */

class DynamicFloatProperty(val propertyName: String, val default: Float) {

  private val prop = new JDynamicFloatProperty(propertyName, default)

  def apply(): Option[Float] = Option(get())

  def get(): Float = prop.get()

  def addCallback(callback: Runnable) {
    prop.addCallback(callback)
  }
}
