/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.lealone.sql;

import java.util.ArrayList;
import java.util.List;

import org.lealone.api.DatabaseEventListener;
import org.lealone.api.ErrorCode;
import org.lealone.common.message.DbException;
import org.lealone.common.message.Trace;
import org.lealone.common.util.StatementBuilder;
import org.lealone.db.CommandParameter;
import org.lealone.db.Database;
import org.lealone.db.ServerSession;
import org.lealone.db.SysProperties;
import org.lealone.db.result.Result;
import org.lealone.db.result.SearchRow;
import org.lealone.db.table.TableFilter;
import org.lealone.db.value.Value;
import org.lealone.sql.expression.Expression;
import org.lealone.sql.expression.Parameter;

/**
 * A parsed and prepared statement.
 */
public abstract class StatementBase implements PreparedStatement, ParsedStatement {

    /**
     * The session.
     */
    protected ServerSession session;

    /**
     * The SQL string.
     */
    protected String sql;

    /**
     * Whether to create a new object (for indexes).
     */
    protected boolean create = true;

    /**
     * The list of parameters.
     */
    protected ArrayList<Parameter> parameters;

    /**
     * If the query should be prepared before each execution. This is set for
     * queries with LIKE ?, because the query plan depends on the parameter
     * value.
     */
    protected boolean prepareAlways;

    private long modificationMetaId;
    private int objectId;
    private int currentRowNumber;
    private int rowScanCount;

    private boolean canReuse;

    /**
     * Create a new object.
     *
     * @param session the session
     */
    public StatementBase(ServerSession session) {
        this.session = session;
        modificationMetaId = session.getDatabase().getModificationMetaId();
    }

    /**
     * Check if this command is transactional.
     * If it is not, then it forces the current transaction to commit.
     *
     * @return true if it is
     */
    public abstract boolean isTransactional();

    /**
     * Get an empty result set containing the meta data.
     *
     * @return the result set
     */
    public abstract Result queryMeta();

    /**
     * Get the command type as defined in CommandInterface
     *
     * @return the statement type
     */
    @Override
    public abstract int getType();

    /**
     * Check if this command is read only.
     *
     * @return true if it is
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Check if the statement needs to be re-compiled.
     *
     * @return true if it must
     */
    public boolean needRecompile() {
        Database db = session.getDatabase();
        if (db == null) {
            throw DbException.get(ErrorCode.CONNECTION_BROKEN_1, "database closed");
        }
        // parser: currently, compiling every create/drop/... twice
        // because needRecompile return true even for the first execution
        return prepareAlways || modificationMetaId < db.getModificationMetaId() || db.getSettings().recompileAlways;
    }

    /**
     * Get the meta data modification id of the database when this statement was
     * compiled.
     *
     * @return the meta data modification id
     */
    long getModificationMetaId() {
        return modificationMetaId;
    }

    /**
     * Set the meta data modification id of this statement.
     *
     * @param id the new id
     */
    void setModificationMetaId(long id) {
        this.modificationMetaId = id;
    }

