//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dataproxy_test

import (
	"context"
	"fmt"
	"time"

	"github.com/apache/inlong/inlong-sdk/dataproxy-sdk-twins/dataproxy-sdk-golang/dataproxy"

	"go.uber.org/atomic"
)

func ExampleClient_Send() {
	client, err := dataproxy.NewClient(
		dataproxy.WithGroupID("test"),
		dataproxy.WithURL("http://127.0.0.1:8083/inlong/manager/openapi/dataproxy/getIpList"),
		dataproxy.WithMetricsName("test"),
	)

	if err != nil {
		fmt.Println(err)
		return
	}

	for i := 0; i < 1000; i++ {
		err := client.Send(context.Background(), dataproxy.Message{
			GroupID:  "test",
			StreamID: "test",
			Payload:  []byte("test|a|b|c"),
		})
		if err != nil {
			fmt.Println(err)
		}
	}

	client.Close()
}

func ExampleClient_SendAsync() {
	client, err := dataproxy.NewClient(
		dataproxy.WithGroupID("test"),
		dataproxy.WithURL("http://127.0.0.1:8083/inlong/manager/openapi/dataproxy/getIpList"),
		dataproxy.WithMetricsName("test"),
	)

	if err != nil {
		fmt.Println(err)
		return
	}

	var success atomic.Uint64
	var failed atomic.Uint64
	for i := 0; i < 1000; i++ {
		client.SendAsync(context.Background(),
			dataproxy.Message{
				GroupID:  "test",
				StreamID: "test",
				Payload:  []byte("test|a|b|c"),
			},
			func(msg dataproxy.Message, err error) {
				if err != nil {
					success.Add(1)
				} else {
					failed.Add(1)
				}
			})
	}

	// wait async send finish
	time.Sleep(3 * time.Second)
	fmt.Println("success:", success.Load())
	fmt.Println("failed:", failed.Load())
	client.Close()
}
