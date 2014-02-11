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

import com.netflix.config.ChainedDynamicProperty

class ChainedBooleanProperty(
  override val propertyNames: Iterable[String],
  override val defaultValue: Boolean,
  callback: Option[Runnable] = None)
extends ChainedProperty[Boolean, java.lang.Boolean]
{

  def this(prefix: Option[String], name: String, suffix: Option[String], default: Boolean, callback: Option[Runnable] = None) = {
    this(ChainMakers.fanPropertyName(prefix, name, suffix), default, callback)
  }

  callback.foreach(addCallback)

  override protected lazy val typeName = classOf[Boolean].getName

  private def wrapRoot(p: String, r: ChainedDynamicProperty.DynamicBooleanPropertyThatSupportsNull) = new ChainedDynamicProperty.BooleanProperty(p, r)
  private def wrapLink(p: String, n: ChainedDynamicProperty.BooleanProperty) = new ChainedDynamicProperty.BooleanProperty(p, n)

  override protected val chain = ChainMakers.deriveChain(
    propertyNames.tail,
    new ChainedDynamicProperty.DynamicBooleanPropertyThatSupportsNull(propertyNames.head, defaultValue),
    wrapRoot,
    wrapLink
  )

  /**
   * Convert the java.lang.Boolean which DynamicBooleanProperty returns to scala.Boolean.
   * The value is guaranteed to be non-null thanks to [[com.netflix.config.scala.ChainedProperty.nonNull]].
   * @return the value of the chain of properties, implicitly converted.
   */
  protected def convert(jv:java.lang.Boolean): Boolean = jv
}
