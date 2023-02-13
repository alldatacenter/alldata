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

package sqllint

import (
	"github.com/pingcap/parser"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

type Linter struct {
	errs    map[string]LintInfo
	reports map[string]map[string][]string
	configs map[string]Config
}

func New(configs map[string]Config) *Linter {
	return &Linter{
		errs:    make(map[string]LintInfo),
		reports: make(map[string]map[string][]string),
		configs: configs,
	}
}

func (r *Linter) Input(moduleName, scriptName string, scriptData []byte) error {
	nodes, warns, err := parser.New().Parse(string(scriptData), "", "")
	if err != nil {
		return err
	}

	s := script.New(scriptName, scriptData)
	r.reports[scriptName] = make(map[string][]string)
	var errs []error
	for _, cfg := range r.configs {
		// retrieve factory method
		factory, ok := Get().Load(cfg.Name)
		if !ok {
			return errors.Errorf("not implement lint: %s, please check your config file", cfg.Name)
		}

		// lint on this script ?
		if cfg.DoNotLintOn(moduleName, scriptName) {
			continue
		}

		// lint on every node in the script
		for _, node := range nodes {
			// generate the lint rule
			rule, err := factory(s, cfg)
			if err != nil {
				return errors.Wrapf(err, "failed to generate the lint rule, lint alias: %s", cfg.Alias)
			}
			if rule == nil {
				return errors.Errorf("failed to generate the lint rule, lint name: %s", cfg.Name)
			}

			// node accept the rule
			node.Accept(rule)
			err = rule.Error()
			if err == nil {
				continue
			}
			errs = append(errs, err)
			lintErr, ok := err.(linterror.LintError)
			if !ok {
				continue
			}
			if stmtName := lintErr.StmtName(); stmtName != "" {
				r.reports[scriptName][stmtName] = append(r.reports[scriptName][stmtName], lintErr.Lint)
			}

		}
	}

	if _, ok := r.errs[scriptName]; !ok {
		r.errs[scriptName] = LintInfo{}
	}
	if len(warns) > 0 {
		lintInfo := r.errs[scriptName]
		lintInfo.Warns = append(lintInfo.Warns, warns...)
		r.errs[scriptName] = lintInfo
	}
	if len(errs) > 0 {
		lintInfo := r.errs[scriptName]
		lintInfo.Lints = append(lintInfo.Lints, errs...)
		r.errs[scriptName] = lintInfo
	}

	return nil
}

func (r *Linter) Errors() map[string]LintInfo {
	return r.errs
}

func (r *Linter) Report() string {
	data, err := yaml.Marshal(r.reports)
	if err != nil {
		return ""
	}
	return string(data)
}

type factoryError struct {
	error
}

type LintInfo struct {
	Lints, Warns []error
}
