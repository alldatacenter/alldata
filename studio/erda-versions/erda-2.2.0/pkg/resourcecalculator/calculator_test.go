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

package resourcecalculator_test

import (
	"testing"

	calcu "github.com/erda-project/erda/pkg/resourcecalculator"
)

type result struct {
	TotalQuotable   uint64
	ProdQuotable    uint64
	StagingQuotable uint64
	TestQuotable    uint64
	DevQuota        uint64
}

func TestResourceCalculator_AddValue(t *testing.T) {
	t.Run("Case-1", testResourceCalculator_AddValue_Case1)
	t.Run("Case-2", testResourceCalculator_AddValue_Case2)
	t.Run("Case-3", testResourceCalculator_AddValue_Case3)
	t.Run("Case-4", testResourceCalculator_AddValue_Case4)
}

func testResourceCalculator_AddValue_Case1(t *testing.T) {
	c := initC(t)
	r := result{
		TotalQuotable:   7,
		ProdQuotable:    1,
		StagingQuotable: 3,
		TestQuotable:    3,
		DevQuota:        4,
	}
	c.DeductionQuota(calcu.Dev, 1, 0)
	c.DeductionQuota(calcu.Test, 1, 0)
	c.DeductionQuota(calcu.Staging, 1, 0)
	t.Logf("quotable cpu: %v, dev: %v, test: %v, staging: %v",
		c.TotalQuotableCPU(), c.QuotableCPUForWorkspace(calcu.Dev), c.QuotableCPUForWorkspace(calcu.Test), c.QuotableCPUForWorkspace(calcu.Staging))
	t.Logf("already took up, dev: %v, test: %v, staging: %v",
		c.AlreadyTookUpCPU(calcu.Dev), c.AlreadyTookUpCPU(calcu.Test), c.AlreadyTookUpCPU(calcu.Staging))
	if c.QuotableCPUForWorkspace(calcu.Prod) != r.ProdQuotable ||
		c.QuotableCPUForWorkspace(calcu.Staging) != r.StagingQuotable ||
		c.QuotableCPUForWorkspace(calcu.Test) != r.TestQuotable ||
		c.QuotableCPUForWorkspace(calcu.Dev) != r.DevQuota {
		t.Fatal("total err")
	}
	if c.TotalQuotableCPU() != r.TotalQuotable {
		t.Fatal("TotalQuotableCPU() error")
	}

	if c.AllocatableCPU(calcu.Dev) != c.QuotableCPUForWorkspace(calcu.Dev)+c.AlreadyTookUpCPU(calcu.Dev) {
		t.Fatal("took up error", c.AllocatableCPU(calcu.Dev), c.QuotableCPUForWorkspace(calcu.Dev), c.AlreadyTookUpCPU(calcu.Dev))
	}
	if c.AllocatableCPU(calcu.Test) != c.QuotableCPUForWorkspace(calcu.Test)+c.AlreadyTookUpCPU(calcu.Test) {
		t.Fatal("took up error")
	}
	if c.AllocatableCPU(calcu.Staging) != c.QuotableCPUForWorkspace(calcu.Staging)+c.AlreadyTookUpCPU(calcu.Staging) {
		t.Fatal("took up error")
	}
	if c.AllocatableCPU(calcu.Prod) != c.QuotableCPUForWorkspace(calcu.Prod)+c.AlreadyTookUpCPU(calcu.Prod) {
		t.Fatal("took up error")
	}
}

