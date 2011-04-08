package com.fidelma.salesforce.jdbc;

import com.fidelma.salesforce.jdbc.metaforce.ColumnMap;
import com.fidelma.salesforce.jdbc.metaforce.ForceResultSet;
import com.fidelma.salesforce.jdbc.metaforce.ResultSetFactory;
import com.fidelma.salesforce.jdbc.metaforce.WscService;
import com.fidelma.salesforce.misc.LoginHelper;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class SfDatabaseMetaData implements DatabaseMetaData {
    private SfConnection sfConnection;
    private ResultSetFactory metaDataFactory;

    public SfDatabaseMetaData(SfConnection sfConnection,
                              ResultSetFactory metaDataFactory) throws Exception {
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
        return false;
    }

    public boolean nullsAreSortedAtStart() throws SQLException {
        return false;
    }

    public boolean nullsAreSortedAtEnd() throws SQLException {
        return false;
    }

    public String getDatabaseProductName() throws SQLException {
        return "SALESFORCE";
    }

    public String getDatabaseProductVersion() throws SQLException {
        return "Winter11"; //TODO: Pull from API?
    }

    public String getDriverName() throws SQLException {
        return "kerry's jdbc driver";
    }

    public String getDriverVersion() throws SQLException {
        return "1.1";
    }

    public int getDriverMajorVersion() {
        return 1;
    }

    public int getDriverMinorVersion() {
        return 1;
    }

    public boolean usesLocalFiles() throws SQLException {
        return false;
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
        return "%";
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
        return false;
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
        return "";
    }

    public boolean isCatalogAtStart() throws SQLException {
        return false;
    }

    public String getCatalogSeparator() throws SQLException {
        return "";
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
        return 0;
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

        return metaDataFactory.getTables(tableNamePattern);

//        ListMetadataQuery query = new ListMetadataQuery();
//        query.setType("CustomObject");
//
//        try {
//            FileProperties[] props = meta.listMetadata(
//                    new ListMetadataQuery[]{query},
//                    LoginHelper.SFDC_VERSION);
//
//            for (FileProperties prop : props) {
//                System.out.println("Component fullName: " + prop.getFullName());
//                System.out.println("Component type: " + prop.getType());
//            }
//
//        } catch (ConnectionException e) {
//            throw new SQLException(e);
//        }
//        return null;

    }

    public ResultSet getSchemas() throws SQLException {
        return new SfResultSet();

//        return metaDataFactory.getSchemas(); // TODO - what are schemas?
    }

    public ResultSet getCatalogs() throws SQLException {
        return new SfResultSet();
    }

    public ResultSet getTableTypes() throws SQLException {
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();
        ColumnMap<String, Object> row = new ColumnMap<String, Object>();
        row.put("TABLE_TYPE", "TABLE");
        maps.add(row);
        return new ForceResultSet(maps);
    }

    public ResultSet getColumns(String catalog, String schemaPattern,
                                String tableNamePattern,
                                String columnNamePattern) throws SQLException {

        return metaDataFactory.getColumns(tableNamePattern, columnNamePattern);
    }

    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        return new SfResultSet(); // Maybe need to do more
    }

    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return new SfResultSet(); // Maybe need to do more
    }

    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
        return new SfResultSet(); // TODO: Could use Id
    }

    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        return new SfResultSet(); // TODO: Could use LastModifiedDate
    }

    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
        return new SfResultSet(); // TODO?

    }

    public ResultSet getTypeInfo() throws SQLException {
        return metaDataFactory.getTypeInfo();
    }

    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        return metaDataFactory.getPrimaryKeys(table);
    }

    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        return metaDataFactory.getImportedKeys(table);
    }

    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        List<ColumnMap<String, Object>> maps = new ArrayList<ColumnMap<String, Object>>();

        // TODO: This is totally incomplete
        ColumnMap<String, Object> row = new ColumnMap<String, Object>();
        row.put("PKTABLE_CAT", null);
        row.put("PKTABLE_SCHEM", null);

        maps.add(row);
        return new ForceResultSet(maps);
    }


    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
        return metaDataFactory.getIndexInfo(table);
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
        return false;
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
        return false;
    }

    public boolean supportsGetGeneratedKeys() throws SQLException {
        return false;
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
        return null;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("Unsupported");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
