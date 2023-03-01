#!/usr/bin/env python3
# coding=utf-8
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import sys

def check_equal(expect, actual):
  if expect == actual:
    return True
  try:
    numberE = float(expect)
    numberA = float(actual)
    if abs(numberE - numberA) < 1e-6:
      return True
  except Exception as e:
    print (e)
    return False

def fun(a):
    return a.startswith('|')


def check(expect, actual):
  with open(expect, 'r') as f:
    expect_records = [line.split() for line in filter(fun, f.read().strip().split('\n'))]
  with open(actual, 'r') as f:
    actual_records = [line.split() for line in filter(fun, f.read().strip().split('\n'))]
  if len(expect_records) != len(actual_records):
    return 1
  lenR = len(expect_records)
  expect_records.sort()
  actual_records.sort()
  for i in range(lenR):
    if len(expect_records[i]) != len(actual_records[i]):
      return 2
    lenC = len(expect_records[i])
    for j in range(lenC):
      if not check_equal(expect_records[i][j], actual_records[i][j]):
        return 3
  return 0


if __name__ == "__main__":
  expect = sys.argv[1]
  actual = sys.argv[2]
  sys.exit(check(expect, actual))
