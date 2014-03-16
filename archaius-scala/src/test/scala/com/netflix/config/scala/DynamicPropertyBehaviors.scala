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

trait DynamicPropertyBehaviors[TYPE] { this: PropertiesTestHelp with ShouldMatchers =>

  val propertyName = s"test.prop.${this.getClass.getSimpleName}"

  def fixture(name: String): DynamicProperty[TYPE]

  def dynamicProperty(defaultValue: TYPE, configuredValue: TYPE) {
    dynamicProperty(defaultValue, configuredValue, defaultValue, configuredValue)
  }

  def dynamicProperty(defaultValue: Any, configuredValue: Any, expectedDefaultValue: TYPE, expectedConfiguredValue: TYPE) {
    "provide access to property name via propertyName field" in {
      fixture(propertyName).propertyName should be(propertyName)
    }
    "provide access to default value via defaultValue field" in {
      fixture(propertyName).defaultValue should be(expectedDefaultValue)
    }
    "retrieve configured value" in {
      setProperty(propertyName, configuredValue)
      val chainOfTwo = fixture(propertyName)
      withClue(markProperty(propertyName)) { chainOfTwo.get should equal( expectedConfiguredValue ) }
    }
  }

}
