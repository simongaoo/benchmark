package com.nsn.benchmark.procs;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

/**
 * @author zhangxu
 */
public class SelectFromPair extends VoltProcedure {
    private final SQLStmt query = new SQLStmt(
            "SELECT T.INDEX, P.SOURCE_HOST, P.SOURCE_PORT, P.START_TIME " +
                    "FROM TEMPORARY T RIGHT OUTER JOIN PAIR P ON (T.CONDITION = P.CONDITION) " +
                    "WHERE " +
                    "    T.HOST = ?" +
                    "    AND THREAD_NAME = ?" +
                    "    AND TIME = TO_TIMESTAMP(MILLIS, ?) " +
                    "ORDER BY T.INDEX;");

    public VoltTable[] run(String host, String threadName, long millis) {
        voltQueueSQL(query, host, threadName, millis);
        return voltExecuteSQL(true);
    }
}