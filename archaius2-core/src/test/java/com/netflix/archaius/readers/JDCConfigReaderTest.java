/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.archaius.readers;

import com.netflix.archaius.config.PollingDynamicConfig;
import com.netflix.archaius.config.polling.ManualPollingStrategy;
import com.netflix.archaius.config.polling.PollingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JDCConfigReaderTest {
    private final DataSource datasource = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final PreparedStatement statement = mock(PreparedStatement.class);
    private final ResultSet resultSet = mock(ResultSet.class);
    private final JDCConfigReader reader = new JDCConfigReader(datasource, "SELECT name, value FROM config", "name", "value");

    @BeforeEach
    public void setUp() throws Exception {
        when(datasource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT name, value FROM config")).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    public void returnsSnapshotWithConfiguredColumns() throws Exception {
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getObject("name")).thenReturn("timeout");
        when(resultSet.getString("value")).thenReturn("42");

        PollingResponse response = reader.call();

        assertTrue(response.hasData());
        assertEquals(Collections.singletonMap("timeout", "42"), response.getToAdd());
        assertTrue(response.getToRemove().isEmpty());
        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    public void canPollAndReplaceConfigurationWithAnEmptySnapshot() throws Exception {
        when(resultSet.next()).thenReturn(true, false, false);
        when(resultSet.getObject("name")).thenReturn("timeout");
        when(resultSet.getString("value")).thenReturn("42");
        Callable<PollingResponse> source = reader;
        ManualPollingStrategy strategy = new ManualPollingStrategy();
        PollingDynamicConfig config = new PollingDynamicConfig(source, strategy);
        try {
            strategy.fire(5, TimeUnit.SECONDS);
            assertEquals("42", config.getRawProperty("timeout"));
            strategy.fire(5, TimeUnit.SECONDS);
            assertTrue(config.isEmpty());
            assertEquals(2, config.getUpdateCounter());
        } finally {
            config.shutdown();
        }
    }

    @Test
    public void propagatesReadFailureAndClosesResources() throws Exception {
        SQLException failure = new SQLException("read failed");
        when(resultSet.next()).thenThrow(failure);

        assertSame(failure, assertThrows(SQLException.class, reader::call));

        verify(resultSet).close();
        verify(statement).close();
        verify(connection).close();
    }
}
