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
class DynamicFloatPropertyTest extends PropertiesTestHelp with ShouldMatchers with DynamicPropertyBehaviors[Float] {

  override def fixture(name: String) =
    DynamicFloatProperty(name, 1.0f)
  override def fixtureWithCallback(name: String, callback: () => Unit) =
    DynamicFloatProperty(name, 1.0f, callback)

  "DynamicFloatProperty" should {
    behave like dynamicProperty(1.0f, 2.2f)
  }
}
