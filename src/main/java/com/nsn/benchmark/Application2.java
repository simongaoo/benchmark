package com.nsn.benchmark;

import com.google_voltpatches.common.base.Stopwatch;
import com.nsn.benchmark.entity.Firewall;
import com.nsn.benchmark.mapper.FirewallMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Application2 {
    public static void loader(int batchSize) throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        // init db
        InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("mybatis-config-mariadb.xml");
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(
                resource, "default", new Properties());
        try (SqlSession session = factory.openSession(true);
             InputStream is = Files.newInputStream(Paths.get("/root/FWLOG_DBL_E8080E_20170917182813_0001.DAT"))) {
            List<Firewall> list = new ArrayList<>();
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

                Firewall firewall = new Firewall();
                firewall.setPrivateHost(ph);
                firewall.setPrivatePort(ByteBuffer.wrap(pp).getShort() & 0XFFFF);
                firewall.setDestinationHost(dh);
                firewall.setDestinationPort(ByteBuffer.wrap(dp).getShort() & 0XFFFF);
                firewall.setSourceHost(sh);
                firewall.setSourcePort(ByteBuffer.wrap(sp).getShort() & 0XFFFF);
                firewall.setStartTime(ByteBuffer.wrap(startTime).getInt());
                list.add(firewall);
                if (list.size() == batchSize) {
                    FirewallMapper mapper = session.getMapper(FirewallMapper.class);
                    mapper.insert(list);
                    list.clear();
                }
            }
        }
        System.out.println("Run time: " + stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) + ", Batch size: " + batchSize);
    }

    public static void main(String[] args) throws Exception {
        // init db
        InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("mybatis-config-mariadb.xml");
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(
                resource, "default", new Properties());

       // for (int batchSize = 500; batchSize < 200000; batchSize += 500) {
            try (SqlSession session = factory.openSession(true)) {
                FirewallMapper firewall = session.getMapper(FirewallMapper.class);
                int count = firewall.deleteAll();
                System.out.println("Delete rows count: " + count);
            }
            loader(1000);
      //  }
    }
}
