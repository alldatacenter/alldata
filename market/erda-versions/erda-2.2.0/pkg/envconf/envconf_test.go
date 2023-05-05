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

package envconf

import (
	"encoding/json"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

type Config struct {
	TestBool     bool           `env:"BOOL"`
	TestString   string         `env:"STRING"`
	TestInt1     int            `env:"INT1" default:"1234"`
	TestInt      int            `env:"INT"`
	TestDuration time.Duration  `env:"DURATION" default:"12h24m36s"`
	TestJsonMap  map[string]int `env:"MAP" default:"{}"`
}

func TestLoad(t *testing.T) {
	os.Setenv("BOOL", "true")
	os.Setenv("STRING", "hello")
	os.Setenv("INT", "4321")
	os.Setenv("MAP", "{\"age\":10}")

	defer func() {
		os.Unsetenv("BOOL")
		os.Unsetenv("STRING")
		os.Unsetenv("INT")
		os.Unsetenv("MAP")
	}()

	config := &Config{}
	err := Load(config)
	assert.Nil(t, err)

	assert.Equal(t, true, config.TestBool)
	assert.Equal(t, "hello", config.TestString)
	assert.Equal(t, 1234, config.TestInt1)
	assert.Equal(t, 4321, config.TestInt)
	assert.Equal(t, time.Hour*12+time.Minute*24+time.Second*36, config.TestDuration)
	assert.Equal(t, 10, config.TestJsonMap["age"])

	// update duration
	os.Setenv("DURATION", "1s")
	defer os.Unsetenv("DURATION")

	err = Load(config)
	assert.Nil(t, err)

	assert.Equal(t, time.Second, config.TestDuration)
}

func TestLoadOnError(t *testing.T) {
	var n int
	err := Load(n)
	assert.NotNil(t, err)
}

type ConfigRequired struct {
	Required string `env:"REQUIRED" required:"true"`
}

func TestLoadRequired(t *testing.T) {
	config := &ConfigRequired{}
	err := Load(config)
	assert.NotNil(t, err)
	t.Log(err)

	// set empty value is useless
	os.Setenv("REQUIRED", "")
	defer os.Unsetenv("REQUIRED")

	err = Load(config)
	assert.NotNil(t, err)
	t.Log(err)

	// set not empty value for required tag
	os.Setenv("REQUIRED", "required")
	defer os.Unsetenv("REQUIRED")

	err = Load(config)
	assert.Nil(t, err)

	assert.Equal(t, "required", config.Required)
}

type ConfigNotSet struct {
	RedisPort        int    `env:"REDIS_PORT"`
	RedisPassword    string `env:"REDIS_PASSWORD"`
	RedisClusterMode bool   `env:"REDIS_CLUSTER_MODE"`
}

func TestLoadNotSet(t *testing.T) {
	config := &ConfigNotSet{}
	err := Load(config)
	assert.Nil(t, err)
	assert.Equal(t, 0, config.RedisPort)
	assert.Equal(t, "", config.RedisPassword)
	assert.Equal(t, false, config.RedisClusterMode)
}

func TestLoadBigNumber(t *testing.T) {
	type APIParam struct {
		Key   string      `json:"key"`
		Value interface{} `json:"value"`
		Desc  string      `json:"desc"`
	}
	type Object struct {
		Params []APIParam `env:"ACTION_PARAMS"`
	}
	_ = os.Setenv("ACTION_PARAMS", `[{"key":"orderId","value":1352141084883972097}]`)

	var obj Object
	err := Load(&obj)
	assert.NoError(t, err)
	// json decoder 若不指定 useNumber()，则转换为 float64(1.352141084883972e+18), 导致 1352141084883972097 => 1352141084883972000, 精度丢失
	assert.Equal(t, json.Number("1352141084883972097"), obj.Params[0].Value)
}
