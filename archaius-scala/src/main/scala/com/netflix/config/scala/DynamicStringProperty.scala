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

import com.netflix.config.DynamicPropertyFactory
import java.lang.{String => jString}

/**
 * User: gorzell
 * Date: 8/6/12
 */
object DynamicStringProperty {
  def apply(propertyName: String, defaultValue: String) =
    new DynamicStringProperty(propertyName, defaultValue)

  def apply(propertyName: String, defaultValue: String, callback: () => Unit) = {
    val p = new DynamicStringProperty(propertyName, defaultValue)
    p.addCallback(callback)
    p
  }
}

class DynamicStringProperty(
  override val propertyName: String,
  override val defaultValue: String)
extends DynamicProperty[String]
{
  override protected val box = new PropertyBox[String, jString] {
    override val prop = DynamicPropertyFactory.getInstance().getStringProperty(propertyName, defaultValue)
    def convert(jt: jString): String = jt
  }
}
