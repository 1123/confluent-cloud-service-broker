package io.confluent.examples.pcf.servicebroker;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

@Slf4j
public class KafkaJunitExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) throws IOException, InterruptedException {
        if (!started) {
            started = true;
            context.getRoot().getStore(GLOBAL).put("kafka", this);
            startUpZookeeper();
            startUpKafka();
        }
    }

    @Override
    public void close() {
        shutDownKafka();
        shutDownZookeeper();
    }

    private static ZooKeeperServer zooKeeperServer;
    private static KafkaServerStartable broker;

    private static File initializeFolder(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            FileUtils.cleanDirectory(file);
        } else {
            file.mkdir();
        }

        return file;
    }

    private static void startUpZookeeper() throws IOException, InterruptedException {
        log.info("Starting zookeeper");
        File zkSnapshostsDir = initializeFolder("/tmp/zk-snapshots");
        File zkLogsDir = initializeFolder("/tmp/zk-logs");

        zooKeeperServer = new ZooKeeperServer(
                zkSnapshostsDir,
                zkLogsDir,
                2000);
        ServerCnxnFactory factory = NIOServerCnxnFactory.createFactory();
        factory.configure(new InetSocketAddress("localhost", 2181), 100);
        factory.startup(zooKeeperServer);
    }

    private static void startUpKafka() {
        log.info("Starting Kafka");
        final Properties kafkaProperties = new Properties();
        kafkaProperties.setProperty("listener.security.protocol.map", "SASL_PLAINTEXT:SASL_PLAINTEXT");
        kafkaProperties.setProperty("listeners", "SASL_PLAINTEXT://localhost:10091");
        kafkaProperties.setProperty("advertised.listeners", "SASL_PLAINTEXT://localhost:10091");
        kafkaProperties.setProperty("inter.broker.listener.name", "SASL_PLAINTEXT");
        kafkaProperties.setProperty("authorizer.class.name", "kafka.security.auth.SimpleAclAuthorizer");
        kafkaProperties.setProperty("super.users", "User:client;User:admin;User:broker;User:connect;User:servicebroker");
        kafkaProperties.setProperty("sasl.enabled.mechanisms", "PLAIN");
        kafkaProperties.setProperty("sasl.mechanism.inter.broker.protocol", "PLAIN");
        kafkaProperties.setProperty("listener.name.sasl_plaintext.plain.sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                        "    username=\"admin\" " +
                        "    password=\"admin-secret\" " +
                        "    user_broker=\"broker-secret\" " +
                        "    user_client=\"client-secret\" " +
                        "    user_servicebroker=\"servicebroker-secret\" " +
                        "    user_admin=\"admin-secret\"; ");
        kafkaProperties.setProperty("zookeeper.connect", "localhost:2181");
        kafkaProperties.setProperty("broker.id", "1");
        kafkaProperties.setProperty("log.dir", "/tmp/kafka/logs");
        kafkaProperties.setProperty("log.flush.interval.messages", "1");
        kafkaProperties.setProperty("delete.topic.enable", "true");
        kafkaProperties.setProperty("offsets.topic.replication.factor", "1");
        kafkaProperties.setProperty("auto.create.topics.enable", "false");
        kafkaProperties.setProperty("zookeeper.connection.timeout.ms", "30000");
        broker = new KafkaServerStartable(new KafkaConfig(kafkaProperties));
        broker.startup();
    }


    private static void shutDownKafka() {
        broker.shutdown();
    }

    private static void shutDownZookeeper() {
        zooKeeperServer.shutdown();
    }
}