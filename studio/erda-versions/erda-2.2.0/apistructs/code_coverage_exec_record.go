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

package apistructs

import (
	"encoding/xml"
	"errors"
	"fmt"
	"strconv"
	"strings"
	"time"
)

type CodeCoverageExecStatus string

const (
	RunningStatus CodeCoverageExecStatus = "running"
	ReadyStatus   CodeCoverageExecStatus = "ready"
	EndingStatus  CodeCoverageExecStatus = "ending"
	CancelStatus  CodeCoverageExecStatus = "cancel"
	SuccessStatus CodeCoverageExecStatus = "success"
	FailStatus    CodeCoverageExecStatus = "fail"
)

var WorkingStatus = []CodeCoverageExecStatus{RunningStatus, ReadyStatus, EndingStatus}

var (
	ProjectFormatter = "总行数： %.0f \n覆盖行数: %0.f \n行覆盖率: %.2f"
	PackageFormatter = "%s <br/>总行数: %.0f <br/>覆盖行数: %.0f<br/>行覆盖率: %.2f<br/>class覆盖率: %.2f"
)

type CounterType string

const (
	LineIdx = iota
	InstructionIdx
	ComplexityIdx
	ClassIdx
	MethodIdx
	BranchIdx
	LinePercentIdx
	InstructionPercentIdx
	ClassCoveredPercentIdx
	LineCoveredIdx
)

var (
	LineCounter        CounterType = "LINE"
	InstructionCounter CounterType = "INSTRUCTION"
	ComplexityCounter  CounterType = "COMPLEXITY"
	ClassCounter       CounterType = "CLASS"
	MethodCounter      CounterType = "METHOD"
	BranchCounter      CounterType = "BRANCH"
)

func (c CounterType) IsLineType() bool {
	return c == LineCounter
}

func (c CounterType) IsInstructionType() bool {
	return c == InstructionCounter
}

func (c CounterType) IsClassType() bool {
	return c == ClassCounter
}

func (c CounterType) IsBranchType() bool {
	return c == BranchCounter
}

func (c CounterType) GetValueIdx() int {
	switch c {
	case LineCounter:
		return LineIdx
	case InstructionCounter:
		return InstructionIdx
	case ComplexityCounter:
		return ComplexityIdx
	case MethodCounter:
		return MethodIdx
	case ClassCounter:
		return ClassIdx
	case BranchCounter:
		return BranchIdx
	default:
		return LineIdx
	}
}

func ConvertXmlToReport(source []byte) (CodeTestReport, error) {
	data := CodeTestReport{}
	err := xml.Unmarshal(source, &data)
	if err != nil {
		return CodeTestReport{}, err
	}
	return data, nil
}

func decimal(value float64) float64 {
	value, _ = strconv.ParseFloat(fmt.Sprintf("%.2f", value), 64)
	return value
}

type ToolTip struct {
	Formatter string `json:"formatter"`
}

type CodeCoverageNode struct {
	Value    []float64           `json:"value"`
	Name     string              `json:"name"`
	Path     string              `json:"path"`
	ToolTip  ToolTip             `json:"tooltip"`
	Nodes    []*CodeCoverageNode `json:"children"`
	counters []ReportCounter     `json:"-"`
}

func (this *CodeCoverageNode) MaxDepth() int {
	if this == nil {
		return 0
	}
	depth := 1
	tmp := 0
	for _, node := range this.Nodes {
		t := node.MaxDepth()
		if t > tmp {
			tmp = t
		}
	}
	return depth + tmp
}

func (c CodeCoverageExecStatus) String() string {
	return string(c)
}

type CodeCoverageStartRequest struct {
	IdentityInfo

	ProjectID uint64 `json:"projectID"`
	Workspace string `json:"workspace"`
}

func (req *CodeCoverageStartRequest) Validate() error {
	if req.ProjectID == 0 {
		return errors.New("the projectID is 0")
	}

	return checkWorkspace(req.Workspace)
}

func checkWorkspace(workspace string) error {
	if workspace == "" {
		return fmt.Errorf("workspace was empty")
	}

	var checker = false
	for _, workEnv := range EnvList {
		if workEnv == DefaultEnv {
			continue
		}
		if workEnv == workspace {
			checker = true
		}
	}

	if !checker {
		return fmt.Errorf("workspace value not ok, use %v %v %v %v", DevEnv, TestEnv, StagingEnv, ProdEnv)
	}
	return nil
}

