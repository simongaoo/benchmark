/* This file is part of VoltDB.
 * Copyright (C) 2008-2018 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.nsn.benchmark.adperformance;

import com.google_voltpatches.common.base.Stopwatch;
import com.google_voltpatches.common.collect.ConcurrentHashMultiset;
import com.google_voltpatches.common.collect.Multiset;
import org.voltdb.CLIConfig;
import org.voltdb.client.*;
import org.voltdb.client.VoltBulkLoader.BulkLoaderFailureCallBack;
import org.voltdb.client.VoltBulkLoader.VoltBulkLoader;
import org.voltdb.types.TimestampType;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AdTrackingBenchmark {

    // handy, rather than typing this out several times
    public static final String HORIZONTAL_RULE =
            "----------" + "----------" + "----------" + "----------" +
                    "----------" + "----------" + "----------" + "----------" + "\n";

    // validated command line configuration
    final AdPerformanceConfig config;
    // Reference to the database connection we will use
    final Client client;
    // Timer for periodic stats printing
    Timer timer;
    // Benchmark start time
    long benchmarkStartTS;
    // Statistics manager objects from the client
    final ClientStatsContext periodicStatsContext;
    final ClientStatsContext fullStatsContext;

    private Random rand = new Random();
    private MathContext mc = new MathContext(2);
    private BigDecimal bd0 = new BigDecimal(0);
    private long startTime = new TimestampType(System.currentTimeMillis() * 1000).getTime();

    // inventory pre-sets
    private int sites = 1000;
    private int pagesPerSite = 20;

    // creatives pre-sets
    private int advertisers = 1000;
    private int campaignsPerAdvertiser = 10;
    private int creativesPerCampaign = 10;
    private int modulus = 100;

    // counters
    private int inventoryMaxID = 0;
    private int creativeMaxID = 0;
    private long iteration = 0L;

    /**
     * Prints headings
     */
    static void printHeading(String heading) {
        System.out.print("\n" + HORIZONTAL_RULE);
        System.out.println(" " + heading);
        System.out.println(HORIZONTAL_RULE);
    }

    /**
     * Uses CLIConfig class to declaratively state command line options
     * with defaults and validation.
     */
    public static class AdPerformanceConfig extends CLIConfig {
        // STANDARD BENCHMARK OPTIONS
        @Option(desc = "Comma separated list of the form server[:port] to connect to.")
        String servers = "localhost";

        @Option(desc = "User name for connection.")
        public String user = "";

        @Option(desc = "Password for connection.")
        public String password = "";

        @Option(desc = "Benchmark duration, in seconds.")
        int duration = 120;

        @Option(desc = "Interval for performance feedback, in seconds.")
        long displayinterval = 5;

        @Option(desc = "Warmup duration in seconds.")
        int warmup = 5;

        @Option(desc = "Maximum TPS rate for benchmark.")
        int ratelimit = 20000;

        @Option(desc = "Filename to write raw summary statistics to.")
        String statsfile = "";

        // CUSTOM OPTIONS
        @Option(desc = "Number of Sites")
        int sites = 100;

        @Option(desc = "Pages per Site")
        int pagespersite = 10;

        @Option(desc = "Number of Advertisers")
        int advertisers = 20;

        @Option(desc = "Campaigns per Site")
        int campaignsperadvertiser = 5;

        @Option(desc = "Creatives per Campaign")
        int creativespercampaign = 5;

        @Override
        public void validate() {
            if (duration <= 0) exitWithMessageAndUsage("duration must be > 0");
            if (warmup < 0) exitWithMessageAndUsage("warmup must be >= 0");
            if (displayinterval <= 0) exitWithMessageAndUsage("displayinterval must be > 0");
            if (ratelimit <= 0) exitWithMessageAndUsage("ratelimit must be > 0");
        }
    }

    /**
     * Provides a callback to be notified on node failure.
     * This example only logs the event.
     */
    class StatusListener extends ClientStatusListenerExt {
        @Override
        public void connectionLost(String hostname, int port, int connectionsLeft, DisconnectCause cause) {
            // if the benchmark is still active
            if ((System.currentTimeMillis() - benchmarkStartTS) < (config.duration * 1000)) {
                System.err.printf("Connection to %s:%d was lost.\n", hostname, port);
            }
        }
    }

    // constructor
    public AdTrackingBenchmark(AdPerformanceConfig config) {
        this.config = config;

        ClientConfig clientConfig = new ClientConfig(config.user, config.password, new StatusListener());
        clientConfig.setMaxTransactionsPerSecond(config.ratelimit);
        client = ClientFactory.createClient(clientConfig);

        periodicStatsContext = client.createStatsContext();
        fullStatsContext = client.createStatsContext();

        printHeading("Command Line Configuration");
        System.out.println(config.getConfigDumpString());

        // set any instance attributes here
        sites = config.sites;
        pagesPerSite = config.pagespersite;
        advertisers = config.advertisers;
        campaignsPerAdvertiser = config.campaignsperadvertiser;
        creativesPerCampaign = config.creativespercampaign;
        modulus = creativesPerCampaign * 3;
        creativeMaxID = advertisers * campaignsPerAdvertiser * creativesPerCampaign;
    }

    /**
     * Connect to a single server with retry. Limited exponential backoff.
     * No timeout. This will run until the process is killed if it's not
     * able to connect.
     *
     * @param server hostname:port or just hostname (hostname can be ip).
     */
    void connectToOneServerWithRetry(String server) {
        int sleep = 1000;
        while (true) {
            try {
                client.createConnection(server);
                break;
            } catch (Exception e) {
                System.err.printf("Connection failed - retrying in %d second(s).\n", sleep / 1000);
                try {
                    Thread.sleep(sleep);
                } catch (Exception interruted) {
                }
                if (sleep < 8000) sleep += sleep;
            }
        }
        System.out.printf("Connected to VoltDB node at: %s.\n", server);
    }

    /**
     * Connect to a set of servers in parallel. Each will retry until
     * connection. This call will block until all have connected.
     *
     * @param servers A comma separated list of servers using the hostname:port
     *                syntax (where :port is optional).
     * @throws InterruptedException if anything bad happens with the threads.
     */
    void connect(String servers) throws InterruptedException {
        System.out.println("Connecting to VoltDB...");

        String[] serverArray = servers.split(",");
        final CountDownLatch connections = new CountDownLatch(serverArray.length);

        // use a new thread to connect to each server
        for (final String server : serverArray) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectToOneServerWithRetry(server);
                    connections.countDown();
                }
            }).start();
        }
        // block until all have connected
        connections.await();
    }

    /**
     * Create a Timer task to display performance data on the Vote procedure
     * It calls printStatistics() every displayInterval seconds
     */
    public void schedulePeriodicStats() {
        timer = new Timer();
        TimerTask statsPrinting = new TimerTask() {
            @Override
            public void run() {
                printStatistics();
            }
        };
        timer.scheduleAtFixedRate(statsPrinting,
                config.displayinterval * 1000,
                config.displayinterval * 1000);
    }

    /**
     * Prints a one line update on performance that can be printed
     * periodically during a benchmark.
     */
    public synchronized void printStatistics() {
        ClientStats stats = periodicStatsContext.fetchAndResetBaseline().getStats();
        long time = Math.round((stats.getEndTimestamp() - benchmarkStartTS) / 1000.0);

        System.out.printf("%02d:%02d:%02d ", time / 3600, (time / 60) % 60, time % 60);
        System.out.printf("Throughput %d/s, ", stats.getTxnThroughput());
        System.out.printf("Aborts/Failures %d/%d, ",
                stats.getInvocationAborts(), stats.getInvocationErrors());

        // cast to stats.getAverageLatency from long to double
        System.out.printf("Avg/95%% Latency %.2f/%dms\n",
                stats.getAverageLatency(),
                stats.kPercentileLatency(0.95));

    }

    /**
     * Prints the results of the voting simulation and statistics
     * about performance.
     *
     * @throws Exception if anything unexpected happens.
     */
    public synchronized void printResults() throws Exception {
        printHeading("Transaction Results");
        BenchmarkCallback.printAllResults();

        ClientStats stats = fullStatsContext.fetch().getStats();

        // 3. Performance statistics
        printHeading("Client Workload Statistics");

        System.out.printf("Average throughput:            %,9d txns/sec\n", stats.getTxnThroughput());
        System.out.printf("Average latency:               %,9.2f ms\n", stats.getAverageLatency());
        System.out.printf("95th percentile latency:       %,9d ms\n", stats.kPercentileLatency(.95));
        System.out.printf("99th percentile latency:       %,9d ms\n", stats.kPercentileLatency(.99));

        printHeading("System Server Statistics");

        System.out.printf("Reported Internal Avg Latency: %,9.2f ms\n", stats.getAverageInternalLatency());

        // 4. Write stats to file if requested
        client.writeSummaryCSV(stats, config.statsfile);
    }

    // Implement the BulkLoaderFailureCallBack for BulkLoader
    public static class SessionBulkloaderFailureCallback implements BulkLoaderFailureCallBack {
        @Override
        public void failureCallback(Object rowHandle, Object[] fieldList, ClientResponse cr) {

            if (cr.getStatus() != ClientResponse.SUCCESS) {
                System.err.println(cr.getStatusString());
            }
        }
    }

    public void initialize() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        VoltBulkLoader bulkLoader = client.getNewBulkLoader("FIRE1", 10000, new SessionBulkloaderFailureCallback());
        int count = 0;
        for (int i = 0; i < 5; i++)
            try (InputStream is = Files.newInputStream(Paths.get("/root/FWLOG_DBL_E8080E_20170917182813_0001.DAT"))) {
                while (is.available() >= 30) {
                    byte[] p = new byte[6];
                    is.read(p);
                    byte[] d = new byte[6];
                    is.read(d);
                    byte[] s = new byte[4];
                    is.read(s);
                    byte[] sp = new byte[2];
                    is.read(sp);
                    byte[] drop = new byte[12];
                    is.read(drop);
                    Object[] row = {p, d, count++, s, ByteBuffer.wrap(sp).getShort()};
                    bulkLoader.insertRow(p, row);
                }
            }
