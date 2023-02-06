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

package diceyml

import (
	"bytes"
	"encoding/json"
	"fmt"
	"regexp"
	"strings"

	"github.com/pkg/errors"
	"sigs.k8s.io/yaml"

	"github.com/erda-project/erda/pkg/strutil"
)

type EnvType int

const (
	BaseEnv EnvType = iota
	DevEnv
	TestEnv
	StagingEnv
	ProdEnv
)

func (e EnvType) String() string {
	switch e {
	case BaseEnv:
		return "base"
	case DevEnv:
		return "development"
	case TestEnv:
		return "test"
	case StagingEnv:
		return "staging"
	case ProdEnv:
		return "production"
	default:
		panic("should not be here!")
	}
}

type DiceYaml struct {
	data     []byte
	obj      *Object
	validate bool
}

// 得到的 DiceYaml 是环境无关的，不能用于获取环境相关的信息
func New(b []byte, validate bool) (*DiceYaml, error) {

	d := &DiceYaml{
		data:     b,
		validate: validate,
	}
	if err := d.Parse(validate); err != nil {
		return nil, err
	}
	return d, nil
}

// 得到对应部署环境的 DiceYaml，可用于部署
func NewDeployable(b []byte, env string, validate bool) (*DiceYaml, error) {
	d := &DiceYaml{
		data:     b,
		validate: validate,
	}
	if err := d.MergeEnv(env); err != nil {
		return nil, err
	}
	if validate {
		if err := validateE(d.obj, d.data); err != nil {
			return nil, err
		}
	}
	return d, nil
}

func (d *DiceYaml) Parse(validate bool) error {
	// diceyml 中存在变量时，基于默认值进行渲染
	data, err := d.getDefaultValueData()
	if err != nil {
		return err
	}
	var obj Object
	if err := yaml.Unmarshal(data, &obj); err != nil {
		return errors.Wrap(err, "fail to yaml unmarshal")
	}
	polish(&obj)
	if validate {
		if err := validateE(&obj, data); err != nil {
			return err
		}
	}

	d.obj = &obj
	return nil
}

// validate result is a map , key: regexp of problem-position-element, value: error info
// NOTE: why use regex to mark the position of problem-elements ?
//   because the yaml package not expose the line information
type ValidateError map[*regexp.Regexp]error

func (d *DiceYaml) Validate() ValidateError {
	data, err := d.getDefaultValueData()
	verr := ValidateError{}
	if err != nil {
		verr[yamlHeaderRegex("_0")] = errors.Wrap(err, "fail to get default value")
		return verr
	}
	var obj Object
	if err := yaml.Unmarshal(data, &obj); err != nil {
		verr[yamlHeaderRegex("_0")] = errors.Wrap(err, "fail to yaml unmarshal")
		return verr
	}
	verr = validate(&obj, data)
	return verr
}

func (d *DiceYaml) Obj() *Object {
	return CopyObj(d.obj)
}

func normalizeEnvStr(env string) (string, error) {
	lower := strutil.ToLower(env)
	var converted string
	switch {
	case strutil.HasPrefixes(lower, "dev"):
		converted = string(WS_DEV)
	case strutil.HasPrefixes(lower, "test"):
		converted = string(WS_TEST)
	case strutil.HasPrefixes(lower, "staging"):
		converted = string(WS_STAGING)
	case strutil.HasPrefixes(lower, "prod"):
		converted = string(WS_PROD)
	default:
		return "", errors.New("invalid ENV")
	}
	return converted, nil
}

func (d *DiceYaml) MergeEnv(env string) error {
	converted, err := normalizeEnvStr(env)
	if err != nil {
		return err
	}
	err = d.mergeEnvValues(converted)
	if err != nil {
		return errors.Wrap(err, "merge value failed")
	}
	MergeEnv(d.obj, converted)
	polish(d.obj)
	return nil
}

func (d *DiceYaml) mergeEnvValues(env string) error {
	dbytes, err := d.getEnvValueData(env)
	if err != nil {
		return err
	}
	var obj Object
	if err := yaml.Unmarshal(dbytes, &obj); err != nil {
		return errors.Wrap(err, "fail to yaml unmarshal")
	}
	obj.Values = nil
	polish(&obj)
	d.obj = &obj
	d.data = dbytes
	return nil
}

type valuesPartial struct {
	Values ValueObjects `yaml:"values"`
}

// 不传 env 时，聚合所有环境的 values 返回
func (d *DiceYaml) extractValues(env ...string) (map[string]string, error) {
	var partial valuesPartial
	if err := yaml.Unmarshal(d.data, &partial); err != nil {
		return nil, errors.Wrap(err, "fail to yaml unmarshal")
	}
	valueMap := map[string]string{}
	if len(env) > 0 {
		normalizedEnvStr, err := normalizeEnvStr(env[0])
		normalizedEnv := WorkspaceStr(normalizedEnvStr)
		if err != nil {
			return nil, err
		}
		if partial.Values[normalizedEnv] != nil {
			valueMap = *partial.Values[normalizedEnv]
		}
		return valueMap, nil
	}
	for _, kv := range partial.Values {
		if kv == nil {
			continue
		}
		for key, value := range *kv {
			valueMap[key] = value
		}
	}
	return valueMap, nil

}