type CodeCoverageUpdateRequest struct {
	IdentityInfo

	ID            uint64 `json:"id"`
	Status        string `json:"status"`
	Msg           string `json:"msg"`
	ReportXmlUUID string `json:"reportXmlUUID"`
	ReportTarUrl  string `json:"reportTarUrl"`
}

func (req *CodeCoverageUpdateRequest) Validate() error {
	if req.ID == 0 {
		return errors.New("the ID is 0")
	}
	return nil
}

type CodeCoverageListRequest struct {
	IdentityInfo

	ProjectID      uint64                   `json:"projectID"`
	PageNo         uint64                   `json:"pageNo"`
	PageSize       uint64                   `json:"pageSize"`
	TimeBegin      string                   `json:"timeBegin"`
	TimeEnd        string                   `json:"timeEnd"`
	Asc            bool                     `json:"asc"`
	Statuses       []CodeCoverageExecStatus `json:"statuses"`
	ReportStatuses []CodeCoverageExecStatus `json:"reportStatuses,omitempty"`
	Workspace      string                   `json:"workspace"`
}

func (req *CodeCoverageListRequest) Validate() error {
	if req.ProjectID == 0 {
		return errors.New("the projectID is 0")
	}
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 10
	}

	return checkWorkspace(req.Workspace)
}

type CodeCoverageExecRecordResponse struct {
	Header
	UserInfoHeader
	Data *CodeCoverageExecRecordData `json:"data"`
}

type CodeCoverageExecRecordData struct {
	Total uint64                      `json:"total"`
	List  []CodeCoverageExecRecordDto `json:"list"`
}

type CodeCoverageExecRecordDto struct {
	ID            uint64              `json:"id"`
	ProjectID     uint64              `json:"projectID"`
	Status        string              `json:"status"`
	ReportStatus  string              `json:"reportStatus"`
	Msg           string              `json:"msg"`
	ReportMsg     string              `json:"reportMsg"`
	Coverage      float64             `json:"coverage"`
	ReportUrl     string              `json:"reportUrl"`
	ReportContent []*CodeCoverageNode `json:"reportContent"`
	StartExecutor string              `json:"startExecutor"`
	EndExecutor   string              `json:"endExecutor"`
	TimeBegin     time.Time           `json:"timeBegin"`
	TimeEnd       time.Time           `json:"timeEnd"`
	TimeCreated   time.Time           `json:"timeCreated"`
	TimeUpdated   time.Time           `json:"timeUpdated"`
	ReportTime    time.Time           `json:"reportTime"`
}

type CodeCoverageCancelRequest struct {
	IdentityInfo

	ProjectID uint64 `json:"projectID"`
	Workspace string `json:"workspace"`
}

func (req *CodeCoverageCancelRequest) Validate() error {
	if req.ProjectID == 0 {
		return errors.New("the projectID is 0")
	}

	return checkWorkspace(req.Workspace)
}

type CodeCoverageExecRecordDetail struct {
	PlanID       uint64 `json:"planID"`
	ProjectID    uint64 `json:"projectID"`
	Status       string `json:"status"`
	MavenSetting string `json:"mavenSetting"`
	Includes     string `json:"includes"`
	Excludes     string `json:"excludes"`
}

type CodeCoverageSetting struct {
	ID           uint64 `json:"id"`
	ProjectID    uint64 `json:"project_id"`
	MavenSetting string `json:"maven_setting"`
	Includes     string `json:"includes"`
	Excludes     string `json:"excludes"`
	Workspace    string `json:"workspace"`
}

type SaveCodeCoverageSettingRequest struct {
	IdentityInfo
	ProjectID    uint64 `json:"project_id"`
	MavenSetting string `json:"maven_setting"`
	Includes     string `json:"includes"`
	Excludes     string `json:"excludes"`
	Workspace    string `json:"workspace"`
}

type CodeReportPrefixTree struct {
	IsEnd  bool
	Node   *CodeCoverageNode
	Nodes  map[string]*CodeReportPrefixTree
	Prefix string
}

func NewPrefix() *CodeReportPrefixTree {
	return &CodeReportPrefixTree{
		Nodes: make(map[string]*CodeReportPrefixTree),
	}
}

func (this *CodeReportPrefixTree) Insert(node *CodeCoverageNode) {
	for _, name := range strings.Split(node.Name, "/") {
		if t := this.Nodes[name]; t == nil {
			this.Nodes[name] = NewPrefix()
		}
		this = this.Nodes[name]
		this.Prefix = name
	}
	this.Node = node
	this.IsEnd = true
}

