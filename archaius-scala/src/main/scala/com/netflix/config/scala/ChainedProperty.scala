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

import com.netflix.config.ChainedDynamicProperty.ChainLink

/**
 * Base functionality of an [[com.netflix.config.ChainedDynamicProperty]], in Scala terms.
 * @tparam TYPE the Scala type provided by the property.
 * @tparam JAVATYPE the Java type in the supporting [[com.netflix.config.ChainedDynamicProperty.ChainLink]].
 */
trait ChainedProperty[TYPE, JAVATYPE] {

  protected def typeName: String

  /*
   * The second generic property is necessary to provide the type for the ChainLink.  Conversions
   * among values are implicit but conversions among classes are not.  So for example a scala.Int 42
   * is implicitly converted to/from a java.lang.Integer 42, but classOf[scala.Int] does not
   * implicitly convert to classOf[java.lang.Integer].
   */
  protected val chain: ChainLink[JAVATYPE]

  /*
   * Provides an anchor for scala's implicit conversions to happen.
   */
  protected def convert(jv: JAVATYPE): TYPE

  protected def nonNull(value: JAVATYPE): JAVATYPE = {
    if ( value == null ) {
      throw new IllegalStateException(s"${propertyName} should somewhere have a hard value instead of null, cannot be converted to scala type ${typeName}")
    }
    value
  }

  def get: TYPE = convert(nonNull(chain.get()))

  def apply: Option[TYPE] = Option(convert(chain.get()))

  def propertyName: String = chain.getName

  def propertyNames: Iterable[String]

  def defaultValue: TYPE = convert(chain.getDefaultValue)

  def addCallback(callback: Runnable) {
    chain.addCallback(callback)
  }

  override def toString: String = s"[${propertyName}] = ${get}"
}
