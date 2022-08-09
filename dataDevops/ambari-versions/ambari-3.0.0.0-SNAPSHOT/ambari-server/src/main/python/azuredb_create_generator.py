#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

# This script transforms SQLServer "create" SQL to idempotent SQL for AzureDB.
# It is a filter, ie. it expects input on stdin, and prints output on stdout.

from __future__ import print_function
import fileinput
import re
from textwrap import dedent

flags = re.DOTALL | re.IGNORECASE
create_table_re = re.compile("CREATE TABLE ([^\s(]+).*", flags = flags)
create_index_re = re.compile("CREATE(?: NONCLUSTERED)? INDEX ([^ (]+).*", flags = flags)
add_fk_const_re = re.compile("ALTER TABLE \S+ ADD CONSTRAINT (\S+) FOREIGN KEY.*", flags = flags)

input_sql = "".join(fileinput.input())
input_statements = re.split(';', input_sql)
statements = []
for statement in input_statements:
  # wrap "CREATE TABLE" in IF for existence check
  statement = re.sub(
    create_table_re,
    dedent('''\
      IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID('\g<1>') AND type = 'U')
      BEGIN
      \g<0>
      END
      '''),
    statement)

  # wrap "CREATE INDEX" in IF for existence check
  statement = re.sub(
    create_index_re,
    dedent('''\
      IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = '\g<1>')
      BEGIN
      \g<0>
      END
      '''),
    statement)

  # wrap "ALTER TABLE ... ADD CONSTRAINT ... FOREIGN KEY" in IF for existence check
  statement = re.sub(
    add_fk_const_re,
    dedent('''\
      IF NOT EXISTS (SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID('\g<1>') AND type = 'F')
      BEGIN
      \g<0>
      END
      '''),
    statement)

  statements.append(statement)

# find all INSERT statements, create a matching DELETE in reverse order, only one per table
sql = "".join(statements)
inserts = re.findall("INSERT INTO ([^ (]+)", sql, flags = re.IGNORECASE)
tables = set()
deletes = []
for table in inserts:
  if table not in tables:
    deletes.append("  DELETE {0};".format(table))
    tables.add(table)
deletes.reverse()
delete_sql = "\n".join(deletes)
sql = re.sub("BEGIN TRANSACTION", "\g<0>\n" + delete_sql, sql, count=1)

print(sql)