    /**
     * Set the parameter list of this statement.
     *
     * @param parameters the parameter list
     */
    public void setParameterList(ArrayList<Parameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * Get the parameter list.
     *
     * @return the parameter list
     */
    @Override
    public ArrayList<Parameter> getParameters() {
        return parameters;
    }

    /**
     * Check if all parameters have been set.
     *
     * @throws DbException if any parameter has not been set
     */
    protected void checkParameters() {
        if (parameters != null) {
            for (int i = 0, size = parameters.size(); i < size; i++) {
                Parameter param = parameters.get(i);
                param.checkSet();
            }
        }
    }

    /**
     * Check if this object is a query.
     *
     * @return true if it is
     */
    @Override
    public boolean isQuery() {
        return false;
    }

    /**
     * Prepare this statement.
     */
    @Override
    public PreparedStatement prepare() {
        // nothing to do
        return this;
    }

    /**
     * Execute the statement.
     *
     * @return the update count
     * @throws DbException if it is a query
     */
    @Override
    public int update() {
        throw DbException.get(ErrorCode.METHOD_NOT_ALLOWED_FOR_QUERY);
    }

    public int updateLocal() {
        return update();
    }

    /**
     * Execute the query.
     *
     * @param maxrows the maximum number of rows to return
     * @return the result set
     * @throws DbException if it is not a query
     */
    @Override
    public Result query(int maxrows) {
        throw DbException.get(ErrorCode.METHOD_ONLY_ALLOWED_FOR_QUERY);
    }

    @Override
    public Result query(int maxRows, boolean scrollable) {
        return query(maxRows);
    }

    public Result queryLocal(int maxrows) {
        return query(maxrows);
    }

    /**
     * Set the SQL statement.
     *
     * @param sql the SQL statement
     */
    public void setSQL(String sql) {
        this.sql = sql;
    }

    /**
     * Get the SQL statement.
     *
     * @return the SQL statement
     */
    public String getSQL() {
        return sql;
    }

    /**
     * Get the object id to use for the database object that is created in this
     * statement. This id is only set when the object is persistent.
     * If not set, this method returns 0.
     *
     * @return the object id or 0 if not set
     */
    protected int getCurrentObjectId() {
        return objectId;
    }

    /**
     * Get the current object id, or get a new id from the database. The object
     * id is used when creating new database object (CREATE statement).
     *
     * @return the object id
     */
    protected int getObjectId() {
        return getObjectId(session.getDatabase());
    }

    protected int getObjectId(Database db) {
        int id = objectId;
        if (id == 0) {
            id = db.allocateObjectId();
        } else {
            objectId = 0;
        }
        return id;
    }

    /**
     * Get the SQL statement with the execution plan.
     *
     * @return the execution plan
     */
    public String getPlanSQL() {
        return null;
    }

    /**
     * Check if this statement was canceled.
     *
     * @throws DbException if it was canceled
     */
    @Override
    public void checkCanceled() {
        session.checkCanceled();
    }

    /**
     * Set the object id for this statement.
     *
     * @param i the object id
     */
    @Override
    public void setObjectId(int i) {
        this.objectId = i;
        this.create = false;
    }

    /**
     * Set the session for this statement.
     *
     * @param currentSession the new session
     */
    public void setSession(ServerSession currentSession) {
        this.session = currentSession;
    }

    /**
     * Print information about the statement executed if info trace level is
     * enabled.
     *
     * @param startTime when the statement was started
     * @param rowCount the query or update row count
     */
    void trace(long startTime, int rowCount) {
        if (session.getTrace().isInfoEnabled() && startTime > 0) {
            long deltaTime = System.currentTimeMillis() - startTime;
            String params = Trace.formatParams(parameters);
            session.getTrace().infoSQL(sql, params, rowCount, deltaTime);
        }
        if (session.getDatabase().getQueryStatistics()) {
            long deltaTime = System.currentTimeMillis() - startTime;
            session.getDatabase().getQueryStatisticsData().update(toString(), deltaTime, rowCount);
        }
    }

    /**
     * Set the prepare always flag.
     * If set, the statement is re-compiled whenever it is executed.
     *
     * @param prepareAlways the new value
     */
    public void setPrepareAlways(boolean prepareAlways) {
        this.prepareAlways = prepareAlways;
    }

    /**
     * Set the current row number.
     *
     * @param rowNumber the row number
     */
    protected void setCurrentRowNumber(int rowNumber) {
        if ((++rowScanCount & 127) == 0) {
            checkCanceled();
        }
        this.currentRowNumber = rowNumber;
        setProgress();
    }

    /**
     * Get the current row number.
     *
     * @return the row number
     */
    public int getCurrentRowNumber() {
        return currentRowNumber;
    }

    /**
     * Notifies query progress via the DatabaseEventListener
     */
    private void setProgress() {
        if ((currentRowNumber & 127) == 0) {
            session.getDatabase().setProgress(DatabaseEventListener.STATE_STATEMENT_PROGRESS, sql,
                    currentRowNumber, 0);
        }
    }

    /**
     * Convert the statement to a String.
     *
     * @return the SQL statement
     */
    @Override
    public String toString() {
        return sql;
    }

    /**
     * Get the SQL snippet of the value list.
     *
     * @param values the value list
     * @return the SQL snippet
     */
    protected static String getSQL(Value[] values) {
        StatementBuilder buff = new StatementBuilder();
        for (Value v : values) {
            buff.appendExceptFirst(", ");
            if (v != null) {
                buff.append(v.getSQL());
            }
        }
        return buff.toString();
    }

    /**
     * Get the SQL snippet of the expression list.
     *
     * @param list the expression list
     * @return the SQL snippet
     */
    protected static String getSQL(Expression[] list) {
        StatementBuilder buff = new StatementBuilder();
        for (Expression e : list) {
            buff.appendExceptFirst(", ");
            if (e != null) {
                buff.append(e.getSQL());
            }
        }
        return buff.toString();
    }

    /**
     * Set the SQL statement of the exception to the given row.
     *
     * @param e the exception
     * @param rowId the row number
     * @param values the values of the row
     * @return the exception
     */
    protected DbException setRow(DbException e, int rowId, String values) {
        StringBuilder buff = new StringBuilder();
        if (sql != null) {
            buff.append(sql);
        }
        buff.append(" -- ");
        if (rowId > 0) {
            buff.append("row #").append(rowId + 1).append(' ');
        }
        buff.append('(').append(values).append(')');
        return e.addSQL(buff.toString());
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    private boolean local = true;

    @Override
    public boolean isLocal() {
        return local;
    }

    @Override
    public void setLocal(boolean local) {
        this.local = local;
    }

    private int fetchSize = SysProperties.SERVER_RESULT_SET_FETCH_SIZE;

    @Override
    public int getFetchSize() {
        return fetchSize;
    }

    @Override
    public void setFetchSize(int fetchSize) {
        if (fetchSize < 0) {
            throw DbException.getInvalidValueException("fetchSize", fetchSize);
        }
        if (fetchSize == 0) {
            fetchSize = SysProperties.SERVER_RESULT_SET_FETCH_SIZE;
        }
        this.fetchSize = fetchSize;
    }

    public ServerSession getSession() {
        return session;
    }

    public boolean isDDL() {
        return false;
    }

    // 多值insert、不带等号PartitionKey条件的delete/update都是一种批量操作，
    // 这类批量操作会当成一个分布式事务处理
    @Override
    public boolean isBatch() {
        return false;
    }

    public static boolean containsEqualPartitionKeyComparisonType(TableFilter tableFilter) {
        return getPartitionKey(tableFilter) != null;
    }

    public static Value getPartitionKey(TableFilter tableFilter) {
        SearchRow startRow = tableFilter.getStartSearchRow();
        SearchRow endRow = tableFilter.getEndSearchRow();

        Value startPK = getPartitionKey(startRow);
        Value endPK = getPartitionKey(endRow);
        if (startPK != null && endPK != null && startPK == endPK)
            return startPK;

        return null;
    }

    public static Value getPartitionKey(SearchRow row) {
        if (row == null)
            return null;
        return row.getRowKey();
    }

    public List<Long> getRowVersions() {
        return null;
    }

    /**
     * Whether the command is already closed (in which case it can be re-used).
     *
     * @return true if it can be re-used
     */

    @Override
    public boolean canReuse() {
        return canReuse;
    }

    /**
     * The command is now re-used, therefore reset the canReuse flag, and the
     * parameter values.
     */

    @Override
    public void reuse() {
        canReuse = false;
        ArrayList<? extends CommandParameter> parameters = getParameters();
        for (int i = 0, size = parameters.size(); i < size; i++) {
            CommandParameter param = parameters.get(i);
            param.setValue(null, true);
        }
    }

    @Override
    public void close() {
        canReuse = true;
    }

    @Override
    public void cancel() {
    }

    @Override
    public Result executeQuery(int maxRows, boolean scrollable) {
        return query(maxRows);
    }

    @Override
    public int executeUpdate() {
        return update();
    }

    @Override
    public Result getMetaData() {
        return queryMeta();
    }
}
