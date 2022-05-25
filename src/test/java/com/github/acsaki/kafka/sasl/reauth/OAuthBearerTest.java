package com.github.acsaki.kafka.sasl.reauth;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerLoginCallbackHandler;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.time.Duration.ofSeconds;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OAuthBearerTest {

    private Logger logger = LoggerFactory.getLogger(OAuthBearerTest.class);

    private String topic = "test-topic";
    private String tokenEndpoint = AuthServerKt.tokenEndpoint;
    private String message = "Hello OAuth! ";

    @Test
    void produceOneRecord() throws ExecutionException, InterruptedException, IOException {
        try (var producer = new KafkaProducer<String, String>(buildConfig(clientSecret()))) {
            while (true) {
                sendAndSleepSync(producer, message());
            }
        }
    }

//    @Test
    void testCredentialChange() throws ExecutionException, InterruptedException, IOException {
        String initialSecret = clientSecret();
        try (var producer = new KafkaProducer<String, String>(buildConfig(initialSecret))) {
            sendAndSleepSync(producer, message());
            changeSecret(initialSecret + "2");
            logger.info("client secret changed is now: {}", clientSecret());
            while (true) {
                sendAndSleepSync(producer, message());
            }
        }
    }

//    @Test
    void consume() throws IOException, InterruptedException {
        try (var consumer = new KafkaConsumer<String, String>(buildConfig(clientSecret()))) {
            consumer.subscribe(Collections.singletonList(topic));
            while (true) {
                var records = consumer.poll(ofSeconds(3)).iterator();
                if (!records.hasNext()) {
                    logger.info("no messages");
                }
                records.forEachRemaining(record -> {
                    String value = record.value();
                    logger.info("got message: {} offset {}", value, record.offset());
                    consumer.commitSync();
                });
                sleep(1000);
            }
        }
    }

    private String message() {
        return message + LocalTime.now();
    }

    private void sendAndSleepSync(KafkaProducer<String, String> producer, String message) throws InterruptedException, ExecutionException {
        producer
                .send(
                        new ProducerRecord<>(topic, message),
                        (metadata, exception) -> {
                            if (exception != null) {
                                logger.error("exception thrown: {}", exception.getMessage());
                            } else {
                                logger.info("produced message: {} got: {}", message, metadata);
                            }
                        }
                )
                .get();
        sleep(1500);
    }

    private String quote(String s) {
        return "\"" + s + "\"";
    }

    private Properties buildConfig(String clientSecret) {
        Properties p = new Properties();
        p.put("security.protocol", "SASL_PLAINTEXT");
        p.put("sasl.mechanism", "OAUTHBEARER");
        p.put("sasl.login.callback.handler.class", OAuthBearerLoginCallbackHandler.class.getName());
        p.put("sasl.jaas.config",
                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required " +
                        "clientId=" + quote(SingleClientRepository.clientId) + " " +
                        "clientSecret=" + quote(clientSecret) + " " +
                        "scope=\"kafka\";"
        );
        p.put("sasl.oauthbearer.token.endpoint.url", endpointURL(tokenEndpoint));
        p.put("sasl.login.refresh.buffer.seconds", "1");
        p.put("sasl.login.refresh.min.period.seconds", "10");

        p.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        var serializer = StringSerializer.class.getName();
        p.put(KEY_SERIALIZER_CLASS_CONFIG, serializer);
        p.put(VALUE_SERIALIZER_CLASS_CONFIG, serializer);
        var deserializer = StringDeserializer.class.getName();
        p.put(KEY_DESERIALIZER_CLASS_CONFIG, deserializer);
        p.put(VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
        p.put(GROUP_ID_CONFIG, "group1");
        p.put(ENABLE_AUTO_COMMIT_CONFIG, "true");
        p.put(AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        p.put(SESSION_TIMEOUT_MS_CONFIG, "30000");
        p.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ACKS_CONFIG, "all");
        return p;
    }

    private String endpointURL(String path) {
        return "http://localhost:8080" + path;
    }

    private String clientSecret() throws IOException, InterruptedException {
        String endpointURL = endpointURL("/client/" + SingleClientRepository.clientId + "/clientSecret");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointURL))
                .build();

        HttpResponse<String> response =
                client.send(request, BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        logger.info("client secret:" + response.body());
        return response.body();
    }

    private void changeSecret(String newSecret) throws IOException, InterruptedException {
        String endpointURL = endpointURL("/client/" + SingleClientRepository.clientId + "/clientSecret");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointURL)).POST(ofString(newSecret))
                .build();
        HttpResponse<?> response = client.send(request, BodyHandlers.discarding());
        assertEquals(200, response.statusCode());
    }

}
