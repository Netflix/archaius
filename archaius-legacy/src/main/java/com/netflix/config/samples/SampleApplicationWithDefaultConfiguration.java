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
package com.netflix.config.samples;


import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.DynamicPropertyFactory;

/**
 * A Sample Application built to showcase how to use the default ConcurrentCompositeConfiguration
 * registered with {@link DynamicPropertyFactory} and automatic registration with JMX
 * <p>
 * To run this sample application, add the following jars to your classpath:
 * <ul>
 * <li>archaius-core-xxx.jar (latest release/snapshot of archaius-core)
 * <li>commons-configuration-1.8.jar
 * <li>commons-lang-2.6.jar
 * <li>commons-logging-1.1.1.jar
 * </ul>
 * 
 * @author awang
 *
 */
public class SampleApplicationWithDefaultConfiguration {
    static {
        // sampleapp.properties is packaged within the shipped jar file
        System.setProperty("archaius.configurationSource.defaultFileName", "sampleapp.properties");
        System.setProperty(DynamicPropertyFactory.ENABLE_JMX, "true");
        System.setProperty("com.netflix.config.samples.SampleApp.SampleBean.sensitiveBeanData", "value from system property");
    }
    
    public static void main(String[] args) {
        new SampleApplication();
        ConcurrentCompositeConfiguration myConfiguration = 
            (ConcurrentCompositeConfiguration) DynamicPropertyFactory.getInstance().getBackingConfigurationSource();
        

        ConcurrentMapConfiguration subConfig = new ConcurrentMapConfiguration();
        subConfig.setProperty("com.netflix.config.samples.SampleApp.SampleBean.name", "A Coffee Bean from Cuba");
        myConfiguration.setProperty("com.netflix.config.samples.sampleapp.prop1", "value1");

        myConfiguration.addConfiguration(subConfig);
        System.out.println("Started SampleApplication. Launch JConsole to inspect and update properties.");
        System.out.println("To see how callback work, update property com.netflix.config.samples.SampleApp.SampleBean.sensitiveBeanData from BaseConfigBean in JConsole");
        
        SampleBean sampleBean = new SampleBean();
        // this should show the bean taking properties from two different sources
        // property "com.netflix.config.samples.SampleApp.SampleBean.numSeeds" is from sampleapp.properites
        // property "com.netflix.config.samples.SampleApp.SampleBean.name" is from subConfig added above 
        System.out.println("SampleBean:" + sampleBean);
        System.out.println(sampleBean.getName());
    }
}
