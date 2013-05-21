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
package com.netflix.config.sources;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource40;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicConfiguration;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.FixedDelayPollingScheduler;
import com.netflix.config.sources.JDBCConfigurationSource;

/**
 * Unit tests for the JDBCConfigurationSource class
 * This class uses the Derby in-memory database.
 * In real life one can replace this with RDBMS products like Oracle, MySQL etc.
 * 
 * @author stonse
 *
 */
public class JDBCConfigurationSourceTest {
	
	private static Logger log = LoggerFactory
			.getLogger(JDBCConfigurationSourceTest.class);
    
	/**
	 * Set up code for the test
	 * @param dbname
	 * @return
	 * @throws Throwable
	 */
	javax.sql.DataSource createDataConfigSource(String dbname) throws Throwable {
		EmbeddedDataSource40 ds = new EmbeddedDataSource40();
		ds.setDatabaseName(dbname);
		ds.setCreateDatabase(dbname);
		ds.setCreateDatabase("create");
		Connection con = null;
		try {
			con = ds.getConnection();

			// Creating a database table
			
			Statement sta = null;
			try{
			    sta = con.createStatement();
				sta.executeUpdate("DROP TABLE MySiteProperties");	
				log.info("Table Dropped");
			}catch (Exception e) {
				log.info("Table did not exist.");
			}
			finally{
				if (sta!=null){
					sta.close();
				}
			}
			try {
			    sta = con.createStatement();
				sta.executeUpdate("CREATE TABLE MySiteProperties (property_key VARCHAR(20),"
						+ " property_value VARCHAR(100))");
				log.info("Table created.");
				sta.close();
			} finally{
				if (sta!=null){
					sta.close();
				}
			}
			try {
				sta = con.createStatement();

				sta.executeUpdate("insert into MySiteProperties values ('prop1','value1')");
				log.info("Properties for testing inserted");
				sta.close();
			} finally{
				if (sta!=null){
					sta.close();
				}
			}
			
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
		} finally{
			if (con != null){
				con.close();
			}
		}

		return ds;
	}
	 
    
    
    @Test
	public void testSimpleInMemoryJDBCDynamicPropertySource() throws Throwable {

		DataSource ds = createDataConfigSource("MySiteConfiguration");
		JDBCConfigurationSource source = new JDBCConfigurationSource(
				ds,
				"select distinct property_key, property_value from MySiteProperties",
				"property_key", "property_value");
		FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(
				0, 10, false);
		DynamicConfiguration configuration = new DynamicConfiguration(source,
				scheduler);
		DynamicPropertyFactory.initWithConfigurationSource(configuration);

		DynamicStringProperty defaultProp = DynamicPropertyFactory.getInstance().getStringProperty(
				"this.prop.does.not.exist.use.default", "default");
		assertEquals("default", defaultProp.get());
		
		DynamicStringProperty prop1 = DynamicPropertyFactory.getInstance().getStringProperty(
				"prop1", "default");
		assertEquals("value1", prop1.get());

	}

}

