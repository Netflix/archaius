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
class ChainedFloatPropertyTest extends PropertiesTestHelp with ShouldMatchers {

  "ChainedFloatProperty" should {
    "understand a name with one part" in {
      val defaultValue = -1.1f
      clearProperty("test.part")
      clearProperty("test.one.part")
      val chainOfTwo = new ChainedFloatProperty(Some("test"), "one", Some("part"), defaultValue)
      withClue(markProperty("")) { chainOfTwo.get should equal( defaultValue ) }
    }
    "retrieve configured base value for a name with one part" in {
      val defaultValue = -1.1f
      val configuredValue = 1.1f
      setProperty("test.part", configuredValue)
      val chainOfTwo = new ChainedFloatProperty(Some("test"), "one", Some("part"), defaultValue)
      withClue(markProperty("test.part")) { chainOfTwo.get should equal( configuredValue ) }
    }
    "retrieve configured specific value for a name with one part" in {
      val defaultValue = -1.1f
      val configuredValue = 1.1f
      clearProperty("test.part")
      setProperty("test.one.part", configuredValue)
      val chainOfTwo = new ChainedFloatProperty(Some("test"), "one", Some("part"), defaultValue)
      withClue(markProperty("test.one.part")) { chainOfTwo.get should equal( configuredValue ) }
    }
    "understand a name with two parts" in {
      val defaultValue = -1.1f
      clearProperty("test.parts")
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedFloatProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("")) { chainOfThree.get should equal( defaultValue ) }
    }
    "retrieve configured most-specific value for a name with two parts" in {
      val defaultValue = -1.1f
      val configuredValue = 1.1f
      clearProperty("test.parts")
      clearProperty("test.one.parts")
      setProperty("test.one.two.parts", configuredValue)
      val chainOfThree = new ChainedFloatProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.one.two.parts")) { chainOfThree.get should equal( configuredValue ) }
    }
    "retrieve configured next-general value for a name with two parts" in {
      val defaultValue = -1.1f
      val configuredValue = 1.1f
      clearProperty("test.parts")
      setProperty("test.one.parts", configuredValue)
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedFloatProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.one.parts")) { chainOfThree.get should equal( configuredValue ) }
    }
    "retrieve configured most-specific value from a multi-part chain" in {
      val defaultValue = -1.1f
      val bottomValue = 0.0f
      val middleValue = 1.1f
      val topValue = 2.2f
      setProperty("test.parts", bottomValue)
      setProperty("test.one.parts", middleValue)
      setProperty("test.one.two.parts", topValue)
      val chainOfThree = new ChainedFloatProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.one.two.parts")) { chainOfThree.get should equal( topValue ) }
    }
    "retrieve configured next-general value from a multi-part chain" in {
      val defaultValue = -1.1f
      val middleValue = 1.1f
      val bottomValue = 0.0f
      setProperty("test.parts", bottomValue)
      setProperty("test.one.parts", middleValue)
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedFloatProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.one.parts")) { chainOfThree.get should equal( middleValue ) }
    }
    "retrieve configured most-general value from a multi-part chain" in {
      val defaultValue = -1.1f
      val bottomValue = 0.0f
      setProperty("test.parts", bottomValue)
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedFloatProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.parts")) { chainOfThree.get should equal( bottomValue ) }
    }
    "retrieve default value from a multi-part chain" in {
      val defaultValue = -1.1f
      clearProperty("test.parts")
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedFloatProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("")) { chainOfThree.get should equal( defaultValue ) }
    }
  }

}
