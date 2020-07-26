package io.github.nguyenbantran.stream;


import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.Materialized;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class BasicApp {

    static String INPUT_TOPIC = "input";
    static String OUTPUT_TOPIC = "output";
    static final String APP_ID = "testapp";

    public static void main(String[] args) throws InterruptedException {
        Properties config = new Properties();
        config.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, BasicApp.APP_ID);
        config.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "foo:1234");
        config.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        config.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        Topology topology = retainWordsLongerThan5Letters();

        KafkaStreams ks = new KafkaStreams(topology, config);
        ks.start();

        new CountDownLatch(1).await();
    }

    static Topology retainWordsLongerThan5Letters() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> stream = builder.stream(INPUT_TOPIC);
        stream.filter((k, v)
                -> v.length() > 5)
                .to(OUTPUT_TOPIC);

        return builder.build();
    }

    static Topology flatMap() {
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> stream = builder.stream(INPUT_TOPIC);

        stream.flatMap(new KeyValueMapper<String, String, Iterable<? extends KeyValue<? extends String, ? extends String>>>() {
            @Override
            public Iterable<? extends KeyValue<? extends String, ? extends String>> apply(String k, String csv) {
                String[] values = csv.split(",");
                return Arrays.asList(values)
                        .stream()
                        .map(value -> new KeyValue<>(k, value))
                        .collect(Collectors.toList());
            }
        }).to(OUTPUT_TOPIC);

        return builder.build();
    }

    static Topology count() {

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> stream = builder.stream(INPUT_TOPIC);

        stream.groupByKey()
                .count()
                .toStream()
                .to(OUTPUT_TOPIC);


        return builder.build();
    }

    static Topology countWithStateStore() {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> stream = builder.stream(INPUT_TOPIC);

        stream.groupByKey()
                .count(Materialized.as("count-store"));

        return builder.build();
    }


}