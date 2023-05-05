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

package exdto

import (
	"fmt"
	"regexp"
	"strings"

	"github.com/pkg/errors"
)

// RedirectType
const (
	RT_SERVICE = "service"
	RT_URL     = "url"
)

type OpenapiDto struct {
	ApiPath             string `json:"apiPath"`
	RedirectType        string `json:"redirectType"`
	RedirectAddr        string `json:"redirectAddr"`
	RedirectPath        string `json:"redirectPath"`
	RedirectApp         string `json:"redirectApp"`
	RedirectService     string `json:"redirectService"`
	RedirectRuntimeId   string `json:"redirectRuntimeId"`
	RedirectRuntimeName string `json:"redirectRuntimeName"`
	Method              string `json:"method,omitempty"`
	AllowPassAuth       bool   `json:"allowPassAuth"`
	//	AclType            string   `json:"aclType"`
	Description        string   `json:"description"`
	AdjustPath         string   `json:"-"`
	AdjustRedirectAddr string   `json:"-"`
	ServiceRewritePath string   `json:"-"`
	IsRegexPath        bool     `json:"-"`
	RouteId            string   `json:"-"`
	ServiceId          string   `json:"-"`
	ZoneId             string   `json:"-"`
	ProjectId          string   `json:"-"`
	Env                string   `json:"-"`
	RuntimeServiceId   string   `json:"-"`
	Hosts              []string `json:"hosts"`
}

const varSlot = ""
const (
	processStrStatus = iota
	processVarStatus
)

func (dto OpenapiDto) pathVariableSplit(path string) ([]string, []string, error) {
	status := processStrStatus
	var rawPaths []string
	var vars []string
	var items []byte
	for i := 0; i < len(path); i++ {
		c := path[i]
		if status == processStrStatus {
			if c == '{' {
				status = processVarStatus
				if len(items) > 0 {
					rawPaths = append(rawPaths, string(items))
					items = nil
				}
			} else if c == '}' {
				return nil, nil, errors.Errorf("invalid path:%s", path)
			} else {
				items = append(items, c)
			}
		} else if status == processVarStatus {
			if c == '{' {
				return nil, nil, errors.Errorf("invalid path:%s", path)
			} else if c == '}' {
				status = processStrStatus
				if len(items) > 0 {
					vars = append(vars, string(items))
					rawPaths = append(rawPaths, varSlot)
					items = nil
				} else {
					return nil, nil, errors.Errorf("invalid path:%s", path)
				}
			} else {
				items = append(items, c)
			}
		}
	}
	if status != processStrStatus {
		return nil, nil, errors.Errorf("invalid path:%s", path)
	}
	if len(items) > 0 {
		rawPaths = append(rawPaths, string(items))
	}
	return rawPaths, vars, nil
}

func (dto *OpenapiDto) pathVariableReplace(rawPaths []string, vars *[]string) (string, error) {
	if len(*vars) == 0 {
		return "", errors.New("invalid vars")
	}
	varIndex := 0
	varRegex := `[^/]+`
	for i, item := range rawPaths {
		if item == varSlot {
			varValue := (*vars)[varIndex]
			colonIndex := strings.Index(varValue, ":")
			if colonIndex != -1 {
				varRegex = varValue[colonIndex+1:]
				varValue = varValue[:colonIndex]
			}
			(*vars)[varIndex] = varValue
			rawPaths[i] = fmt.Sprintf(`(?<%s>%s)`, varValue, varRegex)
			varIndex++
		}
		if varIndex > len(*vars) {
			return "", errors.Errorf("invalid varIndex[%d] of vars[%+v]", varIndex, vars)
		}
	}
	return strings.Join(rawPaths, ""), nil
}

