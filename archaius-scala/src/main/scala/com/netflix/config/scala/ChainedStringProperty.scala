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
import com.netflix.config.{DynamicStringProperty => JavaDynamicStringProperty}

class ChainedStringProperty(
  override val propertyNames: Iterable[String],
  override val defaultValue: String,
  callback: Option[() => Unit] = None)
extends ChainedProperty[String]
{

  def this(prefix: Option[String], name: String, suffix: Option[String], default: String, callback: Option[() => Unit] = None) = {
    this(ChainMakers.fanPropertyName(prefix, name, suffix), default, callback)
  }

  callback.foreach(addCallback)

  override protected val box = new ChainBox[String, java.lang.String] {

    override protected lazy val typeName = classOf[String].getName

    override protected val chain = ChainMakers.deriveChain(
      propertyNames.tail,
      DynamicPropertyFactory.getInstance.getStringProperty(propertyNames.head, ChainedStringProperty.this.defaultValue),
      wrapRoot,
      wrapLink
    )

    private def wrapRoot(p: String, r: JavaDynamicStringProperty) = new ChainedDynamicProperty.StringProperty(p, r)
    private def wrapLink(p: String, n: ChainedDynamicProperty.StringProperty) = new ChainedDynamicProperty.StringProperty(p, n)

    override protected def convert(jv: java.lang.String): String = jv

    override protected def nonNull(jv: java.lang.String) = jv
  }
}
