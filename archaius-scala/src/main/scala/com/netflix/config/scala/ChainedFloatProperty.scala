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

import com.netflix.config.{ChainedDynamicProperty, DynamicPropertyFactory}
import com.netflix.config.{DynamicFloatProperty => JavaDynamicFloatProperty}

class ChainedFloatProperty(
  override val propertyNames: Iterable[String],
  override val defaultValue: Float,
  callback: Option[Runnable] = None)
extends ChainedProperty[Float, java.lang.Float]
{

  def this(prefix: Option[String], name: String, suffix: Option[String], default: Float, callback: Option[Runnable] = None) = {
    this(ChainMakers.fanPropertyName(prefix, name, suffix), default, callback)
  }

  callback.foreach(addCallback)

  override protected lazy val typeName = classOf[Float].getName

  private def wrapRoot(p: String, r: JavaDynamicFloatProperty) = new ChainedDynamicProperty.FloatProperty(p, r)
  private def wrapLink(p: String, n: ChainedDynamicProperty.FloatProperty) = new ChainedDynamicProperty.FloatProperty(p, n)

  override protected val chain = ChainMakers.deriveChain(
    propertyNames.tail,
    DynamicPropertyFactory.getInstance.getFloatProperty(propertyNames.head, defaultValue),
    wrapRoot,
    wrapLink
  )

  /**
   * Convert the java.lang.Boolean which DynamicFloatProperty returns to scala.Float.
   * the caller.  The value is guaranteed to be non-null thanks to [[com.netflix.config.scala.ChainedProperty.nonNull]].
   * @return the value of the chain of properties, implicitly converted.
   */
  protected def convert(jv:java.lang.Float): Float = jv
}