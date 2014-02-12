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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class ChainedBooleanPropertyTest extends PropertiesTestHelp with ShouldMatchers with ChainedPropertyBehaviors[Boolean] {

  val defaultValue = false

  def fixture(pre: Option[String], mid: String, post: Option[String]) = new ChainedBooleanProperty(pre, mid, post, defaultValue)

  "ChainedBooleanProperty" should {
    behave like chainedPropertyWithOnePart(defaultValue, true)
    behave like chainedPropertyWithTwoParts(defaultValue, true)

    // cannot derive from ChainedPropertiesTestHelp due to the values in this test vs the next test
    "retrieve configured most-specific value from a multi-part chain" in {
      val defaultValue = false
      val bottomValue = false
      val middleValue = false
      val topValue = true
      setProperty("test.parts", bottomValue)
      setProperty("test.one.parts", middleValue)
      setProperty("test.one.two.parts", topValue)
      val chainOfThree = new ChainedBooleanProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.one.two.parts")) { chainOfThree.get should equal( topValue ) }
    }
    // cannot derive from ChainedPropertiesTestHelp due to the values in this test vs the previous test
    "retrieve configured next-general value from a multi-part chain" in {
      val defaultValue = false
      val middleValue = true
      val bottomValue = false
      setProperty("test.parts", bottomValue)
      setProperty("test.one.parts", middleValue)
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedBooleanProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.one.parts")) { chainOfThree.get should equal( middleValue ) }
    }
    "retrieve configured most-general value from a multi-part chain" in {
      val defaultValue = false
      val bottomValue = true
      setProperty("test.parts", bottomValue)
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedBooleanProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.parts")) { chainOfThree.get should equal( bottomValue ) }
    }
    // cannot derive from ChainedPropertiesTestHelp due to the values in this test vs the other tests
    "retrieve default value from a multi-part chain" in {
      val defaultValue = true
      clearProperty("test.parts")
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedBooleanProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("")) { chainOfThree.get should equal( defaultValue ) }
    }
  }
}
