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
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_checkWorkspace(t *testing.T) {
	type args struct {
		workspace string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "check_workspace_success",
			args: args{
				workspace: DevEnv,
			},
			wantErr: false,
		},
		{
			name: "check_workspace_default",
			args: args{
				workspace: DefaultEnv,
			},
			wantErr: true,
		},
		{
			name: "check_workspace_fail",
			args: args{
				workspace: "",
			},
			wantErr: true,
		},
		{
			name: "check_workspace_fail",
			args: args{
				workspace: "asd",
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := checkWorkspace(tt.args.workspace); (err != nil) != tt.wantErr {
				t.Errorf("checkWorkspace() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

// TestConvertReportToTree test convert [packages...] to prefix tree
func TestConvertReportToTree(t *testing.T) {
	srouceXml := `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
<report name="JaCoCo Coverage Report">
    <sessioninfo id="eason.local-e67f38c9" start="1631709966866" dump="1631710139092"/>
    <sessioninfo id="eason.local-323b8607" start="1633662478638" dump="1633662554800"/>
    <package name="com/jd/jacoco/configure">
        <class name="com/jd/jacoco/configure/AutoConfigure" sourcefilename="AutoConfigure.java">
            <method name="&lt;init&gt;" desc="()V" line="8">
                <counter type="INSTRUCTION" missed="0" covered="3"/>
                <counter type="LINE" missed="0" covered="1"/>
                <counter type="COMPLEXITY" missed="0" covered="1"/>
                <counter type="METHOD" missed="0" covered="1"/>
            </method>
            <method name="sampleController" desc="()Lcom/jd/jacoco/controller/SampleController;" line="13">
                <counter type="INSTRUCTION" missed="0" covered="4"/>
                <counter type="LINE" missed="0" covered="1"/>
                <counter type="COMPLEXITY" missed="0" covered="1"/>
                <counter type="METHOD" missed="0" covered="1"/>
            </method>
            <counter type="INSTRUCTION" missed="0" covered="7"/>
            <counter type="LINE" missed="0" covered="2"/>
            <counter type="COMPLEXITY" missed="0" covered="2"/>
            <counter type="METHOD" missed="0" covered="2"/>
            <counter type="CLASS" missed="0" covered="1"/>
        </class>
        <sourcefile name="AutoConfigure.java">
            <line nr="8" mi="0" ci="3" mb="0" cb="0"/>
            <line nr="13" mi="0" ci="4" mb="0" cb="0"/>
            <counter type="INSTRUCTION" missed="0" covered="7"/>
            <counter type="LINE" missed="0" covered="2"/>
            <counter type="COMPLEXITY" missed="0" covered="2"/>
            <counter type="METHOD" missed="0" covered="2"/>
            <counter type="CLASS" missed="0" covered="1"/>
        </sourcefile>
        <counter type="INSTRUCTION" missed="0" covered="7"/>
        <counter type="LINE" missed="0" covered="2"/>
        <counter type="COMPLEXITY" missed="0" covered="2"/>
        <counter type="METHOD" missed="0" covered="2"/>
        <counter type="CLASS" missed="0" covered="1"/>
    </package>
    <package name="com/jd/jacoco/controller">
        <class name="com/jd/jacoco/controller/SampleController" sourcefilename="SampleController.java">
            <method name="&lt;init&gt;" desc="()V" line="8">
                <counter type="INSTRUCTION" missed="0" covered="3"/>
                <counter type="LINE" missed="0" covered="1"/>
                <counter type="COMPLEXITY" missed="0" covered="1"/>
                <counter type="METHOD" missed="0" covered="1"/>
            </method>
            <method name="codeCoverageMethod" desc="(I)Ljava/lang/String;" line="13">
                <counter type="INSTRUCTION" missed="0" covered="11"/>
                <counter type="BRANCH" missed="0" covered="2"/>
                <counter type="LINE" missed="0" covered="3"/>
                <counter type="COMPLEXITY" missed="0" covered="2"/>
                <counter type="METHOD" missed="0" covered="1"/>
            </method>
            <method name="callingThisMethod" desc="(I)Ljava/lang/String;" line="23">
                <counter type="INSTRUCTION" missed="3" covered="8"/>
                <counter type="BRANCH" missed="1" covered="1"/>
                <counter type="LINE" missed="1" covered="3"/>
                <counter type="COMPLEXITY" missed="1" covered="1"/>
                <counter type="METHOD" missed="0" covered="1"/>
            </method>
            <method name="callingAnotherMethod" desc="(I)Ljava/lang/String;" line="32">
                <counter type="INSTRUCTION" missed="0" covered="5"/>
                <counter type="LINE" missed="0" covered="2"/>
                <counter type="COMPLEXITY" missed="0" covered="1"/>
                <counter type="METHOD" missed="0" covered="1"/>
            </method>
            <counter type="INSTRUCTION" missed="3" covered="27"/>
            <counter type="BRANCH" missed="1" covered="3"/>
            <counter type="LINE" missed="1" covered="9"/>
            <counter type="COMPLEXITY" missed="1" covered="5"/>
            <counter type="METHOD" missed="0" covered="4"/>
            <counter type="CLASS" missed="0" covered="1"/>
        </class>
        <sourcefile name="SampleController.java">
            <line nr="8" mi="0" ci="3" mb="0" cb="0"/>
            <line nr="13" mi="0" ci="3" mb="0" cb="2"/>
            <line nr="14" mi="0" ci="4" mb="0" cb="0"/>
            <line nr="17" mi="0" ci="4" mb="0" cb="0"/>
            <line nr="23" mi="0" ci="3" mb="0" cb="0"/>
            <line nr="24" mi="0" ci="3" mb="1" cb="1"/>
            <line nr="25" mi="3" ci="0" mb="0" cb="0"/>
            <line nr="27" mi="0" ci="2" mb="0" cb="0"/>
            <line nr="32" mi="0" ci="3" mb="0" cb="0"/>
            <line nr="33" mi="0" ci="2" mb="0" cb="0"/>
            <counter type="INSTRUCTION" missed="3" covered="27"/>
            <counter type="BRANCH" missed="1" covered="3"/>
            <counter type="LINE" missed="1" covered="9"/>
            <counter type="COMPLEXITY" missed="1" covered="5"/>
            <counter type="METHOD" missed="0" covered="4"/>
            <counter type="CLASS" missed="0" covered="1"/>
        </sourcefile>
        <counter type="INSTRUCTION" missed="3" covered="27"/>
        <counter type="BRANCH" missed="1" covered="3"/>
        <counter type="LINE" missed="1" covered="9"/>
        <counter type="COMPLEXITY" missed="1" covered="5"/>
        <counter type="METHOD" missed="0" covered="4"/>
        <counter type="CLASS" missed="0" covered="1"/>
    </package>
    <package name="com/jd/jacoco">
        <class name="com/jd/jacoco/JacocoCodeCoverageExampleApplication" sourcefilename="JacocoCodeCoverageExampleApplication.java">
            <method name="&lt;init&gt;" desc="()V" line="7">
                <counter type="INSTRUCTION" missed="0" covered="3"/>
                <counter type="LINE" missed="0" covered="1"/>
                <counter type="COMPLEXITY" missed="0" covered="1"/>
                <counter type="METHOD" missed="0" covered="1"/>
            </method>
            <method name="main" desc="([Ljava/lang/String;)V" line="10">
                <counter type="INSTRUCTION" missed="0" covered="5"/>
                <counter type="LINE" missed="0" covered="2"/>
                <counter type="COMPLEXITY" missed="0" covered="1"/>
                <counter type="METHOD" missed="0" covered="1"/>
            </method>
            <counter type="INSTRUCTION" missed="0" covered="8"/>
            <counter type="LINE" missed="0" covered="3"/>
            <counter type="COMPLEXITY" missed="0" covered="2"/>
            <counter type="METHOD" missed="0" covered="2"/>
            <counter type="CLASS" missed="0" covered="1"/>
        </class>
        <sourcefile name="JacocoCodeCoverageExampleApplication.java">
            <line nr="7" mi="0" ci="3" mb="0" cb="0"/>
            <line nr="10" mi="0" ci="4" mb="0" cb="0"/>
            <line nr="11" mi="0" ci="1" mb="0" cb="0"/>
            <counter type="INSTRUCTION" missed="0" covered="8"/>
            <counter type="LINE" missed="0" covered="3"/>
            <counter type="COMPLEXITY" missed="0" covered="2"/>
            <counter type="METHOD" missed="0" covered="2"/>
            <counter type="CLASS" missed="0" covered="1"/>
        </sourcefile>
        <counter type="INSTRUCTION" missed="0" covered="8"/>
        <counter type="LINE" missed="0" covered="3"/>
        <counter type="COMPLEXITY" missed="0" covered="2"/>
        <counter type="METHOD" missed="0" covered="2"/>
        <counter type="CLASS" missed="0" covered="1"/>
    </package>
    <counter type="INSTRUCTION" missed="3" covered="42"/>
    <counter type="BRANCH" missed="1" covered="3"/>
    <counter type="LINE" missed="1" covered="14"/>
    <counter type="COMPLEXITY" missed="1" covered="9"/>
    <counter type="METHOD" missed="0" covered="8"/>
    <counter type="CLASS" missed="0" covered="3"/>
</report>`
	codePackages, err := ConvertXmlToReport([]byte(srouceXml))
	assert.NoError(t, err)
	assert.Equal(t, 3, len(codePackages.Packages))
	report, totalCoverage := ConvertReportToTree(codePackages)
	assert.Equal(t, 93.33, totalCoverage)
	assert.Equal(t, "com/jd/jacoco", report[0].Nodes[0].Name)
	assert.Equal(t, 2, len(report[0].Nodes[0].Nodes))
}
