package com.nsn.benchmark;

import org.apache.commons_voltpatches.cli.*;

/**
 * @author zhangxu
 */
public class RedisBenchmark {
    public static void main(String[] args) throws ParseException {
        // create Options object
        Options options = new Options();

        // add t option
        options.addOption("h", "hostname", true, "Server hostname (default 127.0.0.1))");
        options.addOption("p", "port", true, "Server port (default 6379)");
        options.addOption("s", "socket", true, "Server socket (overrides host and port)");
        options.addOption("a", "password", true, "Password for Redis Auth");
        options.addOption("c", "clients", true, "Number of parallel connections (default 50)");
        options.addOption("n", "requests", true, "Total number of requests (default 100000)");
        options.addOption("d", "size", true, "Data size of SET/GET value in bytes (default 3)");
        options.addOption("dbnum", "db", true, "SELECT the specified db number (default 0)");
        options.addOption("k", "boolean", true, "1=keep alive 0=reconnect (default 1)");
        options.addOption("r", "keyspacelen", true, "Use random keys for SET/GET/INCR, random values for SADD Using this option the benchmark will expand the string __rand_int__ inside an argument with a 12 digits number in the specified range from 0 to keyspacelen-1. The substitution changes every time a command is executed. Default tests use this to hit random keys in the specified range.");
        options.addOption("P", "numreq", true, "Pipeline <numreq> requests. Default 1 (no pipeline).");
        options.addOption("e", true, "If server replies with errors, show them on stdout. (no more than 1 error per second is displayed)");
        options.addOption("q", true, "Quiet. Just show query/sec values");
        options.addOption("csv", true, "Output in CSV format");
        options.addOption("l", true, "Loop. Run the tests forever");
        options.addOption("t", "tests", true, "Only run the comma separated list of tests. The procs names are the same as the ones produced as output.");
        options.addOption("I", true, "Idle mode. Just open N idle connections and wait.");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        //HelpFormatter formatter = new HelpFormatter();
        //formatter.printHelp("RedisBenchmark", options);
        String hostname = cmd.getOptionValue("hostname", "127.0.0.1");
        System.out.println(hostname);

        System.out.println(cmd.getOptionValue("port", "6379"));
        System.out.println(cmd.getOptionValue("requests", "100000"));
        System.out.println(cmd.getOptionValue("clients", "50"));
        System.out.println(cmd.getOptionValue("size", "3"));
        System.out.println(cmd.getOptionValue("numreq", "1"));
        System.out.println(cmd.getOptionValue("tests"));
        System.out.println(cmd.getOptionValue("keyspacelen", ""));
    }
}
