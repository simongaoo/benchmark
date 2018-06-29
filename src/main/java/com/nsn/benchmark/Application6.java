package com.nsn.benchmark;

import com.google_voltpatches.common.base.Stopwatch;
import com.nsn.benchmark.mapper.FirewallMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.VoltBulkLoader.BulkLoaderFailureCallBack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Application6 {
    public static void bulkLoader(SqlSession session, int batchSize, int block) throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Random random = new Random();
        int per = Integer.MAX_VALUE / 3;
        int prefix = block * per;
        List<byte[]> conditions = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            int r = random.nextInt(per) + prefix;
            byte[] condition =
                    ByteBuffer.allocate(12)
                            .putInt(r)
                            .putShort((short) block)
                            .putInt(r)
                            .putShort((short) block)
                            .array();
            conditions.add(condition);
        }
        FirewallMapper firewall = session.getMapper(FirewallMapper.class);
        List<Map<String, Object>> result = firewall.select(conditions);

        System.out.println("avg: " + batchSize / stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) * 1000 +
                ", count: " + result.size());
        if (result.size() > 0) {
            System.out.println("first value: " + result.get(0));
        }
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

        int block = Integer.valueOf(args[0]);
        int batchSize = Integer.valueOf(args[1]);
        int repeat = Integer.valueOf(args[2]);
        for (int i = 0; i < repeat; i++) {
            try (SqlSession session = factory.openSession(true)) {
                bulkLoader(session, batchSize, block);
            }
        }
    }
}
