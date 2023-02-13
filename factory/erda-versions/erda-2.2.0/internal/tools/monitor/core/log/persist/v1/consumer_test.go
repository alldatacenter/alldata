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

package storage

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	logmodule "github.com/erda-project/erda/internal/tools/monitor/core/log"
)

func Test_provider_processLog(t *testing.T) {
	mp := mockProvider()
	ass := assert.New(t)

	// default
	log := &logmodule.Log{
		ID:        "aaa",
		Source:    "container",
		Stream:    "",
		Offset:    0,
		Timestamp: 0,
		Content:   "",
		Tags:      nil,
	}
	mp.processLog(log)
	ass.Equal("INFO", log.Tags["level"])
	ass.Equal("stdout", log.Stream)

	// upper level
	log = &logmodule.Log{
		ID:        "aaa",
		Source:    "container",
		Stream:    "",
		Offset:    0,
		Timestamp: 0,
		Content:   "",
		Tags:      map[string]string{"level": "error"},
	}
	mp.processLog(log)
	ass.Equal("ERROR", log.Tags["level"])
	ass.Equal("stdout", log.Stream)

	// id keys
	mp.Cfg.Output.IDKeys = []string{"id_key1"}
	log = &logmodule.Log{
		ID:        "aaa",
		Source:    "container",
		Stream:    "",
		Offset:    0,
		Timestamp: 0,
		Content:   "",
		Tags:      map[string]string{"id_key1": "bbb"},
	}
	mp.processLog(log)
	ass.Equal("bbb", log.ID)

	// time key
	now := time.Unix(1652948267, 0)
	log = &logmodule.Log{
		ID:      "aaa",
		Source:  "container",
		Stream:  "",
		Offset:  0,
		Time:    &now,
		Content: "",
		Tags:    map[string]string{},
	}
	mp.processLog(log)
	ass.Equal(log.Timestamp, int64(1652948267000000000))
	ass.Equal(log.Time, (*time.Time)(nil))
}

func Test_provider_invokeV2(t *testing.T) {
	mp := mockProvider()
	mw := &mockWriter{}
	mp.output = mw
	ass := assert.New(t)

	log := &logmodule.Log{
		ID:        "aaa",
		Source:    "container",
		Stream:    "stdout",
		Offset:    1024,
		Timestamp: time.Now().UnixNano(),
		Content:   "hello world",
		Tags:      map[string]string{"level": "INFO", diceOrgNameKey: "abc"},
	}

	value, err := json.Marshal(log)
	ass.Nil(err)
	err = mp.invoke(nil, value, nil, time.Now())
	ass.Nil(err)
	ass.Equal(&logmodule.Meta{
		ID:     log.ID,
		Source: log.Source,
		Tags:   log.Tags,
	}, mw.datas[0])
	ass.Equal(log, mw.datas[1].(*logmodule.Log))

	// bad value
	err = mp.invoke(nil, []byte(`bad value`), nil, time.Now())
	ass.Error(err)

	// log invalid
	mw = &mockWriter{}
	mp.output = mw
	mp.schema = &mockLogSchema{validateOrgTriggered: true}
	err = mp.invoke(nil, value, nil, time.Now())
	ass.Nil(err)
	ass.Empty(mw.datas)
}

type mockWriter struct {
	datas []interface{}
}

func (m *mockWriter) Write(data interface{}) error {
	m.datas = append(m.datas, data)
	return nil
}

func (m *mockWriter) WriteN(data ...interface{}) (int, error) {
	m.datas = append(m.datas, data...)
	return len(data), nil
}

func (m *mockWriter) Close() error {
	return nil
}
