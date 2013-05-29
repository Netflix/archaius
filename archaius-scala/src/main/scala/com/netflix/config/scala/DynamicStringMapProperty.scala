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

import scala.collection.JavaConversions._

/**
 * User: gorzell
 * Date: 9/25/12
 */
class DynamicStringMapProperty(property: String, default: Map[String, String], delimiterRegex: String) {
  private val prop = new com.netflix.config.DynamicStringMapProperty(property, default, delimiterRegex)

  def apply: Option[Map[String, String]] = Option(get)

  def get: Map[String, String] = prop.getMap.toMap

  def addCallback(callback: Runnable) {
    if (callback != null) prop.addCallback(callback)
  }
}