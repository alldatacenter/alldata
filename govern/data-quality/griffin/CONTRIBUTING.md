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
## Contributing Code

This project takes all contributions through
[pull requests](https://help.github.com/articles/using-pull-requests).
Code should **not** be pushed directly to `master`.

The following guidelines apply to all contributors.

### Making Changes

* Fork the `apache/griffin` repository
* Make your changes and push them to a topic branch in your fork
  * See our commit message guidelines further down in this document
* Submit a pull request to the `apache/griffin` repository

### General Guidelines

Our commit guidelines mirror that of [OpenStack][OpenStack]:

* Only one logical change per commit
* Do not mix whitespace changes with functional code changes
* Do not mix unrelated functional changes
* When writing a commit message:
  * Describe *why* a change is being made
  * Do not assume the reviewer understands what the original problem was
  * Do not assume the code is self-evident/self-documenting
  * Describe any limitations of the current code

### Commit Message Guidelines

Our commit message guidelines mirror that of [OpenStack][OpenStack]:

* Provide a brief description of the change in the first line.
* Insert a single blank line after the first line
* Provide a detailed description of the change in the following lines, breaking
  paragraphs where needed.
* The first line should be limited to 50 characters and should not end in a
  period.
* Subsequent lines should be wrapped at 72 characters.


Note: In Git commits the first line of the commit message has special
significance. It is used as the email subject line, in git annotate messages, in
gitk viewer annotations, in merge commit messages and many more places where
space is at a premium. Please make the effort to write a good first line!

Good Example:

    Support Binary Protocol for mcrouter

    In addition to the existing ASCII text protocol, support the binary
    protocol. The binary protocol has better performance because parsing
    a binary string is more efficient than parsing a text string.


### Java Guidelines

Assuming you're using Eclipse:

Go to `Eclipse > Preferences`, then:

`Java > Editor > Save Actions`:

* Uncheck "Format source code"
* Check "Organize Imports"
* Check "Additional Actions" and add the following actions:
  * "Remove unused imports"
  * "Remove trailing whitespace on all lines"

`General > Editors > Text Editors`

  * Check "Show print margin" and set the value to 80

Additional set of rules from the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
that are enforced:

* 4.1.1 Braces are used where optional
* 4.1.2 Nonempty blocks: K & R style
* 4.4 Column limit: 80
* 4.8.2.2 Declared when needed, initialized as soon as possible
* 6.1 `@Override`: always used
* 6.2 Caught exceptions: not ignored
* 6.3 Static members: qualified using class
* 6.4 Finalizers: not used

[OpenStack]: <https://wiki.openstack.org/wiki/GitCommitMessages>
