<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->
[![Go Report Card](https://goreportcard.com/badge/github.com/apache/incubator-inlong)](https://goreportcard.com/report/github.com/apache/incubator-inlong)
[![Language](https://img.shields.io/badge/Language-Go-blue.svg)](https://golang.org/)
# TubeMQ Go Client Library

## Goal

This project is a pure-Go client library for TubeMQ that does not
depend on the [TubeMQ C++ library](https://github.com/apache/incubator-inlong/tree/master/inlong-tubemq/tubemq-client-twins/tubemq-client-cpp). Production is not supported yet.
## Requirements

- Go 1.11+


## Usage

Import the `client` and`config` library:
```go
import "github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/client"
import "github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/config"
```

Import the `log` library for log if needed:
```go
import "github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/log"
```

Create a Consumer by parsing address:
```go
cfg, err := config.ParseAddress("127.0.0.1:8099?topic=test_1&group=test_group")
if err != nil {
	fmt.Errorf("Failed to parse address %s", err.Error())
	panic(err)
}
c, err := client.NewConsumer(cfg)
if err != nil {
	fmt.Errorf("new consumer error %s", err.Error())
	panic(err)
}

defer c.Close()

cr, err := c.GetMessage()
// need to confirm by yourself.
_, err = c.Confirm(cr.ConfirmContext, true)

for _, msg := range cr.Messages {
	fmt.Printf("Received message msgId: %#v -- content: '%s'\n", msg.ID, string(msg.Data))
}
```

Create a Consumer by constructing a config:

```go
topicFilters := map[string][]string{"topic1": {"filter1", "filter2"}, "topic2": {"filter3", "filter4"}}
partitionOffset := map[string]int64{"181895251:topic1:1": 0, "181895251:topic2:2": 10}
cfg := config.New(config.WithMasters("127.0.0.1:8099"),
config.WithGroup("group"),
//For topic filters
config.WithTopicFilters(topicFilters),
// For bound consume
config.WithBoundConsume("ss", 1, true, partitionOffset))
c, err := client.NewConsumer(cfg)
if err != nil {
	fmt.Errorf("new consumer error %s", err.Error())
	panic(err)
}

defer c.Close()

cr, err := c.GetMessage()
// need to confirm by yourself.
_, err = c.Confirm(cr.ConfirmContext, true)

for _, msg := range cr.Messages {
	fmt.Printf("Received message msgId: %#v -- content: '%s'\n", msg.ID, string(msg.Data))
}
```

## Example
Multiple Goroutines consumption is also supported. More specific examples can be referred in [Go Client Examples](https://github.com/apache/incubator-inlong/tree/master/inlong-tubemq/tubemq-client-twins/tubemq-client-go/example).
