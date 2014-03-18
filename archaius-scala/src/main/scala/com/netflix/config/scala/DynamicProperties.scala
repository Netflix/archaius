/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.config.scala

import com.netflix.config.DynamicListProperty

/**
 * @deprecated Use the factory methods for each class instead of this central factory.
 */
@deprecated("Use the factory methods for each class instead of this central factory.", "0.6.1")
object DynamicProperties {
  private val DefaultDelimiterRegex = DynamicListProperty.DEFAULT_DELIMITER

  def dynamicIntProperty(propertyName: String, default: Int,
                         callback: () => Unit = null): DynamicIntProperty = {
    val prop = new DynamicIntProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  def dynamicLongProperty(propertyName: String, default: Long,
                          callback: () => Unit = null): DynamicLongProperty = {
    val prop = new DynamicLongProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  def dynamicFloatProperty(propertyName: String, default: Float,
                           callback: () => Unit = null): DynamicFloatProperty = {
    val prop = new DynamicFloatProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  def dynamicDoubleProperty(propertyName: String, default: Double,
                            callback: () => Unit = null): DynamicDoubleProperty = {
    val prop = new DynamicDoubleProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  def dynamicBooleanProperty(propertyName: String, default: Boolean,
                             callback: () => Unit = null): DynamicBooleanProperty = {
    val prop = new DynamicBooleanProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  def dynamicStringProperty(propertyName: String, default: String,
                            callback: () => Unit = null): DynamicStringProperty = {
    val prop = new DynamicStringProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  def dynamicStringListProperty(propertyName: String,
                                default: List[String],
                                delimiterRegex: String = DefaultDelimiterRegex,
                                callback: () => Unit = null): DynamicStringListProperty = {
    val prop = new DynamicStringListProperty(propertyName, default, delimiterRegex)
    prop.addCallback(callback)
    prop
  }

  def dynamicStringSetProperty(propertyName: String,
                               default: Set[String],
                               delimiterRegex: String = DefaultDelimiterRegex,
                               callback: () => Unit = null): DynamicStringSetProperty = {
    val prop = new DynamicStringSetProperty(propertyName, default, delimiterRegex)
    prop.addCallback(callback)
    prop
  }

  def dynamicStringMapProperty(propertyName: String,
                               default: Map[String, String],
                               delimiterRegex: String = DefaultDelimiterRegex,
                               callback: () => Unit = null): DynamicStringMapProperty = {
    val prop = new DynamicStringMapProperty(propertyName, default, delimiterRegex)
    prop.addCallback(callback)
    prop
  }

  def dynamicContextualProperty[T](propertyName: String, default: T,
                                   callback: () => Unit = null): DynamicContextualProperty[T] = {
    val prop = new DynamicContextualProperty[T](propertyName, default)
    prop.addCallback(callback)
    prop
  }
}
