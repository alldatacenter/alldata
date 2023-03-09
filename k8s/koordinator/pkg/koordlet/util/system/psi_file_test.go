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
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestGetPSIRecords(t *testing.T) {
	helper := NewFileTestUtil(t)
	helper.CreateFile("cpu.pressure")
	helper.WriteFileContents("cpu.pressure", "some avg10=0.00 avg60=0.00 avg300=0.00 total=0\nfull avg10=0.00 avg60=0.00 avg300=0.00 total=0")

	assert.NotPanics(t, func() {
		_, err := ReadPSI(helper.TempDir + "/cpu.pressure")
		if err != nil {
			return
		}
	})
}

func TestGetPSIRecords_wrongSomeFormat(t *testing.T) {
	helper := NewFileTestUtil(t)
	helper.CreateFile("cpu.pressure")
	helper.WriteFileContents("cpu.pressure", "some avg10=0.00 total=0\nfull avg10=0.00 avg60=0.00 avg300=0.00 total=0")

	_, err := ReadPSI(helper.TempDir + "/cpu.pressure")
	assert.NotNil(t, err)
}

func TestGetPSIRecords_wrongFullFormat(t *testing.T) {
	helper := NewFileTestUtil(t)
	helper.CreateFile("cpu.pressure")
	helper.WriteFileContents("cpu.pressure", "some avg10=0.00 total=0\nfull 0.00 avg300=0.00 total=0")

	_, err := ReadPSI(helper.TempDir + "/cpu.pressure")
	assert.NotNil(t, err)
}

func TestGetPSIRecords_wrongPrefix(t *testing.T) {
	helper := NewFileTestUtil(t)
	helper.CreateFile("cpu.pressure")
	helper.WriteFileContents("cpu.pressure", "wrong avg10=0.00 total=0\nfull 0.00 avg300=0.00 total=0")

	_, err := ReadPSI(helper.TempDir + "/cpu.pressure")
	assert.NotNil(t, err)
}

func TestGetPSIRecords_FullNotSupported(t *testing.T) {
	helper := NewFileTestUtil(t)
	helper.CreateFile("cpu.pressure")
	helper.WriteFileContents("cpu.pressure", "some avg10=0.00 avg60=0.00 avg300=0.00 total=0\n")

	psi, err := ReadPSI(helper.TempDir + "/cpu.pressure")
	assert.Nil(t, err)
	assert.Equal(t, false, psi.FullSupported)
}
