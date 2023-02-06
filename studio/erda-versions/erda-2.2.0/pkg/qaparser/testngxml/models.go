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
	"fmt"
	"strings"
	"time"

	"github.com/erda-project/erda-proto-go/dop/qa/unittest/pb"
	"github.com/erda-project/erda/apistructs"
)

type NgStatus string

const (
	NgPassed  NgStatus = "PASS"
	NgFailed  NgStatus = "FAIL"
	NgSkipped NgStatus = "SKIP"

	NgTotalSkip   NgStatus = "skipped"
	NgTotalFail   NgStatus = "failed"
	NgTotalPassed NgStatus = "passed"
	NgTotalIgnore NgStatus = "ignored"
	NgTotal       NgStatus = "total"
)

func (s NgStatus) ToStatus() apistructs.TestStatus {
	switch s {
	case NgPassed:
		return apistructs.TestStatusPassed
	case NgFailed:
		return apistructs.TestStatusFailed
	case NgSkipped:
		return apistructs.TestStatusSkipped
		// just for no compile error
	default:
		return ""
	}
}

type NgTestResult struct {
	Skipped        int             `json:"skipped" yaml:"skipped"`
	Failed         int             `json:"failed" yaml:"failed"`
	Ignored        int             `json:"ignored"`
	Total          int             `json:"total"`
	Passed         int             `json:"passed"`
	ReporterOutput *ReporterOutput `json:"output"`
	Suites         []*Suite        `json:"suite"`
}

type Suite struct {
	Name       string        `json:"name"`
	Duration   time.Duration `json:"duration"`
	StartedAt  string        `json:"startedAt"`
	FinishedAt string        `json:"finishedAt"`
	Groups     string        `json:"groups"` // ignored
	Tests      []*Test       `json:"test"`
}

type Test struct {
	Name       string        `json:"name"`
	Duration   time.Duration `json:"duration"`
	StartedAt  string        `json:"startedAt"`
	FinishedAt string        `json:"finishedAt"`
	Classes    []*Class      `json:"class"`
}

type Class struct {
	Name    string        `json:"name"`
	Methods []*TestMethod `json:"methods"`
}

type TestMethod struct {
	Status         NgStatus        `json:"status"`
	Signature      string          `json:"signature"`
	Name           string          `json:"name"`
	IsConfig       bool            `json:"isConfig"`
	Duration       time.Duration   `json:"duration"`
	StartedAt      string          `json:"startedAt"`
	FinishedAt     string          `json:"finishedAt"`
	DataProvider   string          `json:"dataProvider"`
	ReporterOutput *ReporterOutput `json:"output"`
	Params         []*Param        `json:"params"`
	Exception      *Exception      `json:"exception"`
}

type ReporterOutput struct {
	Lines []string `json:"line" xml:"line"`
}

type Param struct {
	Index int    `json:"index"`
	Value string `json:"value"`
}

type Exception struct {
	Class          string `json:"class"`
	Message        string `json:"message"`
	FullStacktrace string `json:"fullStacktrace"`
}

func New(skipped, failed, ignored, passed, total int) *NgTestResult {
	return &NgTestResult{
		Skipped: skipped,
		Failed:  failed,
		Ignored: ignored,
		Passed:  passed,
		Total:   total,
		Suites:  []*Suite{},
	}
}

func NewSuite(name, started, finished string, d time.Duration) *Suite {
	return &Suite{
		Name:       name,
		StartedAt:  started,
		FinishedAt: finished,
		Duration:   d,
		Tests:      []*Test{},
	}
}

func NewTest(name string, d time.Duration, started, finished string) *Test {
	return &Test{
		Name:       name,
		Duration:   d,
		StartedAt:  started,
		FinishedAt: finished,
		Classes:    []*Class{},
	}
}

func NewClass(name string) *Class {
	return &Class{
		Name:    name,
		Methods: []*TestMethod{},
	}
}

func NewTestMethod(name, signature, provider, started, finished string, status NgStatus, d time.Duration, isConfig bool) *TestMethod {
	return &TestMethod{
		Name:         name,
		Signature:    signature,
		DataProvider: provider,
		StartedAt:    started,
		FinishedAt:   finished,
		Status:       status,
		Duration:     d,
		IsConfig:     isConfig,
		Params:       []*Param{},
	}
}

func NewParam() *Param {
	return &Param{}
}

func NewReporterOutput() *ReporterOutput {
	return &ReporterOutput{
		Lines: []string{},
	}
}

func NewException(class string) *Exception {
	return &Exception{
		Class: class,
	}
}

func (o *ReporterOutput) String() string {
	if o == nil || o.Lines == nil || len(o.Lines) == 0 {
		return ""
	}
	return strings.Join(o.Lines, " ")
}

func argumentsToString(ps []*Param) string {
	if ps == nil || len(ps) == 0 {
		return ""
	}
	joinStr := ""
	str := "Method arguments: \n"

	for _, param := range ps {
		if param == nil || strings.TrimSpace(param.Value) == "" {
			continue
		}
		joinStr += fmt.Sprintf("\t %s \n", param.Value)
	}

	if strings.TrimSpace(joinStr) == "" {
		return ""
	}

	return str + joinStr
}

// set ng test result to standard output
func (ng *NgTestResult) Transfer() ([]*pb.TestSuite, error) {
	var suites []*pb.TestSuite

	if len(ng.Suites) == 0 {
		return suites, nil
	}

	// suite 一般只有一个
	for _, ngSuite := range ng.Suites {
		// standard suite
		suite := &pb.TestSuite{
			Name: ngSuite.Name,
			Totals: &pb.TestTotal{
				Statuses: make(map[string]int64),
			},
			Extra: make(map[string]string),
		}

		suite.Totals.Duration = int64(ngSuite.Duration)

		// ngTest
		for _, ngTest := range ngSuite.Tests {

			// ngClass
			for _, ngClass := range ngTest.Classes {

				// ngMethod
				for _, ngMethod := range ngClass.Methods {
					// ignore if ngMethod is config ngMethod
					if ngMethod.IsConfig {
						continue
					}
					// set total ngTest( total case)
					suite.Totals.Tests += 1

					test := &pb.Test{
						Name: ngMethod.Name,
					}
					test.Classname = ngClass.Name
					test.Status = string(ngMethod.Status.ToStatus())
					test.Duration = int64(ngMethod.Duration)
					test.Stdout = fmt.Sprintf("%s %s",
						argumentsToString(ngMethod.Params),
						ngMethod.ReporterOutput.String())

					switch ngMethod.Status {
					case NgSkipped:
						suite.Totals.Statuses[string(apistructs.TestStatusSkipped)] += 1
					case NgPassed:
						suite.Totals.Statuses[string(apistructs.TestStatusPassed)] += 1
					case NgFailed:
						suite.Totals.Statuses[string(apistructs.TestStatusFailed)] += 1
						test.Error = &pb.TestError{
							Message: ngMethod.Exception.Message,
							Type:    ngMethod.Exception.Class,
							Body:    ngMethod.Exception.FullStacktrace,
						}
					}

					// add test
					suite.Tests = append(suite.Tests, test)
				}
			}

		}
		suites = append(suites, suite)
	}
	return suites, nil
}
