/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


App.clusterMapper = App.QuickDataMapper.create({
    model : App.Cluster,
    map:function(json){
      if(!this.get('model')) {return;}
      if(json){
        var result = json;
        result = this.parseIt(result, this.config);
        App.store.safeLoad(this.get('model'), result);
        var cluster = App.Cluster.find(result.id);
        var clusterDesiredConfigs = [];
        // Create desired_configs_array
        if(json.Clusters.desired_configs){
          for(var site in json.Clusters.desired_configs){
            var tag = json.Clusters.desired_configs[site].tag;
            var configObj = App.ConfigSiteTag.create({
              site: site,
              tag: tag,
              hostOverrides: {}
            });
            if(json.Clusters.desired_configs[site].host_overrides!=null){
              var hostOverridesArray = {};
              json.Clusters.desired_configs[site].host_overrides.forEach(function(override){
                var hostname = override.host_name;
                hostOverridesArray[hostname] = override.tag;
              });
              configObj.set('hostOverrides', hostOverridesArray);
            }
            clusterDesiredConfigs.push(configObj);
          }
        }
        cluster.set('desiredConfigs', clusterDesiredConfigs);
      }
    },
    config : {
      id:'Clusters.cluster_id',
      cluster_name: 'Clusters.cluster_name',
      stack_name: 'Clusters.stack_name',
      version: 'Clusters.version',
      security_type: 'Clusters.security_type',
      total_hosts: 'Clusters.total_hosts',
      credential_store_properties: 'Clusters.credential_store_properties'
    }
});
