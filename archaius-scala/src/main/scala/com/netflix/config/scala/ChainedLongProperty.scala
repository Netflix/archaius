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
import com.netflix.config.{DynamicLongProperty => JavaDynamicLongProperty}

class ChainedLongProperty(
  override val propertyNames: Iterable[String],
  override val defaultValue: Long,
  callback: Option[() => Unit] = None)
extends ChainedProperty[Long]
{

  def this(prefix: Option[String], name: String, suffix: Option[String], default: Long, callback: Option[() => Unit] = None) = {
    this(ChainMakers.fanPropertyName(prefix, name, suffix), default, callback)
  }

  callback.foreach(addCallback)

  override protected val box = new ChainBox[Long, java.lang.Long] {

    override protected lazy val typeName = classOf[Long].getName

    override protected val chain = ChainMakers.deriveChain(
      propertyNames.tail,
      DynamicPropertyFactory.getInstance.getLongProperty(propertyNames.head, ChainedLongProperty.this.defaultValue),
      wrapRoot,
      wrapLink
    )

    private def wrapRoot(p: String, r: JavaDynamicLongProperty) = new ChainedDynamicProperty.LongProperty(p, r)
    private def wrapLink(p: String, n: ChainedDynamicProperty.LongProperty) = new ChainedDynamicProperty.LongProperty(p, n)

    /**
     * Convert the java.lang.Long which DynamicLongProperty returns to scala.Long.
     * The value is guaranteed to be non-null thanks to [[com.netflix.config.scala.ChainMakers.ChainBox.nonNull]].
     * @return the value of the chain of properties, implicitly converted.
     */
    protected def convert(jv:java.lang.Long): Long = jv
  }
}
