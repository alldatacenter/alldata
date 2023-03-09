<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
# Griffin Project Improvement Proposals (GPIP)

Taking inspiration from [Apache Spark's improvement proposal guide](https://spark.apache.org/improvement-proposals.html),
> The purpose of a SPIP is to inform and involve the user community in major improvements to the Spark codebase throughout
> the development process, to increase the likelihood that user needs are met.

### What is GPIP?
> This guideline should be used for significant user-facing or cross-cutting changes, not small incremental improvements.
> When in doubt, if a committer thinks a change needs a proposal, it does.

A GPIP is similar to a product requirement document commonly used in product management that must do the following,
 - A JIRA ticket labelled "GPIP" (prefix title with **[GPIP]**) proposing a major improvement or change to Apache Griffin,
 - The Template must follow the points defined [below](#GPIP-document-template),
 - Discussions on the JIRA ticket and [dev@griffin.apache.org](mailto:dev@griffin.apache.org) list must be done to evaluate 
  the proposal

### Who can submit a GPIP?

#### GPIP tasks by role 

| Role         | GPIP Task  |
|:-------------|:-----------|
| Member      | Discuss the changes for which a GPIP can be submitted or they can submit a GPIP directly |
| Contributor | Other than Member tasks above, contributors can help with discussions regarding the technical feasibility of a GPIP |
| Committer   | Other than Contributor tasks above, committers can help with discussions regarding whether a GPIP aligns with long-term project goals, and by shepherding GPIPs|

Taking terms from the [Apache Spark's improvement proposal guide](https://spark.apache.org/improvement-proposals.html), 

> ##### Proposal Author
> Any community member who authors an improvement proposal and is committed to pushing the 
>change through the entire process. SPIP authorship can be transferred.

> ##### Proposal Shepherd
> A PMC member who is committed to shepherding the proposed change throughout the entire process.
> Although the shepherd can delegate or work with other committers in the development process, 
> the shepherd is ultimately responsible for the success or failure of the proposal.

### What is the process of proposing a GPIP?
1. Anyone may submit a GPIP, i.e. raise the JIRA ticket, as per the [GPIP tasks by role table](#gpip-tasks-by-role). 
When submitting a proposal, ensure that you are willing to help, at least with discussion ([check GPIP author role](#proposal-author)).
2. After a GPIP is submitted, the author should notify the community about the same at [dev@griffin.apache.org](mailto:dev@griffin.apache.org). 
This will initiate discussions on the JIRA ticket.

**Note:** If a GPIP is too small/ incremental/ wide-scoped and should have been done through the normal JIRA process,
 a committer should remove the GPIP label and notify the author.

#### GPIP Document Template
A GPIP document is a short document with a few questions, inspired by the
 [Heilmeier Catechism](https://en.wikipedia.org/wiki/George_H._Heilmeier):

>1. What are you trying to do? Articulate your objectives using absolutely no jargon.
>2. What problem is this proposal NOT designed to solve?
>3. How is it done today, and what are the limits of current practice?
>4. What is new in your approach and why do you think it will be successful?
>5. Who cares? If you are successful, what difference will it make?
>6. What are the risks?
>7. How long will it take?
>8. What are the mid-term and final “exams” to check for success?

**Appendix A.** Proposed API Changes. An Optional section defining APIs changes, if any. Backward and forward 
compatibility must be taken into account.

**Appendix B.** Optional Design Sketch: How are the goals going to be accomplished? Give sufficient technical detail to 
allow a contributor to judge whether it’s likely to be feasible. Note that this is not a full design document.

**Appendix C.** Optional Rejected Designs: What alternatives were considered? Why were they rejected? If no alternatives
 have been considered, the problem needs more thought.

#### Notes regarding GPIP Discussions and process

 - All discussions of a GPIP should take place publicly, preferably the discussion should be on the JIRA ticket directly. 
 - Any discussions that happen offline should be made available online for the community via meeting notes summarizing
  the discussions.
 - During these discussions, at least 1 shepherd should be identified among PMC members.
 - Once the discussion settles, the shepherd(s) should call for a vote on the GPIP moving forward on the 
 [dev@griffin.apache.org](mailto:dev@griffin.apache.org) list. The vote should be open for at least 72 hours and follows
  the typical Apache vote process and passes upon consensus (at least 3 **+1** votes from PMC members and no **-1** votes from PMC members). 
 - If a committer does not think a GPIP aligns with long-term project goals or is not practical at the point of proposal
 submission, the committer should explicitly vote **-1** for the GPIP and give technical justifications for the same.
 - The Community should be notified of the vote result at [dev@griffin.apache.org](mailto:dev@griffin.apache.org) list.
 - If no PMC members are committed to shepherding a GPIP within a month, the GPIP is considered rejected.

### What is the process of implementing a GPIP?
Implementation should take place via the [standard process](https://griffin.apache.org/docs/contribute.html) for code
 changes. Changes that require GPIPs typically also require design documents to be written and reviewed.