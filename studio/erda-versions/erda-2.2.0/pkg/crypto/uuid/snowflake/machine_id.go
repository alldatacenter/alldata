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
	"os"
)

const (
	POD_IP = "POD_IP"
)

func get12BitMachineID() (uint16, error) {
	ip, err := tryGetIPv4()
	if err != nil {
		return 0, err
	}

	machineID := get12BitMachineIDByIPv4(ip)
	return machineID, nil
}

func get12BitMachineIDByIPv4(ipv4 net.IP) uint16 {
	ipPart2 := ipv4[2] // from part 0 to 3
	movedIPPart2 := uint16(ipPart2) << 8
	fourBitReservedIPPart2 := movedIPPart2 & (0b00001111 << 8)
	ipPart3 := ipv4[3]
	machineID := fourBitReservedIPPart2 | uint16(ipPart3)
	return machineID
}

func tryGetIPv4() (net.IP, error) {
	podIP, ok := tryPodIP()
	if ok {
		return podIP, nil
	}

	as, err := net.InterfaceAddrs()
	if err != nil {
		return nil, err
	}

	for _, a := range as {
		ipnet, ok := a.(*net.IPNet)
		if !ok || ipnet.IP.IsLoopback() {
			continue
		}

		ip := ipnet.IP.To4()
		if ip != nil {
			return ip, nil
		}
	}
	return nil, fmt.Errorf("no ipv4 address")
}

func tryPodIP() (net.IP, bool) {
	podIP := os.Getenv(POD_IP)
	if podIP == "" {
		return nil, false
	}
	ip := net.ParseIP(podIP)
	if ip == nil {
		return nil, false
	}
	return ip.To4(), true
}

func isPrivateIPv4(ip net.IP) bool {
	return ip != nil &&
		(ip[0] == 10 || ip[0] == 172 && (ip[1] >= 16 && ip[1] < 32) || ip[0] == 192 && ip[1] == 168)
}
