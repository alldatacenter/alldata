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
	"errors"
	"io/ioutil"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/qaparser"
)

// IngestFiles will parse the given XML files and return a slice of all
// contained JUnit test suite definitions.
func IngestFiles(filenames []string) ([]*NgTestResult, error) {
	var all []*NgTestResult
	for _, filename := range filenames {
		ng, err := IngestFile(filename)
		if err != nil {
			return nil, err
		}
		all = append(all, ng)
	}

	return all, nil
}

// IngestFile will parse the given XML file and return a slice of all contained
// JUnit test suite definitions.
func IngestFile(filename string) (*NgTestResult, error) {
	data, err := ioutil.ReadFile(filename)
	if err != nil {
		return nil, err
	}

	return Ingest(data)
}

// Ingest will parse the given XML data and return a slice of all contained
// JUnit test suite definitions.
func Ingest(data []byte) (*NgTestResult, error) {
	var err error
	nodes, err := qaparser.NodeParse(data)
	if err != nil {
		return nil, err
	}

	if len(nodes) != 1 {
		return nil, errors.New("format failed")
	}

	node := nodes[0]
	skip, err := qaparser.ParseInt(node.Attr(string(NgTotalSkip)))
	if err != nil {
		return nil, err
	}
	fail, err := qaparser.ParseInt(node.Attr(string(NgTotalFail)))
	if err != nil {
		return nil, err
	}
	ignore, err := qaparser.ParseInt(node.Attr(string(NgTotalIgnore)))
	if err != nil {
		return nil, err
	}
	pass, err := qaparser.ParseInt(node.Attr(string(NgTotalPassed)))
	if err != nil {
		return nil, err
	}
	total, err := qaparser.ParseInt(node.Attr(string(NgTotal)))
	if err != nil {
		return nil, err
	}

	ng := New(skip, fail, ignore, pass, total)

	for _, node := range node.Nodes {
		switch node.XMLName.Local {
		case "reporter-output":
			ng.ReporterOutput = ingestReporterOutput(node)
		case "suite":
			ng.Suites = append(ng.Suites, ingestSuite(node))
		}
	}

	return ng, nil
}

func ingestReporterOutput(root qaparser.XmlNode) *ReporterOutput {
	r := NewReporterOutput()
	for _, node := range root.Nodes {
		r.Lines = append(r.Lines, string(node.Content))
	}
	return r
}

func ingestSuite(root qaparser.XmlNode) *Suite {
	suite := NewSuite(
		root.Attr("name"),
		root.Attr("started-at"),
		root.Attr("finished-at"),
		duration(root.Attr("duration-ms")))

	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "test":
			suite.Tests = append(suite.Tests, ingestTest(node))
		case "groups":
			// ignore
		}
	}

	return suite
}

func ingestTest(root qaparser.XmlNode) *Test {
	test := NewTest(
		root.Attr("name"),
		duration(root.Attr("duration-ms")),
		root.Attr("started-at"),
		root.Attr("finished-at"))

	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "class":
			test.Classes = append(test.Classes, ingestClass(node))
		}
	}

	return test
}

func ingestClass(root qaparser.XmlNode) *Class {
	class := NewClass(root.Attr("name"))
	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "test-method":
			class.Methods = append(class.Methods, ingestMethod(node))
		}
	}
	return class
}

func ingestMethod(root qaparser.XmlNode) *TestMethod {
	method := NewTestMethod(root.Attr("name"),
		root.Attr("signature"),
		root.Attr("data-provider"),
		root.Attr("started-at"),
		root.Attr("finished-at"),
		NgStatus(root.Attr("status")),
		duration(root.Attr("duration-ms")),
		root.Attr("is-config") == "true",
	)

	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "params":
			method.Params = ingestParams(node)
		case "exception":
			method.Exception = ingestException(node)
		case "reporter-output":
			method.ReporterOutput = ingestReporterOutput(node)
		}
	}
	return method
}

func ingestParams(root qaparser.XmlNode) []*Param {
	params := make([]*Param, len(root.Nodes))
	for _, node := range root.Nodes {
		params = append(params, ingestParam(node))
	}
	return params
}

func ingestParam(root qaparser.XmlNode) *Param {
	p := NewParam()
	idxString := root.Attr("index")
	idx, err := qaparser.ParseInt(idxString)
	if err != nil {
		logrus.Errorf("parse value=%s to int error", idxString)
		return p
	}
	p.Index = idx

	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "value":
			p.Value = string(node.Content)
		}
	}
	return p
}

func ingestException(root qaparser.XmlNode) *Exception {
	e := NewException(root.Attr("class"))
	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "message":
			e.Message = string(node.Content)
		case "full-stacktrace":
			e.FullStacktrace = string(node.Content)
		}
	}

	return e
}

func duration(t string) time.Duration {
	// Check if there was a valid decimal value
	if s, err := strconv.ParseFloat(t, 64); err == nil {
		return time.Duration(s) * time.Millisecond
	}

	// Check if there was a valid duration string
	if d, err := time.ParseDuration(t); err == nil {
		return d
	}

	return 0
}
