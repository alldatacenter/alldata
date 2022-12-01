# Kafka consumer server for Spline producer API
Consumes execution plans and events via kafka and stores them to arangoDB

---
### Configuration
All mandatory configs that needs to be provided are in the example bellow.
In addition to this the Kafka consumer can be [configured the standard way](https://kafka.apache.org/documentation/#consumerconfigs). 
Everything that start with prefix `spline.kafka.consumer.` will be send to consumer as a config (with the prefix removed)


spline's `DefaultConfigurationStack` is used so there are many ways how to provide the config.

example properties provided via VM options
```bash
-Dspline.database.connectionUrl=arangodb://localhost/spline
-Dspline.kafka.consumer.bootstrap.servers=localhost:9092
-Dspline.kafka.consumer.group.id=spline-group
-Dspline.kafka.topic=spline-topic
```

### Error recovery
Currently, all received messages are acknowledged. Errors during message processing are logged. 
The consumer is idempotent so if needed it can be rewind back, and the failed messages can be re-ingested. 

---
For general Spline documentation and examples please visit:
- [Spline GitHub Pages](https://absaoss.github.io/spline/)
- [Getting Started](https://github.com/AbsaOSS/spline-getting-started)

---

    Copyright 2019 ABSA Group Limited
    
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
