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
import java.lang.{Long => jLong}

/**
 * User: gorzell
 * Date: 8/10/12
 */
object DynamicLongProperty {
  def apply(propertyName: String, defaultValue: Long) =
    new DynamicLongProperty(propertyName, defaultValue)

  def apply(propertyName: String, defaultValue: Long, callback: () => Unit) = {
    val p = new DynamicLongProperty(propertyName, defaultValue)
    p.addCallback(callback)
    p
  }
}

class DynamicLongProperty(
  override val propertyName: String,
  override val defaultValue: Long)
extends DynamicProperty[Long]
{
  override protected val box = new PropertyBox[Long, jLong] {
    override val prop = DynamicPropertyFactory.getInstance().getLongProperty(propertyName, defaultValue)
    def convert(jt: jLong): Long = jt
  }
}
