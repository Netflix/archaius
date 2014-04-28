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

import com.netflix.config.{DynamicContextualProperty => jDynamicContextualProperty}

object DynamicContextualProperty {
  def apply[T](propertyName: String, defaultValue: T) =
    new DynamicContextualProperty(propertyName, defaultValue)

  def apply[T](propertyName: String, defaultValue: T, callback: () => Unit) = {
    val p = new DynamicContextualProperty(propertyName, defaultValue)
    p.addCallback(callback)
    p
  }
}

class DynamicContextualProperty[T](
  override val propertyName: String,
  override val defaultValue: T)
extends DynamicProperty[T]
{
  override protected val box = new PropertyBox[T, T] {
    override val prop = new jDynamicContextualProperty[T](propertyName, defaultValue)
    def convert(jt: T): T = jt
  }
}
