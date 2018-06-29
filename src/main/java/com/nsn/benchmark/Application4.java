package com.nsn.benchmark;

import com.google_voltpatches.common.base.Stopwatch;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Application4 {
    public static void loader(ShardedJedisPipeline pipeline, int batchSize) throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (InputStream is = Files.newInputStream(Paths.get("/root/FWLOG_DBL_E8080E_20170917182813_0001.DAT"))) {
            int index = 0;
            while (is.available() >= 30) {
                byte[] key = new byte[12];
                is.read(key);
                byte[] value = new byte[18];
                is.read(value);
                pipeline.set(key, value);
                if (index++ == batchSize) {
                    pipeline.sync();
                }
            }
        }
        System.out.println("Run time: " + stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) + ", Batch size: " + batchSize);
    }

    public static void main(String[] args) throws Exception {
        ShardedJedis shardedJedis = new ShardedJedis(
                new ArrayList<JedisShardInfo>() {{
                    add(new JedisShardInfo("10.56.24.50"));
                    add(new JedisShardInfo("10.56.24.53"));
                    add(new JedisShardInfo("10.56.24.59"));
                }}
        );
        //

        for (int batchSize = 500; batchSize < 200000; batchSize += 500) {
            loader(shardedJedis.pipelined(), batchSize);
        }
    }
}
