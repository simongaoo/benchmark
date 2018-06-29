package com.nsn.benchmark;

import com.google_voltpatches.common.base.Stopwatch;
import com.nsn.benchmark.mapper.FirewallMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.voltdb.client.Client;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.VoltBulkLoader.BulkLoaderFailureCallBack;
import org.voltdb.client.VoltBulkLoader.VoltBulkLoader;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Application {
    public static void bulkLoader(Client client, int batchSize) throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // Get a BulkLoader for the table we want to load, with a given batch size and one callback handles failures for any failed batches
        VoltBulkLoader bulkLoader = client.getNewBulkLoader("FIREWALL", batchSize, true,
                new SessionBulkLoaderFailureCallback());
        try (InputStream is = Files.newInputStream(Paths.get("/root/FWLOG_DBL_E8080E_20170917182813_0001.DAT"))) {
            while (is.available() >= 30) {
                byte[] ph = new byte[4];
                is.read(ph);
                byte[] pp = new byte[2];
                is.read(pp);

                byte[] dh = new byte[4];
                is.read(dh);
                byte[] dp = new byte[2];
                is.read(dp);

                byte[] sh = new byte[4];
                is.read(sh);
                byte[] sp = new byte[2];
                is.read(sp);

                byte[] startTime = new byte[4];
                is.read(startTime);
                byte[] drop = new byte[8];
                is.read(drop);
                Object[] row = {
                        ph,
                        ByteBuffer.wrap(pp).getShort() & 0XFFFF,
                        dh,
                        ByteBuffer.wrap(dp).getShort() & 0XFFFF,
                        sh,
                        ByteBuffer.wrap(sp).getShort() & 0XFFFF,
                        ByteBuffer.wrap(startTime).getInt(),
                };
                bulkLoader.insertRow(row[0], row);
            }
        }
        bulkLoader.drain();
        client.drain();
        bulkLoader.close();
        System.out.println("Run time: " + stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) + ", Batch size: " + batchSize);
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

        // init db
        InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(
                resource, "default", new Properties());

        for (int batchSize = 500; batchSize < 200000; batchSize += 500) {
            try (SqlSession session = factory.openSession(true)) {
                FirewallMapper firewall = session.getMapper(FirewallMapper.class);
                int count = firewall.deleteAll();
                System.out.println("Delete rows count: " + count);
            }
            bulkLoader(client, batchSize);
        }
        client.close();
    }
}
