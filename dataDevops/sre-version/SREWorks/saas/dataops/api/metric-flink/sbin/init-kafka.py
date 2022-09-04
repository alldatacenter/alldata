import os
from confluent_kafka.admin import AdminClient, NewTopic

a = AdminClient({'bootstrap.servers': os.environ.get('KAFKA_URL')})

new_topics = [NewTopic(topic, num_partitions=3, replication_factor=1) for topic in ["sreworks-dataops-metric-data", "sreworks-telemetry-metric", "sreworks-pmdb-parsed-metric", "sreworks-health-incident"]]
# Note: In a multi-cluster production scenario, it is more typical to use a replication_factor of 3 for durability.

# Call create_topics to asynchronously create topics. A dict
# of <topic,future> is returned.
fs = a.create_topics(new_topics)

# Wait for each operation to finish.
for topic, f in fs.items():
    try:
        f.result()  # The result itself is None
        print("Topic {} created".format(topic))
    except Exception as e:
        print("Failed to create topic {}: {}".format(topic, e))