#!/usr/bin/env/python

#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging

from apache_atlas.model.glossary import AtlasGlossary, AtlasGlossaryCategory, AtlasGlossaryTerm, AtlasGlossaryHeader

LOG = logging.getLogger('glossary-example')


class GlossaryExample:
    glossaryName = "EmployeeCountry"

    def __init__(self, client):
        self.client               = client
        self.emp_glossary         = None
        self.emp_salary_term      = None
        self.emp_company_category = None

    def create_glossary(self):
        glossary          = AtlasGlossary({ 'name': GlossaryExample.glossaryName, 'shortDescription': 'This is a test Glossary' })
        self.emp_glossary = self.client.glossary.create_glossary(glossary)

        LOG.info("Created glossary with name: %s and guid: %s", self.emp_glossary.name, self.emp_glossary.guid)

        return self.emp_glossary

    def get_glossary_detail(self):
        ext_info = self.client.glossary.get_glossary_ext_info(self.emp_glossary.guid)

        if ext_info:
            LOG.info("Glossary extended info: %s; name: %s; language: %s", ext_info.guid, ext_info.name, ext_info.language)

    def create_glossary_term(self):
        header = AtlasGlossaryHeader({ 'glossaryGuid': self.emp_glossary.guid, 'displayText': self.emp_glossary.name })
        term   = AtlasGlossaryTerm({ 'name': 'EmpSalaryTerm', 'anchor': header })

        self.emp_salary_term = self.client.glossary.create_glossary_term(term)

        if self.emp_salary_term:
            LOG.info("Created Term for Employee Salary: %s with guid: %s", self.emp_salary_term.name, self.emp_salary_term.guid)

    def create_glossary_category(self):
        header   = AtlasGlossaryHeader({ 'glossaryGuid': self.emp_glossary.guid, 'displayText': self.emp_glossary.name })
        category = AtlasGlossaryCategory({ 'name': 'EmpSalaryCategory', 'anchor': header })

        self.emp_company_category = self.client.glossary.create_glossary_category(category)

        if self.emp_company_category:
            LOG.info("Created Category for Employee Category: %s with guid: %s", self.emp_company_category.name, self.emp_company_category.guid)

    def delete_glossary(self):
        if not self.emp_glossary:
            LOG.info("empGlossary is not present. Skipping the delete operation.")

        self.client.glossary.delete_glossary_by_guid(self.emp_glossary.guid)

        LOG.info("Delete is complete for Glossary!")