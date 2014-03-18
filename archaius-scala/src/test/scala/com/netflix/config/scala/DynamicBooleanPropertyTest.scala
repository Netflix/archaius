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

import com.netflix.config.ConfigurationManager
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

/**
 * Date: 5/21/13
 * Time: 10:48 AM
 * @author gorzell
 */

@RunWith(classOf[JUnitRunner])
class DynamicBooleanPropertyTest extends PropertiesTestHelp with ShouldMatchers with DynamicPropertyBehaviors[Boolean] {

  override def fixture(name: String) =
    DynamicBooleanProperty(name, true)
  override def fixtureWithCallback(name: String, callback: () => Unit) =
    DynamicBooleanProperty(name, true, callback)

  "DynamicBooleanProperty" should {
    behave like dynamicProperty(true, false)
  }

  "DynamicBooleanProperty.ifEnabled" should {
    "execute the provided function when value is true" in {
      val config = ConfigurationManager.getConfigInstance
      val property = fixture(propertyName)

      config.setProperty(propertyName, true.toString)

      var executionCount = 0

      val result = property.ifEnabled {
        executionCount += 1
        1
      }

      result should be(Option(1))
      executionCount should be(1)
    }

    "not execute the provided function when value is false" in {
      val config = ConfigurationManager.getConfigInstance
      val property = fixture(propertyName)

      config.setProperty(propertyName, false.toString)

      var executionCount = 0

      val result = property.ifEnabled {
        executionCount += 1
        1
      }

      result should be(None)
      executionCount should be(0)
    }
  }
}
