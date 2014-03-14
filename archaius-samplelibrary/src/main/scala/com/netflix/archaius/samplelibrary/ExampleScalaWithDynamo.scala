/**
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
package com.netflix.archaius.samplelibrary
/*
import com.netflix.config._
import com.amazonaws.auth.AWSCredentials
import com.netflix.config.sources.DynamoDbConfigurationSource
import org.apache.commons.configuration.AbstractConfiguration
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient
/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * You should write something useful here.
 */
class ExampleScalaWithDynamo extends ExampleScala {


     private val tableName = DynamicPropertyConstants.TableNameFormat.format(config.getString("deployment.name"))

     private val finalConfig = new ConcurrentCompositeConfiguration()
     finalConfig.setProperty("com.netflix.config.dynamo.tableName", tableName)
     finalConfig.setProperty("com.netflix.config.dynamo.keyAttributeName", DynamicPropertyConstants.KeyAttributeName)
     finalConfig.setProperty("com.netflix.config.dynamo.valueAttributeName", DynamicPropertyConstants.ValueAttributeName)

     if (ConfigurationManager.isConfigurationInstalled) ConfigurationManager.loadPropertiesFromConfiguration(finalConfig)
     else ConfigurationManager.install(finalConfig)

     info("Loading dynamic properties from dynamo: table=%s, keyName=%s, valueName=%s",
       finalConfig.getProperty("com.netflix.config.dynamo.tableName"),
       finalConfig.getProperty("com.netflix.config.dynamo.keyAttributeName"),
       finalConfig.getProperty("com.netflix.config.dynamo.valueAttributeName"))

     private val endpoint = DefaultAWSEndpointFinder.findEndpoint(awsCredentials,
       config.getString("deployment.region"), "dynamodb")
     info("Using DynamoDB endpoint: %s", endpoint)

     private val dynamo = new AmazonDynamoDBClient(awsCredentials)
     dynamo.setEndpoint(endpoint)

     private val source = new DynamoDbConfigurationSource(dynamo)
     private val scheduler = new FixedDelayPollingScheduler(0, pollFrequencySeconds.seconds.toInt, false)
     private val dynamicConfig = new DynamicConfiguration(source, scheduler)

     // add them in this order to make dynamicConfig override currentConfig
     finalConfig.addConfiguration(dynamicConfig)
     finalConfig.addConfiguration(currentConfig.asInstanceOf[AbstractConfiguration])
  }

}
*/