func testResourceCalculator_AddValue_Case2(t *testing.T) {
	c := initC(t)
	r := result{
		TotalQuotable:   5,
		ProdQuotable:    1,
		StagingQuotable: 3,
		TestQuotable:    1,
		DevQuota:        3,
	}
	c.DeductionQuota(calcu.Dev, 1, 0)
	c.DeductionQuota(calcu.Test, 3, 0)
	c.DeductionQuota(calcu.Staging, 1, 0)
	t.Logf("quotable cpu: %v, dev: %v, test: %v, staging: %v",
		c.TotalQuotableCPU(), c.QuotableCPUForWorkspace(calcu.Dev), c.QuotableCPUForWorkspace(calcu.Test), c.QuotableCPUForWorkspace(calcu.Staging))
	t.Logf("already took up, dev: %v, test: %v, staging: %v",
		c.AlreadyTookUpCPU(calcu.Dev), c.AlreadyTookUpCPU(calcu.Test), c.AlreadyTookUpCPU(calcu.Staging))
	if c.TotalQuotableCPU() != r.TotalQuotable {
		t.Fatal("TotalQuotableCPU error", c.TotalQuotableCPU())
	}
	if c.QuotableCPUForWorkspace(calcu.Prod) != r.ProdQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Prod) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Staging) != r.StagingQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Staging) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Test) != r.TestQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Test) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Dev) != r.DevQuota {
		t.Fatal("QuotableCPUForWorkspace(calcu.Dev) error")
	}

	if c.AllocatableCPU(calcu.Dev) != c.QuotableCPUForWorkspace(calcu.Dev)+c.AlreadyTookUpCPU(calcu.Dev) {
		t.Fatal("took up error", c.AllocatableCPU(calcu.Dev), c.QuotableCPUForWorkspace(calcu.Dev), c.AlreadyTookUpCPU(calcu.Dev))
	}
	if c.AllocatableCPU(calcu.Test) != c.QuotableCPUForWorkspace(calcu.Test)+c.AlreadyTookUpCPU(calcu.Test) {
		t.Fatal("took up error")
	}
	if c.AllocatableCPU(calcu.Staging) != c.QuotableCPUForWorkspace(calcu.Staging)+c.AlreadyTookUpCPU(calcu.Staging) {
		t.Fatal("took up error")
	}
	if c.AllocatableCPU(calcu.Prod) != c.QuotableCPUForWorkspace(calcu.Prod)+c.AlreadyTookUpCPU(calcu.Prod) {
		t.Fatal("took up error")
	}
}

func testResourceCalculator_AddValue_Case3(t *testing.T) {
	c := initC(t)
	r := result{
		TotalQuotable:   4,
		ProdQuotable:    1,
		StagingQuotable: 2,
		TestQuotable:    0,
		DevQuota:        2,
	}
	c.DeductionQuota(calcu.Dev, 1, 0)
	c.DeductionQuota(calcu.Test, 4, 0)
	c.DeductionQuota(calcu.Staging, 1, 0)
	t.Logf("quotable cpu: %v, dev: %v, test: %v, staging: %v",
		c.TotalQuotableCPU(), c.QuotableCPUForWorkspace(calcu.Dev), c.QuotableCPUForWorkspace(calcu.Test), c.QuotableCPUForWorkspace(calcu.Staging))
	t.Logf("already took up, dev: %v, test: %v, staging: %v",
		c.AlreadyTookUpCPU(calcu.Dev), c.AlreadyTookUpCPU(calcu.Test), c.AlreadyTookUpCPU(calcu.Staging))
	if c.TotalQuotableCPU() != r.TotalQuotable {
		t.Fatal("TotalQuotableCPU error")
	}
	if c.QuotableCPUForWorkspace(calcu.Prod) != r.ProdQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Prod) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Staging) != r.StagingQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Staging) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Test) != r.TestQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Test) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Dev) != r.DevQuota {
		t.Fatal("QuotableCPUForWorkspace(calcu.Dev) error")
	}

	if c.AllocatableCPU(calcu.Dev) != c.QuotableCPUForWorkspace(calcu.Dev)+c.AlreadyTookUpCPU(calcu.Dev) {
		t.Fatal("took up error", c.AllocatableCPU(calcu.Dev), c.QuotableCPUForWorkspace(calcu.Dev), c.AlreadyTookUpCPU(calcu.Dev))
	}
	if c.AllocatableCPU(calcu.Test) != c.QuotableCPUForWorkspace(calcu.Test)+c.AlreadyTookUpCPU(calcu.Test) {
		t.Fatal("took up error")
	}
	if c.AllocatableCPU(calcu.Staging) != c.QuotableCPUForWorkspace(calcu.Staging)+c.AlreadyTookUpCPU(calcu.Staging) {
		t.Fatal("took up error")
	}
	if c.AllocatableCPU(calcu.Prod) != c.QuotableCPUForWorkspace(calcu.Prod)+c.AlreadyTookUpCPU(calcu.Prod) {
		t.Fatal("took up error")
	}
}

