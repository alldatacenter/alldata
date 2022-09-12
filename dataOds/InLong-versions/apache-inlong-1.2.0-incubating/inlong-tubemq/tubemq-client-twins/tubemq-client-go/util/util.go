// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

// Package util defines the constants and helper functions.
package util

import (
	"errors"
	"fmt"
	"net"
	"regexp"
	"strconv"
	"strings"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/protocol"
)

// InvalidValue defines the invalid value of TubeMQ config.
var InvalidValue = int64(-2)

// GetLocalHost returns the local host name.
func GetLocalHost() string {
	ifaces, err := net.Interfaces()
	if err != nil {
		return ""
	}
	for _, iface := range ifaces {
		if iface.Flags&net.FlagUp == 0 {
			continue // interface down
		}
		if iface.Flags&net.FlagLoopback != 0 {
			continue // loopback interface
		}
		addrs, err := iface.Addrs()
		if err != nil {
			return ""
		}
		for _, addr := range addrs {
			var ip net.IP
			switch v := addr.(type) {
			case *net.IPNet:
				ip = v.IP
			case *net.IPAddr:
				ip = v.IP
			}
			if ip == nil || ip.IsLoopback() {
				continue
			}
			ip = ip.To4()
			if ip == nil {
				continue // not an ipv4 address
			}
			return ip.String()
		}
	}
	return ""
}

// GenBrokerAuthenticateToken generates the broker authenticate token.
func GenBrokerAuthenticateToken(username string, password string) string {
	return ""
}

// GenMasterAuthenticateToken generates the master authenticate token.
func GenMasterAuthenticateToken(authInfo *protocol.AuthenticateInfo, username string, password string) {
}

// ParseConfirmContext parses the confirm context to partition key and bookedTime.
func ParseConfirmContext(confirmContext string) (string, int64, error) {
	res := strings.Split(confirmContext, "@")
	if len(res) == 0 {
		return "", 0, fmt.Errorf("illegal confirmContext content: unregular value format")
	}
	partitionKey := res[0]
	bookedTime, err := strconv.ParseInt(res[1], 10, 64)
	if err != nil {
		return "", 0, err
	}
	return partitionKey, bookedTime, nil
}

// SplitToMap split the given string by the two step strings to map.
// Source format $msgType$=metadata_journal_log,$msgTime$=202111081911,tdbusip=127.0.0.1
func SplitToMap(source string, step1 string, step2 string) map[string]string {
	m := make(map[string]string)
	if len(source) == 0 {
		return m
	}

	str := strings.Split(source, step1)
	for _, ss := range str {
		s := strings.Split(ss, step2)
		if len(s) == 1 {
			continue
		}
		key := strings.TrimSpace(s[0])
		if len(key) == 0 {
			continue
		}
		m[key] = strings.TrimSpace(s[1])
	}
	return m
}

// IsValidString returns whether a string is valid.
func IsValidString(s string) (bool, error) {
	if matched, _ := regexp.Match("^[a-zA-Z]\\w+$", []byte(s)); !matched {
		return false,
			errors.New(fmt.Sprintf("illegal parameter: %s must begin with a letter, " +
				"can only contain characters,numbers,and underscores", s))
	}
	return true, nil
}

// IsValidFilterItem returns whether a filter is valid.
func IsValidFilterItem(s string) (bool, error) {
	if matched, _ := regexp.Match("^[_A-Za-z0-9]+$", []byte(s)); !matched {
		return false, errors.New("value only contain characters,numbers,and underscores")
	}
	return true, nil
}

// Join returns the joined result of a map.
func Join(m map[string]string, step1 string, step2 string) string {
	cnt := 0
	s := ""
	for k, v := range m {
		if cnt > 0 {
			s += step1
		}
		s += k + step2 + v
		cnt++
	}
	return s
}
