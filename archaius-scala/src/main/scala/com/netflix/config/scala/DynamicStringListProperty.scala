package com.netflix.config.scala

import scala.collection.JavaConversions._

/**
 * User: gorzell
 * Date: 9/25/12
 */
class DynamicStringListProperty(property: String, default: List[String], delimiterRegex: String) {
  private val prop = new com.netflix.config.DynamicStringListProperty(property, default, delimiterRegex)

  def apply(): Option[List[String]] = Option(get())

  def get(): List[String] = prop.get.toList

  def addCallback(callback: Runnable) {
    if (callback != null) prop.addCallback(callback)
  }
}
