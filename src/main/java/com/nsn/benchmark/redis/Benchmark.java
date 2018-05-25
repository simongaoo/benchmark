package com.nsn.benchmark.redis;

import com.google_voltpatches.common.base.Stopwatch;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Benchmark {
    private static final int COUNT = 1000;
    private static final int LIMIT = 10000;
    private static final int ALL = COUNT * LIMIT;

    private final static String servers = "redis1,redis2";
    private final static List<JedisShardInfo> shards = Arrays.stream(servers.split(","))
            .map(JedisShardInfo::new).collect(Collectors.toList());

    private Stream<List<String>> keyGenerator() {
        return IntStream
                .range(0, COUNT)
                .mapToObj((i) -> IntStream
                        .range(0, LIMIT)
                        .mapToObj((index) -> String.format("Key-%d-Index-%d", i, index))
                        .collect(Collectors.toList()));
    }

    private void setBenchmark() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        keyGenerator()
                .parallel()
                .forEach((keys) -> {
                    ShardedJedis shardedJedis = new ShardedJedis(shards);
                    ShardedJedisPipeline pipeline = shardedJedis.pipelined();
                    keys.forEach((k) -> pipeline.set(k, k.replaceFirst("Key", "Value")));
                    pipeline.sync();
                    shardedJedis.close();
                });
        long mills = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
        System.out.println(String.format("Set: %d/s, keys: %d, mills: %d", ALL / mills * 1000, ALL, mills));
    }

    private void getBenchmark() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        keyGenerator()
                .parallel()
                .forEach((keys) -> {
                    ShardedJedis shardedJedis = new ShardedJedis(shards);
                    ShardedJedisPipeline pipeline = shardedJedis.pipelined();
                    keys.forEach(pipeline::get);
                    List<Object> list = pipeline.syncAndReturnAll();
                    shardedJedis.close();

                    /*
                    // 实际项目中的处理逻辑是：
                    ShardedJedis shardedJedis = new ShardedJedis(shards);
                    ShardedJedisPipeline pipeline = shardedJedis.pipelined();
                    // 解析 csv 文件中的内容，提取其中的关键字作为 key，使用 pipeline 批量查询。
                    List<String[]> cache = new ArrayList<>();
                    for (String ...){
                        String line = "0,1,2,3,KEY,5,6,VALUE,8,9";
                        String[] params = line.split(",");
                        // 设置一个查询
                        pipeline.get(params[4]);
                        // 缓存全部数据用以取得结果后的后续处理
                        cache.add(params);
                    }

                    // 查询全部结果
                    List<Object> list = pipeline.syncAndReturnAll();
                    // 由于 redis 的处理机制，使得返回内容的顺序同查询调用舒徐完全一致。可直接循环调用获取到对应的内容。
                    for (int i = 0; i < cache.size(); i++) {
                        String[] params = cache.get(i);
                        // 将查询到结果合并
                        params[7] = (String) list.get(i);
                        // 输出 params.join(",")
                    }
                    shardedJedis.close();
                    */
                });
        long mills = stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
        System.out.println(String.format("Get: %d/s, keys: %d, mills: %d", ALL / mills * 1000, ALL, mills));
    }

    public static void main(String[] args) {
        Benchmark benchmark = new Benchmark();
        benchmark.setBenchmark();
        benchmark.getBenchmark();
    }
}
