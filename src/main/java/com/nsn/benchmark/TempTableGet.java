package com.nsn.benchmark;

import com.google_voltpatches.common.base.Stopwatch;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.VoltBulkLoader.BulkLoaderFailureCallBack;
import org.voltdb.client.VoltBulkLoader.VoltBulkLoader;

import java.io.InputStream;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhangxu
 */
public class TempTableGet {
    private static final Logger logger = LoggerFactory.getLogger(TempTableGet.class);
    private static String HOST;
    private static AtomicInteger index = new AtomicInteger();

    static {
        try {
            HOST = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void bulkLoader(Client client, SqlSession session, int batchSize, int block) throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        String threadName = Thread.currentThread().getName();
        long currentTime = System.currentTimeMillis() * 1000;

        VoltBulkLoader bulkLoader = client.getNewBulkLoader("TEMPORARY", batchSize, false,
                new SessionBulkLoaderFailureCallback());

        Random random = new Random();
        int per = Integer.MAX_VALUE / 3;
        int prefix = block * per;
        for (int i = 0; i < batchSize; i++) {
            int r = index.incrementAndGet();
            byte[] condition = ByteBuffer
                    .allocate(12)
                    .putInt(r)
                    .putShort((short) block)
                    .putInt(r)
                    .putShort((short) block)
                    .array();
            bulkLoader.insertRow(condition, new Object[]{condition, HOST, threadName, currentTime, i});
        }
        bulkLoader.flush();
        bulkLoader.drain();
        bulkLoader.close();

        logger.info("avg: {} / {} = {}", bulkLoader.getCompletedRowCount(),
                stopwatch.elapsed(TimeUnit.MILLISECONDS) * 1000,
                bulkLoader.getCompletedRowCount() / stopwatch.elapsed(TimeUnit.MILLISECONDS) * 1000);

        // -- select
        ClientResponse response = client.callProcedure("SelectFromPair", HOST, threadName, currentTime / 1000);
        int rowCount = response.getResults()[0].getRowCount();
        logger.info("found: {}", rowCount);
        // -- delete
        VoltTable table = client.callProcedure("DeleteFromPair", HOST, threadName, currentTime / 1000).getResults()[0];
        table.advanceRow();
        logger.info("delete: {}", table.get(0, VoltType.INTEGER));
        logger.info("get: {} / {} = {}, speed: {}/s", rowCount, batchSize, (double) rowCount / batchSize,
                batchSize / stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) * 1000);
        client.drain();
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
        InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(
                resource, "default", new Properties());
        Client client = ClientFactory.createClient();
        client.createConnection("10.56.24.50");

        int block = Integer.valueOf(args[0]);
        int batchSize = Integer.valueOf(args[1]);
        int repeat = Integer.valueOf(args[2]);
        for (int i = 0; i < repeat; i++) {
            try (SqlSession session = factory.openSession(true)) {
                bulkLoader(client, session, batchSize, block);
            }
        }
    }
}