func (this *CodeReportPrefixTree) ConvertToReport() []*CodeCoverageNode {
	reports := make([]*CodeCoverageNode, 0)
	for _, next := range this.GetNextEnds() {
		node := next.Node
		nodeNexts := next.ConvertToReport()

		node.Nodes = nodeNexts

		reports = append(reports, node)
	}
	return reports
}

func (this *CodeCoverageNode) GetNum() int {
	total := 1
	for _, node := range this.Nodes {
		tmp := node.GetNum()
		total += tmp
	}
	return total
}

func (this *CodeReportPrefixTree) GetNextEnds() []*CodeReportPrefixTree {
	res := []*CodeReportPrefixTree{}
	for _, node := range this.Nodes {
		if node.IsEnd {
			res = append(res, node)
		} else {
			// tree node must have end, leaf node will return empty prefix tree list
			tmp := node.GetNextEnds()
			res = append(res, tmp...)
		}
	}
	return res
}

func (this *CodeCoverageNode) ResetCounter() []ReportCounter {
	for _, node := range this.Nodes {
		tmpCounters := node.ResetCounter()
		node.Name = strings.TrimPrefix(node.Name, this.Name+"/")
		for _, c := range tmpCounters {
			for idx, s := range this.counters {
				if s.Type == c.Type {
					this.counters[idx].Covered += c.Covered
					this.counters[idx].Missed += c.Missed
				}
			}
		}
		setNodeValue(node, node.counters)
		node.ToolTip.Formatter = fmt.Sprintf(PackageFormatter, node.Path, node.Value[LineIdx], node.Value[LineCoveredIdx], node.Value[LinePercentIdx], node.Value[ClassCoveredPercentIdx])
	}
	return this.counters
}

type CodeTestReport struct {
	ProjectID   uint64          `json:"projectID"`
	ProjectName string          `json:"projectName"`
	XMLName     xml.Name        `xml:"report"`
	Name        string          `xml:"name,attr"`
	Packages    []ReportPackage `xml:"package"`
	Counters    []ReportCounter `xml:"counter"`
}

type ReportCounter struct {
	Covered int         `xml:"covered,attr"`
	Missed  int         `xml:"missed,attr"`
	Type    CounterType `xml:"type,attr"`
}

type ReportPackage struct {
	Name     string          `xml:"name,attr"`
	Classes  []ReportClass   `xml:"class"`
	Counters []ReportCounter `xml:"counter"`
}

type ReportClass struct {
	Name           string          `xml:"name,attr"`
	SourceFilename string          `xml:"sourcefilename,attr"`
	Methods        []ReportMethod  `xml:"method"`
	Counters       []ReportCounter `xml:"counter"`
}

type ReportMethod struct {
	Name     string          `xml:"name,attr"`
	Desc     string          `xml:"desc,attr"`
	Line     int             `xml:"line,attr"`
	Counters []ReportCounter `xml:"counter"`
}

func setNodeValue(root *CodeCoverageNode, counters []ReportCounter) {
	root.Value = make([]float64, 10)
	for _, c := range counters {
		v := float64(c.Missed + c.Covered)
		root.Value[c.Type.GetValueIdx()] = v
		if c.Type.IsLineType() && v != 0 {
			root.Value[LinePercentIdx] = decimal((float64(c.Covered) / v) * 100)
			root.Value[LineCoveredIdx] = decimal(float64(c.Covered))
		}
		if c.Type.IsInstructionType() && v != 0 {
			root.Value[InstructionPercentIdx] = decimal((float64(c.Covered) / v) * 100)
		}
		if c.Type.IsClassType() && v != 0 {
			root.Value[ClassCoveredPercentIdx] = decimal((float64(c.Covered) / v) * 100)
		}
	}
}

func ConvertReportToTree(r CodeTestReport) ([]*CodeCoverageNode, float64) {
	var root = &CodeCoverageNode{}
	if r.Packages == nil {
		return []*CodeCoverageNode{}, 0
	}
	setNodeValue(root, r.Counters)
	coverage := root.Value[LinePercentIdx]
	root.Name = r.ProjectName
	root.Path = r.ProjectName
	prefix := NewPrefix()
	prefix.Node = root
	root.ToolTip.Formatter = fmt.Sprintf(ProjectFormatter, root.Value[LineIdx], root.Value[LineCoveredIdx], root.Value[LinePercentIdx])
	for _, p := range r.Packages {
		pNode := &CodeCoverageNode{}
		pNode.counters = p.Counters
		pNode.Name = p.Name
		pNode.Path = p.Name
		prefix.Insert(pNode)
	}
	root.Nodes = prefix.ConvertToReport()
	root.ResetCounter()

	return []*CodeCoverageNode{root}, coverage
}