func (dto *OpenapiDto) CheckValid() (bool, string) {
	if dto.RedirectType == RT_URL {
		if ok, _ := regexp.MatchString(`^(http://|https://)[0-9a-zA-z-_\.:]+$`, dto.RedirectAddr); !ok {
			return false, fmt.Sprintf("invalid redirect addr: %s", dto.RedirectAddr)
		}
		if ok, _ := regexp.MatchString(`^[/0-9a-zA-z-_\.}{]+$`, dto.RedirectPath); !ok {
			return false, fmt.Sprintf("invalid redirect path: %s", dto.RedirectPath)
		}
	} else if dto.RedirectType == RT_SERVICE {
		if dto.RuntimeServiceId == "" && (dto.RedirectApp == "" || dto.RedirectService == "" || dto.RedirectRuntimeId == "") {
			return false, fmt.Sprintf("invalid dto: %+v", dto)
		}
	} else {
		return false, fmt.Sprintf("invalid redirect type: %s", dto.RedirectType)
	}
	return true, ""
}

func (dto *OpenapiDto) Adjust() error {
	if dto.RedirectType == RT_SERVICE {
		return nil
	}
	dto.RedirectPath = strings.Replace(dto.RedirectPath, "//", "/", -1)
	dto.RedirectAddr = strings.TrimSuffix(dto.RedirectAddr, "/") + "/" + strings.TrimPrefix(dto.RedirectPath, "/")
	if strings.HasSuffix(dto.ApiPath, "/") {
		validPath := strings.TrimSuffix(dto.ApiPath, "/")
		dto.ApiPath = validPath
	}
	dto.ApiPath = strings.Replace(dto.ApiPath, "//", "/", -1)
	if dto.ApiPath == "" {
		dto.ApiPath = "/"
	}
	findBegin := strings.Index(dto.RedirectAddr, "://")
	if findBegin < 0 {
		return errors.Errorf("invalid dto:%+v", dto)
	}
	firstSlash := strings.Index(dto.RedirectAddr[findBegin+3:], "/")
	servicePath := ""
	if firstSlash > -1 {
		servicePath = dto.RedirectAddr[findBegin+3+firstSlash:]
	} else {
		firstSlash = 0
	}
	rawPaths, vars, err := dto.pathVariableSplit(dto.ApiPath)
	if err != nil {
		return err
	}
	serviceRawPaths, serviceVars, err := dto.pathVariableSplit(servicePath)
	if err != nil {
		return err
	}
	dto.AdjustPath = dto.ApiPath
	if len(vars) > 0 {
		dto.IsRegexPath = true
		adjustPath, err := dto.pathVariableReplace(rawPaths, &vars)
		if err != nil {
			return err
		}
		dto.AdjustPath = adjustPath
	}
	dto.AdjustRedirectAddr = dto.RedirectAddr
	if len(serviceVars) > 0 {
		varIndex := 0
		for i, item := range serviceRawPaths {
			if item == varSlot {
				varValue := serviceVars[varIndex]
				colonIndex := strings.Index(varValue, ":")
				if colonIndex != -1 {
					varValue = varValue[:colonIndex]
				}
				serviceRawPaths[i] = fmt.Sprintf(`{%s}`, varValue)
				serviceVars[varIndex] = varValue
				varIndex++
			}
			if varIndex > len(serviceVars) {
				return errors.Errorf("invalid servicePath[%s]", servicePath)
			}
		}
		servicePath = strings.Join(serviceRawPaths, "")
		for _, serviceVarItem := range serviceVars {
			exist := false
			for _, varItem := range vars {
				if varItem == serviceVarItem {
					exist = true
					break
				}
			}
			if !exist {
				return errors.Errorf("service var[%s] not exists in vars[%+v] of path[%s]",
					serviceVarItem, vars, dto.ApiPath)
			}
		}
		dto.AdjustRedirectAddr = dto.RedirectAddr[:findBegin+3+firstSlash]
		dto.RedirectAddr = dto.AdjustRedirectAddr + servicePath
		dto.ServiceRewritePath = servicePath
	}
	return nil
}
