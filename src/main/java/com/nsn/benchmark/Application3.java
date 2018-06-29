package com.nsn.benchmark;

import com.google_voltpatches.common.base.Stopwatch;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ShardedJedis;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class Application3 {
    public static void loader(Pipeline pipeline, int batchSize) throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (
                InputStream is = Files.newInputStream(Paths.get("/root/FWLOG_DBL_E8080E_20170917182813_0001.DAT"))) {
            int index = 0;
            while (is.available() >= 30) {
                byte[] key = new byte[12];
                is.read(key);
                byte[] value = new byte[18];
                is.read(value);
                pipeline.set(key, value);
                if (index++ == batchSize) {
                    pipeline.sync();
                    pipeline.clear();
                }
            }
        }
        System.out.println("Run time: " + stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) + ", Batch size: " + batchSize);
    }

    public static void main(String[] args) throws Exception {
        //10.56.24.50
        Jedis jedis = new Jedis("10.56.24.50");

        for (int batchSize = 500; batchSize < 200000; batchSize += 500) {
            try (Pipeline pipeline = jedis.pipelined()) {
                pipeline.flushAll();
                pipeline.sync();
                pipeline.clear();
                loader(pipeline, batchSize);
            }
        }
    }
}
