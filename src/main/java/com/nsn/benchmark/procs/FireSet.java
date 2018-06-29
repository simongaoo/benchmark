package com.nsn.benchmark.procs;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.client.ClientResponse;

public class FireSet extends VoltProcedure {
    public final SQLStmt insertFire = new SQLStmt(
            "INSERT INTO FIRE VALUES (?, ?, ?, ?, ?, ?, 1, 2, 3, 0);");

    public long run(String[][] list) throws VoltProcedure.VoltAbortException {
        for (String[] fw : list) {
            voltQueueSQL(insertFire, fw[0], fw[1], fw[2], fw[3], fw[4], fw[5]);
            voltExecuteSQL();
        }
        return ClientResponse.SUCCESS;
    }
}