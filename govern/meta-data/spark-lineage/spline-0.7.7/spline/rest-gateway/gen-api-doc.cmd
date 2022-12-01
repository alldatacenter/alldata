@echo off

::  Copyright 2019 ABSA Group Limited
::
::  Licensed under the Apache License, Version 2.0 (the "License");
::  you may not use this file except in compliance with the License.
::  You may obtain a copy of the License at
::
::      http://www.apache.org/licenses/LICENSE-2.0
::
::  Unless required by applicable law or agreed to in writing, software
::  distributed under the License is distributed on an "AS IS" BASIS,
::  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
::  See the License for the specific language governing permissions and
::  limitations under the License.


echo "Generating REST API documentation"

:: Install NPM dependencies
npm ci --no-color

:: Generate consumer documentation
npm run gen-api-doc --no-color -- -o $DOCS_DIR/consumer.html $DOCS_DIR/consumer.swagger.json

:: Generate producer documentation
npm run gen-api-doc --no-color -- -o $DOCS_DIR/producer.html $DOCS_DIR/producer.swagger.json
