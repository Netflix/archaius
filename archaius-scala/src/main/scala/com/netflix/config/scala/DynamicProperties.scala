/*
 *
 *  Copyright 2012 Sumo Logic, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.config.scala

import com.netflix.config.DynamicListProperty

/**
 * User: gorzell
 * Date: 8/6/12
 */

trait DynamicProperties {

  private val DefaultDelimiterRegex = DynamicListProperty.DEFAULT_DELIMITER

  protected def dynamicIntProperty(propertyName: String, default: Int,
                                   callback: Runnable = null): DynamicIntProperty = {
    val prop = new DynamicIntProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicLongProperty(propertyName: String, default: Long,
                                    callback: Runnable = null): DynamicLongProperty = {
    val prop = new DynamicLongProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicFloatProperty(propertyName: String, default: Float,
                                     callback: Runnable = null): DynamicFloatProperty = {
    val prop = new DynamicFloatProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicDoubleProperty(propertyName: String, default: Double,
                                      callback: Runnable = null): DynamicDoubleProperty = {
    val prop = new DynamicDoubleProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicBooleanProperty(propertyName: String, default: Boolean,
                                       callback: Runnable = null): DynamicBooleanProperty = {
    val prop = new DynamicBooleanProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicStringProperty(propertyName: String, default: String,
                                      callback: Runnable = null): DynamicStringProperty = {
    val prop = new DynamicStringProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicStringListProperty(propertyName: String,
                                          default: List[String],
                                          delimiterRegex: String = DefaultDelimiterRegex,
                                          callback: Runnable = null): DynamicStringListProperty = {
    val prop = new DynamicStringListProperty(propertyName, default, delimiterRegex)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicStringSetProperty(propertyName: String,
                                         default: Set[String],
                                         delimiterRegex: String = DefaultDelimiterRegex,
                                         callback: Runnable = null): DynamicStringSetProperty = {
    val prop = new DynamicStringSetProperty(propertyName, default, delimiterRegex)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicStringMapProperty(propertyName: String,
                                         default: Map[String, String],
                                         delimiterRegex: String = DefaultDelimiterRegex,
                                         callback: Runnable = null): DynamicStringMapProperty = {
    val prop = new DynamicStringMapProperty(propertyName, default, delimiterRegex)
    prop.addCallback(callback)
    prop
  }
}