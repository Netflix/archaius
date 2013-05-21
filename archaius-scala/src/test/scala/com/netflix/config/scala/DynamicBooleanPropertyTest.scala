package com.netflix.config.scala

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import com.netflix.config.scala.DynamicProperties._
import com.netflix.config.ConfigurationManager

/**
 * Date: 5/21/13
 * Time: 10:48 AM
 * @author gorzell
 */

@RunWith(classOf[JUnitRunner])
class DynamicBooleanPropertyTest extends WordSpec with ShouldMatchers {
  private val propertyName = "dynamicExecutionTest"
  private val property = dynamicBooleanProperty(propertyName, true)

  private val config = ConfigurationManager.getConfigInstance

  "DynamicBooleanPropertyTest" should {
    "Execute the code" in {
      config.setProperty(propertyName, true.toString)

      var executionCount = 0

      val result = property.ifEnabled {
        executionCount += 1
        1
      }

      result should be(Option(1))
      executionCount should be(1)
    }

    "Not execute the code" in {
      config.setProperty(propertyName, false.toString)

      var executionCount = 0

      val result = property.ifEnabled {
        executionCount += 1
        1
      }

      result should be(None)
      executionCount should be(0)
    }
  }
}