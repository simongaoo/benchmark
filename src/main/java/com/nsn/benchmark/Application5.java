package com.nsn.benchmark;

import com.google_voltpatches.common.base.Stopwatch;
import org.voltdb.client.Client;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.VoltBulkLoader.BulkLoaderFailureCallBack;
import org.voltdb.client.VoltBulkLoader.VoltBulkLoader;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Application5 {
    public static void bulkLoader(Client client, int block, int batchSize, int all) throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // Get a BulkLoader for the table we want to load, with a given batch size and one callback handles failures for any failed batches
        VoltBulkLoader bulkLoader = client.getNewBulkLoader("PAIR", batchSize, true,
                new SessionBulkLoaderFailureCallback());
        Random random = new Random();
        int per = Integer.MAX_VALUE / 3;
        int prefix = block * per;
        for (int i = 0; i < all; i++) {
            int r = random.nextInt(per) + prefix;
            byte[] condition =
                    ByteBuffer.allocate(12)
                            .putInt(r)
                            .putShort((short) block)
                            .putInt(r)
                            .putShort((short) block)
                            .array();
            Object[] row = {
                    condition,
                    ByteBuffer.allocate(4).putInt(r).array(),
                    block,
                    System.currentTimeMillis() / 1000,
            };
            bulkLoader.insertRow(row[0], row);
        }
        bulkLoader.drain();
        client.drain();
        bulkLoader.close();
        System.out.println("avg: " + all / stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) * 1000);
    }

    // Implement the BulkLoaderFailureCallBack for BulkLoader
    public static class SessionBulkLoaderFailureCallback implements BulkLoaderFailureCallBack {
        @Override
        public void failureCallback(Object rowHandle, Object[] fieldList, ClientResponse cr) {
            if (cr.getStatus() != ClientResponse.SUCCESS) {
                System.err.println(cr.getStatusString());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Client client = ClientFactory.createClient();
        client.createConnection("10.56.24.50");

        int block = Integer.valueOf(args[0]);
        int batchSize = Integer.valueOf(args[1]);
        int all = Integer.valueOf(args[2]);
        int repeat = Integer.valueOf(args[3]);

        System.out.println(MessageFormat.format("block={0}, batchSize={1}, all={2}, repeat={3}", (Object[]) args));
        for (int i = 0; i < repeat; i++) {
            bulkLoader(client, block, batchSize, all);
        }
        client.close();
    }
}
