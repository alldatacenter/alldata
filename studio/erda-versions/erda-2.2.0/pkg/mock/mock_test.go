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
	"reflect"
	"strconv"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRandString(t *testing.T) {
	s := randString(Integer)
	i, err := strconv.Atoi(s)
	assert.NoError(t, err)
	fmt.Println(s, i)

	s = randString(String)
	fmt.Println(s)
}

func TestGetTime(t *testing.T) {
	t.Log("s:", getTime(TimeStamp))
	t.Log("s-hour:", getTime(TimeStampHour))
	t.Log("s-after-hour:", getTime(TimeStampAfterHour))
	t.Log("s-day:", getTime(TimeStampDay))
	t.Log("s-after-day:", getTime(TimeStampAfterDay))
	t.Log("ms:", getTime(TimeStampMs))
	t.Log("ms-hour:", getTime(TimeStampMsHour))
	t.Log("ms-after-hour:", getTime(TimeStampMsAfterHour))
	t.Log("ms-day:", getTime(TimeStampMsDay))
	t.Log("ms-after-day:", getTime(TimeStampMsAfterDay))
	t.Log("ns:", getTime(TimeStampNs))
	t.Log("ns-hour:", getTime(TimeStampNsHour))
	t.Log("ns-after-hour:", getTime(TimeStampNsAfterHour))
	t.Log("ns-day:", getTime(TimeStampNsDay))
	t.Log("ns-after-day:", getTime(TimeStampNsAfterDay))

	for k := range dateTimeCustoms {
		t.Log(DateTimeCustom+k+":", getTime(DateTimeCustom+k))
	}
}

func Test_randIntegerByLength(t *testing.T) {
	type args struct {
		length string
	}
	tests := []struct {
		name          string
		args          args
		Interval      int
		IntervalStart *int
	}{
		{
			name: "test_default_length",
			args: args{
				"length",
			},
			Interval:      18,
			IntervalStart: &[]int{0}[0],
		},
		{
			name: "test_other_length",
			args: args{
				"other",
			},
			Interval:      18,
			IntervalStart: &[]int{0}[0],
		},
		{
			name: "test_1_length",
			args: args{
				"1",
			},
			Interval: 1,
		},
		{
			name: "test_5_length",
			args: args{
				"5",
			},
			Interval: 5,
		},
		{
			name: "test_18_length",
			args: args{
				"18",
			},
			Interval: 18,
		},
		{
			name: "test_empty_length",
			args: args{
				"",
			},
			Interval:      18,
			IntervalStart: &[]int{0}[0],
		},
		{
			name: "test_20_length",
			args: args{
				"20",
			},
			Interval:      18,
			IntervalStart: &[]int{0}[0],
		},
	}
	for _, tt := range tests {
		// please increase the number of loops for local testing
		for i := 0; i <= 1; i++ {
			t.Run(tt.name, func(t *testing.T) {
				got := randIntegerByLength(tt.args.length)
				if tt.IntervalStart != nil {
					assert.True(t, got >= *tt.IntervalStart && got < int(math.Pow10(tt.Interval)))
				} else {
					assert.True(t, got >= int(math.Pow10(tt.Interval-1)) && got < int(math.Pow10(tt.Interval)))
				}
			})
		}
	}
}

func TestMockValue(t *testing.T) {
	type args struct {
		mockType string
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "test_integer_n",
			args: args{
				mockType: "integer_n",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := MockValue(tt.args.mockType)
			assert.True(t, got != nil && got != "")
		})
	}
}

func Test_getTime(t *testing.T) {
	type args struct {
		timeType string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getTime(tt.args.timeType); got != tt.want {
				t.Errorf("getTime() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_mockValue(t *testing.T) {
	type args struct {
		mockType string
	}
	tests := []struct {
		name string
		args args
		want interface{}
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := mockValue(tt.args.mockType); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("mockValue() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_randBool(t *testing.T) {
	tests := []struct {
		name string
		want bool
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := randBool(); got != tt.want {
				t.Errorf("randBool() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_randFloat(t *testing.T) {
	tests := []struct {
		name string
		want float64
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := randFloat(); got != tt.want {
				t.Errorf("randFloat() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_randInteger(t *testing.T) {
	tests := []struct {
		name string
		want int
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := randInteger(); got != tt.want {
				t.Errorf("randInteger() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_randIntegerByLength1(t *testing.T) {
	type args struct {
		length string
	}
	tests := []struct {
		name string
		args args
		want int
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := randIntegerByLength(tt.args.length); got != tt.want {
				t.Errorf("randIntegerByLength() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_randMobile(t *testing.T) {
	tests := []struct {
		name string
		want string
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := randMobile(); got != tt.want {
				t.Errorf("randMobile() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_randString(t *testing.T) {
	type args struct {
		randType string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := randString(tt.args.randType); got != tt.want {
				t.Errorf("randString() = %v, want %v", got, tt.want)
			}
		})
	}
}
