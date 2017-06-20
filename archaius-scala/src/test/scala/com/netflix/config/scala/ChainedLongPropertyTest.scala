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
class ChainedLongPropertyTest extends PropertiesTestHelp with Matchers with ChainedPropertyBehaviors[Long] {

  val defaultValue = -1L

  def fixture(pre: Option[String], mid: String, post: Option[String]) = new ChainedLongProperty(pre, mid, post, defaultValue)

  "ChainedLongProperty" should {
    behave like chainedPropertyWithOnePart(defaultValue, 1L)
    behave like chainedPropertyWithTwoParts(defaultValue, 1L)
    behave like chainedPropertyWithManyParts(defaultValue, 0L, 1L, 2L)
  }
}
