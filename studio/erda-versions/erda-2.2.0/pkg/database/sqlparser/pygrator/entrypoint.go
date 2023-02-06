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

package pygrator

import (
	"io"
	"text/template"
)

const EntrypointPattern = `# encoding: utf8

from django.db import connection
import feature
import datetime


collector_filename = "{{.CollectorFilename}}"


if __name__ == "__main__":
    print("Running Erda migration in Python")
    for task in feature.entries:
        print("run task: {{.DeveloperScriptFilename}}.%s" % (task.__name__))
        task()

    [print(query) for query in connection.queries]
    if len(collector_filename) > 0:
        with open(collector_filename, "a+") as collector:
            for query in connection.queries:
                try:
                    collector.write('/*-Python BEGIN: {}-*/\n'.format(datetime.datetime.now(datetime.timezone.utc).isoformat()))
                    collector.write(query["sql"])
                    collector.write("  /*-LINE END-*/\n")
                except Exception as e:
                    print(e)

`

const EntrypointWithRollback = `# encoding: utf8

from django.db import connection
from django.db.transaction import rollback, set_autocommit
import feature

# close autocommit
set_autocommit(False)

if __name__ == "__main__":
    print("Running Erda migration in Python")
    # rollback every while
    try:    
        for task in feature.entries:
            print("run task: {{.DeveloperScriptFilename}}.%s" % (task.__name__))
            task()
    except Exception as e:
        print("failed to run task: {{.DeveloperScriptFilename}}.%s: %E" % (task.__name__, e))
    finally:
        rollback()
    [print(query) for query in connection.queries]

`

type Entrypoint struct {
	DeveloperScriptFilename string
	CollectorFilename       string
}

// GenEntrypoint generates python module entrypoint text and write it to  rw
func GenEntrypoint(rw io.ReadWriter, entrypoint Entrypoint, commit bool) error {
	if commit {
		return generate(rw, "EntrypointPattern", EntrypointPattern, entrypoint)
	}
	return generate(rw, "EntrypointWithRollback", EntrypointWithRollback, entrypoint)
}

func generate(rw io.ReadWriter, name, pattern string, data interface{}) error {
	t, err := template.New(name).Parse(pattern)
	if err != nil {
		return err
	}
	if err = t.Execute(rw, data); err != nil {
		return err
	}
	return nil
}
