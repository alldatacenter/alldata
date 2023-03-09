/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package executor

import (
	"sort"
	"strconv"
	"strings"

	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

const (
	// L3SchemataPrefix is the prefix of l3 cat schemata
	L3SchemataPrefix = "L3:"
	// MbSchemataPrefix is the prefix of l3 cat schemata
	MbSchemataPrefix = "MB:"
)

func CalculateL3SchemataResource(group, schemataDelta string, l3Num int) ResourceUpdater {
	schemata := L3SchemataPrefix
	// the last ';' will be auto ignored
	for i := 0; i < l3Num; i++ {
		schemata = schemata + strconv.Itoa(i) + "=" + schemataDelta + ";"
	}
	// the trailing '\n' is necessary to append
	schemata += "\n"

	schemataFile := sysutil.GetResctrlSchemataFilePath(group)

	// write to $schemataFile with valued $schemata
	updaterKey := schemataFile + ":" + L3SchemataPrefix
	return NewDetailCommonResourceUpdater(updaterKey, schemataFile, schemata, GroupOwnerRef(group), UpdateResctrlSchemataFunc)
}

func CalculateMbSchemataResource(group, schemataDelta string, l3Num int) ResourceUpdater {
	schemata := MbSchemataPrefix
	// the last ';' will be auto ignored
	for i := 0; i < l3Num; i++ {
		schemata = schemata + strconv.Itoa(i) + "=" + schemataDelta + ";"
	}
	// the trailing '\n' is necessary to append
	schemata += "\n"

	schemataFile := sysutil.GetResctrlSchemataFilePath(group)

	// write to $schemataFile with valued $schemata
	updaterKey := schemataFile + ":" + MbSchemataPrefix
	return NewDetailCommonResourceUpdater(updaterKey, schemataFile, schemata, GroupOwnerRef(group), UpdateResctrlSchemataFunc)
}

func CalculateL3TasksResource(group string, taskIds []int) ResourceUpdater {
	// join ids into updater value and make the id updates one by one
	tasksPath := sysutil.GetResctrlTasksFilePath(group)

	// use ordered slice
	sort.Ints(taskIds)
	var builder strings.Builder
	for _, id := range taskIds {
		builder.WriteString(strconv.Itoa(id))
		builder.WriteByte('\n')
	}

	return NewDetailCommonResourceUpdater(tasksPath, tasksPath, builder.String(), GroupOwnerRef(group), UpdateResctrlTasksFunc)
}
