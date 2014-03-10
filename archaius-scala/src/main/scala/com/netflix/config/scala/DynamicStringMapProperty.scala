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

import scala.collection.JavaConverters._
import com.netflix.config.{DynamicStringMapProperty => jDynamicStringMapProperty}
import java.util.{Map => jMap}

/**
 * User: gorzell
 * Date: 9/25/12
 */
class DynamicStringMapProperty(
  override val propertyName: String,
  override val defaultValue: Map[String, String],
  delimiterRegex: String)
extends DynamicProperty[Map[String, String]]
{
  override protected val box = new PropertyBox[Map[String, String], jMap[String, String]] {
    override val prop = new jDynamicStringMapProperty(propertyName, defaultValue.asJava, delimiterRegex)
    def convert(jt: jMap[String, String]): Map[String, String] = jt.asScala.toMap
  }
}
