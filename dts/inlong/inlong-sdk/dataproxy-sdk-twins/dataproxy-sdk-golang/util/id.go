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

package util

import (
	"log"

	"github.com/bwmarrin/snowflake"
	"github.com/gofrs/uuid"
	"github.com/zentures/cityhash"
)

var (
	snowflakeNode *snowflake.Node
)

func init() {
	ip, err := GetFirstPrivateIP()
	if err != nil {
		log.Fatal(err)
	}

	id := IPtoUInt(ip)
	snowflakeNode, err = snowflake.NewNode(int64(id % 1024))
	if err != nil {
		log.Fatal(err)
	}
}

// UInt64UUID generates an uint64 UUID
func UInt64UUID() (uint64, error) {
	guid, err := uuid.NewV4()
	if err != nil {
		return 0, err
	}

	bytes := guid.Bytes()
	length := len(bytes)
	return cityhash.CityHash64WithSeeds(bytes, uint32(length), 13329145742295551469, 7926974186468552394), nil
}

// SnowFlakeID generates a snowflake ID
func SnowFlakeID() string {
	return snowflakeNode.Generate().String()
}
