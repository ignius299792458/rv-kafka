# Kafka Data Model

This guide explores Kafka's data model, including `message structure`, `log compaction`, and `retention policies`, with practical examples in a banking system context.

## `1. Message Structure`

A Kafka **`message`** (or **`record`**) is the unit of data within Kafka. Each message consists of:

### 1.1 Key-Value Structure

- **Key**: Optional identifier that determines the partition assignment
- **Value**: The actual payload data
- **Timestamp**: When the message was created or added to the broker
- **Headers**: Optional metadata as key-value pairs
- **Offset**: Auto-incrementing identifier within a partition

**Banking Example:**

```json
{
  "key": "customer-47291",
  "value": {
    "transactionId": "tx-938217",
    "accountId": "acct-72819",
    "amount": 500.0,
    "currency": "USD",
    "type": "DEPOSIT",
    "timestamp": "2025-05-18T10:15:30Z",
    "description": "ATM Deposit"
  },
  "timestamp": 1716112530000,
  "headers": {
    "source-system": "mobile-app",
    "region": "north-america",
    "version": "1.2"
  },
  "offset": 8291,
  "partition": 7,
  "topic": "account-transactions"
}
```

### 1.2 Serialization

Messages in Kafka are stored and transmitted as byte arrays. Serialization is the process of converting data objects into byte arrays. Common formats include:

- JSON
- Avro
- Protobuf
- Plain text

**Banking Example:**

```java
// Example of producing a transaction using Avro serialization
public void sendTransaction(Transaction transaction) {
    ProducerRecord<String, GenericRecord> record = new ProducerRecord<>(
        "account-transactions",
        transaction.getAccountId(),
        convertToAvro(transaction)  // Convert to Avro format
    );
    producer.send(record);
}

private GenericRecord convertToAvro(Transaction transaction) {
    GenericRecord avroRecord = new GenericData.Record(transactionSchema);
    avroRecord.put("transactionId", transaction.getId());
    avroRecord.put("accountId", transaction.getAccountId());
    avroRecord.put("amount", transaction.getAmount());
    avroRecord.put("currency", transaction.getCurrency());
    avroRecord.put("type", transaction.getType().toString());
    avroRecord.put("timestamp", transaction.getTimestamp());
    avroRecord.put("description", transaction.getDescription());
    return avroRecord;
}
```

### 1.3 Schema Registry

For strongly typed message formats (like Avro, Protobuf), a Schema Registry is often used alongside Kafka to:

- Store and retrieve schemas
- Ensure compatibility between producer and consumer schemas
- Enable schema evolution over time

**Banking Example:**

In a banking system, schemas will evolve as new features are added:

```
// Version 1: Initial transaction schema
{
  "type": "record",
  "name": "Transaction",
  "fields": [
    {"name": "transactionId", "type": "string"},
    {"name": "accountId", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "currency", "type": "string"},
    {"name": "type", "type": "string"},
    {"name": "timestamp", "type": "long"},
    {"name": "description", "type": "string"}
  ]
}

// Version 2: Added location field
{
  "type": "record",
  "name": "Transaction",
  "fields": [
    {"name": "transactionId", "type": "string"},
    {"name": "accountId", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "currency", "type": "string"},
    {"name": "type", "type": "string"},
    {"name": "timestamp", "type": "long"},
    {"name": "description", "type": "string"},
    {"name": "location", "type": ["null", "string"], "default": null}
  ]
}
```

The Schema Registry ensures that old consumers can still read new messages and new consumers can read old messages during this transition.

## 2. Log Structure

Kafka topics are structured as **logs**, which are append-only, immutable sequences of records.

### 2.1 Partition Log

Each partition maintains its own log:

```
Partition 0: [Message 0] → [Message 1] → [Message 2] → ... → [Message N]
```

**Banking Example:**

For the `account-transactions` topic with multiple partitions:

