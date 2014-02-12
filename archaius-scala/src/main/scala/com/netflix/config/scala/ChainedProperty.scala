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
 * Base functionality of a [[com.netflix.config.ChainedDynamicProperty]], in Scala terms.
 * @tparam TYPE the Scala type produced by the property.
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

  /**
   * Validate the value's nullability for this property's Scala type.
   * @param value the value to return, as the Java type.
   * @return the value if it meets nullability requirements for the Scala type.
   */
  protected def nonNull(value: JAVATYPE): JAVATYPE = {
    if ( value == null ) {
      throw new IllegalStateException(s"${propertyName} should somewhere have a hard value instead of null, cannot be converted to scala type ${typeName}")
    }
    value
  }

  /**
   * Produce the most-appropriate current value of the chain of properties.  Where the Scala type allows
   * it, it may be null, ie. scala.String is [[scala.AnyRef]] may be null but numbers are [[scala.AnyVal]]
   * and so may not be null.
   * @return the value derived from the chain of properties.
   */
  def get: TYPE = convert(nonNull(chain.get()))

  /**
   * Produce the most-appropriate current value of the chain of properties, as an [[scala.Option]].  Null
   * values results are represented as [[scala.None]], regardless of the possibility of the Scala type to
   * be null.
   * @return the value derived from the chain of properties.
   */
  def apply: Option[TYPE] = Option(convert(chain.get()))

  /**
   * Get the name of the property.
   * @return the property name
   */
  def propertyName: String = chain.getName

  /**
   * Get the names of all properties in the chain.
   * @return all property names
   */
  def propertyNames: Iterable[String]

  /**
   * Get the default value of the chain of properties.  Where the Scala type allows it, it may be null,
   * ie. scala.String is [[scala.AnyRef]] may be null but numbers are [[scala.AnyVal]] and so may not
   * be null.
   * @return the default value from the chain of properties.
   */
  def defaultValue: TYPE = convert(chain.getDefaultValue)


  /**
   * Add a callback to be triggered when the value of the property is
   * changed.
   * @param callback a [[java.lang.Runnable]] to call on changes.
   */
  def addCallback(callback: Runnable) {
    chain.addCallback(callback)
  }

  override def toString: String = s"[${propertyName}] = ${get}"
}
