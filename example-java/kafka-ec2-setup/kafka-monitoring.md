# To monitor incoming topics and messages in your Kafka

List All Topics

```
cd ~/kafka/kafka_2.13-3.4.0
bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

Describe a Specific Topic

```
bin/kafka-topics.sh --describe --topic test-topic --bootstrap-server localhost:9092
```

Console Consumer (to see messages in real-time)

```
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --from-beginning
```

Consumer Groups List

```
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

Consumer Group Details (e.g., lag)

```
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group consumer-group
```

Check Topic Offsets

```
bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic test-topic
```

Topic Metrics with JMX

```
bin/kafka-run-class.sh kafka.tools.JmxTool --jmx-url service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi --object-name kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec
```

(Note: This requires JMX to be configured in your server.properties)

Monitor Log Segments

```
ls -la /tmp/kafka-logs/test-topic-\*
```

(Adjust the path if you changed the log.dirs in server.properties)

Simple Performance Testing

```
bin/kafka-producer-perf-test.sh --topic test-topic --num-records 100000 --record-size 1000 --throughput 10000 --producer-props bootstrap.servers=localhost:9092
```
