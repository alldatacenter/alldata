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

package snowflake

import (
	"errors"
	"fmt"
	"sync"
	"time"
)

// Inspired by Sonyflake.

// A Snowflake ID is composed of below parts:
//     35 bits for time in units of 10 msec (lifetime: 10 years)
//      6 bits for a sequence number (64 IDs/10msec)
//     12 bits for a machine id (4096 machines/thread)
const (
	BitTotal        = 53                                     // bit length of Snowflake ID
	BitLenTime      = 35                                     // bit length of time
	BitLenSequence  = 6                                      // bit length of sequence number
	BitLenMachineID = BitTotal - BitLenTime - BitLenSequence // bit length of machine id ; 12
)

// Settings configures Snowflake:
//
// StartTime is the time since which the Snowflake time is defined as the elapsed time.
// If StartTime is 0, the start time of the Snowflake is set to "2022-04-19 00:00:00 +0000 UTC".
// If StartTime is ahead of the current time, Snowflake is not created.
//
// MachineID returns the unique ID of the Snowflake instance.
// If MachineID returns an error, Snowflake is not created.
// If MachineID is nil, default MachineID is used.
// Default MachineID returns the lower 16 bits of the private IP address.
//
// CheckMachineID validates the uniqueness of the machine ID.
// If CheckMachineID returns false, Snowflake is not created.
// If CheckMachineID is nil, no validation is done.
type Settings struct {
	StartTime      time.Time
	MachineID      func() (uint16, error)
	CheckMachineID func(uint16) bool
}

// Snowflake is a distributed unique ID generator.
type Snowflake struct {
	mutex       *sync.Mutex
	startTime   int64
	elapsedTime int64
	sequence    uint16
	machineID   uint16
}

// NewSnowflake returns a new Snowflake configured with the given Settings.
// NewSnowflake returns nil in the following cases:
// - Settings.StartTime is ahead of the current time.
// - Settings.MachineID returns an error.
// - Settings.CheckMachineID returns false.
func NewSnowflake(st Settings) *Snowflake {
	sf := new(Snowflake)
	sf.mutex = new(sync.Mutex)
	sf.sequence = uint16(1<<BitLenSequence - 1)

	if st.StartTime.After(time.Now()) {
		return nil
	}
	if st.StartTime.IsZero() {
		sf.startTime = toSnowflakeTime(time.Date(2022, 4, 19, 0, 0, 0, 0, time.UTC))
	} else {
		sf.startTime = toSnowflakeTime(st.StartTime)
	}

	var err error
	if st.MachineID == nil {
		sf.machineID, err = get12BitMachineID()
	} else {
		sf.machineID, err = st.MachineID()
	}
	if err != nil {
		panic(err)
	}
	if st.CheckMachineID != nil && !st.CheckMachineID(sf.machineID) {
		panic(fmt.Errorf("failed to check machineID"))
	}

	return sf
}

// NextID generates a next unique ID.
// After the Snowflake time overflows, NextID returns an error.
func (sf *Snowflake) NextID() (uint64, error) {
	const maskSequence = uint16(1<<BitLenSequence - 1)

	sf.mutex.Lock()
	defer sf.mutex.Unlock()

	current := currentElapsedTime(sf.startTime)
	if sf.elapsedTime < current {
		sf.elapsedTime = current
		sf.sequence = 0
	} else { // sf.elapsedTime >= current
		sf.sequence = (sf.sequence + 1) & maskSequence
		if sf.sequence == 0 {
			sf.elapsedTime++
			overtime := sf.elapsedTime - current
			time.Sleep(sleepTime(overtime))
		}
	}

	return sf.toID()
}

const snowflakeTimeUnit = 1e7 // nsec, i.e. 10 msec

func toSnowflakeTime(t time.Time) int64 {
	return t.UTC().UnixNano() / snowflakeTimeUnit
}

func currentElapsedTime(startTime int64) int64 {
	return toSnowflakeTime(time.Now()) - startTime
}

func sleepTime(overtime int64) time.Duration {
	return time.Duration(overtime)*10*time.Millisecond -
		time.Duration(time.Now().UTC().UnixNano()%snowflakeTimeUnit)*time.Nanosecond
}

func (sf *Snowflake) toID() (uint64, error) {
	if sf.elapsedTime >= 1<<BitLenTime {
		return 0, errors.New("over the time limit")
	}

	return uint64(sf.elapsedTime)<<(BitLenSequence+BitLenMachineID) |
		uint64(sf.sequence)<<BitLenMachineID |
		uint64(sf.machineID), nil
}

// Decompose returns a set of Snowflake ID parts.
func Decompose(id uint64) map[string]uint64 {
	const maskSequence = uint64((1<<BitLenSequence - 1) << BitLenMachineID)
	const maskMachineID = uint64(1<<BitLenMachineID - 1)

	msb := id >> BitTotal
	time := id >> (BitLenSequence + BitLenMachineID)
	sequence := id & maskSequence >> BitLenMachineID
	machineID := id & maskMachineID
	return map[string]uint64{
		"id":         id,
		"msb":        msb,
		"time":       time,
		"sequence":   sequence,
		"machine-id": machineID,
	}
}
