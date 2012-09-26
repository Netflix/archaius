package com.netflix.config.scala

import scala.collection.JavaConversions._

/**
 * User: gorzell
 * Date: 9/25/12
 */
class DynamicStringMapProperty(property: String, default: Map[String, String], delimiterRegex: String) {
  private val prop = new com.netflix.config.DynamicStringMapProperty(property, default, delimiterRegex)

  def apply(): Option[Map[String, String]] = Option(get())

  def get(): Map[String, String] = prop.getMap.toMap

  def addCallback(callback: Runnable) {
    if (callback != null) prop.addCallback(callback)
  }
}