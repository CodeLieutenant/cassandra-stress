/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2016 Cloudius Systems
 *
 * Modified by Cloudius Systems
 */

/*
 * This file is part of Scylla.
 *
 * Scylla is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Scylla is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scylla.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.scylladb.tools;

import static com.datastax.driver.core.Cluster.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.Config;
import org.apache.cassandra.config.EncryptionOptions;
import org.apache.cassandra.config.YamlConfigurationLoader;
import org.apache.cassandra.db.DecoratedKey;
import org.apache.cassandra.dht.IPartitioner;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.dht.Token;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.utils.FBUtilities;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.SSLOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.TokenRange;

public class BulkLoader {
    public static class CmdLineOptions extends Options {
        /**
         * Add option without argument
         *
         * @param opt
         *            shortcut for option name
         * @param longOpt
         *            complete option name
         * @param description
         *            description of the option
         * @return updated Options object
         */
        public Options addOption(String opt, String longOpt,
                String description) {
            return addOption(new Option(opt, longOpt, false, description));
        }

        /**
         * Add option with argument and argument name
         *
         * @param opt
         *            shortcut for option name
         * @param longOpt
         *            complete option name
         * @param argName
         *            argument name
         * @param description
         *            description of the option
         * @return updated Options object
         */
        public Options addOption(String opt, String longOpt, String argName,
                String description) {
            Option option = new Option(opt, longOpt, true, description);
            option.setArgName(argName);

            return addOption(option);
        }
    }

    static class CQLClient implements Client {
        private final Cluster cluster;
        private final Session session;
        private final Metadata metadata;
        private final KeyspaceMetadata keyspaceMetadata;
        private final IPartitioner partitioner;
        private final boolean simulate;
        private final boolean verbose;
        private BatchStatement batchStatement;

        private Object tokenKey;

        public CQLClient(LoaderOptions options, String keyspace)
                throws NoSuchAlgorithmException, FileNotFoundException,
                IOException, KeyStoreException, CertificateException,
                UnrecoverableKeyException, KeyManagementException,
                ConfigurationException {
            this.simulate = options.simulate;
            this.verbose = options.verbose;
            Cluster.Builder builder = builder().addContactPoints(options.hosts)
                    .withProtocolVersion(ProtocolVersion.V3);
            if (options.user != null && options.passwd != null) {
                builder = builder.withCredentials(options.user, options.passwd);
            }
            if (options.ssl) {
                EncryptionOptions enco = options.encOptions;
                SSLContext ctx = SSLContext
                        .getInstance(options.encOptions.protocol);

                try (FileInputStream tsf = new FileInputStream(enco.truststore);
                        FileInputStream ksf = new FileInputStream(
                                enco.keystore)) {
                    KeyStore ts = KeyStore.getInstance(enco.store_type);
                    ts.load(tsf, enco.truststore_password.toCharArray());
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                            TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(ts);

                    KeyStore ks = KeyStore.getInstance("JKS");
                    ks.load(ksf, enco.keystore_password.toCharArray());
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                            KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(ks, enco.keystore_password.toCharArray());
                    ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
                            new SecureRandom());
                }
                builder = builder
                        .withSSL(new SSLOptions(ctx, enco.cipher_suites));
            }
            cluster = builder.build();
            session = cluster.connect(keyspace);
            metadata = cluster.getMetadata();
            keyspaceMetadata = metadata.getKeyspace(keyspace);
            partitioner = FBUtilities.newPartitioner(metadata.getPartitioner());
        }

        @Override
        public void finish() {
            if (batchStatement != null
                    && !batchStatement.getStatements().isEmpty()) {
            	if (verbose) {
            		System.out.println("BATCH: ");
            		for (Statement s : batchStatement.getStatements()) {
                        System.out.print("  ");
                        System.out.println(s);
            		}
            		System.out.println("END");            		
            	}
            	if (!simulate) {
            		session.execute(batchStatement);
            	}
            }
            batchStatement = new BatchStatement();
        }

        private BatchStatement getBatchStatement(Object callback) {
            if (tokenKey == callback && batchStatement != null) {
                return batchStatement;
            }
            finish();
            tokenKey = callback;
            return batchStatement;
        }

        @Override
        public CFMetaData getCFMetaData(String keyspace, String cfName) {
            KeyspaceMetadata ks = metadata.getKeyspace(keyspace);
            TableMetadata cf = ks.getTable(cfName);
            return CFMetaData.compile(cf.asCQLQuery(), keyspace);
        }

        @Override
        public Map<InetAddress, Collection<Range<Token>>> getEndpointRanges() {
            HashMap<InetAddress, Collection<Range<Token>>> map = new HashMap<>();
            for (TokenRange range : metadata.getTokenRanges()) {
                Range<Token> tr = new Range<Token>(getToken(range.getStart()),
                        getToken(range.getEnd()), getPartitioner());
                for (Host host : metadata.getReplicas(getKeyspace(), range)) {
                    Collection<Range<Token>> c = map.get(host.getAddress());
                    if (c == null) {
                        c = new ArrayList<>();
                        map.put(host.getAddress(), c);
                    }
                    c.add(tr);
                }
            }
            return map;
        }

        private String getKeyspace() {
            return keyspaceMetadata.getName();
        }

        @Override
        public IPartitioner getPartitioner() {
            return partitioner;
        }

        private Token getToken(com.datastax.driver.core.Token t) {
            DataType type = t.getType();
            Object value = t.getValue();
            return getPartitioner().getTokenFactory()
                    .fromByteArray(type.serialize(value, ProtocolVersion.V3));
        }

        @Override
        public void processStatment(Object callback, DecoratedKey key,
                long timestamp, String what, List<Object> objects) {
            if (simulate || verbose) {
                System.out.print("CQL: '");
                System.out.print(what);
                System.out.print("'");
                if (!objects.isEmpty()) {
                    System.out.print(" ");
                    System.out.print(objects);
                }
                System.out.println();
            }
            
            SimpleStatement s = new SimpleStatement(what, objects.toArray());
            s.setDefaultTimestamp(timestamp);
            s.setKeyspace(getKeyspace());
            s.setRoutingKey(key.getKey());

            BatchStatement bs = getBatchStatement(callback);
            bs.add(s);
        }
    }

    static class LoaderOptions {
        private static void errorMsg(String msg, CmdLineOptions options) {
            System.err.println(msg);
            printUsage(options);
            System.exit(1);
        }

        private static CmdLineOptions getCmdLineOptions() {
            CmdLineOptions options = new CmdLineOptions();
            options.addOption("v", VERBOSE_OPTION, "verbose output");
            options.addOption("sim", SIMULATE,
                    "simulate. Only print CQL generated");
            options.addOption("h", HELP_OPTION, "display this help message");
            options.addOption(null, NOPROGRESS_OPTION,
                    "don't display progress");
            options.addOption("i", IGNORE_NODES_OPTION, "NODES",
                    "don't stream to this (comma separated) list of nodes");
            options.addOption("d", INITIAL_HOST_ADDRESS_OPTION, "initial hosts",
                    "Required. try to connect to these hosts (comma separated) initially for ring information");
            options.addOption("p", PORT_OPTION, "port",
                    "port used for connections (default 9042)");
            options.addOption("t", THROTTLE_MBITS, "throttle",
                    "throttle speed in Mbits (default unlimited)");
            options.addOption("u", USER_OPTION, "username",
                    "username for cassandra authentication");
            options.addOption("pw", PASSWD_OPTION, "password",
                    "password for cassandra authentication");
            options.addOption("cph", CONNECTIONS_PER_HOST, "connectionsPerHost",
                    "number of concurrent connections-per-host.");
            // ssl connection-related options
            options.addOption("s", SSL, "SSL", "Use SSL connection(s)");
            options.addOption("ts", SSL_TRUSTSTORE, "TRUSTSTORE",
                    "Client SSL: full path to truststore");
            options.addOption("tspw", SSL_TRUSTSTORE_PW, "TRUSTSTORE-PASSWORD",
                    "Client SSL: password of the truststore");
            options.addOption("ks", SSL_KEYSTORE, "KEYSTORE",
                    "Client SSL: full path to keystore");
            options.addOption("kspw", SSL_KEYSTORE_PW, "KEYSTORE-PASSWORD",
                    "Client SSL: password of the keystore");
            options.addOption("prtcl", SSL_PROTOCOL, "PROTOCOL",
                    "Client SSL: connections protocol to use (default: TLS)");
            options.addOption("alg", SSL_ALGORITHM, "ALGORITHM",
                    "Client SSL: algorithm (default: SunX509)");
            options.addOption("st", SSL_STORE_TYPE, "STORE-TYPE",
                    "Client SSL: type of store");
            options.addOption("ciphers", SSL_CIPHER_SUITES, "CIPHER-SUITES",
                    "Client SSL: comma-separated list of encryption suites to use");
            options.addOption("f", CONFIG_PATH, "path to config file",
                    "cassandra.yaml file path for streaming throughput and client/server SSL.");
            return options;
        }

        public static LoaderOptions parseArgs(String cmdArgs[]) {
            CommandLineParser parser = new GnuParser();
            CmdLineOptions options = getCmdLineOptions();
            try {
                CommandLine cmd = parser.parse(options, cmdArgs, false);

                if (cmd.hasOption(HELP_OPTION)) {
                    printUsage(options);
                    System.exit(0);
                }

                String[] args = cmd.getArgs();
                if (args.length == 0) {
                    System.err.println("Missing sstable directory argument");
                    printUsage(options);
                    System.exit(1);
                }

                if (args.length > 1) {
                    System.err.println("Too many arguments");
                    printUsage(options);
                    System.exit(1);
                }

                String dirname = args[0];
                File dir = new File(dirname);

                if (!dir.exists()) {
                    errorMsg("Unknown directory: " + dirname, options);
                }

                if (!dir.isDirectory()) {
                    errorMsg(dirname + " is not a directory", options);
                }

                LoaderOptions opts = new LoaderOptions(dir);

                opts.verbose = cmd.hasOption(VERBOSE_OPTION);
                opts.simulate = cmd.hasOption(SIMULATE);
                opts.noProgress = cmd.hasOption(NOPROGRESS_OPTION);

                if (cmd.hasOption(PORT_OPTION)) {
                    opts.port = Integer
                            .parseInt(cmd.getOptionValue(PORT_OPTION));
                }

                if (cmd.hasOption(USER_OPTION)) {
                    opts.user = cmd.getOptionValue(USER_OPTION);
                }

                if (cmd.hasOption(PASSWD_OPTION)) {
                    opts.passwd = cmd.getOptionValue(PASSWD_OPTION);
                }

                if (cmd.hasOption(INITIAL_HOST_ADDRESS_OPTION)) {
                    String[] nodes = cmd
                            .getOptionValue(INITIAL_HOST_ADDRESS_OPTION)
                            .split(",");
                    try {
                        for (String node : nodes) {
                            opts.hosts.add(InetAddress.getByName(node.trim()));
                        }
                    } catch (UnknownHostException e) {
                        errorMsg("Unknown host: " + e.getMessage(), options);
                    }

                } else {
                    System.err.println("Initial hosts must be specified (-d)");
                    printUsage(options);
                    System.exit(1);
                }

                if (cmd.hasOption(IGNORE_NODES_OPTION)) {
                    String[] nodes = cmd.getOptionValue(IGNORE_NODES_OPTION)
                            .split(",");
                    try {
                        for (String node : nodes) {
                            opts.ignores
                                    .add(InetAddress.getByName(node.trim()));
                        }
                    } catch (UnknownHostException e) {
                        errorMsg("Unknown host: " + e.getMessage(), options);
                    }
                }

                if (cmd.hasOption(CONNECTIONS_PER_HOST)) {
                    opts.connectionsPerHost = Integer
                            .parseInt(cmd.getOptionValue(CONNECTIONS_PER_HOST));
                }

                // try to load config file first, so that values can be
                // rewritten with other option values.
                // otherwise use default config.
                Config config;
                if (cmd.hasOption(CONFIG_PATH)) {
                    File configFile = new File(cmd.getOptionValue(CONFIG_PATH));
                    if (!configFile.exists()) {
                        errorMsg("Config file not found", options);
                    }
                    config = new YamlConfigurationLoader()
                            .loadConfig(configFile.toURI().toURL());
                } else {
                    config = new Config();
                }
                opts.port = config.native_transport_port;
                opts.throttle = config.stream_throughput_outbound_megabits_per_sec;
                opts.encOptions = config.client_encryption_options;

                if (cmd.hasOption(THROTTLE_MBITS)) {
                    opts.throttle = Integer
                            .parseInt(cmd.getOptionValue(THROTTLE_MBITS));
                }

                if (cmd.hasOption(SSL)) {
                    opts.ssl = true;
                }
                if (cmd.hasOption(SSL_TRUSTSTORE)) {
                    opts.encOptions.truststore = cmd
                            .getOptionValue(SSL_TRUSTSTORE);
                }

                if (cmd.hasOption(SSL_TRUSTSTORE_PW)) {
                    opts.encOptions.truststore_password = cmd
                            .getOptionValue(SSL_TRUSTSTORE_PW);
                }

                if (cmd.hasOption(SSL_KEYSTORE)) {
                    opts.encOptions.keystore = cmd.getOptionValue(SSL_KEYSTORE);
                    // if a keystore was provided, lets assume we'll need to use
                    // it
                    opts.encOptions.require_client_auth = true;
                }

                if (cmd.hasOption(SSL_KEYSTORE_PW)) {
                    opts.encOptions.keystore_password = cmd
                            .getOptionValue(SSL_KEYSTORE_PW);
                }

                if (cmd.hasOption(SSL_PROTOCOL)) {
                    opts.encOptions.protocol = cmd.getOptionValue(SSL_PROTOCOL);
                }

                if (cmd.hasOption(SSL_ALGORITHM)) {
                    opts.encOptions.algorithm = cmd
                            .getOptionValue(SSL_ALGORITHM);
                }

                if (cmd.hasOption(SSL_STORE_TYPE)) {
                    opts.encOptions.store_type = cmd
                            .getOptionValue(SSL_STORE_TYPE);
                }

                if (cmd.hasOption(SSL_CIPHER_SUITES)) {
                    opts.encOptions.cipher_suites = cmd
                            .getOptionValue(SSL_CIPHER_SUITES).split(",");
                }

                return opts;
            } catch (ParseException | ConfigurationException
                    | MalformedURLException e) {
                errorMsg(e.getMessage(), options);
                return null;
            }
        }

        public static void printUsage(Options options) {
            String usage = String.format("%s [options] <dir_path>", TOOL_NAME);
            String header = System.lineSeparator()
                    + "Bulk load the sstables found in the directory <dir_path> to the configured cluster."
                    + "The parent directories of <dir_path> are used as the target keyspace/table name. "
                    + "So for instance, to load an sstable named Standard1-g-1-Data.db into Keyspace1/Standard1, "
                    + "you will need to have the files Standard1-g-1-Data.db and Standard1-g-1-Index.db into a directory /path/to/Keyspace1/Standard1/.";
            String footer = System.lineSeparator()
                    + "You can provide cassandra.yaml file with -f command line option to set up streaming throughput, client and server encryption options. "
                    + "Only stream_throughput_outbound_megabits_per_sec, server_encryption_options and client_encryption_options are read from yaml. "
                    + "You can override options read from cassandra.yaml with corresponding command line options.";
            new HelpFormatter().printHelp(usage, header, options, footer);
        }

        public final File directory;
        public boolean ssl;
        public boolean debug;
        public boolean verbose;
        public boolean simulate;
        public boolean noProgress;
        public int port = 9042;
        public String user;

        public String passwd;
        public int throttle = 0;

        public EncryptionOptions encOptions = new EncryptionOptions.ClientEncryptionOptions();

        public int connectionsPerHost = 1;

        public final Set<InetAddress> hosts = new HashSet<>();

        public final Set<InetAddress> ignores = new HashSet<>();

        LoaderOptions(File directory) {
            this.directory = directory;
        }
    }

    private static final String TOOL_NAME = "sstableloader";
    private static final String SIMULATE = "simulate";
    private static final String VERBOSE_OPTION = "verbose";
    private static final String HELP_OPTION = "help";
    private static final String NOPROGRESS_OPTION = "no-progress";
    private static final String IGNORE_NODES_OPTION = "ignore";
    private static final String INITIAL_HOST_ADDRESS_OPTION = "nodes";
    private static final String PORT_OPTION = "port";

    private static final String USER_OPTION = "username";
    private static final String PASSWD_OPTION = "password";
    private static final String THROTTLE_MBITS = "throttle";
    /* client encryption options */
    private static final String SSL = "ssl";
    private static final String SSL_TRUSTSTORE = "truststore";
    private static final String SSL_TRUSTSTORE_PW = "truststore-password";
    private static final String SSL_KEYSTORE = "keystore";
    private static final String SSL_KEYSTORE_PW = "keystore-password";
    private static final String SSL_PROTOCOL = "ssl-protocol";
    private static final String SSL_ALGORITHM = "ssl-alg";
    private static final String SSL_STORE_TYPE = "store-type";

    private static final String SSL_CIPHER_SUITES = "ssl-ciphers";

    private static final String CONNECTIONS_PER_HOST = "connections-per-host";

    private static final String CONFIG_PATH = "conf-path";

    public static void main(String args[]) {
        Config.setClientMode(true);
        LoaderOptions options = LoaderOptions.parseArgs(args);

        try {
            String keyspace = options.directory.getParentFile().getName();

            CQLClient client = new CQLClient(options, keyspace);
            SSTableToCQL ssTableToCQL = new SSTableToCQL(keyspace, client);
            ssTableToCQL.stream(options.directory);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}