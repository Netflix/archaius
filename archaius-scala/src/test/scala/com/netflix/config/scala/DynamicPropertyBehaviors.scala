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
import org.scalatest.concurrent.{IntegrationPatience, Eventually}
import org.scalatest.time.{Millis, Seconds, Span}

trait DynamicPropertyBehaviors[TYPE] extends Eventually with IntegrationPatience { this: PropertiesTestHelp with ShouldMatchers =>

  override implicit val patienceConfig = PatienceConfig(
    timeout = scaled(Span(15, Seconds)),
    interval = scaled(Span(150, Millis))
  )

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
      eventually {
        val property = fixture(propertyName)
        withClue(markProperty(propertyName)) {
          val current = property.get
          current should equal( expectedConfiguredValue )
        }
      }
    }
    "call a registered callback on change" in {
      var executionCount = 0
      def callback() {
        executionCount += 1
      }
      val property = fixture(propertyName)
      property.addCallback(callback)
      setProperty(propertyName, configuredValue)
      eventually { executionCount should be(1) }
    }
    "not call a registered callback after callbacks are removed, on change" in {
      var executionCount = 0
      def callback() {
        executionCount += 1
      }
      val property = fixture(propertyName)
      property.addCallback(callback)
      setProperty(propertyName, configuredValue)
      eventually {
        executionCount should be(1)
        property.removeAllCallbacks()
        setProperty(propertyName, defaultValue)
        eventually {
          executionCount should be(1)
          setProperty(propertyName, configuredValue)
          eventually { executionCount should be(1) }
        }
      }
    }
  }

}
