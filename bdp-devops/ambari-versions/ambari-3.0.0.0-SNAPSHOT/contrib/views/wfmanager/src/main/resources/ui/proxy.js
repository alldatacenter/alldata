/*
 *    Licensed to the Apache Software Foundation (ASF) under one or more
 *    contributor license agreements.  See the NOTICE file distributed with
 *    this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0
 *    (the "License"); you may not use this file except in compliance with
 *    the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

var http = require('http'),
    args = process.argv.slice(2),
    params = { onLocalPort: args[0] || 11002, toRemoteHost: args[1] || "localhost", toRemotePort: args[2] || 11000 };

http.createServer(function (request, response) {
    var corsHeaders = {
            "Access-Control-Allow-Origin": '*',
            'Access-Control-Allow-Methods': 'PUT, GET, POST, DELETE, OPTIONS',
            "Access-Control-Allow-Headers": 'X-Requested-With, X-Requested-By'
        },
        req;

    if (request.method === 'OPTIONS') {
        console.log(`Handling options request and enabling cors for: ${request.url}`);
        response.writeHead(200, corsHeaders);
        response.end();
        return;
    }

    console.log(`${request.connection.remoteAddress} ${request.method} ${request.url}`);
    /*request.headers["user.name"] = "oozie";
    request.headers["doAs"] = "guest";
    console.log(request.headers);*/
    req = http.request({
        port: params.toRemotePort,
        host: params.toRemoteHost,
        method: request.method,
        path: request.url,
        headers: request.headers
    }, function (res) {
        console.log(`STATUS: ${res.statusCode}`);
        console.log(`HEADERS: ${JSON.stringify(res.headers)}`);

        res.on('data', (chunk) => {
            response.write(chunk, 'binary');
        });
        res.on('end', () => {
            response.end();
        });
        for (var header in corsHeaders) {
            //console.log(header, corsHeaders[header]);
            res.headers[header] = corsHeaders[header];
        }
        //response.writeHead("Access-Control-Allow-Origin", '*');
        response.writeHead(res.statusCode, res.headers);
    });

    req.on('error', (e) => {
        console.log(`problem with request: ${e.message}`);
    });

    req.end();

}).listen(params.onLocalPort);

console.log(`Proxy on: localhost:${params.onLocalPort} -> ${params.toRemoteHost}:${params.toRemotePort}`);