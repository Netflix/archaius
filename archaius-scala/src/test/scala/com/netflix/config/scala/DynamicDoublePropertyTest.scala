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
import org.scalatest.Matchers

@RunWith(classOf[JUnitRunner])
class DynamicDoublePropertyTest extends PropertiesTestHelp with Matchers with DynamicPropertyBehaviors[Double] {

  override def fixture(name: String) =
    DynamicDoubleProperty(name, 1.0d)
  override def fixtureWithCallback(name: String, callback: () => Unit) =
    DynamicDoubleProperty(name, 1.0d, callback)

  "DynamicDoubleProperty" should {
    behave like dynamicProperty(1.0d, 2.2d)
  }
}
