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

package mock

import (
	"fmt"
	"math"
	"math/rand"
	"strconv"
	"strings"
	"time"
)

// 数据类型
const (
	String         = "string"
	Integer        = "integer"
	Float          = "float"
	Boolean        = "boolean"
	Upper          = "upper"
	Lower          = "lower"
	Mobile         = "mobile"
	DigitalLetters = "digital_letters"
	Letters        = "letters"
	Character      = "character"
	List           = "list"
)

func randFloat() float64 {
	fd := fmt.Sprintf("%s.%s", randString(Integer), randString(Integer))
	f, _ := strconv.ParseFloat(fd, 64)

	return f
}

func randBool() bool {
	rand.Seed(time.Now().UnixNano())
	t := rand.Intn(2)
	if t == 0 {
		return false
	} else {
		return true
	}
}

func randInteger() int {
	intStr := randString(Integer)
	num, _ := strconv.Atoi(intStr)
	return num
}

func randIntegerByLength(length string) int {
	lenInt, err := strconv.ParseInt(length, 10, 32)
	if err != nil || lenInt <= 0 || lenInt > 18 {
		return randInteger()
	}

	return rangeIn(int(math.Pow10(int(lenInt-1))), int(math.Pow10(int(lenInt)))-1)
}

func rangeIn(low, hi int) int {
	return low + rand.Intn(hi-low)
}

func randMobile() string {
	prefix := []string{
		"133", "153", "180", "189", "130", "131", "155", "156", "185",
		"186", "134", "135", "136", "137", "138", "139", "147", "150",
		"151", "152", "157", "158", "159", "182", "187", "188", "170",
	}

	rand.Seed(time.Now().UnixNano())
	rs := make([]string, 8)
	for start := 0; start < 8; start++ {
		rs = append(rs, strconv.Itoa(rand.Intn(10)))
	}

	return fmt.Sprint(prefix[rand.Intn(len(prefix))], strings.Join(rs, ""))
}

const (
	asciiCodeIndex_A = 65
	asciiCodeIndex_a = 97
	asciiCodeIndex_0 = 48
	alphabetLength   = 26
	numberLength     = 10
)

// see: https://www.ascii-code.com/
func randString(randType string) string {
	rand.Seed(time.Now().UnixNano())
	var length int
	// length must larger than 0
	for {
		rn := rand.Intn(16)
		if rn > 0 {
			length = rn
			break
		}
	}
	asciiValues := make([]int, 0, length)
	switch randType {
	case Integer:
		for start := 0; start < length; start++ {
			asciiValues = append(asciiValues, asciiCodeIndex_0+rand.Intn(numberLength))
		}
	case Lower:
		for start := 0; start < length; start++ {
			asciiValues = append(asciiValues, asciiCodeIndex_a+rand.Intn(alphabetLength))
		}
	case Upper:
		for start := 0; start < length; start++ {
			asciiValues = append(asciiValues, asciiCodeIndex_A+rand.Intn(alphabetLength))
		}
	case Letters:
		for start := 0; start < length; start++ {
			t := rand.Intn(2)
			if t == 0 {
				asciiValues = append(asciiValues, asciiCodeIndex_A+rand.Intn(alphabetLength))
			} else {
				asciiValues = append(asciiValues, asciiCodeIndex_a+rand.Intn(alphabetLength))
			}
		}
	case DigitalLetters, String:
		for start := 0; start < length; start++ {
			t := rand.Intn(3)
			if t == 0 {
				asciiValues = append(asciiValues, asciiCodeIndex_0+rand.Intn(numberLength))
			} else if t == 1 {
				asciiValues = append(asciiValues, asciiCodeIndex_A+rand.Intn(alphabetLength))
			} else {
				asciiValues = append(asciiValues, asciiCodeIndex_a+rand.Intn(alphabetLength))
			}
		}
	case Character:
		t := rand.Intn(2)
		if t == 0 {
			asciiValues = append(asciiValues, asciiCodeIndex_A+rand.Intn(alphabetLength))
		} else {
			asciiValues = append(asciiValues, asciiCodeIndex_a+rand.Intn(alphabetLength))
		}
	}

	var s []rune
	for _, asciiValue := range asciiValues {
		s = append(s, rune(asciiValue))
	}
	return string(s)
}

func MockValue(mockType string) interface{} {
	return mockValue(mockType)
}

func mockValue(mockType string) interface{} {
	switch mockType {
	case String, Upper, Lower, DigitalLetters, Letters, Character:
		return randString(mockType)
	case Integer:
		return randInteger()
	case Float:
		return randFloat()
	case Boolean:
		return randBool()
	case Mobile:
		return randMobile()
	default:
		if strings.HasPrefix(mockType, fmt.Sprintf("%v_", Integer)) {
			length := strings.Replace(mockType, fmt.Sprintf("%v_", Integer), "", 1)
			return randIntegerByLength(length)
		}
	}

	time := getTime(mockType)
	if time != "" {
		return time
	}

	return nil
}
