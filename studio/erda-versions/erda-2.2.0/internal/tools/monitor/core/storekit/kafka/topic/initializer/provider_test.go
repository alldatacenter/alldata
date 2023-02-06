// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package initializer

import (
	"reflect"
	"testing"

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func Test_getTopicList(t *testing.T) {
	type args struct {
		topics                           []string
		metadata                         *kafka.Metadata
		numPartitions, replicationFactor int
	}
	tests := []struct {
		name string
		args args
		want []kafka.TopicSpecification
	}{
		{
			name: "normal",
			args: args{
				topics: []string{
					"abc",
					"edf",
					"xyz",
				},
				metadata: &kafka.Metadata{
					Brokers: nil,
					Topics: map[string]kafka.TopicMetadata{
						"abc": {
							Topic: "abc",
							Error: kafka.NewError(kafka.ErrNoError, "", false),
						},
						"edf": {
							Topic: "edf",
							Error: kafka.NewError(kafka.ErrAllBrokersDown, "", false),
						},
					},
					OriginatingBroker: kafka.BrokerMetadata{},
				},
				numPartitions:     9,
				replicationFactor: 1,
			},
			want: []kafka.TopicSpecification{
				{
					Topic:             "edf",
					NumPartitions:     9,
					ReplicationFactor: 1,
				},
				{
					Topic:             "xyz",
					NumPartitions:     9,
					ReplicationFactor: 1,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getTopicList(tt.args.topics, tt.args.metadata, tt.args.numPartitions, tt.args.replicationFactor); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getTopicList() = %v, want %v", got, tt.want)
			}
		})
	}
}
