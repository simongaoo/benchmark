package com.nsn.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Example {
    private Map<Integer, byte[]> redis = new HashMap<>();
    // 批量执行的大小
    private static final int LIMIT = 10000;

    // 模拟存储至 redis
    public void setRedis(Map<byte[], byte[]> kv) {
        kv.forEach((k, v) -> redis.put(Arrays.hashCode(k), v));
    }

    // 模拟使用 key 获取 redis
    public List<byte[]> getRedis(List<byte[]> keys) {
        return keys.stream()
                .map(bytes -> redis.getOrDefault(Arrays.hashCode(bytes), null))
                .collect(Collectors.toList());
    }

    public void saveFirewall(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            Map<byte[], byte[]> map = new HashMap<>();
            // 文件每 30 个字节作为一条数据
            for (; is.available() >= 30; ) {
                // 30 个字节的前 12 个作为 key， 后 18 个作为 value
                /**
                 * key 中
                 * [0:4] = IP地址
                 * [4:6] = 端口
                 * [6:10] = IP地址
                 * [10:12] = 端口
                 * value 中
                 * [12:16] = IP地址
                 * [16:18] = 端口
                 */
                byte[] key = new byte[12], value = new byte[18];
                is.read(key);
                is.read(value);
                map.put(key, value);
            }
            // 批量写入到 redis 当中
            setRedis(map);
        }
    }

    private void mergeData(Path gnPath) throws IOException {
        List<String> all = Files.readAllLines(gnPath);
        for (int i = 0; i * LIMIT <= all.size(); i++) {
            // 拿到批量执行的数据条数
            List<String> lines = all.subList(i, all.size() >= LIMIT ? (i + 1) * LIMIT : i * LIMIT + all.size() % LIMIT);
            // 将数据按照 csv 格式进行解析
            List<String[]> foo = lines.stream().map((s) -> s.split(",", -1)).collect(Collectors.toList());
            List<byte[]> keys = new ArrayList<>();
            // 从每一条记录当中获取对应 redis 存储的 key 值。并将其转换未对应的二进制格式
            for (String[] bar : foo) {
                ByteBuffer key = ByteBuffer.allocate(12);
                key.put(Inet4Address.getByName(bar[1]).getAddress());
                // short 使用无符号
                key.putShort((short) (Integer.valueOf(bar[2]) & 0xffff));
                key.put(Inet4Address.getByName(bar[3]).getAddress());
                key.putShort((short) (Integer.valueOf(bar[4]) & 0xffff));
                keys.add(key.array());
            }
            // 批量获取 redis 内容以减少网络请求次数，提高吞吐
            List<byte[]> values = getRedis(keys);
            // 因为 redis 返回内容的顺序同请求顺序一致，直接遍历并找到对应的值
            for (int j = 0; j < foo.size(); j++) {
                byte[] v = values.get(j);
                if (v != null) {
                    ByteBuffer byteBuffer = ByteBuffer.wrap(v);
                    byte[] temp = new byte[4];
                    byteBuffer.get(temp);
                    // 讲获取到的值插入到对应的字段位置当中
                    foo.get(j)[5] = Inet4Address.getByAddress(temp).getHostAddress();
                    foo.get(j)[6] = String.valueOf(byteBuffer.getShort() & 0xffff);
                    // 输出合并后的内容
                    System.out.println(String.join(",", foo.get(j)));
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        // 合成资源文件
        Path fwPath = Paths.get("D:\\resources\\benchmark\\src\\main\\resources\\FWLOG_ST6B2F_FORT_20180524170442_0000.DAT");
        // 合成数据文件，在合成过程中，使用 [1:5] 个字段作为 key，找到对应的 value
        Path gnPath = Paths.get("D:\\resources\\benchmark\\src\\main\\resources\\GN.TXT");

        Example example = new Example();
        example.saveFirewall(fwPath);
        example.mergeData(gnPath);
    }
}
