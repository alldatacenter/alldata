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

package uuid

import (
	"fmt"
	"os"
	"strconv"
	"testing"

	"github.com/erda-project/erda/pkg/crypto/uuid/snowflake"
)

func TestSnowFlakeUUID(t *testing.T) {
	fmt.Println(SnowFlakeIDUint64())
	fmt.Println(SnowFlakeIDUint64())
	fmt.Println(SnowFlakeIDUint64())
	fmt.Println(SnowFlakeIDUint64())
	fmt.Println(SnowFlakeIDUint64())
	fmt.Println(SnowFlakeIDUint64())
}

func TestSnowFlakeUUIDConcurrency(t *testing.T) {
	// different pod ip cause not duplicated
	os.Setenv(snowflake.POD_IP, "1.1.1.1")
	sf1 := snowflake.NewSnowflake(snowflake.Settings{})
	os.Setenv(snowflake.POD_IP, "1.1.1.2")
	sf2 := snowflake.NewSnowflake(snowflake.Settings{})
	var sf1Results []string
	var sf2Results []string
	sf1Done := make(chan struct{})
	sf2Done := make(chan struct{})
	const length = 10000
	go func() {
		sf1Map := make(map[uint64]struct{}, length)
		for i := 0; i < length; i++ {
			id1, _ := sf1.NextID()
			sf1Results = append(sf1Results, strconv.FormatUint(id1, 10))
			if _, ok := sf1Map[id1]; ok {
				panic(fmt.Errorf("id %d duplicated", id1))
			}
		}
		sf1Done <- struct{}{}
	}()
	go func() {
		sf2Map := make(map[uint64]struct{}, length)
		for i := 0; i < length; i++ {
			id2, _ := sf2.NextID()
			sf2Results = append(sf2Results, strconv.FormatUint(id2, 10))
			if _, ok := sf2Map[id2]; ok {
				panic(fmt.Errorf("id %d duplicated", id2))
			}
		}
		sf2Done <- struct{}{}
	}()
	<-sf1Done
	<-sf2Done
	sf1Map := make(map[string]struct{})
	for _, r1 := range sf1Results {
		sf1Map[r1] = struct{}{}
	}
	for _, r2 := range sf2Results {
		if _, ok := sf1Map[r2]; ok {
			panic(fmt.Errorf("id %s duplicated", r2))
		}
	}
}

//func TestSnowFlakeUUIDConcurrencyNotOK(t *testing.T) {
//	// no machine id cause duplicated when distributed
//	sf1 := sonyflake.NewSonyflake(sonyflake.Settings{
//		//MachineID: func() (uint16, error) { return podIP(), nil },
//	})
//	sf2 := sonyflake.NewSonyflake(sonyflake.Settings{
//		//MachineID: func() (uint16, error) { return podIP(), nil },
//	})
//	var sf1Results []string
//	var sf2Results []string
//	sf1Done := make(chan struct{})
//	sf2Done := make(chan struct{})
//	go func() {
//		for i := 0; i < 10000; i++ {
//			id1, _ := sf1.NextID()
//			sf1Results = append(sf1Results, strconv.FormatUint(id1, 10))
//		}
//		sf1Done <- struct{}{}
//	}()
//	go func() {
//		for i := 0; i < 10000; i++ {
//			id2, _ := sf2.NextID()
//			sf2Results = append(sf2Results, strconv.FormatUint(id2, 10))
//		}
//		sf2Done <- struct{}{}
//	}()
//	<-sf1Done
//	<-sf2Done
//	sf1Map := make(map[string]struct{})
//	for _, r1 := range sf1Results {
//		sf1Map[r1] = struct{}{}
//	}
//	for _, r2 := range sf2Results {
//		if _, ok := sf1Map[r2]; ok {
//			panic(fmt.Errorf("id %s duplicated", r2))
//		}
//	}
//}
