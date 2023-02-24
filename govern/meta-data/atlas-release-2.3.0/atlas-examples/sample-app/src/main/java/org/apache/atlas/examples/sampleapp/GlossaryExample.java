/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.examples.sampleapp;

import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.glossary.AtlasGlossary.AtlasGlossaryExtInfo;
import org.apache.atlas.model.glossary.AtlasGlossaryCategory;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.glossary.relations.AtlasGlossaryHeader;

public class GlossaryExample {
    private static final String GLOSSARY_NAME = "EmployeeCountry";

    private final AtlasClientV2         client;
    private       AtlasGlossary         empGlossary;
    private       AtlasGlossaryTerm     empSalaryTerm;
    private       AtlasGlossaryCategory empCompanyCategory;

    GlossaryExample(AtlasClientV2 client) {
        this.client = client;
    }

    public void createGlossary() throws Exception {
        AtlasGlossary glossary = new AtlasGlossary();

        glossary.setName(GLOSSARY_NAME);
        glossary.setLanguage("English");
        glossary.setShortDescription("This is a test Glossary");

        empGlossary = client.createGlossary(glossary);
    }

    public void getGlossaryDetail() throws Exception {
        AtlasGlossaryExtInfo extInfo = client.getGlossaryExtInfo(empGlossary.getGuid());

        assert (extInfo != null);

        SampleApp.log("Glossary extended info: " + extInfo.getGuid() + "; name: " + extInfo.getName() + "; language: " + extInfo.getLanguage());
    }

    public void createGlossaryTerm() throws Exception {
        if (empSalaryTerm != null) {
            SampleApp.log("EmpSalaryTerm: term already exists");
            return;
        }

        AtlasGlossaryHeader glossary = new AtlasGlossaryHeader();
        AtlasGlossaryTerm   term     = new AtlasGlossaryTerm();

        glossary.setGlossaryGuid(empGlossary.getGuid());
        glossary.setDisplayText(empGlossary.getName());

        term.setAnchor(glossary);
        term.setName("EmpSalaryTerm");

        empSalaryTerm = client.createGlossaryTerm(term);

        if (empSalaryTerm != null) {
            SampleApp.log("Created term for Employee Salary: " + empSalaryTerm);
        }
    }

    public void createGlossaryCategory() throws Exception {
        if (empCompanyCategory != null) {
            SampleApp.log("EmpSalaryCategory: category already exists");
            return;
        }

        AtlasGlossaryHeader   glossary  = new AtlasGlossaryHeader();
        AtlasGlossaryCategory category = new AtlasGlossaryCategory();

        glossary.setGlossaryGuid(empGlossary.getGuid());
        glossary.setDisplayText(empGlossary.getName());

        category.setAnchor(glossary);
        category.setName("EmpSalaryCategory");

        empCompanyCategory = client.createGlossaryCategory(category);

        if (empCompanyCategory != null) {
            SampleApp.log("Created Category for Employee Category :- " + empCompanyCategory);
        }
    }

    public void deleteGlossary() throws Exception {
        if (empGlossary != null) {
            client.deleteGlossaryByGuid(empGlossary.getGuid());

            SampleApp.log("empGlossary is not present. Skipping the delete operation.");
        }

        empGlossary        = null;
        empSalaryTerm      = null;
        empCompanyCategory = null;
    }
}
