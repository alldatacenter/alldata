/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.coord;

import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceInstanceBuilder;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.CoordinationProtos.DrillServiceInstance;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

public class DrillServiceInstanceHelper {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillServiceInstanceHelper.class);


  public static final InstanceSerializer<DrillbitEndpoint> SERIALIZER = new DrillServiceInstanceSerializer();

  private static class DrillServiceInstanceSerializer implements InstanceSerializer<DrillbitEndpoint>{

    @Override
    public byte[] serialize(ServiceInstance<DrillbitEndpoint> i) throws Exception {
      DrillServiceInstance.Builder b = DrillServiceInstance.newBuilder();
      b.setId(i.getId());
      b.setRegistrationTimeUTC(i.getRegistrationTimeUTC());
      b.setEndpoint(i.getPayload());
      return b.build().toByteArray();
    }

    @Override
    public ServiceInstance<DrillbitEndpoint> deserialize(byte[] bytes) throws Exception {
      DrillServiceInstance i = DrillServiceInstance.parseFrom(bytes);
      ServiceInstanceBuilder<DrillbitEndpoint> b = ServiceInstance.<DrillbitEndpoint>builder();
      b.id(i.getId());
      b.name(ExecConstants.SERVICE_NAME);
      b.registrationTimeUTC(i.getRegistrationTimeUTC());
      b.payload(i.getEndpoint());
      return b.build();
    }

  }
}
