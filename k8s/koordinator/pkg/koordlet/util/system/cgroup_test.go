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
	"math"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestCgroupFileWriteIfDifferent(t *testing.T) {
	taskDir := "/"
	type args struct {
		cgroupTaskDir string
		resource      Resource
		value         string
		currentValue  string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "currentValue is the same as value",
			args: args{
				cgroupTaskDir: taskDir,
				resource:      CPUShares,
				value:         "1024",
				currentValue:  "1024",
			},
			wantErr: false,
		},
		{
			name: "currentValue is different with value",
			args: args{
				cgroupTaskDir: taskDir,
				resource:      CPUShares,
				value:         "1024",
				currentValue:  "512",
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := NewFileTestUtil(t)
			helper.CreateCgroupFile(taskDir, tt.args.resource)

			err := CgroupFileWrite(taskDir, tt.args.resource, tt.args.currentValue)
			assert.NoError(t, err)

			gotErr := CgroupFileWriteIfDifferent(taskDir, tt.args.resource, tt.args.currentValue)
			assert.Equal(t, tt.wantErr, gotErr != nil)
		})
	}
}

func TestCgroupFileReadInt(t *testing.T) {
	taskDir := "/"
	testingInt64 := int64(1024)
	testingMaxInt64 := int64(math.MaxInt64)
	type args struct {
		file      Resource
		value     string
		supported bool
	}
	tests := []struct {
		name      string
		args      args
		expect    *int64
		expectErr bool
	}{
		{
			name: "test_read_success",
			args: args{
				file:      CPUShares,
				value:     "1024",
				supported: true,
			},
			expect:    &testingInt64,
			expectErr: false,
		},
		{
			name: "test_read_error",
			args: args{
				file:      CPUShares,
				value:     "unknown",
				supported: true,
			},
			expect:    nil,
			expectErr: true,
		},
		{
			name: "test_read_value_for_max_str",
			args: args{
				file:      MemoryHigh,
				value:     CgroupMaxSymbolStr,
				supported: true,
			},
			expect:    &testingMaxInt64,
			expectErr: false,
		},
		{
			name: "test_read_value_for_resource_unsupported",
			args: args{
				file:      MemoryWmarkRatio,
				value:     "0",
				supported: false,
			},
			expect:    nil,
			expectErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := NewFileTestUtil(t)
			helper.SetResourcesSupported(true, tt.args.file) // set supported for initialization
			helper.CreateCgroupFile(taskDir, tt.args.file)

			err := CommonFileWrite(tt.args.file.Path(taskDir), tt.args.value)
			assert.NoError(t, err)

			helper.SetResourcesSupported(tt.args.supported, tt.args.file)
			got, gotErr := CgroupFileReadInt(taskDir, tt.args.file)

			assert.Equal(t, tt.expect, got)
			assert.Equal(t, tt.expectErr, gotErr != nil)
		})
	}
}

func genCPUStatContent() string {
	return "nr_periods 18491717\n" +
		"nr_throttled 12\n" +
		"throttled_time 123\n"
}

func TestReadCPUStatRaw(t *testing.T) {
	helper := NewFileTestUtil(t)
	testCPUDir := "cpu"
	filePath := GetCgroupFilePath(testCPUDir, CPUStat)

	goodContent := genCPUStatContent()
	helper.WriteCgroupFileContents(testCPUDir, CPUStat, goodContent)
	got, err := ReadCPUStatRaw(filePath)
	assert.NoError(t, err)
	assert.Equal(t, int64(18491717), got.NrPeriods)
	assert.Equal(t, int64(12), got.NrThrottled)
	assert.Equal(t, int64(123), got.ThrottledNanoSeconds)

	badContent1 := "nr_periods a"
	helper.WriteCgroupFileContents(testCPUDir, CPUStat, badContent1)
	_, err1 := ReadCPUStatRaw(filePath)
	assert.Error(t, err1)

	badContent2 := "nr_throttled a"
	helper.WriteCgroupFileContents(testCPUDir, CPUStat, badContent2)
	_, err2 := ReadCPUStatRaw(filePath)
	assert.Error(t, err2)

	badContent3 := "throttled_time a"
	helper.WriteCgroupFileContents(testCPUDir, CPUStat, badContent3)
	_, err3 := ReadCPUStatRaw(filePath)
	assert.Error(t, err3)

	badContent4 := "nr_periods 18491717\n" + "nr_throttled 12\n"
	helper.WriteCgroupFileContents(testCPUDir, CPUStat, badContent4)
	_, err4 := ReadCPUStatRaw(filePath)
	assert.Error(t, err4)
}

func TestCalcCPUThrottledRatio(t *testing.T) {
	type args struct {
		curPoint *CPUStatRaw
		prePoint *CPUStatRaw
	}
	tests := []struct {
		name string
		args args
		want float64
	}{
		{
			name: "calculate-throttled-ratio",
			args: args{
				curPoint: &CPUStatRaw{
					NrPeriods:            200,
					NrThrottled:          40,
					ThrottledNanoSeconds: 40000,
				},
				prePoint: &CPUStatRaw{
					NrPeriods:            100,
					NrThrottled:          20,
					ThrottledNanoSeconds: 20000,
				},
			},
			want: 0.2,
		},
		{
			name: "calculate-throttled-ratio-zero-period",
			args: args{
				curPoint: &CPUStatRaw{
					NrPeriods:            100,
					NrThrottled:          20,
					ThrottledNanoSeconds: 20000,
				},
				prePoint: &CPUStatRaw{
					NrPeriods:            100,
					NrThrottled:          20,
					ThrottledNanoSeconds: 20000,
				},
			},
			want: 0,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := CalcCPUThrottledRatio(tt.args.curPoint, tt.args.prePoint); got != tt.want {
				t.Errorf("CalcCPUThrottledRatio() = %v, want %v", got, tt.want)
			}
		})
	}
}
