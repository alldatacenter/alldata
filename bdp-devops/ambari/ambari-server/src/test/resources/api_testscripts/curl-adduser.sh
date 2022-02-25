# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

curl -i -o - -X GET http://localhost:8080/api/users
curl -i -o - -X GET http://localhost:8080/api/users/admin
#POST - to create a user test3 with password ambari belonging to both roles user and admin, the roles can just be admin
curl -i -X POST -d '{"Users": {"password" : "ambari", "roles" : "user,admin"}}' http://localhost:8080/api/users/test3
#PUT -
#similar to post for update, for password change you will have to do something like:
curl -i -X PUT -d '{"Users": {"password" : "ambari2", "old_password" : "ambari"}}' http://localhost:8080/api/users/test3
curl -i -o - -X GET http://localhost:8080/api/users/
curl -i -o - -X GET http://localhost:8080/api/users/admin
