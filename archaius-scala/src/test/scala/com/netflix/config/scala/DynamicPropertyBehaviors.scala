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

  def fixtureWithCallback(name: String, callback: () => Unit): DynamicProperty[TYPE]

  // see dynamicProperty(Any, Any, TYPE, TYPE) below
  var directlySetExecutionCount = 0
  def directlySetCallback() {
    directlySetExecutionCount += 1
  }

  // see dynamicProperty(Any, Any, TYPE, TYPE) below
  var factorySetExecutionCount = 0
  def factorySetCallback() {
    factorySetExecutionCount += 1
  }

  def dynamicProperty(defaultValue: TYPE, configuredValue: TYPE) {
    dynamicProperty(defaultValue, configuredValue, defaultValue, configuredValue)
  }

  def dynamicProperty(defaultValue: Any, configuredValue: Any, expectedDefaultValue: TYPE, expectedConfiguredValue: TYPE) {

    // Required single copies of the properties, initialized once and with any callbacks attached,
    // to more closely model the behavior expected by ConfigurationManager.  Re-creating the
    // property ends up getting the original property back.  Then manipulating callbacks breaks
    // the callback mechanism.  Any attempt to create a specific ConfigurationManager environment
    // underneath the properties runs afoul of ConfigurationManager's assumptions of being
    // configured once-and-once-only, leaving isolatable testing methodologies in the lurch.
    val propertyWithDirectlySetCallback = fixture(propertyName)
    propertyWithDirectlySetCallback.addCallback(directlySetCallback)
    val propertyWithFactorySetCallback = fixtureWithCallback(propertyName, factorySetCallback)
    
    "provide access to property name via propertyName field" in {
      propertyWithDirectlySetCallback.propertyName should be(propertyName)
    }
    "provide access to default value via defaultValue field" in {
      propertyWithDirectlySetCallback.defaultValue should be(expectedDefaultValue)
    }
    "retrieve configured value" in {
      setProperty(propertyName, configuredValue)
      eventually {
        withClue(markProperty(propertyName)) {
          val current = propertyWithDirectlySetCallback.get
          withClue(s"default=${expectedDefaultValue}, configured=${expectedConfiguredValue}, ") { current should equal( expectedConfiguredValue ) }
        }
      }
    }
    "call a callback registered explicitly on change" in {
      propertyWithDirectlySetCallback.removeAllCallbacks()
      setProperty(propertyName, configuredValue)
      eventually { directlySetExecutionCount should be(1) }
    }
    "call a callback registered via factory method on change" in {
      setProperty(propertyName, configuredValue)
      eventually { factorySetExecutionCount should be(1) }
    }
    "not call a registered callback after callbacks are removed, on change" in {
      setProperty(propertyName, configuredValue)
      eventually {
        factorySetExecutionCount should be(1)
        propertyWithFactorySetCallback.removeAllCallbacks()
        setProperty(propertyName, defaultValue)
        eventually {
          factorySetExecutionCount should be(1)
          setProperty(propertyName, configuredValue)
          eventually { factorySetExecutionCount should be(1) }
        }
      }
    }
  }

}
