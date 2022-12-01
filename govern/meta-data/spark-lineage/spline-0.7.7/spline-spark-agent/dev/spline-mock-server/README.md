Spline Gateway Server Mock
===

Simple server mocking producer API. It allows you to test the agent without any existing Spline server.

The mock will throw away any request, but it returns a valid response to the agent.

### Instalation
```
npm install -g mockserver
```

### Usage
```
./start.sh
```
This will start the server on `localhost:8080`

---

    Copyright 2020 ABSA Group Limited
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
