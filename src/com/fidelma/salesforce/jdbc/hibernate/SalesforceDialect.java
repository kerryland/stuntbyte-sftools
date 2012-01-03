package com.fidelma.salesforce.jdbc.hibernate;

import org.hibernate.Hibernate;
import org.hibernate.JDBCException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.exception.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.LockMode;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Types;

/**
 * http://docs.jboss.org/hibernate/core/3.5/api/org/hibernate/dialect/Dialect.html
 */
public class SalesforceDialect extends org.hibernate.dialect.Dialect {
    public SalesforceDialect() {

        super();

//        getDefaultProperties().setProperty( Environment.VERSION, DEFAULT_BATCH_SIZE );
        getDefaultProperties().setProperty(Environment.USE_GET_GENERATED_KEYS, "true");

        registerColumnType(Types.VARCHAR, "string");
        registerColumnType(Types.BIGINT, "int");
        registerColumnType(Types.CHAR, "string");
        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.TIMESTAMP, "datetime");
        registerColumnType(Types.DECIMAL, "decimal");
        registerColumnType(Types.DOUBLE, "double");
        registerColumnType(Types.FLOAT, "double");
        registerColumnType(Types.INTEGER, "int");
        registerColumnType(Types.LONGVARCHAR, "textarea" );
        registerColumnType(Types.BOOLEAN, "boolean" );
        registerColumnType(Types.TIME, "time" );

        /*

        registerColumnType( Types.BIGINT, "bigint" );
        registerColumnType( Types.BINARY, "binary" );
        registerColumnType( Types.BIT, "bit" );
        registerColumnType( Types.CHAR, "char(1)" );
        registerColumnType( Types.DATE, "date" );
        registerColumnType( Types.DECIMAL, "decimal" );
        registerColumnType( Types.DOUBLE, "double" );
        registerColumnType( Types.FLOAT, "float" );
        registerColumnType( Types.INTEGER, "integer" );
        registerColumnType( Types.LONGVARBINARY, "longvarbinary" );
        registerColumnType( Types.LONGVARCHAR, "longvarchar" );
        registerColumnType( Types.SMALLINT, "smallint" );
        registerColumnType( Types.TINYINT, "tinyint" );
        registerColumnType( Types.TIME, "time" );
        registerColumnType( Types.TIMESTAMP, "timestamp" );
        registerColumnType( Types.VARCHAR, "varchar($l)" );
        registerColumnType( Types.VARBINARY, "varbinary($l)" );
        registerColumnType( Types.NUMERIC, "numeric" );

        registerColumnType( Types.BLOB, "longvarbinary" );
        registerColumnType( Types.CLOB, "longvarchar" );
        */

//            registerFunction( "ascii", new StandardSQLFunction( "ascii", Hibernate.INTEGER ) );
//            registerFunction( "char", new StandardSQLFunction( "char", Hibernate.CHARACTER ) );
//            registerFunction( "length", new StandardSQLFunction( "length", Hibernate.LONG ) );
//            registerFunction( "lower", new StandardSQLFunction( "lower" ) );
//            registerFunction( "upper", new StandardSQLFunction( "upper" ) );
//            registerFunction( "lcase", new StandardSQLFunction( "lcase" ) );
//            registerFunction( "ucase", new StandardSQLFunction( "ucase" ) );
//            registerFunction( "soundex", new StandardSQLFunction( "soundex", Hibernate.STRING ) );
//            registerFunction( "ltrim", new StandardSQLFunction( "ltrim" ) );
//            registerFunction( "rtrim", new StandardSQLFunction( "rtrim" ) );
//            registerFunction( "reverse", new StandardSQLFunction( "reverse" ) );
//            registerFunction( "space", new StandardSQLFunction( "space", Hibernate.STRING ) );
//            registerFunction( "rawtohex", new StandardSQLFunction( "rawtohex" ) );
//            registerFunction( "hextoraw", new StandardSQLFunction( "hextoraw" ) );

//            registerFunction( "user", new NoArgSQLFunction( "user", Hibernate.STRING ) );
//            registerFunction( "database", new NoArgSQLFunction( "database", Hibernate.STRING ) );

//            registerFunction( "current_date", new NoArgSQLFunction( "current_date", Hibernate.DATE, false ) );
//            registerFunction( "curdate", new NoArgSQLFunction( "curdate", Hibernate.DATE ) );
//            registerFunction( "current_timestamp", new NoArgSQLFunction( "current_timestamp", Hibernate.TIMESTAMP, false ) );
//            registerFunction( "now", new NoArgSQLFunction( "now", Hibernate.TIMESTAMP ) );
//            registerFunction( "current_time", new NoArgSQLFunction( "current_time", Hibernate.TIME, false ) );
//            registerFunction( "curtime", new NoArgSQLFunction( "curtime", Hibernate.TIME ) );
//            registerFunction( "day", new StandardSQLFunction( "day", Hibernate.INTEGER ) );
//            registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", Hibernate.INTEGER ) );
//            registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", Hibernate.INTEGER ) );
//            registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", Hibernate.INTEGER ) );
//            registerFunction( "month", new StandardSQLFunction( "month", Hibernate.INTEGER ) );
//            registerFunction( "year", new StandardSQLFunction( "year", Hibernate.INTEGER ) );
//            registerFunction( "week", new StandardSQLFunction( "week", Hibernate.INTEGER ) );
//            registerFunction( "quater", new StandardSQLFunction( "quater", Hibernate.INTEGER ) );
//            registerFunction( "hour", new StandardSQLFunction( "hour", Hibernate.INTEGER ) );
//            registerFunction( "minute", new StandardSQLFunction( "minute", Hibernate.INTEGER ) );
//            registerFunction( "second", new StandardSQLFunction( "second", Hibernate.INTEGER ) );
//            registerFunction( "dayname", new StandardSQLFunction( "dayname", Hibernate.STRING ) );
//            registerFunction( "monthname", new StandardSQLFunction( "monthname", Hibernate.STRING ) );


