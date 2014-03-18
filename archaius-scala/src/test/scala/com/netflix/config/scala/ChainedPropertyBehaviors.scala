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

import org.scalatest.matchers.ShouldMatchers

trait ChainedPropertyBehaviors[TYPE] { this: PropertiesTestHelp with ShouldMatchers =>

  def fixture(pre: Option[String], mid: String, post: Option[String]): ChainedProperty[TYPE]

  def chainedPropertyWithOnePart(defaultValue: TYPE, configuredValue: TYPE) {
    chainedPropertyWithOnePart(defaultValue, configuredValue, defaultValue, configuredValue)
  }

  def chainedPropertyWithOnePart(defaultValue: Any, configuredValue: Any, expectedDefaultValue: TYPE, expectedConfiguredValue: TYPE) {
    "understand a name with one part" in {
      clearProperty("test.part")
      clearProperty("test.one.part")
      val chainOfTwo = fixture(Some("test"), "one", Some("part"))
      withClue(markProperty("")) { chainOfTwo.get should equal( expectedDefaultValue ) }
    }
    "retrieve configured base value for a name with one part" in {
      setProperty("test.part", configuredValue)
      val chainOfTwo = fixture(Some("test"), "one", Some("part"))
      withClue(markProperty("test.part")) { chainOfTwo.get should equal( expectedConfiguredValue ) }
    }
    "retrieve configured specific value for a name with one part" in {
      clearProperty("test.part")
      setProperty("test.one.part", configuredValue)
      val chainOfTwo = fixture(Some("test"), "one", Some("part"))
      withClue(markProperty("test.one.part")) { chainOfTwo.get should equal( expectedConfiguredValue ) }
    }
  }

  def chainedPropertyWithTwoParts(defaultValue: TYPE, configuredValue: TYPE) {
    chainedPropertyWithTwoParts(defaultValue, configuredValue, defaultValue, configuredValue)
  }

  def chainedPropertyWithTwoParts(defaultValue: Any, configuredValue: Any, expectedDefaultValue: TYPE, expectedConfiguredValue: TYPE) {
    "understand a name with two parts" in {
      clearProperty("test.parts")
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = fixture(Some("test"), "one.two", Some("parts"))
      withClue(markProperty("")) { chainOfThree.get should equal( expectedDefaultValue ) }
    }
    "retrieve configured most-specific value for a name with two parts" in {
      clearProperty("test.parts")
      clearProperty("test.one.parts")
      setProperty("test.one.two.parts", configuredValue)
      val chainOfThree = fixture(Some("test"), "one.two", Some("parts"))
      withClue(markProperty("test.one.two.parts")) { chainOfThree.get should equal( expectedConfiguredValue ) }
    }
    "retrieve configured next-general value for a name with two parts" in {
      clearProperty("test.parts")
      setProperty("test.one.parts", configuredValue)
      clearProperty("test.one.two.parts")
      val chainOfThree = fixture(Some("test"), "one.two", Some("parts"))
      withClue(markProperty("test.one.parts")) { chainOfThree.get should equal( expectedConfiguredValue ) }
    }
  }

  def chainedPropertyWithManyParts(defaultValue: TYPE, bottomValue: TYPE, middleValue: TYPE, topValue: TYPE) {
    chainedPropertyWithManyParts(defaultValue, bottomValue, middleValue, topValue, defaultValue, bottomValue, middleValue, topValue)
  }

  def chainedPropertyWithManyParts(defaultValue: Any, bottomValue: Any, middleValue: Any, topValue: Any, expectedDefaultValue: TYPE, expectedBottomValue: TYPE, expectedMiddleValue: TYPE, expectedTopValue: TYPE) {
    "retrieve configured most-specific value from a multi-part chain" in {
      setProperty("test.parts", bottomValue)
      setProperty("test.one.parts", middleValue)
      setProperty("test.one.two.parts", topValue)
      val chainOfThree = fixture(Some("test"), "one.two", Some("parts"))
      withClue(markProperty("test.one.two.parts")) { chainOfThree.get should equal( expectedTopValue ) }
    }
    "retrieve configured next-general value from a multi-part chain" in {
      setProperty("test.parts", bottomValue)
      setProperty("test.one.parts", middleValue)
      clearProperty("test.one.two.parts")
      val chainOfThree = fixture(Some("test"), "one.two", Some("parts"))
      withClue(markProperty("test.one.parts")) { chainOfThree.get should equal( expectedMiddleValue ) }
    }
    "retrieve configured most-general value from a multi-part chain" in {
      setProperty("test.parts", bottomValue)
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = fixture(Some("test"), "one.two", Some("parts"))
      withClue(markProperty("test.parts")) { chainOfThree.get should equal( expectedBottomValue ) }
    }
    "retrieve default value from a multi-part chain" in {
      clearProperty("test.parts")
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = fixture(Some("test"), "one.two", Some("parts"))
      withClue(markProperty("")) { chainOfThree.get should equal( expectedDefaultValue ) }
    }
  }
}
