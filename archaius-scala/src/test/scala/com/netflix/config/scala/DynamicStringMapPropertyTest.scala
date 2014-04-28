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
class DynamicStringMapPropertyTest extends PropertiesTestHelp with ShouldMatchers with DynamicPropertyBehaviors[Map[String, String]] {

  override def fixture(name: String) =
    DynamicStringMapProperty(name, Map("a" -> "x", "b" -> "y", "c" -> "z"), ",")
  override def fixtureWithCallback(name: String, callback: () => Unit) =
    DynamicStringMapProperty(name, Map("a" -> "x", "b" -> "y", "c" -> "z"), ",", callback)

  "DynamicStringMapProperty" should {
    behave like dynamicProperty("a=x,b=y,c=z", "1=q,2=r,3=s", Map("a" -> "x", "b" -> "y", "c" -> "z"), Map("1" -> "q", "2" -> "r", "3" -> "s"))
  }

}