```
Partition 0: [TX_A1] → [TX_A2] → [TX_A3] → ... → [TX_A100]
Partition 1: [TX_B1] → [TX_B2] → [TX_B3] → ... → [TX_B95]
Partition 2: [TX_C1] → [TX_C2] → [TX_C3] → ... → [TX_C87]
...
```

Each transaction record is appended to the appropriate partition log based on its key (account ID hash).

### 2.2 Segment Files

Behind the scenes, Kafka stores each partition log as a series of segment files:

- Active segment: The current file being written to
- Closed segments: Older files no longer being written to

**Banking Example:**

For a busy bank processing millions of transactions daily:

```
Partition 0:
  - 00000000000000000000.log (closed)
  - 00000000000001000000.log (closed)
  - 00000000000002000000.log (active)
```

These segments store transaction records efficiently and allow Kafka to handle log retention and cleanup operations.

## 3. Log Compaction

**Log compaction** is a feature that ensures Kafka retains at least the last known value for each message key within the partitioned log.

### 3.1 How Log Compaction Works

1. For each key in a log, keep at least the most recent value
2. Older messages with the same key can be discarded during compaction
3. The log head (recent messages) remains untouched
4. Compaction runs in the background periodically

**Banking Example:**

Consider an `account-balances` topic that stores the current balance for each account:

```
Before compaction:
[acct-001: $100] → [acct-002: $250] → [acct-001: $150] → [acct-003: $500] → [acct-002: $300]

After compaction:
[acct-001: $150] → [acct-003: $500] → [acct-002: $300]
```

The older balance records for `acct-001` ($100) and `acct-002` ($250) are removed during compaction because newer values exist.

### 3.2 Use Cases for Log Compaction

- **Latest state storage**: Maintaining current account balances
- **Configuration management**: Storing the latest system configuration
- **Cache rebuilding**: Providing data to rebuild application caches

**Banking Example:**

```java
// Configure a compacted topic for account balances
Map<String, String> topicConfig = new HashMap<>();
topicConfig.put("cleanup.policy", "compact");
topicConfig.put("min.compaction.lag.ms", "86400000"); // 24 hours
topicConfig.put("delete.retention.ms", "86400000");   // 24 hours

adminClient.createTopics(Collections.singleton(
    new NewTopic("account-balances", 10, (short) 3)
        .configs(topicConfig)
));
```

## 4. Retention Policies

Kafka provides configurable **retention policies** that determine how long messages are kept in topics.

### 4.1 Time-Based Retention

Messages are kept for a specified time period, regardless of whether they've been consumed.

**Banking Example:**

```
# Keep transaction data for 7 days
transactions.retention.ms=604800000

# Keep customer login events for 30 days
customer-logins.retention.ms=2592000000

# Keep fraud alerts for 90 days
fraud-alerts.retention.ms=7776000000
```

### 4.2 Size-Based Retention

Messages are kept until the log reaches a specified size.

**Banking Example:**

```
# Limit transaction topic to 50GB total
transactions.retention.bytes=53687091200

# Limit each partition to 5GB
transactions.segment.bytes=5368709120
```

### 4.3 Combined Policies

Kafka can apply both time and size policies—data is removed when either limit is reached.

**Banking Example:**

For regulatory compliance, a bank might configure:

```
# Store transaction records for either 7 years or until 1TB space limit is reached
compliance-transactions.retention.ms=220752000000  # 7 years
compliance-transactions.retention.bytes=1099511627776  # 1TB
```

### 4.4 Infinite Retention

Set `retention.ms` to `-1` or `retention.bytes` to `-1` for unlimited retention.

**Banking Example:**

```
# Critical audit records must be kept forever
critical-audit-events.retention.ms=-1
```

## 5. Banking System Implementation Examples

### 5.1 Topic Design with Appropriate Data Models

