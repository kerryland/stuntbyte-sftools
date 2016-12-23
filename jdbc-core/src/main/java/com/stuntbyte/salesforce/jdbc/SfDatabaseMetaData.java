/**
 * The MIT License
 * Copyright © 2011-2017 Kerry Sainsbury
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.stuntbyte.salesforce.jdbc;

import com.stuntbyte.salesforce.jdbc.metaforce.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class SfDatabaseMetaData implements DatabaseMetaData {
    private SfConnection sfConnection;
    private ResultSetFactory metaDataFactory;

    public SfDatabaseMetaData(SfConnection sfConnection,
                              ResultSetFactory metaDataFactory) throws SQLException {
        this.sfConnection = sfConnection;
        this.metaDataFactory = metaDataFactory;
    }

    public boolean allProceduresAreCallable() throws SQLException {
        return false;
    }

    public boolean allTablesAreSelectable() throws SQLException {
        return true;
    }

    public String getURL() throws SQLException {
        return sfConnection.getServer();
    }

    public String getUserName() throws SQLException {
        return sfConnection.getUsername();
    }

    public boolean isReadOnly() throws SQLException {
        return false;
    }

    public boolean nullsAreSortedHigh() throws SQLException {
        return false;
    }

    public boolean nullsAreSortedLow() throws SQLException {
        return true;
    }

    public boolean nullsAreSortedAtStart() throws SQLException {
        return true;
    }

    public boolean nullsAreSortedAtEnd() throws SQLException {
        return false;
    }

    public String getDatabaseProductName() throws SQLException {
        return "Salesforce";
    }

    public String getDatabaseProductVersion() throws SQLException {
        return "Spring12"; //TODO: Pull from API?
    }

    public String getDriverName() throws SQLException {
        return "Stunt Byte";
    }

    public String getDriverVersion() throws SQLException {
        return "1.5";
    }

    public int getDriverMajorVersion() {
        return 1;
    }

    public int getDriverMinorVersion() {
        return 5;
    }

    public boolean usesLocalFiles() throws SQLException {
        return true;
    }

    public boolean usesLocalFilePerTable() throws SQLException {
        return false;
    }

    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return false;
    }

    public boolean storesUpperCaseIdentifiers() throws SQLException {
        return false;
    }

    public boolean storesLowerCaseIdentifiers() throws SQLException {
        return false;
    }

    public boolean storesMixedCaseIdentifiers() throws SQLException {
        return false;
    }

    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    public String getIdentifierQuoteString() throws SQLException {

        return " ";
    }

    public String getSQLKeywords() throws SQLException {
        return "";
    }

    public String getNumericFunctions() throws SQLException {
        return "";
    }

    public String getStringFunctions() throws SQLException {
        return "";
    }

    public String getSystemFunctions() throws SQLException {
        return "";
    }

    public String getTimeDateFunctions() throws SQLException {
        return "";
    }

    public String getSearchStringEscape() throws SQLException {
        return "";
    }

    public String getExtraNameCharacters() throws SQLException {
        return "";
    }

    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return false;
    }

    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return false;
    }

    public boolean supportsColumnAliasing() throws SQLException {
        return true;
    }

    public boolean nullPlusNonNullIsNull() throws SQLException {
        return false;
    }

    public boolean supportsConvert() throws SQLException {
        return false;
    }

    public boolean supportsConvert(int fromType, int toType) throws SQLException {
        return false;
    }

    public boolean supportsTableCorrelationNames() throws SQLException {
        return false;
    }

    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        return false;
    }

    public boolean supportsExpressionsInOrderBy() throws SQLException {
        return false;
    }

    public boolean supportsOrderByUnrelated() throws SQLException {
        return false;
    }

    public boolean supportsGroupBy() throws SQLException {
        return true;
    }

    public boolean supportsGroupByUnrelated() throws SQLException {
        return false;
    }

    public boolean supportsGroupByBeyondSelect() throws SQLException {
        return false;
    }

    public boolean supportsLikeEscapeClause() throws SQLException {
        return false;
    }

    public boolean supportsMultipleResultSets() throws SQLException {
        return false;
    }

    public boolean supportsMultipleTransactions() throws SQLException {
        return false;
    }

    public boolean supportsNonNullableColumns() throws SQLException {
        return false;
    }

    public boolean supportsMinimumSQLGrammar() throws SQLException {
        return false;
    }

    public boolean supportsCoreSQLGrammar() throws SQLException {
        return false;
    }

    public boolean supportsExtendedSQLGrammar() throws SQLException {
        return false;
    }

    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return false;
    }

    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return false;
    }

    public boolean supportsANSI92FullSQL() throws SQLException {
        return false;
    }

    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        return false;
    }

    public boolean supportsOuterJoins() throws SQLException {
        return false;
    }

    public boolean supportsFullOuterJoins() throws SQLException {
        return false;
    }

    public boolean supportsLimitedOuterJoins() throws SQLException {
        return false;
    }

    public String getSchemaTerm() throws SQLException {
        return "SCHEMA";
    }

    public String getProcedureTerm() throws SQLException {
        return "";
    }

    public String getCatalogTerm() throws SQLException {
        return "CATALOG";
    }

    public boolean isCatalogAtStart() throws SQLException {
        return true;
    }

    public String getCatalogSeparator() throws SQLException {
        return ".";
    }

    public boolean supportsSchemasInDataManipulation() throws SQLException {
        return false;
    }

    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        return false;
    }

    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        return false;
    }

    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        return false;
    }

    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        return false;
    }

    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        return false;
    }

    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return false;
    }

    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        return false;
    }

    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        return false;
    }

    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        return false;
    }

    public boolean supportsPositionedDelete() throws SQLException {
        return false;
    }

    public boolean supportsPositionedUpdate() throws SQLException {
        return false;
    }

    public boolean supportsSelectForUpdate() throws SQLException {
        return false;
    }

    public boolean supportsStoredProcedures() throws SQLException {
        return false;
    }

    public boolean supportsSubqueriesInComparisons() throws SQLException {
        return false;
    }

    public boolean supportsSubqueriesInExists() throws SQLException {
        return false;
    }

    public boolean supportsSubqueriesInIns() throws SQLException {
        return false;
    }

    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return false;
    }

    public boolean supportsCorrelatedSubqueries() throws SQLException {
        return false;
    }

    public boolean supportsUnion() throws SQLException {
        return false;
    }

    public boolean supportsUnionAll() throws SQLException {
        return false;
    }

    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return false;
    }

    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        return false;
    }

    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        return false;
    }

    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        return false;
    }

    public int getMaxBinaryLiteralLength() throws SQLException {
        return 0;
    }

    public int getMaxCharLiteralLength() throws SQLException {
        return 0;
    }

    public int getMaxColumnNameLength() throws SQLException {
        return 0;
    }

    public int getMaxColumnsInGroupBy() throws SQLException {
        return 0;
    }

    public int getMaxColumnsInIndex() throws SQLException {
        return 0;
    }

    public int getMaxColumnsInOrderBy() throws SQLException {
        return 0;
    }

    public int getMaxColumnsInSelect() throws SQLException {
        return 0;
    }

    public int getMaxColumnsInTable() throws SQLException {
        return 0;
    }

    public int getMaxConnections() throws SQLException {
        return 0;
    }

    public int getMaxCursorNameLength() throws SQLException {
        return 0;
    }

    public int getMaxIndexLength() throws SQLException {
        return 0;
    }

    public int getMaxSchemaNameLength() throws SQLException {
        return 10;
    }

    public int getMaxProcedureNameLength() throws SQLException {
        return 0;
    }

    public int getMaxCatalogNameLength() throws SQLException {
        return 0;
    }

    public int getMaxRowSize() throws SQLException {
        return 0;
    }

    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return false;
    }

    public int getMaxStatementLength() throws SQLException {
        return 0;
    }

    public int getMaxStatements() throws SQLException {
        return 0;
    }

    public int getMaxTableNameLength() throws SQLException {
        return 0;
    }

    public int getMaxTablesInSelect() throws SQLException {
        return 0;
    }

    public int getMaxUserNameLength() throws SQLException {
        return 0;
    }

    public int getDefaultTransactionIsolation() throws SQLException {
        return 0;
    }

    public boolean supportsTransactions() throws SQLException {
        return false;
    }

    public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
        return false;
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        return false;
    }

    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        return false;
    }

    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        return false;
    }

    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        return false;
    }

    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        return new SfResultSet();
    }

    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
        return new SfResultSet();
    }

    public ResultSet getTables(String catalog,
                               String schemaPattern,
                               String tableNamePattern,
                               String[] types) throws SQLException {

        return metaDataFactory.createTableResultSet(schemaPattern, tableNamePattern, types);
    }

    public ResultSet getSchemas() throws SQLException {
        return metaDataFactory.getSchemas();
    }

    public ResultSet getCatalogs() throws SQLException {
        return metaDataFactory.getCatalogs();
    }

    public ResultSet getTableTypes() throws SQLException {
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        ColumnMap<String, Object> row = new ColumnMap<String, Object>();
        row.put("TABLE_TYPE", "TABLE");
        maps.add(row);
//        row = new ColumnMap<String, Object>();
//        row.put("TABLE_TYPE", "SYSTEM TABLE");
//        maps.add(row);
        return new ForceResultSet(maps);
    }

    public ResultSet getColumns(String catalog, String schemaPattern,
                                String tableNamePattern,
                                String columnNamePattern) throws SQLException {

        return metaDataFactory.getColumns(schemaPattern, tableNamePattern, columnNamePattern);
    }

    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        return new SfResultSet(); // Maybe need to do more
    }

    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return new SfResultSet(); // Maybe need to do more
    }

    public ResultSet getBestRowIdentifier(String catalog, final String schema, String table, int scope, boolean nullable) throws SQLException {

        final List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();

        metaDataFactory.getTables(schema, table, new TableEvent() {
            public void onTable(Table table) {
                ColumnMap<String, Object> row = new ColumnMap<String, Object>();
                if (ResultSetFactory.DEPLOYABLE.equalsIgnoreCase(table.getSchema())) {
                    row.put("COLUMN_NAME", "Identifier");
                    row.put("TYPE_NAME", metaDataFactory.getType("String"));
                } else {
                    row.put("COLUMN_NAME", "Id");
                    row.put("TYPE_NAME", metaDataFactory.getType("id"));
                }
                row.put("SCOPE", DatabaseMetaData.bestRowSession);
                row.put("DATA_TYPE", Types.VARCHAR);

                row.put("COLUMN_SIZE", 18);
                row.put("BUFFER_LENGTH", null);
                row.put("DECIMAL_DIGITS", null);
                row.put("PSEUDO_COLUMN", DatabaseMetaData.bestRowNotPseudo);
                maps.add(row);
            }
        });

        return new ForceResultSet(maps);
    }

    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        final List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();

        metaDataFactory.getTables(schema, table, new TableEvent() {
            public void onTable(Table table) throws SQLException {
                if (table.getSchema().equals(ResultSetFactory.schemaName)) {
                    ColumnMap<String, Object> row = new ColumnMap<String, Object>();
                    row.put("SCOPE", null);
                    row.put("COLUMN_NAME", "LastModifiedDAte");
                    row.put("DATA_TYPE", Types.TIMESTAMP);
                    row.put("TYPE_NAME", "datetime");
                    row.put("COLUMN_SIZE", 24);
                    row.put("BUFFER_LENGTH", 24);
                    row.put("DECIMAL_DIGITS", null);
                    row.put("PSEUDO_COLUMN", DatabaseMetaData.bestRowNotPseudo);
                    maps.add(row);
                }
            }
        });
        return new ForceResultSet(maps);
    }


    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
        return new SfResultSet(); // TODO?

    }

    public ResultSet getTypeInfo() throws SQLException {
        return metaDataFactory.getTypeInfo();
    }

    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        final List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();

        metaDataFactory.getTables(schema, table, new TableEvent() {
            public void onTable(Table table) throws SQLException {
                for (Column column : table.getColumns()) {
                    if (column.getName().equalsIgnoreCase("Id")) {
                        ColumnMap<String, Object> map = new ColumnMap<String, Object>();
                        map.put("TABLE_CAT", table.getCatalog());
                        map.put("TABLE_SCHEM", table.getSchema());
                        map.put("TABLE_NAME", table.getName());
                        map.put("COLUMN_NAME", column.getName());
                        map.put("KEY_SEQ", 1);
                        map.put("PK_NAME", "PK" + table.getName() + column.getOrdinal());
                        maps.add(map);
                        break;
                    }
                }
            }
        });
        return new ForceResultSet(maps);
    }

    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        final List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();

        metaDataFactory.getTables(schema, table, new TableEvent() {
            public void onTable(Table table) throws SQLException {
                for (Column column : table.getColumns()) {
                    if (column.getReferencedTable() != null && column.getReferencedColumn() != null) {
                        ColumnMap<String, Object> map = new ColumnMap<String, Object>();
                        map.put("PKTABLE_CAT", table.getCatalog());
                        map.put("PKTABLE_SCHEM", table.getSchema());
                        map.put("PKTABLE_NAME", column.getReferencedTable());
                        map.put("PKCOLUMN_NAME", column.getReferencedColumn());
                        map.put("FKTABLE_CAT", table.getCatalog());
                        map.put("FKTABLE_SCHEM", table.getSchema());
                        map.put("FKTABLE_NAME", table.getName());
                        map.put("FKCOLUMN_NAME", column.getName());
                        map.put("KEY_SEQ", 1);
                        map.put("UPDATE_RULE", 0);
                        map.put("DELETE_RULE", 0);
                        map.put("FK_NAME", "FK" + table.getName() + column.getOrdinal());
                        map.put("PK_NAME", "PK" + table.getName() + column.getOrdinal());
                        map.put("DEFERRABILITY", 5);
                        maps.add(map);
                    }
                }
            }
        });
        return new ForceResultSet(maps);
    }

    
    public ResultSet getExportedKeys(String catalog, String schema, String tableName) throws SQLException {
        final List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();

        for (Table table : metaDataFactory.getTables()) {
            for (Column column : table.getColumns()) {
                if (tableName.equalsIgnoreCase(column.getReferencedTable())) {
                    ColumnMap<String, Object> map = new ColumnMap<String, Object>();
                    map.put("PKTABLE_CAT", table.getCatalog());
                    map.put("PKTABLE_SCHEM", table.getSchema());
                    map.put("PKTABLE_NAME", column.getReferencedTable());
                    map.put("PKCOLUMN_NAME", column.getReferencedColumn());

                    map.put("FKTABLE_CAT", table.getCatalog());
                    map.put("FKTABLE_SCHEM", table.getSchema());
                    map.put("FKTABLE_NAME", table.getName());
                    map.put("FKCOLUMN_NAME", column.getName());

                    map.put("KEY_SEQ", 1);
                    map.put("UPDATE_RULE", DatabaseMetaData.importedKeyRestrict);
                    map.put("DELETE_RULE", DatabaseMetaData.importedKeySetNull);
                    map.put("FK_NAME", "FK" + table.getName() + column.getOrdinal());
                    map.put("PK_NAME", "PK" + table.getName() + column.getOrdinal());
                    map.put("DEFERRABILITY", 5);
                    maps.add(map);
                }
            }
        }

        return new ForceResultSet(maps);
    }


    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
//        return metaDataFactory.getIndexInfo(table);
        return new SfResultSet();
    }

    public boolean supportsResultSetType(int type) throws SQLException {
        return type == ResultSet.TYPE_FORWARD_ONLY;
    }

    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
        return false;
    }

    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        return false;
    }

    public boolean ownDeletesAreVisible(int type) throws SQLException {
        return false;
    }

    public boolean ownInsertsAreVisible(int type) throws SQLException {
        return false;
    }

    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        return false;
    }

    public boolean othersDeletesAreVisible(int type) throws SQLException {
        return false;
    }

    public boolean othersInsertsAreVisible(int type) throws SQLException {
        return false;
    }

    public boolean updatesAreDetected(int type) throws SQLException {
        return false;
    }

    public boolean deletesAreDetected(int type) throws SQLException {
        return false;
    }

    public boolean insertsAreDetected(int type) throws SQLException {
        return false;
    }

    public boolean supportsBatchUpdates() throws SQLException {
        return true;
    }

    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
        return new SfResultSet();
    }

    public Connection getConnection() throws SQLException {
        return sfConnection;
    }

    public boolean supportsSavepoints() throws SQLException {
        return false;
    }

    public boolean supportsNamedParameters() throws SQLException {
        return false;
    }

    public boolean supportsMultipleOpenResults() throws SQLException {
        return true;
    }

    public boolean supportsGetGeneratedKeys() throws SQLException {
        return true;
    }

    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
        return new SfResultSet();
    }

    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return new SfResultSet();
    }

    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
        return new SfResultSet();
    }

    public boolean supportsResultSetHoldability(int holdability) throws SQLException {
        return false;
    }

    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    public int getDatabaseMajorVersion() throws SQLException {
        return 0;
    }

    public int getDatabaseMinorVersion() throws SQLException {
        return 0;
    }

    public int getJDBCMajorVersion() throws SQLException {
        return 0;
    }

    public int getJDBCMinorVersion() throws SQLException {
        return 0;
    }

    public int getSQLStateType() throws SQLException {
        return 0;
    }

    public boolean locatorsUpdateCopy() throws SQLException {
        return false;
    }

    public boolean supportsStatementPooling() throws SQLException {
        return false;
    }

    public RowIdLifetime getRowIdLifetime() throws SQLException {
        return RowIdLifetime.ROWID_UNSUPPORTED;
    }

    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        return getSchemas();
    }

    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        return false;
    }

    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return false;
    }

    public ResultSet getClientInfoProperties() throws SQLException {
        return new SfResultSet();
    }

    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        return new SfResultSet();
    }

    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
        return new SfResultSet();
    }

    public ResultSet getPseudoColumns(String s, String s1, String s2, String s3) throws SQLException {
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        return new ForceResultSet(maps);
    }

    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return true;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("Unsupported");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
