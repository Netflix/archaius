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
class ChainedLongPropertyTest extends ChainedPropertiesTest with ShouldMatchers {

  "ChainedLongProperty" should {
    "understand a name with one part" in {
      val defaultValue = -1L
      clearProperty("test.part")
      clearProperty("test.one.part")
      val chainOfTwo = new ChainedLongProperty(Some("test"), "one", Some("part"), defaultValue)
      withClue(markProperty("")) { chainOfTwo.get should equal( defaultValue ) }
    }
    "retrieve configured base value for a name with one part" in {
      val defaultValue = -1L
      val configuredValue = 1L
      setProperty("test.part", configuredValue)
      val chainOfTwo = new ChainedLongProperty(Some("test"), "one", Some("part"), defaultValue)
      withClue(markProperty("test.part")) { chainOfTwo.get should equal( configuredValue ) }
    }
    "retrieve configured specific value for a name with one part" in {
      val defaultValue = -1L
      val configuredValue = 1L
      clearProperty("test.part")
      setProperty("test.one.part", configuredValue)
      val chainOfTwo = new ChainedLongProperty(Some("test"), "one", Some("part"), defaultValue)
      withClue(markProperty("test.one.part")) { chainOfTwo.get should equal( configuredValue ) }
    }
    "understand a name with two parts" in {
      val defaultValue = -1L
      clearProperty("test.parts")
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedLongProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("")) { chainOfThree.get should equal( defaultValue ) }
    }
    "retrieve configured most-specific value for a name with two parts" in {
      val defaultValue = -1L
      val configuredValue = 1L
      clearProperty("test.parts")
      clearProperty("test.one.parts")
      setProperty("test.one.two.parts", configuredValue)
      val chainOfThree = new ChainedLongProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.one.two.parts")) { chainOfThree.get should equal( configuredValue ) }
    }
    "retrieve configured next-general value for a name with two parts" in {
      val defaultValue = -1L
      val configuredValue = 1L
      clearProperty("test.parts")
      setProperty("test.one.parts", configuredValue)
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedLongProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.one.parts")) { chainOfThree.get should equal( configuredValue ) }
    }
    "retrieve configured most-specific value from a multi-part chain" in {
      val defaultValue = -1L
      val bottomValue = 0L
      val middleValue = 1L
      val topValue = 2L
      setProperty("test.parts", bottomValue)
      setProperty("test.one.parts", middleValue)
      setProperty("test.one.two.parts", topValue)
      val chainOfThree = new ChainedLongProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.one.two.parts")) { chainOfThree.get should equal( topValue ) }
    }
    "retrieve configured next-general value from a multi-part chain" in {
      val defaultValue = -1L
      val middleValue = 1L
      val bottomValue = 0L
      setProperty("test.parts", bottomValue)
      setProperty("test.one.parts", middleValue)
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedLongProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.one.parts")) { chainOfThree.get should equal( middleValue ) }
    }
    "retrieve configured most-general value from a multi-part chain" in {
      val defaultValue = -1L
      val bottomValue = 0L
      setProperty("test.parts", bottomValue)
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedLongProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("test.parts")) { chainOfThree.get should equal( bottomValue ) }
    }
    "retrieve default value from a multi-part chain" in {
      val defaultValue = -1L
      clearProperty("test.parts")
      clearProperty("test.one.parts")
      clearProperty("test.one.two.parts")
      val chainOfThree = new ChainedLongProperty(Some("test"), "one.two", Some("parts"), defaultValue)
      withClue(markProperty("")) { chainOfThree.get should equal( defaultValue ) }
    }
  }

}
