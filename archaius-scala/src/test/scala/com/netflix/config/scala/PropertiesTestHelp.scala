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

import com.netflix.config.{ConfigurationManager, DynamicPropertyFactory, SimpleDeploymentContext}
import org.scalatest.{WordSpec, BeforeAndAfterAll}
import scala.collection.JavaConverters._
import java.util.Properties

object PropertiesTestHelp {
  val context = new SimpleDeploymentContext()
  context.setApplicationId(getClass.getSimpleName)
  context.setDeploymentEnvironment("dev")
  context.setDeploymentStack("none")
}

trait PropertiesTestHelp extends WordSpec with BeforeAndAfterAll {

  import PropertiesTestHelp._

  override def beforeAll() {
    ConfigurationManager.getConfigInstance.clear()
    ConfigurationManager.setDeploymentContext(context)
    ConfigurationManager.getConfigInstance.setProperty("com.blackpearl.config.dynamo.disable", "true")
    ConfigurationManager.getConfigInstance.setProperty("com.netflix.karyon.eureka.disable", "true")
  }

  override def afterAll() {
    ConfigurationManager.getConfigInstance.clear()
  }

  def clearProperty(p: String) = ConfigurationManager.getConfigInstance.clearProperty(p)

  def setProperty(p: String, v: Any) = ConfigurationManager.getConfigInstance.setProperty(p, v)

  def markProperty(markName: String): String = {
    val keys = ConfigurationManager.getConfigInstance.getKeys("test").asScala.toSeq.sorted
    val kvs = keys.seq.map( key =>
      s"${if(key.equals(markName)) "*" else " "}${key}\t = ${ConfigurationManager.getConfigInstance.getProperty(key)}"
    )
    s"Current properties:\n\t${kvs.mkString("\n\t")}\nFailure: "
  }

  def intProperty(name: String, default: Int) = {
    DynamicPropertyFactory.getInstance.getIntProperty(name, default)
  }
}
