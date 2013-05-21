/**
 * Copyright 2013 Netflix, Inc.
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

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import com.netflix.config.scala.DynamicProperties._
import com.netflix.config.ConfigurationManager

/**
 * Date: 5/21/13
 * Time: 10:48 AM
 * @author gorzell
 */

@RunWith(classOf[JUnitRunner])
class DynamicBooleanPropertyTest extends WordSpec with ShouldMatchers {
  private val propertyName = "dynamicExecutionTest"
  private val property = dynamicBooleanProperty(propertyName, true)

  private val config = ConfigurationManager.getConfigInstance

  "DynamicBooleanPropertyTest" should {
    "Execute the code" in {
      config.setProperty(propertyName, true.toString)

      var executionCount = 0

      val result = property.ifEnabled {
        executionCount += 1
        1
      }

      result should be(Option(1))
      executionCount should be(1)
    }

    "Not execute the code" in {
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