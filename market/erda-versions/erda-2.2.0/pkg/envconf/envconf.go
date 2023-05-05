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

// Package envconf 从环境变量加载配置项用于初始化具体的配置对象。
// 用法：
// type Config struct {
//		Addr  string `env:"ADDR" default:""`
//		Level string `env:"LEVEL" required:"true"`
// }
//
// config := &Config{}
// if err := Load(config); err != nil {
//		return err
// }
//
// 配置项支持的类型有：
//	bool
//	int
//	int64
//  uint64
//	float64
//	string
//	time.Duration
//	其他非基本类型使用json解析
//
package envconf

import (
	"bytes"
	"encoding/json"
	"os"
	"reflect"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
)

// MustLoad 必须 Load 配置成功，失败直接 panic.
func MustLoad(obj interface{}) {
	if err := Load(obj); err != nil {
		panic(err)
	}
}

// Load 分析配置对象 obj，并从环境变量里获取值初始化配置对象.
func Load(obj interface{}, envOpts ...map[string]string) error {
	typ := reflect.TypeOf(obj)
	if typ.Kind() != reflect.Ptr {
		return errors.New("not a pointer")
	}
	val := reflect.ValueOf(obj)

	num := typ.Elem().NumField()

	keyRegexStr := `^[A-Z][A-Z0-9_]*$`
	keyRegex := regexp.MustCompile(keyRegexStr)

	// use passed in envs if have
	var envs map[string]string
	var usePassedInEnvs bool
	if len(envOpts) > 0 {
		usePassedInEnvs = true
		envs = envOpts[0]
	}

	for i := 0; i < num; i++ {
		typeField := typ.Elem().Field(i)
		valueField := val.Elem().Field(i)

		key := typeField.Tag.Get("env")

		// tag: env 不存在，该字段不需要解析
		if key == "" {
			continue
		}

		match := keyRegex.MatchString(key)
		if !match {
			return errors.Errorf("failed to match \"%s\", key: %s", keyRegex, key)
		}
		value := typeField.Tag.Get("default")

		if usePassedInEnvs {
			value = envs[key]
		} else {
			if v := os.Getenv(key); v != "" {
				value = v
			}
		}
		value = strings.TrimSpace(value)

		// tag: required 表示 value 不能为空
		if strings.EqualFold(typeField.Tag.Get("required"), "true") && value == "" {
			return errors.Errorf("failed to found required environment variable, key: %s", key)
		}

		// 没有声明 required 且 value 为空，则使用对应类型的零值
		if value == "" {
			continue
		}

		switch typeField.Type.Kind() {
		case reflect.String:
			valueField.SetString(value)

		case reflect.Int:
			n, err := strconv.Atoi(value)
			if err != nil {
				return err
			}
			valueField.Set(reflect.ValueOf(n))

		case reflect.Int64:
			if valueField.Type().String() == "time.Duration" {
				d, err := time.ParseDuration(value)
				if err != nil {
					return err
				}
				valueField.Set(reflect.ValueOf(d))
			} else {
				n, err := strconv.ParseInt(value, 10, 64)
				if err != nil {
					return err
				}
				valueField.SetInt(n)
			}

		case reflect.Uint64:
			n, err := strconv.ParseUint(value, 10, 64)
			if err != nil {
				return err
			}
			valueField.SetUint(n)

		case reflect.Float64:
			n, err := strconv.ParseFloat(value, 64)
			if err != nil {
				return err
			}
			valueField.SetFloat(n)

		case reflect.Bool:
			switch strings.ToLower(value) {
			case "true":
				valueField.SetBool(true)
			case "false":
				valueField.SetBool(false)
			}

		default:
			instance := reflect.New(valueField.Type())
			buf := bytes.NewBufferString(value)
			d := json.NewDecoder(buf)
			d.UseNumber()
			err := d.Decode(instance.Interface())
			if err != nil {
				return errors.Errorf("failed to parse json ENV: %s, value: %s, err: %v", key, value, err)
			}
			valueField.Set(instance.Elem())
		}
	}
	return nil
}
