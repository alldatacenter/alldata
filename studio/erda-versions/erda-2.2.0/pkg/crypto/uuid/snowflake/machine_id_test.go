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
	"fmt"
	"net"
	"testing"
)

func Test_get12BitMachineIDByIPv4(t *testing.T) {
	// last 12-bit: 1000 00111001
	ipsWithSameLast12Bits := []string{
		"30.43.40.57",
		"30.43.8.57",
		"1.1.24.57",
	}

	var machineID uint16
	for i, ipStr := range ipsWithSameLast12Bits {
		ip := net.ParseIP(ipStr).To4()
		calMachineID := get12BitMachineIDByIPv4(ip)
		if i == 0 {
			machineID = calMachineID
			continue
		}
		if calMachineID != machineID {
			panic(fmt.Errorf("machineID not same, ip: %s", ipStr))
		}
	}
}

func TestDecompose(t *testing.T) {
	id := uint64(1154715486265)
	fmt.Println(Decompose(id))

	id = uint64(1154715490361)
	fmt.Println(Decompose(id))
}
