# Set up Apache Kafka on an Ubuntu EC2 instance and configuration

## 1. Launch and Configure EC2 Instance

First, make sure your EC2 instance is running Ubuntu

Has sufficient resources (recommend at least t2.medium with 4GB RAM)

Has security-groups configured to allow:

```sh
SSH (port 22)

Kafka broker port (9092)

Zookeeper port (2181)
```

## 2. Install Java

SSH into your EC2 instance and install Java:

```sh
sudo apt update
sudo apt install -y openjdk-11-jdk
java -version # Verify installation
```

## 3. Install Apache Kafka

Download and set up Kafka:

```sh
mkdir -p ~/kafka
cd ~/kafka

#Download Kafka (this is for version 3.4.0, check for latest version)

wget https://archive.apache.org/dist/kafka/3.4.0/kafka_2.13-3.4.0.tgz
tar -xzf kafka_2.13-3.4.0.tgz
cd kafka_2.13-3.4.0
```

## 4. Configure Kafka

Edit the Kafka server properties:

```sh
# either use vim
nano config/server.properties
```

Make these important changes:

Set advertised.listeners to your EC2's public IP or DNS:

```sh
advertised.listeners=PLAINTEXT://YOUR_EC2_PUBLIC_IP:9092
```

Increase log retention if needed:

```
log.retention.hours=168
```

## 5. Create Systemd Services

**`For Zookeeper:`**

```sh
sudo nano /etc/systemd/system/zookeeper.service
```

Add:

```sh

[Unit]
Description=Apache Zookeeper Server
After=network.target

[Service]
Type=simple
User=ubuntu
ExecStart=/home/ubuntu/kafka/kafka_2.13-3.4.0/bin/zookeeper-server-start.sh /home/ubuntu/kafka/kafka_2.13-3.4.0/config/zookeeper.properties
ExecStop=/home/ubuntu/kafka/kafka_2.13-3.4.0/bin/zookeeper-server-stop.sh
Restart=on-failure

[Install]
WantedBy=multi-user.target

```

**`For Kafka:`**

```sh
sudo nano /etc/systemd/system/kafka.service
```

Add:

```sh
[Unit]
Description=Apache Kafka Server
After=zookeeper.service

[Service]
Type=simple
User=ubuntu
ExecStart=/home/ubuntu/kafka/kafka_2.13-3.4.0/bin/kafka-server-start.sh /home/ubuntu/kafka/kafka_2.13-3.4.0/config/server.properties
ExecStop=/home/ubuntu/kafka/kafka_2.13-3.4.0/bin/kafka-server-stop.sh
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

## 6. Start Services

```sh
sudo systemctl daemon-reload
sudo systemctl start zookeeper
sudo systemctl start kafka
sudo systemctl enable zookeeper
sudo systemctl enable kafka
```

## 7. Create a Test Topic

```
cd ~/kafka/kafka_2.13-3.4.0
bin/kafka-topics.sh --create --topic test-topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

```

## 9. Verify and Monitor

Check if services are running:

```sh
sudo systemctl status zookeeper
sudo systemctl status kafka
```

List topics:

```sh
bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```
