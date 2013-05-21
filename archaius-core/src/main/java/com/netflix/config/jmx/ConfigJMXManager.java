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
package com.netflix.config.jmx;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.commons.configuration.AbstractConfiguration;


/**
 * A JMX Manager class that helps in registering and unregistering MBeans
 * @author stonse
 *
 */
public class ConfigJMXManager {

	public ConfigJMXManager() {
		
	}
	
	public static ConfigMBean registerConfigMbean(AbstractConfiguration config) {
		StandardMBean  mbean = null;
		ConfigMBean bean = null;
		try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            bean = new BaseConfigMBean(config);           
            mbean = new StandardMBean(bean, ConfigMBean.class);
            mbs.registerMBean(mbean, getJMXObjectName(config, bean));
        } catch (NotCompliantMBeanException e) {
            throw new RuntimeException(
                    "NotCompliantMBeanException", e);
        } catch (InstanceAlreadyExistsException e) {
            throw new RuntimeException( "InstanceAlreadyExistsException", e);
        } catch (MBeanRegistrationException e) {
            throw new RuntimeException(
                   "MBeanRegistrationException", e);
        } catch (Exception e) {
        	throw new RuntimeException(
                    "registerConfigMbeanException", e);
		}
		return bean;
	}
	
	 public static void unRegisterConfigMBean(AbstractConfiguration config, ConfigMBean  mbean) {
	        
	        if (mbean == null) {
	            throw new RuntimeException(
	                    "Cannot unregister JMX Mbean. The object is null");
	        }        
	        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();        
	        try {
	            mbs.unregisterMBean(getJMXObjectName(config, mbean));
	        } catch (InstanceNotFoundException e) {          
	            throw new RuntimeException(
	                    "InstanceNotFoundException", e);
	        } catch (MBeanRegistrationException e) {
	            throw new RuntimeException(
	            		"MBeanRegistrationException", e);
	        } catch (Exception e) {
	        	throw new RuntimeException(
	                    "unRegisterConfigMBeanException", e);
			}
	            
	    }
	
	private static ObjectName getJMXObjectName(
            AbstractConfiguration config, ConfigMBean bean)
            throws Exception {
        try {
            Class<? extends ConfigMBean> c = bean.getClass();
            String className = c.getName();
            int lastDot = className.lastIndexOf('.');
            ObjectName name = new ObjectName("Config-"
                    + className.substring(0, lastDot) + ":class="
                    + className.substring(lastDot + 1));
            return name;
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(
                   "MalformedObjectNameException", e);
        } catch (NullPointerException e) {
            throw new RuntimeException(
                   "NullPointerException", e);
        }
    }
}
