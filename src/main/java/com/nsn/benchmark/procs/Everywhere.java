package com.nsn.benchmark.procs;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

/**
 * @author zhangxu
 */
public class Everywhere extends VoltProcedure {
    private final SQLStmt query = new SQLStmt(
            "SELECT count(*) FROM FIREWALL;");

    public VoltTable[] run(long id) {
        // Count the phone numbers who voted.
        voltQueueSQL(query);
        return voltExecuteSQL(true);
    }
}