        // http://www.salesforce.com/us/developer/docs/api/Content/sforce_api_calls_soql_select_date_functions.htm
        registerFunction("CALENDAR_MONTH", new StandardSQLFunction("year", Hibernate.INTEGER));


//            registerFunction( "abs", new StandardSQLFunction( "abs" ) );
//            registerFunction( "sign", new StandardSQLFunction( "sign", Hibernate.INTEGER ) );
//
//            registerFunction( "acos", new StandardSQLFunction( "acos", Hibernate.DOUBLE ) );
//            registerFunction( "asin", new StandardSQLFunction( "asin", Hibernate.DOUBLE ) );
//            registerFunction( "atan", new StandardSQLFunction( "atan", Hibernate.DOUBLE ) );
//            registerFunction( "cos", new StandardSQLFunction( "cos", Hibernate.DOUBLE ) );
//            registerFunction( "cot", new StandardSQLFunction( "cot", Hibernate.DOUBLE ) );
//            registerFunction( "exp", new StandardSQLFunction( "exp", Hibernate.DOUBLE ) );
//            registerFunction( "log", new StandardSQLFunction( "log", Hibernate.DOUBLE ) );
//            registerFunction( "log10", new StandardSQLFunction( "log10", Hibernate.DOUBLE ) );
//            registerFunction( "sin", new StandardSQLFunction( "sin", Hibernate.DOUBLE ) );
//            registerFunction( "sqrt", new StandardSQLFunction( "sqrt", Hibernate.DOUBLE ) );
//            registerFunction( "tan", new StandardSQLFunction( "tan", Hibernate.DOUBLE ) );
//            registerFunction( "pi", new NoArgSQLFunction( "pi", Hibernate.DOUBLE ) );
//            registerFunction( "rand", new StandardSQLFunction( "rand", Hibernate.FLOAT ) );
//
//            registerFunction( "radians", new StandardSQLFunction( "radians", Hibernate.DOUBLE ) );
//            registerFunction( "degrees", new StandardSQLFunction( "degrees", Hibernate.DOUBLE ) );
//            registerFunction( "roundmagic", new StandardSQLFunction( "roundmagic" ) );
//
//            registerFunction( "ceiling", new StandardSQLFunction( "ceiling" ) );
//            registerFunction( "floor", new StandardSQLFunction( "floor" ) );