//        IntStream.range(0, 1000_000)
//                .mapToObj(value -> {
//                    try {
//                        return Inet4Address.getByAddress(ByteBuffer.allocate(4).putInt(value).array()).getHostAddress();
//                    } catch (UnknownHostException e) {
//                        return null;
//                    }
//                })
//                .forEach(s -> {
//                    Object[] row = {s, 101, s, 102, s, 103, 1, 2, 3, new TimestampType()};
//                    try {
//                        bulkLoader.insertRow(s, row);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                });

        bulkLoader.drain();
        client.drain();
        bulkLoader.close();
        System.out.println("Run time: " + stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
    }

    public static class BenchmarkCallback implements ProcedureCallback {

        private static Multiset<String> stats = ConcurrentHashMultiset.create();
        private static ConcurrentHashMap<String, Integer> procedures = new ConcurrentHashMap<String, Integer>();
        String procedureName;
        long maxErrors;

        public static int count(String procedureName, String event) {
            return stats.add(procedureName + event, 1);
        }

        public static int getCount(String procedureName, String event) {
            return stats.count(procedureName + event);
        }

        public static void printProcedureResults(String procedureName) {
            System.out.println("  " + procedureName);
            System.out.println("        calls: " + getCount(procedureName, "call"));
            System.out.println("      commits: " + getCount(procedureName, "commit"));
            System.out.println("    rollbacks: " + getCount(procedureName, "rollback"));
        }

        public static void printAllResults() {
            List<String> l = new ArrayList<String>(procedures.keySet());
            Collections.sort(l);
            for (String e : l) {
                printProcedureResults(e);
            }
        }

        public BenchmarkCallback(String procedure, long maxErrors) {
            super();
            this.procedureName = procedure;
            this.maxErrors = maxErrors;
            procedures.putIfAbsent(procedure, 1);
        }

        public BenchmarkCallback(String procedure) {
            this(procedure, 5l);
        }

        @Override
        public void clientCallback(ClientResponse cr) {

            count(procedureName, "call");

            if (cr.getStatus() == ClientResponse.SUCCESS) {
                count(procedureName, "commit");
            } else {
                long totalErrors = count(procedureName, "rollback");

                if (totalErrors > maxErrors) {
                    System.err.println("exceeded " + maxErrors + " maximum database errors - exiting client");
                    System.exit(-1);
                }

                System.err.println("DATABASE ERROR: " + cr.getStatusString());
            }
        }
    }

    public void iterate(int value) throws Exception {
        String host = Inet4Address.getByAddress(ByteBuffer.allocate(4).putInt(value).array()).getHostAddress();

        client.callProcedure(new BenchmarkCallback("FireGet"),
                "FireGet",
                host, 8080,
                host, 8080);
    }

    /**
     * Core benchmark code.
     * Connect. Initialize. Run the loop. Cleanup. Print Results.
     *
     * @throws Exception if anything unexpected happens.
     */
    public void runBenchmark() throws Exception {
        printHeading("Setup & Initialization");

        // connect to one or more servers, loop until success
        connect(config.servers);

        // initialize using synchronous call
        System.out.println("\nPre-loading Tables...\n");
        initialize();

        // ensure all data from initialize is committed before proceeding to iterate().
        client.drain();

        // Run the benchmark loop for the requested warmup time
        // The throughput may be throttled depending on client configuration
        System.out.println("Warming up for the specified " + config.warmup + " seconds...");
        final long warmupEndTime = System.currentTimeMillis() + (1000l * config.warmup);
        int value = 0;
        while (warmupEndTime > System.currentTimeMillis()) {
            iterate(value++);
        }

        printHeading("Starting Benchmark");

        // reset the stats after warmup
        fullStatsContext.fetchAndResetBaseline();
        periodicStatsContext.fetchAndResetBaseline();

        // print periodic statistics to the console
        benchmarkStartTS = System.currentTimeMillis();
        schedulePeriodicStats();

        // Run the benchmark loop for the requested duration
        // The throughput may be throttled depending on client configuration
        System.out.println("\nRunning benchmark...");
        final long benchmarkEndTime = System.currentTimeMillis() + (1000l * config.duration);
        value = 0;
        while (benchmarkEndTime > System.currentTimeMillis()) {
            iterate(value++);
        }

        // cancel periodic stats printing
        timer.cancel();

        // block until all outstanding txns return
        client.drain();

        // print the summary results
        printResults();

        // close down the client connections
        client.close();
    }

    /**
     * Main routine creates a benchmark instance and kicks off the run method.
     *
     * @param args Command line arguments.
     * @throws Exception if anything goes wrong.
     * @see {@link AdPerformanceConfig}
     */
    public static void main(String[] args) throws Exception {
        AdPerformanceConfig config = new AdPerformanceConfig();
        config.parse(AdTrackingBenchmark.class.getName(), args);

        AdTrackingBenchmark benchmark = new AdTrackingBenchmark(config);
        benchmark.runBenchmark();

        printHeading("Note: The database must be restarted before running this benchmark again.");

    }
}