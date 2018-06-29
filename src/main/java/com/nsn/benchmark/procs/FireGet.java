package com.nsn.benchmark.procs;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.client.ClientResponse;

public class FireGet extends VoltProcedure {
    public final SQLStmt selectFire = new SQLStmt(
            "SELECT PUBLIC_IP, PUBLIC_PORT " +
                    "FROM FIRE WHERE " +
                    "PRIVATE_IP = ? AND PRIVATE_PORT = ? AND " +
                    "DEST_IP = ? AND DEST_PORT = ? ;");

    public long run(String privateIp, int privatePort,
                    String destIp, int destPort) throws VoltAbortException {
        voltQueueSQL(selectFire,
                privateIp, privatePort,
                destIp, destPort);

        voltExecuteSQL();

        return ClientResponse.SUCCESS;
    }
}