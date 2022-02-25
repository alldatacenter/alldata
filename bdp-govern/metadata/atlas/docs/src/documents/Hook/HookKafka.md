---
name: Kafka
route: /HookKafka
menu: Documentation
submenu: Hooks
---

import  themen  from 'theme/styles/styled-colors';
import  * as theme  from 'react-syntax-highlighter/dist/esm/styles/hljs';
import SyntaxHighlighter from 'react-syntax-highlighter';
import Img from 'theme/components/shared/Img'


# Apache Atlas Hook for Apache Kafka

## Kafka Model
Kafka model includes the following types:
   * Entity types:
      * kafka_topic
         * super-types: DataSet
         * attributes: qualifiedName, name, description, owner, topic, uri, partitionCount

Kafka entities are created and de-duped in Atlas using unique attribute qualifiedName, whose value should be formatted as detailed below.
Note that qualifiedName will have topic name in lower case.
<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
   {`topic.qualifiedName: <topic>@<clusterName>`}
</SyntaxHighlighter>


## Setup
Binary files are present in apache-atlas-<release-version/>-kafka-hook.tar.gz

Copy apache-atlas-kafka-hook-<release-version/>/hook/kafka folder to <atlas package/>/hook/    directory

Copy apache-atlas-kafka-hook-<release-version/>/hook-bin folder to  <atlas package/>/hook-bin directory

## Importing Kafka Metadata
Apache Atlas provides a command-line utility, import-kafka.sh, to import metadata of Apache Kafka topics into Apache Atlas.
This utility can be used to initialize Apache Atlas with topics present in Apache Kafka.
This utility supports importing metadata of a specific topic or all topics.

<SyntaxHighlighter wrapLines={true} language="shell" style={theme.dark}>
{`sage 1: <atlas package>/hook-bin/import-kafka.sh
Usage 2: <atlas package>/hook-bin/import-kafka.sh [-t <topic prefix> OR --topic <topic prefix>]
Usage 3: <atlas package>/hook-bin/import-kafka.sh [-f <filename>]
         File Format:
            topic1
            topic2
            topic3`}
</SyntaxHighlighter>
