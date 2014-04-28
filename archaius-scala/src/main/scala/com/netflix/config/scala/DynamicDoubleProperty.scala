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
import java.lang.{Double => jDouble}

/**
 * User: gorzell
 * Date: 8/10/12
 */
object DynamicDoubleProperty {
  def apply(propertyName: String, defaultValue: Double) =
    new DynamicDoubleProperty(propertyName, defaultValue)

  def apply(propertyName: String, defaultValue: Double, callback: () => Unit) = {
    val p = new DynamicDoubleProperty(propertyName, defaultValue)
    p.addCallback(callback)
    p
  }
}

class DynamicDoubleProperty(
  override val propertyName: String,
  override val defaultValue: Double)
extends DynamicProperty[Double]
{
  override protected val box = new PropertyBox[Double, jDouble] {
    override val prop = DynamicPropertyFactory.getInstance().getDoubleProperty(propertyName, defaultValue)
    def convert(jt: jDouble): Double = jt
  }
}
