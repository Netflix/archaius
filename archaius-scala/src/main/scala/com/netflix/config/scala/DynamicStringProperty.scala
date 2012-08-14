package com.netflix.config.scala

import com.netflix.config.{DynamicStringProperty => JDynamicStringProperty}

/**
 * User: gorzell
 * Date: 8/6/12
 */

class DynamicStringProperty(property: String, default: String) {

  private val prop: JDynamicStringProperty = new JDynamicStringProperty(property, default)

  def apply(): Option[String] = Option(get())

  def get(): String = prop.get()

  def addCallback(callback: Runnable) {
    prop.addCallback(callback)
  }
}