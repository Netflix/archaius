package com.netflix.config.scala

import com.netflix.config.{DynamicIntProperty => JDynamicIntProperty}

/**
 * User: gorzell
 * Date: 8/6/12
 */

class DynamicIntProperty(val property: String, val default: Int) {

  private val prop = new JDynamicIntProperty(property, default)

  def apply(): Option[Int] = Option(get())

  def get(): Int = prop.get()

  def addCallback(callback: Runnable) {
    prop.addCallback(callback)
  }
}