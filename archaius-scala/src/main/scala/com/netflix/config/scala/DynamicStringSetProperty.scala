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

import com.netflix.config.{DynamicStringSetProperty => jDynamicStringSetProperty, Property}
import java.lang.{String => jString}
import java.util.{Set => jSet}
import scala.collection.JavaConverters._

/**
 * User: gorzell
 * Date: 9/25/12
 */
object DynamicStringSetProperty {
  def apply(propertyName: String, defaultValue: Set[String], delimiterRegex: String) =
    new DynamicStringSetProperty(propertyName, defaultValue, delimiterRegex)

  def apply(propertyName: String, defaultValue: Set[String], delimiterRegex: String, callback: () => Unit) = {
    val p = new DynamicStringSetProperty(propertyName, defaultValue, delimiterRegex)
    p.addCallback(callback)
    p
  }
}

class DynamicStringSetProperty(
  override val propertyName: String,
  override val defaultValue: Set[String],
  delimiterRegex: String)
extends DynamicProperty[Set[String]]
{
  override protected val box = new PropertyBox[Set[String], jSet[jString]] {
    override val prop: Property[jSet[jString]]= new jDynamicStringSetProperty(propertyName, defaultValue.asJava, delimiterRegex)
    def convert(jt: jSet[String]): Set[String] = jt.asScala.map(x => x).toSet
  }
}
