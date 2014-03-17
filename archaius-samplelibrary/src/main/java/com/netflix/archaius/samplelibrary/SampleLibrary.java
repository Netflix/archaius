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
package com.netflix.archaius.samplelibrary;

/**
 * Simple Sample Library. To demonstrate how to utilize the properties
 * management aspect of Archaius in a library loaded in applications and web
 * apps.
 * 
 * Libraries (published as JAR files) can contain properties conventionally
 * located at META-INF/conf/config.proeprties.
 * 
 * The ClasspathPropertiesConfiguration of Archaius will scan through all JAR
 * files in the classpath and load all properties located in the above location
 * in each JAR file.
 * 
 * As an example, look at the META-INF/conf/config.properties file of this JAR/library
 * 
 * @author stonse
 * 
 */
public class SampleLibrary {

	String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
