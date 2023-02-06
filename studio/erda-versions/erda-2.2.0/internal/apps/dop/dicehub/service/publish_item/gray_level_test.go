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

package publish_item

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestMatchMod(t *testing.T) {
	mod := 100

	// gray level percent: 0-100
	assert.True(t, matchGrayMod(1, mod, 99))
	assert.True(t, matchGrayMod(50, mod, 99))
	assert.True(t, matchGrayMod(99, mod, 99))
	assert.True(t, matchGrayMod(100, mod, 99))

	// gray level percent: 0-30
	assert.True(t, matchGrayMod(1, mod, 30))
	assert.True(t, matchGrayMod(30, mod, 30))
	assert.False(t, matchGrayMod(31, mod, 30))

	// gray level percent: 0-15
	assert.True(t, matchGrayMod(1, mod, 30))
	assert.True(t, matchGrayMod(14, mod, 30))
	assert.True(t, matchGrayMod(15, mod, 30))
	assert.True(t, matchGrayMod(16, mod, 30))

	// gray level percent: 0-0
	assert.False(t, matchGrayMod(1, mod, 0))
	assert.False(t, matchGrayMod(14, mod, 0))
	assert.False(t, matchGrayMod(15, mod, 0))
}

func TestGetRemoteIP(t *testing.T) {
	r := http.Request{Header: make(http.Header)}
	r.Header.Set(headerXFF, "122.235.82.217, 100.122.56.227")
	ip := getRemoteIP(&r)
	assert.NotNil(t, ip)
	assert.Equal(t, "122.235.82.217", ip.String())

	r.Header.Set(headerXFF, "")
	ip = getRemoteIP(&r)
	assert.Nil(t, ip)

	r.Header.Set(headerXFF, "122.235.82.217")
	ip = getRemoteIP(&r)
	assert.NotNil(t, ip)
	assert.Equal(t, "122.235.82.217", ip.String())
}

func TestIPGray(t *testing.T) {
	r := &http.Request{Header: make(http.Header)}
	r.Header.Set(headerXFF, "42.120.75.156")
	ip := getRemoteIP(r)
	// 满足灰度
	hashStr := ip.String() + "mobile-test"
	grayLevelPercent := 9
	hashNum := getHashedNum([]byte(hashStr))
	fmt.Println("hashNum:", hashNum)
	fmt.Println("is gray:", matchGrayMod(hashNum, 100, grayLevelPercent))
}

func TestBatchIPGray(t *testing.T) {
	expectedGrayPercents := []int{10, 20, 30, 40, 50, 60, 70, 80, 90, 100}
	for _, expectedGrayPercent := range expectedGrayPercents {
		grayMap := make(map[int]int) // key: 0-100, value: num
		r := &http.Request{Header: make(http.Header)}
		for i := 0; i < 254; i++ {
			r.Header.Set(headerXFF, "192.168.0."+fmt.Sprintf("%d", i))
			ip := getRemoteIP(r)
			// 满足灰度
			hashStr := ip.String() + "cnooc-mobil"
			hashNum := getHashedNum([]byte(hashStr))
			grayMap[hashNum%100]++
		}
		resultMap := make(map[string]int)
		for i, n := range grayMap {
			if i <= expectedGrayPercent {
				resultMap["isGray"] += n
			} else {
				resultMap["isNotGray"] += n
			}
		}
		trulyGrayPercent := float64(resultMap["isGray"]) / float64(resultMap["isGray"]+resultMap["isNotGray"]) * 100
		fmt.Printf("expect gray percent: %v, trulyGrayPercent: %.2f\n", expectedGrayPercent, trulyGrayPercent)
	}
}
