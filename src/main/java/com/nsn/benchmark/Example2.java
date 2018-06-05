package com.nsn.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Example2 {
    private Map<String, List<String>> redis = new HashMap<>();
    // 批量执行的大小
    private static final int LIMIT = 10000;

    // 模拟存储至 redis
    public void setRedisByAppend(Map<String, String> kv) {
        kv.forEach((k, v) -> redis.computeIfAbsent(k, _k -> new ArrayList<>()).add(kv.get(k)));
    }

    // 模拟使用 key 获取 redis
    public List<List<String>> getRedis(List<String> keys) {
        return keys.stream()
                .map(bytes -> redis.getOrDefault(bytes, null))
                .collect(Collectors.toList());
    }

    /**
     * 存储 imsi imei 对应关系到 redis 中
     *
     * @param pdpPath
     * @throws IOException
     */
    private void saveImsiImei(Path pdpPath) throws IOException {
        try (Stream<String> stream = Files.lines(pdpPath)) {
            stream.forEach(s -> {
                String[] foo = s.split(",", 2);
                // 实际应用中会在资源累积到 LIMIT 或者当前文件没有可读内容后统一存储
                setRedisByAppend(new HashMap<String, String>() {{
                    put(foo[0], foo[1]);
                }});
            });
        }
        System.out.println("Redis data: " + redis);
    }

    private void mergeData(Path gnPath) throws IOException {
        try (Stream<String> stream = Files.lines(gnPath)) {
            stream.forEach(s -> {
                String[] foo = s.split(",", -1);
                long dataTime = Long.valueOf(foo[3]);
                List<List<String>> list = getRedis(Collections.singletonList(foo[1]));
                List<String> values = list.get(0);

                long distance = Long.MAX_VALUE;
                for (String v : values) {
                    String[] bar = v.split(",", -1);
                    long time = Long.valueOf(bar[1]);
                    // 计算 redis 中存储内容与数据文件中得时间差，并使用时间差最小的值回填
                    if (Math.abs(dataTime - time) < distance) {
                        distance = Math.abs(dataTime - time);
                        foo[2] = bar[0];
                    }
                }
                // 输出
                System.out.println(String.join(",", foo));
            });
        }
    }

    public static void main(String[] args) throws IOException {
        // 合成资源文件
        Path pdpPath = Paths.get("D:\\resources\\benchmark\\src\\main\\resources\\PDP2.TXT");
        // 合成数据文件，手机号,imsi,,时间
        Path gnPath = Paths.get("D:\\resources\\benchmark\\src\\main\\resources\\GN2.TXT");

        Example2 example = new Example2();
        example.saveImsiImei(pdpPath);
        example.mergeData(gnPath);
    }
}
