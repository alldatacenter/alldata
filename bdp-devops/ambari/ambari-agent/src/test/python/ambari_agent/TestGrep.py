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

from unittest import TestCase
from ambari_agent.Grep import Grep
import socket
import os, sys
import logging

class TestGrep(TestCase):

  logger = logging.getLogger()
  string_good = None
  string_bad = None
  grep = Grep()

  def setUp(self):
    self.string_good = open('ambari_agent' + os.sep + 'dummy_puppet_output_good.txt', 'r').read().replace("\n", os.linesep)
    self.string_bad = open('ambari_agent' + os.sep + 'dummy_puppet_output_error.txt', 'r').read().replace("\n", os.linesep)
    pass

  def test_grep_many_lines(self):
    fragment = self.grep.grep(self.string_bad, "err", 1000, 1000)
    desired = self.string_bad.strip()
    self.assertEquals(fragment, desired, "Grep grep function should return all lines if there are less lines than n")


  def test_grep_few_lines(self):
    fragment = self.grep.grep(self.string_bad, "Err", 3, 3)
    desired = """
debug: /Schedule[never]: Skipping device resources because running on a host
debug: Exec[command_good](provider=posix): Executing 'wget e432423423xample.com/badurl444111'
debug: Executing 'wget e432423423xample.com/badurl444111'
err: /Stage[main]//Exec[command_good]/returns: change from notrun to 0 failed: wget e432423423xample.com/badurl444111 returned 4 instead of one of [0] at /root/puppet-learn/2-bad.pp:5
debug: /Schedule[weekly]: Skipping device resources because running on a host
debug: /Schedule[puppet]: Skipping device resources because running on a host
debug: Finishing transaction 70171639726240
""".replace("\n", os.linesep).strip()
    self.assertEquals(fragment, desired, "Grep grep function should return only last 3 lines of file")

  def test_grep_no_result(self):
    fragment = self.grep.grep(self.string_good, "Err", 3, 3)
    desired = None
    self.assertEquals(fragment, desired, 'Grep grep function should return None if result is not found')

  def test_grep_empty_string(self):
    fragment = self.grep.grep("", "Err", 1000, 1000)
    desired = None
    self.assertEquals(fragment, desired, 'Grep grep function should return None for empty string')

  def test_grep_all(self):
    fragment = self.grep.grep(self.string_bad, "Err", 35, 9)
    desired = self.string_bad.strip()
    self.assertEquals(fragment, desired, 'Grep grep function contains bug in index arithmetics')


  def test_tail_many_lines(self):
    fragment = self.grep.tail(self.string_good, 1000)
    desired = self.string_good.strip()
    self.assertEquals(fragment, desired, "Grep tail function should return all lines if there are less lines than n")

  def test_tail_few_lines(self):
    fragment = self.grep.tail(self.string_good, 3)
    desired = """
debug: Finishing transaction 70060456663980
debug: Received report to process from ambari-dmi
debug: Processing report from ambari-dmi with processor Puppet::Reports::Store
""".replace("\n", os.linesep).strip()
    self.assertEquals(fragment, desired, "Grep tail function should return only last 3 lines of file")

  def test_tail_no_lines(self):
    fragment = self.grep.tail("", 3)
    desired = ''
    self.assertEquals(fragment, desired, 'Grep tail function should return "" for empty string')

  def test_tail_all(self):
    fragment = self.grep.tail("", 47)
    desired = ''
    self.assertEquals(fragment, desired, 'Grep tail function contains bug in index arithmetics')


  def test_tail_by_symbols_many_lines(self):
    desired_size = len(self.string_good.strip())
    fragment = self.grep.tail_by_symbols(self.string_good, desired_size)
    desired = self.string_good.strip()
    self.assertEquals(fragment, desired, "Grep tail function should return all symbols if there are less symbols than n")
    self.assertEquals(len(fragment), desired_size, "Grep tail function should return all symbols if there are less symbols than n")

  def test_tail_by_symbols_few_lines(self):
    original = """
debug: Finishing transaction 70060456663980
debug: Received report to process from ambari-dmi
debug: Processing report from ambari-dmi with processor Puppet::Reports::Store
"""
    desired = original.replace("\n", os.linesep).strip()
    desired_size = len(original)

    fragment = self.grep.tail_by_symbols(self.string_good, desired_size)
    self.assertEquals(fragment, desired, "Grep tail function should return only last 3 lines of file")

    fragment = self.grep.tail_by_symbols(self.string_good, desired_size - 1)
    self.assertEquals(fragment, desired, "Grep tail function should return only last 2 lines of file")

    fragment = self.grep.tail_by_symbols(self.string_good, desired_size + 1)
    self.assertEquals(fragment, desired, "Grep tail function should return only last 3 lines of file")

  def test_tail_by_symbols_no_lines(self):
    fragment = self.grep.tail_by_symbols("", 3)
    desired = ''
    self.assertEquals(fragment, desired, 'Grep tail function should return "" for empty string')

  def tearDown(self):
    pass

  def test_cleanByTemplate(self):
    fragment = self.grep.cleanByTemplate(self.string_bad, "debug")
    desired = """
info: Applying configuration version '1352127563'
err: /Stage[main]//Exec[command_good]/returns: change from notrun to 0 failed: wget e432423423xample.com/badurl444111 returned 4 instead of one of [0] at /root/puppet-learn/2-bad.pp:5
notice: Finished catalog run in 0.23 seconds
""".replace("\n", os.linesep).strip()
    self.assertEquals(fragment, desired, 'Grep cleanByTemplate function should return string without debug lines.')


