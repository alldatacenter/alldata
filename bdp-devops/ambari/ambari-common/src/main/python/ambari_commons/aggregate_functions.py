#!/usr/bin/env python

"""
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
"""

from math import sqrt

def sample_standard_deviation(lst):
  """calculates standard deviation"""
  if len(lst) < 2:
    return 0
  variance = sum([(element-mean(lst))**2 for element in lst]) / (len(lst) - 1)
  return sqrt(variance)

def mean(lst):
  """calculates mean"""
  if len(lst) < 1:
    return 0
  return sum(lst) / len(lst)

def sample_standard_deviation_percentage(lst):
  """calculates sample standard deviation percentage"""
  try:
    return sample_standard_deviation(lst) / mean(lst) * 100
  except ZeroDivisionError:
    # should not be a case for this alert
    return 0

def count(lst):
  """calculates number of data points"""
  return len(lst)
