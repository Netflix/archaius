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

import com.netflix.config.scala.ChainMakers.ChainBox
import com.netflix.config.{ChainedDynamicProperty, DynamicPropertyFactory}
import com.netflix.config.{DynamicFloatProperty => JavaDynamicFloatProperty}

class ChainedFloatProperty(
  override val propertyNames: Iterable[String],
  override val defaultValue: Float,
  callback: Option[() => Unit] = None)
extends ChainedProperty[Float]
{

  def this(prefix: Option[String], name: String, suffix: Option[String], default: Float, callback: Option[() => Unit] = None) = {
    this(ChainMakers.fanPropertyName(prefix, name, suffix), default, callback)
  }

  callback.foreach(addCallback)

  override protected val box =  new ChainBox[Float, java.lang.Float] {

    override protected lazy val typeName = classOf[Float].getName

    override protected val chain = ChainMakers.deriveChain(
      propertyNames.tail,
      DynamicPropertyFactory.getInstance.getFloatProperty(propertyNames.head, ChainedFloatProperty.this.defaultValue),
      wrapRoot,
      wrapLink
    )

    private def wrapRoot(p: String, r: JavaDynamicFloatProperty) = new ChainedDynamicProperty.FloatProperty(p, r)
    private def wrapLink(p: String, n: ChainedDynamicProperty.FloatProperty) = new ChainedDynamicProperty.FloatProperty(p, n)

    /**
     * Convert the java.lang.Boolean which DynamicFloatProperty returns to scala.Float.
     * The value is guaranteed to be non-null thanks to [[com.netflix.config.scala.ChainMakers.ChainBox.nonNull]].
     * @return the value of the chain of properties, implicitly converted.
     */
    protected def convert(jv:java.lang.Float): Float = jv
  }
}
