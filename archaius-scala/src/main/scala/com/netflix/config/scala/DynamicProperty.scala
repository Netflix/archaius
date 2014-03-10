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

import com.netflix.config.PropertyWrapper

/**
 * Base functionality of a [[com.netflix.config.DynamicProperty]], in Scala terms.
 * @tparam T the Scala type produced by the property.
 */
trait DynamicProperty[T] {
  /**
   * Get the name of the property.
   * @return the property name
   */
  def propertyName: String = box.propertyName

  /**
   * Get the default value of the property.  Where the Scala type allows it, it may be null,
   * ie. scala.String is [[scala.AnyRef]] may be null but numbers are [[scala.AnyVal]] and so may not
   * be null.
   * @return the default value from the property.
   */
  def defaultValue: T = box.defaultValue

  /**
   * Produce the current value of the property.  Where the Scala type allows
   * it, it may be null, ie. scala.String is [[scala.AnyRef]] may be null but numbers are [[scala.AnyVal]]
   * and so may not be null.
   * @return the value retrieved from the property.
   */
  def get: T = box.get

  /**
   * Produce the current value of the property, as an [[scala.Option]].  Null
   * values results are represented as [[scala.None]], regardless of the possibility of the Scala type to
   * be null.
   * @return the value retrieved from the property.
   */
  def apply(): Option[T] = box()

  /**
   * Allow derivation of a new type of DynamicProperty by mapping the type of this one.
   * @param fn the transformation function which produces the new type.
   * @return a new DynamicProperty for the target type.
   */
  def map[B](fn: (T) => B)(implicit mapType: Manifest[B]): ChainedProperty[B] = new MapBy(box, fn, mapType)

  /**
   * Add a callback to be triggered when the value of the property is
   * changed.
   * @param callback a function to call on changes.
   */
  def addCallback(callback: () => Unit) {
    box.addCallback(Option(callback))
  }

  override def toString: String = s"[${propertyName}] = ${get}"

  protected val box: PropertyBox[T, _]

  protected abstract class PropertyBox[T, JT] {
    def prop: PropertyWrapper[JT]
    def propertyName: String = prop.getName
    def get: T = convert(prop.getValue)
    def apply(): Option[T] = Option(prop.getValue).map(convert)
    def defaultValue: T = convert(prop.getDefaultValue)
    def addCallback(callback: Option[() => Unit]) {
      callback.map( c => prop.addCallback( new Runnable { def run() { c() } } ) )
        .getOrElse(prop.addCallback(null))
    }
    def convert(jt: JT): T
  }

  protected class BoxConverter[B, TYPE](propertyBox: PropertyBox[TYPE,_], fn: (TYPE) => B, mapType: Manifest[B])
    extends PropertyBox[B, TYPE]
  {
    protected lazy val typeName = mapType.runtimeClass.getName

    // all calls in some way divert to the provided PropertyBox
    override protected val prop: PropertyWrapper[TYPE] = null

    protected def convert(cv: TYPE): B = fn(cv)

    override def apply(): Option[B] = propertyBox().map(fn)

    override def get: B = fn(propertyBox.get)

    override def addCallback(callback: Option[() => Unit]) {
      propertyBox.addCallback(callback)
    }

    override def propertyName: String = propertyBox.propertyName

    override def defaultValue: B = convert(propertyBox.defaultValue)
  }

  protected class MapBy[B, TYPE](unmappedBox: PropertyBox[TYPE, _], fn: (TYPE) => B, mapType: Manifest[B])
    extends ChainedProperty[B]
  {
    override protected val chainBox = new BoxConverter[B, TYPE](unmappedBox, fn, mapType)

    override def propertyNames: Iterable[String] = propertyNames
  }
}