        // Multi-param dialect functions...
//            registerFunction( "mod", new StandardSQLFunction( "mod", Hibernate.INTEGER ) );

        // function templates
//            registerFunction( "concat", new VarArgsSQLFunction( Hibernate.STRING, "(", "||", ")" ) );

//            getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
    }

    public String getAddColumnString() {
        return "add column";
    }

    public boolean supportsIdentityColumns() {
        return false;
    }

    public String getIdentityColumnString() {
        return null;
//        return "generated by default as identity"; //not null is implicit
//            return ""; //not null is implicit
    }


    public String getIdentitySelectString() {
        return null;
//            return "Xcall identity()";
//        return "wtf";
    }

    public String getSelectGUIDString() {
//                    return "call identity()";
        return null;
    }


    public String getIdentityInsertString() {
        return null;
    }

    public String getForUpdateString() {
        return "";
    }

    public boolean supportsUnique() {
        return false;
    }

    public boolean supportsLimit() {
        return true;
    }

    public String getLimitString(String sql, boolean hasOffset) {
        return new StringBuffer(sql.length() + 10)
                .append(sql).append(" limit ?")
//                    .insert(sql.toLowerCase().indexOf("select") + 6, hasOffset ? " limit ? ?" : " top ?")
                .toString();
    }

    public boolean bindLimitParametersFirst() {
        return true;
    }

    public boolean supportsIfExistsAfterTableName() {
        return true;
    }

    public boolean supportsColumnCheck() {
        return false;
    }

    public boolean supportsSequences() {
        return false;
    }

    public boolean supportsPooledSequences() {
        return false;
    }


//        protected String getCreateSequenceString(String sequenceName) {
//            return "create sequence " + sequenceName;
//        }
//
//        protected String getDropSequenceString(String sequenceName) {
//            return "drop sequence " + sequenceName;
//        }
//
//        public String getSelectSequenceNextValString(String sequenceName) {
//            return "next value for " + sequenceName;
//        }
//
//        public String getSequenceNextValString(String sequenceName) {
//            return "call next value for " + sequenceName;
//        }

//        public String getQuerySequencesString() {
    // this assumes schema support, which is present in 1.8.0 and later...
//            return "select sequence_name from information_schema.system_sequences";
//        }

//        public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
//            return EXTRACTER;
//        }

    public boolean supportsTemporaryTables() {
        return false;
    }

    public boolean supportsCurrentTimestampSelection() {
        return false;
    }

    public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
        // HSQLDB only supports READ_UNCOMMITTED transaction isolation
        return new ReadUncommittedLockingStrategy(lockable, lockMode);
    }

    public static class ReadUncommittedLockingStrategy extends SelectLockingStrategy {
        public ReadUncommittedLockingStrategy(Lockable lockable, LockMode lockMode) {
            super(lockable, lockMode);
        }

/*
        public void lock(Serializable id, Object version, Object object, SessionImplementor session)
                throws StaleObjectStateException, JDBCException {
            if (getLockMode().greaterThan(LockMode.READ)) {
//                    log.warn( "HSQLDB supports only READ_UNCOMMITTED isolation" );
            }
            super.lock(id, version, object, session);
        }
*/
    }


    // Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public boolean supportsEmptyInList() {
        return false;
    }

    public boolean supportsLobValueChangePropogation() {
        return false;
    }
}
