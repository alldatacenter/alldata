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

package testngxml

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

//import (
//	"encoding/json"
//	"fmt"
//	"io/ioutil"
//	"testing"
//
//	"github.com/sirupsen/logrus"
//	"github.com/stretchr/testify/assert"
//)
//
//func TestParse(t *testing.T) {
//	filename := "testng-results.xml"
//	r, err := (NgParser{}).Parse("127.0.0.1:9009", "accesskey", "secretkey", "test1", filename)
//	assert.Nil(t, err)
//
//	js, err := json.Marshal(r)
//	assert.Nil(t, err)
//	logrus.Info(string(js))
//}
//
//func TestIngest(t *testing.T) {
//	bs, err := ioutil.ReadFile("../testdata/testng-results.xml")
//	assert.Nil(t, err)
//
//	ng, err := Ingest(bs)
//	assert.Nil(t, err)
//
//	js, _ := json.Marshal(ng)
//	logrus.Info(string(js))
//
//	fmt.Println(string(js))
//
//}

func TestTransfer(t *testing.T) {
	result := &NgTestResult{
		Suites: []*Suite{
			{
				Name: "test1",
				Tests: []*Test{
					{
						Name: "test1",
					},
				},
			},
		},
	}
	suites, err := result.Transfer()
	assert.NoError(t, err)
	assert.Equal(t, 1, len(suites))
}
