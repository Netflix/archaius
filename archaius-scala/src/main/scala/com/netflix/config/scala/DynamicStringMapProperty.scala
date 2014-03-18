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

import com.netflix.config.{DynamicStringMapProperty => jDynamicStringMapProperty}
import java.util.{Map => jMap}
import scala.collection.JavaConverters._

/**
 * User: gorzell
 * Date: 9/25/12
 */
object DynamicStringMapProperty {
  def apply(propertyName: String, defaultValue: Map[String, String], delimiterRegex: String) =
    new DynamicStringMapProperty(propertyName, defaultValue, delimiterRegex)

  def apply(propertyName: String, defaultValue: Map[String, String], delimiterRegex: String, callback: () => Unit) = {
    val p = new DynamicStringMapProperty(propertyName, defaultValue, delimiterRegex)
    p.addCallback(callback)
    p
  }
}

class DynamicStringMapProperty(
  override val propertyName: String,
  override val defaultValue: Map[String, String],
  delimiterRegex: String)
extends DynamicProperty[Map[String, String]]
{
  override protected val box = new PropertyBox[Map[String, String], jMap[String, String]] {
    override val prop = null
    val mapProp = new jDynamicStringMapProperty(propertyName, defaultValue.asJava, delimiterRegex)
    override def get: Map[String, String] = convert(mapProp.getMap)
    override def apply(): Option[Map[String, String]] = Option(mapProp.getMap).map(convert)
    override def addCallback(callback: () => Unit) {
      mapProp.addCallback( CallbackWrapper( callback ) )
    }
    override def removeAllCallbacks() {
      mapProp.removeAllCallbacks()
    }
    override protected def convert(jt: jMap[String, String]): Map[String, String] = jt.asScala.toMap
  }
}
