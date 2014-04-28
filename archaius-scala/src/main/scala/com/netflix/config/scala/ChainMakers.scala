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
import com.netflix.config.Property

/**
 * Utilities for assembling the chains of dynamic properties underlying a [[com.netflix.config.scala.ChainedProperty]].
 */
protected[scala] object ChainMakers {

  trait ChainBox[TYPE, JAVATYPE] extends PropertyBox[TYPE, JAVATYPE] {

    override protected def prop = chain
    protected def chain: ChainLink[JAVATYPE]

    protected def typeName: String

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
     * Get the name of the property.
     * @return the property name
     */
    def propertyName: String = chain.getName

    /**
     * Get the default value of the chain of properties.  Where the Scala type allows it, it may be null,
     * ie. scala.String is [[scala.AnyRef]] may be null but numbers are [[scala.AnyVal]] and so may not
     * be null.
     * @return the default value from the chain of properties.
     */
    def defaultValue: TYPE = convert(chain.getDefaultValue)

    /**
     * Produce the most-appropriate current value of the chain of properties.  Where the Scala type allows
     * it, it may be null, ie. scala.String is [[scala.AnyRef]] may be null but numbers are [[scala.AnyVal]]
     * and so may not be null.
     * @return the value derived from the chain of properties.
     */
    override def get: TYPE = convert(nonNull(chain.get()))
  }

  class ChainBoxConverter[B, TYPE](chainBox: ChainBox[TYPE,_], fn: (TYPE) => B, mapType: Manifest[B])
  extends ChainBox[B, TYPE]
  {
    override protected lazy val typeName = mapType.runtimeClass.getName

    // all calls in some way divert to the provided chainBox
    override protected val chain : ChainLink[TYPE] = null

    override protected def convert(cv: TYPE): B = fn(cv)

    override def apply(): Option[B] = chainBox().map(fn)

    override def get: B = fn(chainBox.get)

    override def addCallback(callback: () => Unit) {
      chainBox.addCallback(callback)
    }

    override def propertyName: String = chainBox.propertyName

    override def defaultValue: B = convert(chainBox.defaultValue)
  }

  /**
   * Assemble a set of property names from
   *
   *  - an optional prefix;
   *  - a name which may be a dot-separated hierarchy of names;
   *  - an optional suffix.
   *
   * The set of names is derived from the hierarchy of the central name.  For example, for prefix x, suffix y, and name a.b.c,
   * the set is
   *
   *  - x.y
   *  - x.a.y
   *  - x.a.b.y
   *  - x.a.b.c.y
   *
   * @param basePropertyPrefix the beginning of the property names.
   * @param name the (optionally dot-separated) name which is processed to create the set of property names.
   * @param basePropertySuffix the end of the property names.
   * @return the set of property names.
   */
  def fanPropertyName(basePropertyPrefix: Option[String], name: String, basePropertySuffix: Option[String]): Iterable[String] = {
    val segments = name.split("\\.")
    (0 to segments.length).map { n =>
      (Seq(basePropertyPrefix).flatten ++ segments.take(n) ++ Seq(basePropertySuffix).flatten).mkString(".")
    }
  }

  /**
   * Create the [[com.netflix.config.ChainedDynamicProperty.ChainLink]]s which underpin a [[com.netflix.config.scala.ChainedProperty]], properly related.
   *
   * @param propertyNames the set of property names for which to create the chain.  The second-most general property is the
   *                      first; the most specific property is last.
   * @param root          the PropertyWrapper for the most general property.
   * @param rootWrap      a function to create the [[com.netflix.config.ChainedDynamicProperty.ChainLink]] which contains the root [[com.netflix.config.PropertyWrapper]].
   * @param linkWrap      a function to create all [[com.netflix.config.ChainedDynamicProperty.ChainLink]]s which contain another [[com.netflix.config.ChainedDynamicProperty.ChainLink]].
   * @tparam T            the type of the value which the chain returns.
   * @tparam PWT          the type of the root PropertyWrapper of the chain.
   * @tparam CLT          the type of all [[com.netflix.config.ChainedDynamicProperty.ChainLink]]s in the chain.
   * @return              a [[com.netflix.config.ChainedDynamicProperty.ChainLink]] which represents the most specific property, containing all other [[com.netflix.config.ChainedDynamicProperty.ChainLink]]s.
   */
  def deriveChain[T, PWT, CLT](
    propertyNames: => Iterable[String],
    root: PWT,
    rootWrap: (String, PWT) => CLT,
    linkWrap: (String, CLT) => CLT): CLT =
  {
    val derivatives = propertyNames.foldLeft[Option[CLT]]( None ) { (chain, propName) =>
      val derivative = chain match {
        case Some(previous) => linkWrap(propName, previous)
        case None           => rootWrap(propName, root)
      }
      Some( derivative )
    }
    derivatives.get
  }
}
