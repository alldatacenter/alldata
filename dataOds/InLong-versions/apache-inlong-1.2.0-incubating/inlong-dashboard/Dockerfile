#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
FROM nginx
# add built files from target output
ARG DASHBOARD_FILE
WORKDIR /usr/share/nginx/html
ADD ${DASHBOARD_FILE} /usr/share/nginx/html
ADD nginx.conf /etc/nginx/conf.d/default.conf
ENV MANAGER_API_ADDRESS=127.0.0.1:8083
ADD dashboard-docker.sh /docker-entrypoint.d/
RUN chmod +x /docker-entrypoint.d/dashboard-docker.sh
CMD ["/docker-entrypoint.d/dashboard-docker.sh"]