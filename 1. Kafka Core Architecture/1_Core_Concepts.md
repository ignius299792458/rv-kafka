# Kafka Core Concepts

Apache Kafka is a `distributed streaming platform` that has become essential for `building real-time data pipelines and streaming applications`. This guide will explain the fundamental Kafka concepts and demonstrate their application in a banking system.

## 1. Core Concepts

### 1.1 Topics

A **`topic`** is a category or feed name to which records are published. In Kafka, topics are multi-subscriber—they can have zero, one, or many consumers that subscribe to the data written to them.

Topics in Kafka are always multi-subscriber; that is, a topic can have zero, one, or many consumers that subscribe to the data written to it.

**Banking Example:**
In a banking system, you might have topics like:

- `account-transactions` - All financial transactions
- `fraud-alerts` - Potential fraudulent activities
- `customer-logins` - Authentication events
- `fund-transfers` - Money transfer events
- `account-balance-updates` - Changes to account balances

### 1.2 Partitions

Each topic is divided into **partitions**, which are the units of parallelism in Kafka. Each partition is an ordered, immutable sequence of records that is continually appended to. The records in the partitions are each assigned a sequential ID number called the **offset**.

**Banking Example:**
The `account-transactions` topic might be divided into multiple partitions:

- You could partition by account ID hash to ensure all transactions for a specific account go to the same partition
- For a large bank, you might have hundreds of partitions to handle the high volume of transactions
- Example configuration:
  ```
  account-transactions: 20 partitions
  fraud-alerts: 10 partitions
  customer-logins: 15 partitions
  ```

### 1.3 Offsets

An **offset** is a sequential ID number given to messages within a partition. It uniquely identifies each record within the partition.

**Banking Example:**

- A transaction with offset 1001 in partition 3 of the `account-transactions` topic represents a specific transaction
- Consumer services track which messages they've processed by storing the last offset they've consumed
- If a payment processing service crashes, it can resume from its last saved offset, ensuring no transaction is lost

### 1.4 Producers

**Producers** publish data to topics of their choice. The producer is responsible for choosing which record to assign to which partition within the topic.

**Banking Example:**

- `TransactionService` produces messages to the `account-transactions` topic whenever a customer performs a financial operation
- `AuthenticationService` produces messages to the `customer-logins` topic whenever a customer logs in
- `FraudDetectionService` produces messages to the `fraud-alerts` topic when suspicious activities are detected

```java
// Example producer for a banking transaction
public class TransactionProducer {
    public void recordTransaction(Transaction transaction) {
        ProducerRecord<String, Transaction> record = new ProducerRecord<>(
            "account-transactions",
            transaction.getAccountId(),  // Key (for partitioning)
            transaction                  // Value
        );
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                logger.info("Transaction recorded: offset = " + metadata.offset());
            } else {
                logger.error("Failed to record transaction", exception);
            }
        });
    }
}
```

### 1.5 Consumers

**Consumers** read data from topics of their choice. Consumers label themselves with a consumer group name, and each record published to a topic is delivered to one consumer instance within each subscribing consumer group.

**Banking Example:**

- `AccountBalanceService` consumer group processes transactions to update account balances
- `NotificationService` consumer group processes transactions to send alerts to customers
- `AuditService` consumer group processes all transactions for compliance and audit purposes

```java
// Example consumer for processing transactions
public class AccountBalanceConsumer {
    public void startProcessing() {
        consumer.subscribe(Arrays.asList("account-transactions"));

        while (true) {
            ConsumerRecords<String, Transaction> records = consumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, Transaction> record : records) {
                String accountId = record.key();
                Transaction transaction = record.value();
                updateAccountBalance(accountId, transaction);

                // Commit offset after processing
                consumer.commitSync();
            }
        }
    }
}
```

### 1.6 Consumer Groups

A **consumer group** is a set of consumers that cooperate to consume data from some topics. Partitions in topics are divided among the consumers in a group, so each partition is consumed by exactly one consumer in the group.

**Banking Example:**

- Multiple instances of the `AccountBalanceService` form a consumer group, each handling different partitions of the `account-transactions` topic
- If a new instance of `AccountBalanceService` is added, Kafka automatically rebalances the partitions
- If an instance fails, its partitions are reassigned to other instances in the group

### 1.7 Brokers

A Kafka **broker** is a server that runs the Kafka software. A Kafka cluster consists of one or more brokers.

**Banking Example:**

- A large bank might run a cluster of 5-10 brokers to handle the volume of transactions
- Each broker might be deployed in a different data center for high availability
- The brokers collectively manage the persistence and replication of message data

### 1.8 Clusters

A Kafka **cluster** is a group of Kafka brokers working together. One broker in the cluster acts as the controller, which is responsible for administrative operations, including assigning partitions to brokers and monitoring for broker failures.

**Banking Example:**

- A global bank might have multiple clusters:
  - North America cluster (5 brokers)
  - Europe cluster (4 brokers)
  - Asia Pacific cluster (4 brokers)
- Data can be mirrored between clusters using Kafka's MirrorMaker tool
- This setup provides regional isolation and disaster recovery capabilities

## 2. Putting It All Together: Banking System Architecture

Let's see how these concepts come together in a banking system:

### Core Banking Microservices:

1. **Account Service**: Manages account creation, updates, and queries
2. **Transaction Service**: Processes financial transactions
3. **Authentication Service**: Handles user login and security
4. **Notification Service**: Sends alerts to customers
5. **Fraud Detection Service**: Monitors for suspicious activities
6. **Reporting Service**: Generates financial reports
7. **Compliance Service**: Ensures regulatory compliance

### Kafka Integration:

```
                  │
     ┌────────────▼─────────────┐
     │      Kafka Cluster       │
     │   (Multiple Brokers)     │
     └─────────────┬────────────┘
                   │
                   ▼
┌─────────────────────────────────────┐
│               Topics                │
├─────────────────────────────────────┤
│ account-transactions (20 partitions)│
│ fraud-alerts (10 partitions)        │
│ customer-logins (15 partitions)     │
│ fund-transfers (15 partitions)      │
│ account-balance-updates (10 part.)  │
└──────────────┬──────────────────────┘
               │
     ┌─────────▼──────────┐
     │                    │
┌────▼───────┐      ┌─────▼──────┐
│ Producers  │      │ Consumers  │
└────────────┘      └────────────┘
```

### Event Flow Example:

1. A customer initiates a fund transfer in the web application
2. The `TransactionService` (Producer) publishes a message to the `fund-transfers` topic
3. Multiple consumer services process this event:
   - `AccountBalanceService` updates account balances
   - `NotificationService` sends an SMS to the customer
   - `FraudDetectionService` analyzes the transaction for risk
   - `AuditService` logs the transaction for compliance

### Scaling Example:

As transaction volume grows:

1. Add more partitions to the `account-transactions` topic
2. Deploy additional consumer instances in the `AccountBalanceService` consumer group
3. Kafka automatically redistributes the partitions among the consumers
4. Processing capacity scales linearly with the number of consumers (up to the number of partitions)

### Fault Tolerance Example:

If a server running an `AccountBalanceService` instance fails:

1. Kafka detects the consumer failure
2. The partitions assigned to that consumer are reassigned to other consumers in the group
3. Processing continues without data loss
4. When the failed server is restored, it rejoins the consumer group and gets assigned partitions

This architecture provides a robust foundation for a high-throughput, fault-tolerant banking system that can process millions of transactions reliably.
