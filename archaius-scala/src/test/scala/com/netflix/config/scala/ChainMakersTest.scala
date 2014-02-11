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

import com.netflix.config.{ChainedDynamicProperty, DynamicIntProperty => JavaDynamicIntProperty}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class ChainMakersTest extends PropertiesTestHelp with ShouldMatchers {

  import ChainMakers._

  private def wrapRoot(p: String, r: JavaDynamicIntProperty) = new ChainedDynamicProperty.IntProperty(p, r)
  private def wrapLink(p: String, n: ChainedDynamicProperty.IntProperty) = new ChainedDynamicProperty.IntProperty(p, n)

  "fanPropertyName" should {
    "understand a single-part name with prefix and suffix" in {
      fanPropertyName(Some("x"), "a", Some("y")) should equal(Seq("x.y", "x.a.y"))
    }
    "understand a dual-part name with prefix and suffix" in {
      fanPropertyName(Some("x"), "a.b", Some("y")) should equal(Seq("x.y", "x.a.y", "x.a.b.y"))
    }
  }

  "deriveChain" should {
    "produce a chain given property names and helper functions" in {
      val baseName = "foo"
      val topName = s"${baseName}.bar"
      val default = -1
      Seq(baseName, topName) foreach clearProperty
      val chain = deriveChain( Seq(topName), intProperty(baseName, default), wrapRoot, wrapLink )
      chain should not be null
      chain.getName should be(topName)
      chain.getDefaultValue should be(default)
    }
    "produce a chain which produces the default value when properties are not set" in {
      val baseName = "foo"
      val topName = s"${baseName}.bar"
      val default = -1
      Seq(baseName, topName) foreach clearProperty
      val chain = deriveChain( Seq(topName), intProperty(baseName, default), wrapRoot, wrapLink )
      chain should not be null
      chain.getName should be(topName)
      chain.get() should be(default)
    }
    "produce a chain which responds to setting the root property" in {
      val baseName = "foo"
      val topName = s"${baseName}.bar"
      val baseValue = 1
      val default = -1
      Seq(baseName, topName) foreach clearProperty
      val chain = deriveChain( Seq(topName), intProperty(baseName, default), wrapRoot, wrapLink )
      chain should not be null
      chain.get() should be(default)
      setProperty(baseName, baseValue)
      chain.get() should be(baseValue)
    }
    "produce a chain which responds to setting a non-root property" in {
      val baseName = "foo"
      val topName = s"${baseName}.bar"
      val topValue = 2
      val default = -1
      Seq(baseName, topName) foreach clearProperty
      val chain = deriveChain( Seq(topName), intProperty(baseName, default), wrapRoot, wrapLink )
      chain should not be null
      chain.get() should be(default)
      setProperty(topName, topValue)
      chain.get() should be(topValue)
    }
    "produce a chain which prefers a non-root property's value over the root property" in {
      val baseName = "foo"
      val topName = s"${baseName}.bar"
      val baseValue = 1
      val topValue = 2
      val default = -1
      Seq(baseName, topName) foreach clearProperty
      val chain = deriveChain( Seq(topName), intProperty(baseName, default), wrapRoot, wrapLink )
      chain should not be null
      chain.get() should be(default)
      setProperty(baseName, baseValue)
      chain.get() should be(baseValue)
      setProperty(topName, topValue)
      chain.get() should be(topValue)
    }
    "explode given null property names" in {
      intercept[NullPointerException] {
        deriveChain( null, intProperty("foo", -1), wrapRoot, wrapLink )
      }
    }
    "explode given no property names" in {
      intercept[NoSuchElementException] {
        deriveChain( Seq.empty, intProperty("foo", -1), wrapRoot, wrapLink )
      }
    }
  }
}
