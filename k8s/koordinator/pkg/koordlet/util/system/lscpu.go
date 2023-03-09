//go:build !arm64
// +build !arm64

/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package system

import (
	"fmt"
	"strconv"
	"strings"
)

// GetCacheInfo parses the output of `lscpu -e=CACHE` into l1l2 and l3 infos
// e.g.
// - input: "1:1:1:0"
// - output: "1", 0, nil
func GetCacheInfo(str string) (string, int32, error) {
	// e.g.
	// $ `lscpu -e=CPU,NODE,SOCKET,CORE,CACHE,ONLINE`
	// CPU NODE SOCKET CORE L1d:L1i:L2:L3 ONLINE
	//  0    0      0    0 0:0:0:0          yes
	//  1    0      0    0 0:0:0:0          yes
	//  2    0      0    1 1:1:1:0          yes
	//  3    0      0    1 1:1:1:0          yes
	infos := strings.Split(strings.TrimSpace(str), ":")
	// assert l1, l2 are private cache, so they has the same id with the core
	if len(infos) != 4 {
		return "", 0, fmt.Errorf("invalid cache info %s", str)
	}
	l1l2 := infos[0]
	l3, err := strconv.ParseInt(infos[3], 10, 32)
	if err != nil {
		return "", 0, err
	}
	return l1l2, int32(l3), nil
}
