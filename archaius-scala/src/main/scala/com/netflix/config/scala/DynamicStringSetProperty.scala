package com.netflix.config.scala

import scala.collection.JavaConversions._

/**
 * User: gorzell
 * Date: 9/25/12
 */
class DynamicStringSetProperty(property: String, default: Set[String], delimiterRegex: String) {
  private val prop = new com.netflix.config.DynamicStringSetProperty(property, default, delimiterRegex)

  def apply(): Option[Set[String]] = Option(get())

  def get(): Set[String] = prop.get.toSet

  def addCallback(callback: Runnable) {
    if (callback != null) prop.addCallback(callback)
  }
}
