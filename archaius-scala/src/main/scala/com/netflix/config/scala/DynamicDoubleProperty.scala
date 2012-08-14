package com.netflix.config.scala

import com.netflix.config.{DynamicDoubleProperty => JDynamicDoubleProperty}

/**
 * User: gorzell
 * Date: 8/10/12
 */

class DynamicDoubleProperty(val propertyName: String, val default: Double) {

  private val prop = new JDynamicDoubleProperty(propertyName, default)

  def apply(): Option[Double] = Option(get())

  def get(): Double = prop.get()

  def addCallback(callback: Runnable) {
    prop.addCallback(callback)
  }
}
