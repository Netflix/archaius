package com.netflix.config.scala


class DynamicContextualProperty[T](val propertyName: String, val default: T) {
  private val prop = new DynamicContextualProperty[T](propertyName, default)

  def apply: Option[T] = Option(get)

  def get: T = prop.get

  def addCallback(callback: Runnable) {
    if (callback != null) prop.addCallback(callback)
  }
}