func testResourceCalculator_AddValue_Case4(t *testing.T) {
	c := initC(t)
	r := result{
		TotalQuotable:   3,
		ProdQuotable:    1,
		StagingQuotable: 2,
		TestQuotable:    0,
		DevQuota:        1,
	}
	c.DeductionQuota(calcu.Dev, 3, 0)
	c.DeductionQuota(calcu.Test, 3, 0)
	c.DeductionQuota(calcu.Staging, 1, 0)

	t.Logf("quotable cpu: %v, dev: %v, test: %v, staging: %v",
		c.TotalQuotableCPU(), c.QuotableCPUForWorkspace(calcu.Dev), c.QuotableCPUForWorkspace(calcu.Test), c.QuotableCPUForWorkspace(calcu.Staging))
	t.Logf("already took up, dev: %v, test: %v, staging: %v",
		c.AlreadyTookUpCPU(calcu.Dev), c.AlreadyTookUpCPU(calcu.Test), c.AlreadyTookUpCPU(calcu.Staging))
	if c.TotalQuotableCPU() != r.TotalQuotable {
		t.Fatal("TotalQuotableCPU error")
	}
	if c.QuotableCPUForWorkspace(calcu.Prod) != r.ProdQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Prod) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Staging) != r.StagingQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Staging) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Test) != r.TestQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Test) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Dev) != r.DevQuota {
		t.Fatal("QuotableCPUForWorkspace(calcu.Dev) error")
	}

	if c.AllocatableCPU(calcu.Dev) != c.QuotableCPUForWorkspace(calcu.Dev)+c.AlreadyTookUpCPU(calcu.Dev) {
		t.Fatal("took up error", c.AllocatableCPU(calcu.Dev), c.QuotableCPUForWorkspace(calcu.Dev), c.AlreadyTookUpCPU(calcu.Dev))
	}
	if c.AllocatableCPU(calcu.Test) != c.QuotableCPUForWorkspace(calcu.Test)+c.AlreadyTookUpCPU(calcu.Test) {
		t.Fatal("took up error")
	}
	if c.AllocatableCPU(calcu.Staging) != c.QuotableCPUForWorkspace(calcu.Staging)+c.AlreadyTookUpCPU(calcu.Staging) {
		t.Fatal("took up error")
	}
	if c.AllocatableCPU(calcu.Prod) != c.QuotableCPUForWorkspace(calcu.Prod)+c.AlreadyTookUpCPU(calcu.Prod) {
		t.Fatal("took up error")
	}
}

func testResourceCalculator_AddValue_Case5(t *testing.T) {
	c := initC(t)
	r := result{
		TotalQuotable:   3,
		ProdQuotable:    1,
		StagingQuotable: 2,
		TestQuotable:    0,
		DevQuota:        1,
	}
	c.DeductionQuota(calcu.Dev, 3, 0)
	c.DeductionQuota(calcu.Test, 3, 0)
	c.DeductionQuota(calcu.Staging, 1, 0)

	t.Logf("quotable cpu: %v, dev: %v, test: %v, staging: %v",
		c.TotalQuotableCPU(), c.QuotableCPUForWorkspace(calcu.Dev), c.QuotableCPUForWorkspace(calcu.Test), c.QuotableCPUForWorkspace(calcu.Staging))
	if c.TotalQuotableCPU() != r.TotalQuotable {
		t.Fatal("TotalQuotableCPU error")
	}
	if c.QuotableCPUForWorkspace(calcu.Prod) != r.ProdQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Prod) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Staging) != r.StagingQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Staging) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Test) != r.TestQuotable {
		t.Fatal("QuotableCPUForWorkspace(calcu.Test) error")
	}
	if c.QuotableCPUForWorkspace(calcu.Dev) != r.DevQuota {
		t.Fatal("QuotableCPUForWorkspace(calcu.Dev) error")
	}
}

