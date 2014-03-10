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

import com.netflix.config.{PropertyWrapper, DynamicPropertyFactory}
import java.lang.{Boolean => jBoolean}

/**
 * User: gorzell
 * Date: 8/10/12
 */
class DynamicBooleanProperty(
  override val propertyName: String,
  override val defaultValue: Boolean,
  callback: Option[() => Unit] = None)
extends DynamicProperty[Boolean]
{

  callback.foreach(addCallback)

  override protected val box = new PropertyBox[Boolean, jBoolean] {
    override val prop: PropertyWrapper[jBoolean] = DynamicPropertyFactory.getInstance().getBooleanProperty(propertyName, defaultValue)

    def convert(jt: jBoolean): Boolean = jt
  }

  def ifEnabled[T] (r: => T): Option[T] = {
    box.get match {
      case true => Some(r)
      case false => None
    }
  }
}
