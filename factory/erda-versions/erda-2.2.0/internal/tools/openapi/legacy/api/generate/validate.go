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

package main

import (
	"net/url"
	"regexp"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
	"github.com/erda-project/erda/pkg/strutil"

	"github.com/pkg/errors"
	"gopkg.in/yaml.v2"
)

func validate(r *apis.ApiSpec) error {
	scheme, err := spec.SchemeFromString(r.Scheme)
	if err != nil {
		return err
	}
	if r.Method == "" && strutil.ToLower(r.Scheme) != "ws" {
		return errors.New("Method field must not be empty")
	}
	if r.Host == "" && r.Custom == nil {
		return errors.New("Host field must not be empty")
	}
	if err := validateURL(r.Scheme + "://" + r.Host); err != nil {
		return err
	}
	if r.K8SHost != "" {
		if err := validateURL(strutil.Concat(r.Scheme, "://", r.K8SHost)); err != nil {
			return err
		}
	}
	if err := validatePath(r.Path); err != nil {
		return err
	}
	s := &spec.Spec{
		Path:           spec.NewPath(r.Path),
		BackendPath:    spec.NewPath(r.BackendPath),
		Host:           r.Host,
		Scheme:         scheme,
		Custom:         r.Custom,
		CustomResponse: r.CustomResponse,
		CheckLogin:     r.CheckLogin,
	}
	if err := s.Validate(); err != nil {
		return err
	}
	return nil

}

func validateURL(s string) error {
	_, err := url.Parse(s)
	return err
}

func validatePath(s string) error {
	r := regexp.MustCompile("<.*?>")
	s_ := r.ReplaceAllString(s, "<dummy>")

	if strutil.Contains(s_, "_") {
		return errors.New("validate Path: should '-' instead of '_'")
	}
	return nil
}

type SwaggerDoc struct {
	Summary   string                 `yaml:"summary"`
	Produces  interface{}            `yaml:"produces"`
	Responses map[string]interface{} `yaml:"responses"`
}

func validateDoc(api apis.ApiSpec) error {
	var doc SwaggerDoc
	if err := yaml.Unmarshal([]byte(api.Doc), &doc); err != nil {
		return errors.Wrap(err, api.Path)
	}
	if doc.Summary == "" {
		return errors.Wrap(errors.New("need to provide [summary]"), api.Path)
	}
	if doc.Produces == nil {
		return errors.Wrap(errors.New("need to provide [produces]"), api.Path)
	}
	if doc.Responses == nil {
		return errors.Wrap(errors.New("need to provide [responses]"), api.Path)
	}
	if _, ok := doc.Responses["200"]; !ok {
		return errors.Wrap(errors.New("need to provide [200 responses]"), api.Path)
	}
	return nil
}
