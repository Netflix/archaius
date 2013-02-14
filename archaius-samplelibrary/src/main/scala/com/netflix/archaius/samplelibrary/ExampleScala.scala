package com.netflix.archaius.samplelibrary

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * Example of loading and using dynamic properties in scala
 */
class ExampleScala extends App {

  import com.netflix.config.scala.DynamicProperties._

  val numRetries = dynamicIntProperty("numRetries", 1)
  val sayHello = dynamicBooleanProperty("hello", false)
  val helloText = dynamicStringProperty("helloText", "Hello world")

  if (sayHello.get) println(helloText.get)

  println("Number of times to try=%d", numRetries.get)
}
