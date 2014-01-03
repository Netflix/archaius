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

@RunWith(classOf[JUnitRunner])
class DynamicIntPropertyTest extends WordSpec with ShouldMatchers {
  private val propertyName = "dynamicIntTest"
  private val property = dynamicIntProperty(propertyName, 1)

  "DynamicIntProperty" should {
    "provide access to property name via propertyName field" in {
      property.propertyName should be(propertyName)
    }

    "provide access to property name via property field" in {
      property.property should be(propertyName)
    }
  }
}
