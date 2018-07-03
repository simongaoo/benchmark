package com.nsn.benchmark;

import com.google_voltpatches.common.base.Stopwatch;
import org.voltdb.client.Client;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.VoltBulkLoader.BulkLoaderFailureCallBack;
import org.voltdb.client.VoltBulkLoader.VoltBulkLoader;

import java.io.InputStream;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Everywhere {
    /**
     * Run the a query (or stored procedure) on every partition, as a s
     * ingle-partition transaction.
     *
     * @throws Exception if anything unexpected happens.
     */
    public static void run_everywhere(Client client) throws Exception {
        // Get the partition key for each partition from the database.
        // Once we have partition ids for all partitions we'll loop through
        // and invoke our query or transaction as a single-part transaction.

        org.voltdb.VoltTable results[] = client.callProcedure("@GetPartitionKeys", "INTEGER")
                .getResults();
        org.voltdb.VoltTable keys = results[0];
        for (int k = 0;k < keys.getRowCount(); k++) {
            long key = keys.fetchRow(k).getLong(1);
            org.voltdb.VoltTable voter_count_table = client.callProcedure("Everywhere", key)
                    .getResults()[0];
            System.out.println("Partition " + key + " row count = " +
                    voter_count_table.fetchRow(0).getLong(0));
        }
    }

    public static void main(String[] args) throws Exception {
        Client client = ClientFactory.createClient();
        client.createConnection("127.0.0.1");
        run_everywhere(client);
    }
}
