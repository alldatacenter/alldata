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
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_ReadResctrlTasksMap(t *testing.T) {
	type args struct {
		groupPath string
	}
	type fields struct {
		tasksStr    string
		invalidPath bool
	}
	tests := []struct {
		name    string
		args    args
		fields  fields
		want    map[int32]struct{}
		wantErr bool
	}{
		{
			name:    "do not panic but throw an error for empty input",
			want:    map[int32]struct{}{},
			wantErr: false,
		},
		{
			name:    "invalid path",
			fields:  fields{invalidPath: true},
			want:    nil,
			wantErr: true,
		},
		{
			name:    "parse correctly",
			fields:  fields{tasksStr: "101\n111\n"},
			want:    map[int32]struct{}{101: {}, 111: {}},
			wantErr: false,
		},
		{
			name:    "parse correctly 1",
			args:    args{groupPath: "BE"},
			fields:  fields{tasksStr: "101\n111\n"},
			want:    map[int32]struct{}{101: {}, 111: {}},
			wantErr: false,
		},
		{
			name:    "parse error for invalid task str",
			fields:  fields{tasksStr: "101\n1aa\n"},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			sysFSRootDir := t.TempDir()
			resctrlDir := filepath.Join(sysFSRootDir, ResctrlDir, tt.args.groupPath)
			err := os.MkdirAll(resctrlDir, 0700)
			assert.NoError(t, err)

			tasksPath := filepath.Join(resctrlDir, ResctrlTasksName)
			err = os.WriteFile(tasksPath, []byte(tt.fields.tasksStr), 0666)
			assert.NoError(t, err)

			Conf = &Config{
				SysFSRootDir: sysFSRootDir,
			}
			if tt.fields.invalidPath {
				Conf.SysFSRootDir = "invalidPath"
			}

			got, err := ReadResctrlTasksMap(tt.args.groupPath)
			assert.Equal(t, tt.wantErr, err != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestResctrlSchemataRaw(t *testing.T) {
	type fields struct {
		l3Num     int
		l3Mask    string
		mbPercent string
	}
	tests := []struct {
		name         string
		fields       fields
		wantL3String string
		wantMBString string
	}{
		{
			name: "new l3 schemata",
			fields: fields{
				l3Num:  1,
				l3Mask: "f",
			},
			wantL3String: "L3:0=f;\n",
		},
		{
			name: "new mba schemata",
			fields: fields{
				l3Num:     1,
				mbPercent: "90",
			},
			wantMBString: "MB:0=90;\n",
		},
		{
			name: "new l3 with mba schemata",
			fields: fields{
				l3Num:     2,
				l3Mask:    "fff",
				mbPercent: "100",
			},
			wantL3String: "L3:0=fff;1=fff;\n",
			wantMBString: "MB:0=100;1=100;\n",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := NewResctrlSchemataRaw()
			r.WithL3Num(tt.fields.l3Num).WithL3Mask(tt.fields.l3Mask).WithMBPercent(tt.fields.mbPercent)
			if len(tt.fields.l3Mask) > 0 {
				got := r.L3String()
				assert.Equal(t, tt.wantL3String, got)
			}
			if len(tt.fields.mbPercent) > 0 {
				got1 := r.MBString()
				assert.Equal(t, tt.wantMBString, got1)
			}
		})
	}
}

func Test_CheckAndTryEnableResctrlCat(t *testing.T) {
	type fields struct {
		cbmStr      string
		invalidPath bool
	}
	tests := []struct {
		name    string
		fields  fields
		wantErr bool
	}{
		{
			name:    "return disabled for a invalid path",
			fields:  fields{invalidPath: true},
			wantErr: true,
		},
		{
			name:    "return enabled for a valid l3_cbm",
			fields:  fields{cbmStr: "3f"},
			wantErr: false,
		},
		// TODO: add mount case
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			sysFSRootDir := t.TempDir()
			resctrlDir := filepath.Join(sysFSRootDir, ResctrlDir)
			l3CatDir := filepath.Join(resctrlDir, RdtInfoDir, L3CatDir)
			err := os.MkdirAll(l3CatDir, 0700)
			assert.NoError(t, err)

			cbmPath := filepath.Join(l3CatDir, ResctrlCbmMaskName)
			err = os.WriteFile(cbmPath, []byte(tt.fields.cbmStr), 0666)
			assert.NoError(t, err)

			Conf = &Config{
				SysFSRootDir: sysFSRootDir,
			}
			if tt.fields.invalidPath {
				Conf.SysFSRootDir = "invalidPath"
			}

			gotErr := CheckAndTryEnableResctrlCat()

			assert.Equal(t, tt.wantErr, gotErr != nil)
		})
	}
}

func Test_MountResctrlSubsystem(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		sysFSRootDir := t.TempDir()
		resctrlDir := filepath.Join(sysFSRootDir, ResctrlDir)
		err := os.MkdirAll(resctrlDir, 0700)
		assert.NoError(t, err)

		schemataPath := filepath.Join(resctrlDir, ResctrlSchemataName)
		err = os.WriteFile(schemataPath, []byte("    L3:0=ff;1=ff\n    MB:0=100;1=100\n"), 0666)
		assert.NoError(t, err)

		Conf = &Config{
			SysFSRootDir: sysFSRootDir,
		}

		got, err := MountResctrlSubsystem()

		// resctrl is only supported by linux
		if runtime.GOOS != "linux" {
			assert.Equal(t, false, got)
			assert.EqualError(t, err, "only support linux")
			return
		}

		assert.Equal(t, false, got)
		assert.NoError(t, err)
	})
}