| Topic                 | Key           | Value Format | Retention | Compaction | Purpose                         |
| --------------------- | ------------- | ------------ | --------- | ---------- | ------------------------------- |
| account-transactions  | accountId     | Avro         | 7 years   | No         | Complete transaction history    |
| account-balances      | accountId     | Avro         | Infinite  | Yes        | Current balance of each account |
| customer-sessions     | sessionId     | JSON         | 24 hours  | No         | User session tracking           |
| fraud-alerts          | transactionId | Avro         | 90 days   | No         | Fraud detection alerts          |
| system-configurations | configKey     | JSON         | Infinite  | Yes        | System configuration            |

### 5.2 Message Structure Example for a Fund Transfer

```json
// Producer message for fund transfer
{
  "key": "acct-72819", // Source account ID as key for partitioning
  "value": {
    "transactionId": "tx-938217",
    "sourceAccountId": "acct-72819",
    "destinationAccountId": "acct-95132",
    "amount": 1000.0,
    "currency": "USD",
    "type": "TRANSFER_OUT",
    "timestamp": "2025-05-18T10:15:30Z",
    "status": "INITIATED",
    "reference": "Payment for invoice #12345",
    "channel": "MOBILE_APP",
    "fees": 0.0
  }
}
```

### 5.3 Log Compaction for Account Balances

```java
// AccountBalanceUpdater service
public class AccountBalanceUpdater {
    private final KafkaProducer<String, AccountBalance> producer;
    private final String balanceTopic = "account-balances";

    public void updateBalance(String accountId, double newBalance, String currency) {
        AccountBalance balance = new AccountBalance(accountId, newBalance, currency, System.currentTimeMillis());

        ProducerRecord<String, AccountBalance> record = new ProducerRecord<>(
            balanceTopic,
            accountId,  // Key (for log compaction)
            balance     // Value
        );

        producer.send(record);
    }
}

// Consumer reconstructing current account balances
public Map<String, AccountBalance> rebuildAccountBalanceCache() {
    Map<String, AccountBalance> balances = new HashMap<>();

    consumer.subscribe(Collections.singletonList("account-balances"));
    ConsumerRecords<String, AccountBalance> records = consumer.poll(Duration.ofSeconds(10));

    for (ConsumerRecord<String, AccountBalance> record : records) {
        String accountId = record.key();
        AccountBalance balance = record.value();
        balances.put(accountId, balance);
    }

    return balances;
}
```

### 5.4 Retention Policy Configuration for Banking Compliance

```java
// Define retention policies for different topics
public void configureRetentionPolicies() {
    Map<String, ConfigEntry> transactionConfig = new HashMap<>();
    // Keep transactions for 7 years (regulatory requirement)
    transactionConfig.put("retention.ms", new ConfigEntry("retention.ms", "220752000000"));

    Map<String, ConfigEntry> sessionConfig = new HashMap<>();
    // Keep session data for 24 hours only
    sessionConfig.put("retention.ms", new ConfigEntry("retention.ms", "86400000"));

    Map<String, ConfigEntry> balanceConfig = new HashMap<>();
    // Compact the balances topic
    balanceConfig.put("cleanup.policy", new ConfigEntry("cleanup.policy", "compact"));

    // Apply configurations
    adminClient.alterConfigs(Map.of(
        new ConfigResource(ConfigResource.Type.TOPIC, "account-transactions"),
        new Config(new ArrayList<>(transactionConfig.values())),

        new ConfigResource(ConfigResource.Type.TOPIC, "customer-sessions"),
        new Config(new ArrayList<>(sessionConfig.values())),

        new ConfigResource(ConfigResource.Type.TOPIC, "account-balances"),
        new Config(new ArrayList<>(balanceConfig.values()))
    ));
}
```

## 6. Conclusion

Kafka's data model provides powerful capabilities for banking systems:

1. **Structured messages** allow for clear communication between microservices
2. **Schema evolution** supports system growth while maintaining backward compatibility
3. **Log compaction** efficiently maintains the latest state for key-based data
4. **Flexible retention policies** support both operational needs and regulatory compliance

These features enable banks to build resilient, scalable systems that can process millions of financial transactions while maintaining data integrity and meeting regulatory requirements.
