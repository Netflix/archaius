package com.netflix.config.scala

import com.netflix.config.{DynamicLongProperty => JDynamicLongProperty}

/**
 * User: gorzell
 * Date: 8/10/12
 */

class DynamicLongProperty(val propertyName: String, val default: Long) {

  private val prop = new JDynamicLongProperty(propertyName, default)

  def apply(): Option[Long] = Option(get())

  def get(): Long = prop.get()

  def addCallback(callback: Runnable) {
    prop.addCallback(callback)
  }
}
