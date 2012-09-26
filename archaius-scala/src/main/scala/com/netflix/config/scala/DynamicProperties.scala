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

import com.netflix.config.scala._

/**
 * User: gorzell
 * Date: 8/6/12
 */

trait DynamicProperties {

  protected def dynamicIntProperty(propertyName: String, default: Int, callback: Runnable): DynamicIntProperty = {
    val prop = new DynamicIntProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicIntProperty(propertyName: String, default: Int): DynamicIntProperty =
    dynamicIntProperty(propertyName, default, null)

  protected def dynamicLongProperty(propertyName: String, default: Long, callback: Runnable): DynamicLongProperty = {
    val prop = new DynamicLongProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicLongProperty(propertyName: String, default: Long): DynamicLongProperty =
    dynamicLongProperty(propertyName, default, null)

  protected def dynamicFloatProperty(propertyName: String, default: Float, callback: Runnable): DynamicFloatProperty = {
    val prop = new DynamicFloatProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicFloatProperty(propertyName: String, default: Float): DynamicFloatProperty =
    dynamicFloatProperty(propertyName, default, null)

  protected def dynamicDoubleProperty(propertyName: String, default: Double, callback: Runnable): DynamicDoubleProperty = {
    val prop = new DynamicDoubleProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicDoubleProperty(propertyName: String, default: Double): DynamicDoubleProperty =
    dynamicDoubleProperty(propertyName, default, null)

  protected def dynamicBooleanProperty(propertyName: String, default: Boolean, callback: Runnable): DynamicBooleanProperty = {
    val prop = new DynamicBooleanProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicBooleanProperty(propertyName: String, default: Boolean): DynamicBooleanProperty =
    dynamicBooleanProperty(propertyName, default, null)

  protected def dynamicStringProperty(propertyName: String, default: String, callback: Runnable): DynamicStringProperty = {
    val prop = new DynamicStringProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicStringProperty(propertyName: String, default: String): DynamicStringProperty =
    dynamicStringProperty(propertyName, default, null)

  protected def dynamicStringListProperty(propertyName: String, default: List[String], callback: Runnable): DynamicStringProperty = {
    val prop = new DynamicStringListProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicStringListProperty(propertyName: String, default: List[String]): DynamicStringProperty =
    dynamicStringListProperty(propertyName, default, null)

  protected def dynamicStringSetProperty(propertyName: String, default: Set[String], callback: Runnable): DynamicStringProperty = {
    val prop = new DynamicStringSetProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicStringSetProperty(propertyName: String, default: Set[String]): DynamicStringProperty =
    dynamicStringSetProperty(propertyName, default, null)

  protected def dynamicStringMapProperty(propertyName: String, default: Map[String, String], callback: Runnable): DynamicStringProperty = {
    val prop = new DynamicStringMapProperty(propertyName, default)
    prop.addCallback(callback)
    prop
  }

  protected def dynamicStringMapProperty(propertyName: String, default: Map[String, String]): DynamicStringProperty =
    dynamicStringMapProperty(propertyName, default, null)
}