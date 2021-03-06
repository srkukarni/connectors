/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.openconnectors.source;

import org.openconnectors.data.ColumnType;
import org.openconnectors.data.Record;
import org.openconnectors.data.TableMetaData;
import org.openconnectors.util.ConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public abstract class TableQuerier {
    protected final String tableName;
    protected TableMetaData tableMetaData;
    protected String schemaPattern;
    protected PreparedStatement preparedStatement;
    private ConnectionProvider connectionProvider;

    protected TableQuerier(ConnectionProvider connectionProvider, String schemaPattern, String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("TableName cannot be null or empty.");
        }
        this.tableName = tableName;
        this.schemaPattern = schemaPattern;
        this.connectionProvider = connectionProvider;
    }

    public List<Record> query() throws SQLException {
        ResultSet resultSet = executeQuery();
        int columnsCount = tableMetaData.getColumnsCount();
        List<Record> records = new ArrayList<>();
        Object[] values;
        while (resultSet.next()) {
            values = new Object[columnsCount];
            for (int i = 1; i <= columnsCount; i++) {
                values[i - 1] = resultSet.getObject(i);
            }
            records.add(new Record(values, tableMetaData));
        }
        updateCursor(records.size());
        return records;
    }

    protected abstract ResultSet executeQuery() throws SQLException;

    protected abstract void updateCursor(int newRecordsNumber);

    public PreparedStatement getPreparedStatement() throws SQLException {
        if (preparedStatement == null) {
            preparedStatement = createPreparedStatement();
            createTableMetaData();

        }
        return preparedStatement;
    }

    protected Connection getConnection() throws SQLException {
        return connectionProvider.getValidConnection();
    }

    protected abstract PreparedStatement createPreparedStatement() throws SQLException;

    private void createTableMetaData() throws SQLException {
        ResultSetMetaData resultSetMetaData = preparedStatement.getMetaData();
        int columnCount = resultSetMetaData.getColumnCount();
        TableMetaData.TableMetaDataBuilder tableMetaDataBuilder =
                new TableMetaData.TableMetaDataBuilder(this.tableName, columnCount);
        for (int i = 1; i <= columnCount; i++) {
            String columnName = resultSetMetaData.getColumnName(i);
            ColumnType type = null;
            switch (resultSetMetaData.getColumnType(i)) {
                case Types.BIT:
                    type = ColumnType.BOOLEAN;
                    break;
                case Types.TINYINT:
                    type = ColumnType.BYTE;
                    break;
                case Types.SMALLINT:
                    type = ColumnType.SHORT;
                    break;
                case Types.INTEGER:
                    type = ColumnType.INT;
                    break;
                case Types.BIGINT:
                    type = ColumnType.LONG;
                    break;
                case Types.FLOAT:
                case Types.REAL:
                    type = ColumnType.FLOAT;
                    break;
                case Types.DOUBLE:
                    type = ColumnType.DOUBLE;
                    break;
                case Types.NUMERIC:
                    type = ColumnType.BIG_DECIMAL;
                    break;
                case Types.CHAR:
                case Types.LONGNVARCHAR:
                case Types.LONGVARCHAR:
                case Types.VARCHAR:
                case Types.NCHAR:
                case Types.NVARCHAR:
                    type = ColumnType.STRING;
                    break;
                case Types.DATE:
                    type = ColumnType.DATE;
                    break;
                case Types.TIME:
                    type = ColumnType.TIME;
                    break;
                case Types.TIMESTAMP:
                    type = ColumnType.TIMESTAMP;
                    break;
                case Types.BLOB:
                    type = ColumnType.BLOB;
                    break;
                case Types.CLOB:
                    type = ColumnType.CLOB;
                    break;
                case Types.REF:
                    type = ColumnType.REF;
                    break;
                // TODO: handle other types
                default:
                    break;
            }
            tableMetaDataBuilder.addColumn(i, columnName, type);
        }
        tableMetaData = tableMetaDataBuilder.build();
    }

}
