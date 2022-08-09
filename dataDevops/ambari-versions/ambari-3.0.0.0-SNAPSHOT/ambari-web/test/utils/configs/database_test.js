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

var dbUtils = require('utils/configs/database');

describe('Database Utils', function() {
  describe('#getDBLocationFromJDBC', function() {
    [
      {
        jdbcUrl: 'jdbc:mysql://localhost/somedb',
        e: 'localhost'
      },
      {
        jdbcUrl: 'jdbc:postgresql://some.hostname.com:5432/somedb',
        e: 'some.hostname.com'
      },
      {
        jdbcUrl: 'jdbc:derby:/some/dir/another_dir/somedb',
        e: '/some/dir/another_dir'
      },
      {
        jdbcUrl: 'jdbc:derby:${oozie-env/data-dir}/${oozie-env/database_name}-db',
        e: '${oozie-env/data-dir}'
      },
      {
        jdbcUrl: 'jdbc:sqlserver://127.0.0.1;databaseName=some-db;integratedSecurity=true',
        e: '127.0.0.1'
      },
      {
        jdbcUrl: 'jdbc:sqlserver://127.0.0.1:3030;databaseName=some-db;integratedSecurity=true',
        e: '127.0.0.1'
      },
      {
        jdbcUrl: 'jdbc:oracle:thin:@//localhost.com:1521/someDb',
        e: 'localhost.com'
      },
      {
        jdbcUrl: 'jdbc:oracle:thin:@ec2-52-5-27-33.compute-1.amazonaws.com:1521:ORCL',
        e: 'ec2-52-5-27-33.compute-1.amazonaws.com'
      },
      {
        jdbcUrl: 'jdbc:oracle:thin:@ec2-52-5-27-33.compute-1.amazonaws.com:3301:ORCL',
        e: 'ec2-52-5-27-33.compute-1.amazonaws.com'
      },
      {
        jdbcUrl: 'jdbc:oracle:thin:@//{0}:1521/{1}',
        e: ""
      },
      {
        jdbcUrl: 'jdbc:oracl:thin:@//some.com:1521/some-db',
        e: ""
      }
    ].forEach(function(test) {
      it('when jdbc url is ' + test.jdbcUrl + ' host name is ' + test.e, function() {
        expect(dbUtils.getDBLocationFromJDBC(test.jdbcUrl)).to.eql(test.e);
      });
    });
  });

  describe('#parseJdbcUrl', function() {
    [
      {
        jdbcUrl: 'jdbc:mysql://localhost/somedb',
        e: {
          dbType: 'mysql',
          location: 'localhost'
        }
      },
      {
        jdbcUrl: 'jdbc:postgresql://some.hostname.com:5432/somedb',
        e: {
          dbType: 'postgres',
          location: 'some.hostname.com'
        }
      },
      {
        jdbcUrl: 'jdbc:postgresql://some.hostname.com:1111/somedb',
        e: {
          dbType: 'postgres',
          location: 'some.hostname.com'
        }
      },
      {
        jdbcUrl: 'jdbc:derby:/some/dir/another_dir/somedb',
        e: {
          dbType: 'derby',
          location: '/some/dir/another_dir'
        }
      },
      {
        jdbcUrl: 'jdbc:derby:${oozie-env/data-dir}/${oozie-env/database_name}-db',
        e: {
          dbType: 'derby',
          location: '${oozie-env/data-dir}'
        }
      },
      {
        jdbcUrl: 'jdbc:derby:${oozie.data.dir}/${oozie.db.schema.name}-db;create=true',
        e: {
          dbType: 'derby',
          location: '${oozie.data.dir}'
        }
      },
      {
        jdbcUrl: 'jdbc:sqlserver://127.0.0.1;databaseName=some-db;integratedSecurity=true',
        e: {
          dbType: 'mssql',
          location: '127.0.0.1'
        }
      },
      {
        jdbcUrl: 'jdbc:sqlserver://127.0.0.1:3011;databaseName=some-db;integratedSecurity=true',
        e: {
          dbType: 'mssql',
          location: '127.0.0.1'
        }
      },
      {
        jdbcUrl: 'jdbc:oracle:thin:@//localhost.com:1521/someDb',
        e: {
          dbType: 'oracle',
          location: 'localhost.com'
        }
      },
      {
        jdbcUrl: 'jdbc:oracle:thin:@localhost.com:1521:someDb',
        e: {
          dbType: 'oracle',
          location: 'localhost.com'
        }
      },
      {
        jdbcUrl: 'jdbc:oracle:thin:@//{0}:1521/{1}',
        e: {
          dbType: 'oracle',
          location: ""
        }
      },
      {
        jdbcUrl: 'jdbc:oracle:thin:@//localhost:3301/somedb',
        e: {
          dbType: 'oracle',
          location: 'localhost'
        }
      },
      {
        jdbcUrl: 'jdbc:oracle:thin:@localhost:3302/somedb',
        e: {
          dbType: 'oracle',
          location: 'localhost'
        }
      },
      {
        jdbcUrl: 'jdbc:sqlanywhere:host=some.com;database=somedb',
        e: {
          dbType: 'sqla',
          location: 'some.com'
        }
      },
      {
        jdbcUrl: 'jdbc:sqlanywhere:host=some.com:333;database=somedb',
        e: {
          dbType: 'sqla',
          location: 'some.com'
        }
      },
      {
        jdbcUrl: 'jdbc:sqlanywhere:database=somedb;host=some.com:333',
        e: {
          dbType: 'sqla',
          location: 'some.com'
        }
      },
      {
        jdbcUrl: 'jdbc:sqlanywhere:database=somedb;host=some2.com:333;someadditional=some_param',
        e: {
          dbType: 'sqla',
          location: 'some2.com'
        }
      },
      {
        jdbcUrl: 'jdbc:oracle:thin:scott/tiger@myhost:1521:orcl',
        e: {
          dbType: 'oracle',
          location: 'myhost'
        }
      },
      {
        jdbcUrl: 'jdbc:custom:custom/@@@',
        e: {
          dbType: null,
          location: ''
        }
      }
    ].forEach(function(test) {
      it('when jdbc url is ' + test.jdbcUrl + ' result is ' + JSON.stringify(test.e), function() {
        expect(dbUtils.parseJdbcUrl(test.jdbcUrl)).to.be.deep.eql(test.e);
      });
    });
  });
});