func initC(t *testing.T) *calcu.Calculator {
	clusterName := "erda-hongkong"
	c := calcu.New(clusterName)
	c.AddValue(2, 0, calcu.Dev)
	c.AddValue(1, 0, calcu.Dev, calcu.Test)
	c.AddValue(1, 0, calcu.Dev, calcu.Prod)
	c.AddValue(1, 0, calcu.Dev, calcu.Test, calcu.Staging)
	c.AddValue(2, 0, calcu.Test)
	c.AddValue(3, 0, calcu.Staging)
	t.Logf("initC allocatable cpu prod: %v, staging: %v, test: %v, dev: %v",
		c.AllocatableCPU(calcu.Prod), c.AllocatableCPU(calcu.Staging), c.AllocatableCPU(calcu.Test), c.AllocatableCPU(calcu.Dev))
	return c
}

func TestWorkspaceString(t *testing.T) {
	for _, workspace := range calcu.Workspaces {
		t.Log(calcu.WorkspaceString(workspace))
	}
	if calcu.WorkspaceString(4) != "" {
		t.Log("default value error")
	}
}

func TestCoreToMillcore(t *testing.T) {
	var (
		v float64 = 1.2
		r uint64  = 1200
	)
	if calcu.CoreToMillcore(v) != r {
		t.Fatal("calculate error")
	}
}

func TestMillcoreToCore(t *testing.T) {
	var (
		v uint64  = 1200
		r float64 = 1.2
	)
	if calcu.MillcoreToCore(v, 3) != r {
		t.Fatal("calculate error")
	}
}

func TestGibibyteToByte(t *testing.T) {
	var (
		v float64 = 1.5
		r uint64  = 1.5 * 1024 * 1024 * 1024
	)
	if calcu.GibibyteToByte(v) != r {
		t.Fatal("calculate error")
	}
}

func TestByteToGibibyte(t *testing.T) {
	var (
		v uint64  = 1.5 * 1024 * 1024 * 1024
		r float64 = 1.5
	)
	if calcu.ByteToGibibyte(v, 3) != r {
		t.Fatal("calculate error")
	}
}

func TestWorkspacesString(t *testing.T) {
	t.Log(calcu.WorkspacesString([]calcu.Workspace{calcu.Prod, calcu.Dev, calcu.Staging}))
}

func TestResourceToString(t *testing.T) {
	t.Log(calcu.ResourceToString(1000, "cpu"))
	t.Log(calcu.ResourceToString(5*1024*1024*1024, "memory"))
	t.Log(calcu.ResourceToString(1000, "error key"))
}

func TestAccuracy(t *testing.T) {
	var (
		PROD            = 0.01
		STAGING float64 = 200
		TEST            = 0.007
		DEV             = 0.7
	)
	prod := calcu.CoreToMillcore(PROD)
	staging := calcu.CoreToMillcore(STAGING)
	test := calcu.CoreToMillcore(TEST)
	dev := calcu.CoreToMillcore(DEV)

	total := prod + staging + test + dev
	TOTAL := calcu.MillcoreToCore(total, 3)

	t.Log(PROD, STAGING, TEST, DEV)
	t.Log(prod, staging, test, dev)
	t.Log(total, TOTAL)
	t.Log(PROD+STAGING+TEST+DEV, calcu.CoreToMillcore(PROD+STAGING+TEST+DEV))

	t.Logf("%f, %v", 0.001*1024*1024*1024, calcu.ByteToGibibyte(calcu.GibibyteToByte(0.001), 3))
}
