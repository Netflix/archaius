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
package com.netflix.config.sources;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;

/**
 * Simple JDBC source of config properties. Assuming you have a table with a
 * column that stores the keys and a column to store values, this class can be
 * used as your source of polled configuration.
 * 
 * @author stonse
 * 
 */
public class JDBCConfigurationSource implements PolledConfigurationSource {

	private static Logger log = LoggerFactory
			.getLogger(JDBCConfigurationSource.class);

	/** The datasource to connect to the database. */
	private DataSource datasource;

	/** The JDBC query to obtain properties stored in an RDBMS. */
	// example:
	// "Select distinct property_key, property_value from SITE_PROPERTIES"
	private String query;

	/** The column containing the keys. */
	private String keyColumnName = "property_key";
	/** The column containing the values. */
	private String valueColumnName = "property_value";

	/**
	 * Constructor
	 * 
	 * @param datasource
	 *            The DataSource object for the JDBC; e.g.
	 *            <code>new OracleDataSource(databaseName, schema);</code>
	 * @param query
	 *            The query statement to fetch the properties; e.g.
	 *            <code>"Select distinct property_key, property_value from SITE_PROPERTIES"</code>
	 * @param keyColumnName
	 *            The column name which stores the property keys; e.g.
	 *            <code>property_key</code>
	 * @param valueColumnName
	 *            The column name which stores the property values; e.g.
	 *            <code>property_value</code>
	 */
	public JDBCConfigurationSource(DataSource datasource, String query,
			String keyColumnName, String valueColumnName) {
		this.datasource = datasource;
		this.query = query;
		this.keyColumnName = keyColumnName;
		this.valueColumnName = valueColumnName;
	}

	// ...
	@Override
	public PollResult poll(boolean initial, Object checkPoint) throws Exception {
		// implement logic to retrieve properties from DB
		Map<String, Object> map = load();
		return PollResult.createFull(map);
	}

	/**
	 * Returns a <code>Map<String, Object></code> of properties stored in the
	 * database
	 * 
	 * @throws Exception
	 */
	synchronized Map<String, Object> load() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(query.toString());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String key = (String) rs.getObject(keyColumnName);
				Object value = rs.getObject(valueColumnName);
				map.put(key, value);
			}

		} catch (SQLException e) {
			throw e;
		} finally {
			close(conn, pstmt, rs);
		}
		return map;
	}

	/**
	 * Returns the used <code>DataSource</code> object.
	 * 
	 * @return the data source
	 * @since 1.4
	 */
	public DataSource getDatasource() {
		return datasource;
	}

	/**
	 * Returns a <code>Connection</code> object. This method is called when ever
	 * the database is to be accessed. This implementation returns a connection
	 * from the current <code>DataSource</code>.
	 * 
	 * @return the <code>Connection</code> object to be used
	 * @throws SQLException
	 *             if an error occurs
	 * @since 1.4
	 */
	protected Connection getConnection() throws SQLException {
		return getDatasource().getConnection();
	}

	/**
	 * Close a <code>Connection</code> and, <code>Statement</code>. Avoid
	 * closing if null and hide any SQLExceptions that occur.
	 * 
	 * @param conn
	 *            The database connection to close
	 * @param stmt
	 *            The statement to close
	 */
	private void close(Connection conn, Statement stmt, ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			log.error("An error occured on closing the ResultSet", e);
		}

		try {
			if (stmt != null) {
				stmt.close();
			}
		} catch (SQLException e) {
			log.error("An error occured on closing the statement", e);
		}

		try {
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
			log.error("An error occured on closing the connection", e);
		}
	}

}
