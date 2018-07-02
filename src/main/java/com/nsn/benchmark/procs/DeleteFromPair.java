package com.nsn.benchmark.procs;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

/**
 * @author zhangxu
 */
public class DeleteFromPair extends VoltProcedure {
    private final SQLStmt query = new SQLStmt(
            "DELETE FROM TEMPORARY " +
                    "WHERE" +
                    "   HOST = ?" +
                    "   AND THREAD_NAME = ?" +
                    "   AND (TIME = TO_TIMESTAMP(MILLIS, ?) " +
                    "       OR TIME < DATEADD(HOUR, -1, TO_TIMESTAMP(MILLIS, ?)));");

    public VoltTable[] run(String host, String threadName, long millis) {
        voltQueueSQL(query, host, threadName, millis, millis);
        return voltExecuteSQL(true);
    }
}