func (d *DiceYaml) getDefaultValueData() ([]byte, error) {
	return d.getEnvValueData()
}

var matchRegex = regexp.MustCompile(`\$\{[\w-]+(:[^\}]*)?\}`)
var keyRegex = regexp.MustCompile(`[\w-]+`)
var valueRegex = regexp.MustCompile(`:[^\}]*`)
var reservedExpressionPrefix = []string{"env."}

func (d *DiceYaml) getEnvValueData(env ...string) ([]byte, error) {
	valueMap, err := d.extractValues(env...)
	if err != nil {
		return nil, err
	}
	// 不传环境时，以 default 为优先
	priorUseDefault := len(env) == 0
	return matchRegex.ReplaceAllFunc(d.data, func(match []byte) []byte {
		key := keyRegex.Find(match)

		for _, prefix := range reservedExpressionPrefix {
			if bytes.HasPrefix(bytes.TrimSpace(key), []byte(prefix)) {
				return match
			}
		}

		var value, findValue, defaultValue []byte
		if find, ok := valueMap[string(key)]; ok {
			findValue = []byte(find)
		}
		defaultValue = valueRegex.Find(match)
		if defaultValue == nil && findValue == nil {
			return match
		}
		if defaultValue != nil && (priorUseDefault || findValue == nil) {
			// delete ":"
			value = defaultValue[1:]
		} else {
			value = findValue
		}
		return []byte(strings.TrimSpace(string(value)))
	}), nil

}

func (d *DiceYaml) Compose(env string, yml *DiceYaml) error {
	if d.obj == nil {
		return errors.New("modify dice.yml base on raw bytes is not allowed")
	}
	Compose(d.obj, yml.obj, env)
	polish(d.obj)
	return nil
}

func (d *DiceYaml) InsertImage(images map[string]string, envs map[string]map[string]string) error {
	if d.obj == nil {
		return errors.New("modify dice.yml base on raw bytes is not allowed")
	}
	if err := InsertImage(d.obj, images, envs); err != nil {
		return err
	}
	polish(d.obj)
	return nil
}

// images: map[servicename]map[sidecarname]image
func (d *DiceYaml) InsertSideCarImage(images map[string]map[string]string) error {
	if d.obj == nil {
		return errors.New("modify dice.yml base on raw bytes is not allowed")
	}
	if err := InsertSideCarImage(d.obj, images); err != nil {
		return err
	}
	polish(d.obj)
	return nil
}

func (d *DiceYaml) InsertAddonOptions(env EnvType, addon string, options map[string]string) error {
	if d.obj == nil {
		return errors.New("modify dice.yml base on raw bytes is not allowed")
	}
	InsertAddonOptions(d.obj, env, addon, options)
	polish(d.obj)
	return nil
}

func (d *DiceYaml) JSON() (string, error) {
	b, err := json.Marshal(d.obj)
	if err != nil {
		return "", err
	}
	return string(b), nil
}

func (d *DiceYaml) YAML() (string, error) {
	b, err := yaml.Marshal(d.obj)
	if err != nil {
		return "", err
	}
	return string(b), nil
}

func polish(obj *Object) {
	SetDefaultValue(obj)
	CompatibleExpose(obj)
}

func validateE(obj *Object, raw []byte) error {
	errs := validate(obj, raw)
	if len(errs) > 0 {
		es := []string{}
		for _, e := range errs {
			es = append(es, e.Error())
		}
		s, err := prettyPrint(es)
		if err != nil {
			return err
		}
		return fmt.Errorf("%v", s)
	}
	return nil
}

func validate(obj *Object, raw []byte) ValidateError {
	errs := ValidateError{}

	errs = FieldnameValidate(obj, raw)
	if len(errs) > 0 {
		return errs
	}
	errsNameCheck := ServiceNameCheck(obj)
	if len(errsNameCheck) > 0 {
		errs = mergeValidateErr(errs, errsNameCheck)
	}
	errsBasicValidate := BasicValidate(obj)
	if len(errsBasicValidate) > 0 {
		errs = mergeValidateErr(errs, errsBasicValidate)
	}
	lacks := FindLackService(obj)
	if len(lacks) > 0 {
		errs = mergeValidateErr(errs, lacks)
	}
	checkEnvSize := CheckEnvSize(obj)
	if len(checkEnvSize) > 0 {
		errs = mergeValidateErr(errs, checkEnvSize)
	}

	return errs

}

func prettyPrint(v interface{}) (string, error) {
	b, err := json.MarshalIndent(v, "", "  ")
	if err != nil {
		return "", err
	}
	return string(b), nil
}

func mergeValidateErr(major, minor ValidateError) ValidateError {
	r := minor
	for i, j := range major {
		r[i] = j
	}
	return r
}
