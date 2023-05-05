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

package surefilexml

import (
	"io/ioutil"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda-proto-go/dop/qa/unittest/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/qaparser"
)

// IngestDir will search the given directory for XML files and return a slice
// of all contained JUnit test suite definitions.
func IngestDir(directory string) ([]*pb.TestSuite, error) {
	var filenames []string

	err := filepath.Walk(directory, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		// Add all regular files that end with ".xml"
		if info.Mode().IsRegular() && strings.HasSuffix(info.Name(), ".xml") {
			filenames = append(filenames, path)
		}

		return nil
	})

	if err != nil {
		return nil, err
	}

	return IngestFiles(filenames)
}

// IngestFiles will parse the given XML files and return a slice of all
// contained JUnit test suite definitions.
func IngestFiles(filenames []string) ([]*pb.TestSuite, error) {
	var all []*pb.TestSuite

	for _, filename := range filenames {
		suites, err := IngestFile(filename)
		if err != nil {
			return nil, err
		}
		all = append(all, suites...)
	}

	return all, nil
}

// IngestFile will parse the given XML file and return a slice of all contained
// JUnit test suite definitions.
func IngestFile(filename string) ([]*pb.TestSuite, error) {
	data, err := ioutil.ReadFile(filename)
	if err != nil {
		return nil, err
	}

	return Ingest(data)
}

// Ingest will parse the given XML data and return a slice of all contained
// JUnit test suite definitions.
func Ingest(data []byte) ([]*pb.TestSuite, error) {
	var (
		suiteChan = make(chan pb.TestSuite)
		suites    []*pb.TestSuite
	)

	nodes, err := qaparser.NodeParse(data)
	if err != nil {
		return nil, err
	}

	go func() {
		findSuites(nodes, suiteChan)
		close(suiteChan)
	}()

	for temp := range suiteChan {
		suite := temp
		if suite.Totals.Tests == 0 || suite.Tests == nil {
			continue
		}
		suites = append(suites, &suite)
	}

	return suites, nil
}

// findparser.Suites performs a depth-first search through the XML document, and
// attempts to ingest any "testsuite" tags that are encountered.
func findSuites(nodes []qaparser.XmlNode, suites chan pb.TestSuite) {
	for _, node := range nodes {
		switch node.XMLName.Local {
		case "testsuite":
			suites <- ingestSuite(node)
		default:
			findSuites(node.Nodes, suites)
		}
	}
}

func ingestSuite(root qaparser.XmlNode) pb.TestSuite {
	suite := pb.TestSuite{
		Name:    root.Attr("name"),
		Package: root.Attr("package"),
	}

	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "testcase":
			testcase := ingestTestcase(node)
			suite.Tests = append(suite.Tests, testcase)
		case "properties":
			props := ingestProperties(node)
			suite.Properties = props
		case "system-out":
			suite.Stdout = string(node.Content)
		case "system-err":
			suite.Stderr = string(node.Content)
		}
	}

	su := &qaparser.Suite{&suite}

	su.Aggregate()
	return suite
}

func ingestProperties(root qaparser.XmlNode) map[string]string {
	props := make(map[string]string, len(root.Nodes))

	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "property":
			name := node.Attr("name")
			value := node.Attr("value")
			props[name] = value
		}
	}

	return props
}

func ingestTestcase(root qaparser.XmlNode) *pb.Test {
	test := pb.Test{
		Name:      root.Attr("name"),
		Classname: root.Attr("classname"),
		Duration:  int64(duration(root.Attr("time"))),
		Status:    string(apistructs.TestStatusPassed),
	}

	for _, node := range root.Nodes {
		switch node.XMLName.Local {
		case "skipped":
			test.Status = string(apistructs.TestStatusSkipped)
		case "failure":
			test.Error = ingestError(node)
			test.Status = string(apistructs.TestStatusFailed)
		case "error":
			test.Error = ingestError(node)
			test.Status = string(apistructs.TestStatusError)
		case "system-out":
			test.Stdout = string(node.Content)
		case "system-err":
			test.Stderr = string(node.Content)
		}
	}

	return &test
}

func ingestError(root qaparser.XmlNode) *pb.TestError {
	return &pb.TestError{
		Body:    string(root.Content),
		Type:    root.Attr("type"),
		Message: root.Attr("message"),
	}
}

func duration(t string) time.Duration {
	// Check if there was a valid decimal value
	if s, err := strconv.ParseFloat(t, 64); err == nil {
		return time.Duration(s*1000000) * time.Microsecond
	}

	// Check if there was a valid duration string
	if d, err := time.ParseDuration(t); err == nil {
		return d
	}

	return 0
}
