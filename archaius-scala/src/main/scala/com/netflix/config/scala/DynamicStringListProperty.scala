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

import com.netflix.config.{DynamicStringListProperty => jDynamicStringListProperty}
import java.util.{List => jList}
import scala.collection.JavaConverters._

/**
 * User: gorzell
 * Date: 9/25/12
 */
object DynamicStringListProperty {
  def apply(propertyName: String, defaultValue: List[String], delimiterRegex: String) =
    new DynamicStringListProperty(propertyName, defaultValue, delimiterRegex)

  def apply(propertyName: String, defaultValue: List[String], delimiterRegex: String, callback: () => Unit) = {
    val p = new DynamicStringListProperty(propertyName, defaultValue, delimiterRegex)
    p.addCallback(callback)
    p
  }
}

class DynamicStringListProperty(
  override val propertyName: String,
  override val defaultValue: List[String],
  delimiterRegex: String)
extends DynamicProperty[List[String]]
{
  override protected val box = new PropertyBox[List[String], jList[String]] {
    override val prop = new jDynamicStringListProperty(propertyName, defaultValue.asJava, delimiterRegex)
    def convert(jt: jList[String]): List[String] = jt.asScala.toList
  }
}
