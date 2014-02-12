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
class ChainedStringPropertyTest extends PropertiesTestHelp with ShouldMatchers with ChainedPropertyBehaviors[String] {

  val defaultValue = "voo"

  def fixture(pre: Option[String], mid: String, post: Option[String]) = new ChainedStringProperty(pre, mid, post, defaultValue)

  "ChainedStringProperty" should {
    behave like chainedPropertyWithOnePart(defaultValue, "bar")
    behave like chainedPropertyWithTwoParts(defaultValue, "zar")
    behave like chainedPropertyWithManyParts(defaultValue, "cats", "gonz", "k00")
  }
}
