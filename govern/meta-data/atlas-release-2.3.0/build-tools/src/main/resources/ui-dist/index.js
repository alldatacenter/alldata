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

(function () {
    var gatewayUrl,
    _csrfToken,
    csrfEnabled = false,
    restCsrfCustomHeader,
    restCsrfMethodsToIgnore = [],
    swaggerSpecFileName = "swagger.json";

    window.onload = function() {
        const ui = SwaggerUIBundle({
            url: getSwaggerBaseUrl(window.location.pathname) + "/" + swaggerSpecFileName,
            dom_id: '#swagger-ui',
            deepLinking: true,
            presets: [
                SwaggerUIBundle.presets.apis,
                SwaggerUIStandalonePreset
            ],
            plugins: [
                SwaggerUIBundle.plugins.DownloadUrl
            ],
            layout: "StandaloneLayout",
            requestInterceptor: function(request) {
                if (!request.url.includes(swaggerSpecFileName)) {
                    request.url = getAPIUrl(request.url);
                    setCsrfHeaderToRequest(request);
                }

                return request;
            },
            docExpansion: 'none',
            validatorUrl: 'none'
        })
        window.ui = ui;

        atlasLogo = gatewayUrl + "/img/atlas_logo.svg";
        $('#swagger-ui img').attr("src", atlasLogo);

        fetchCsrfHeader();
    }

    function getSwaggerBaseUrl(url) {
        var path = url.replace(/\/[\w-]+.(jsp|html)|\/+$/ig, '');
        splitPath = path.split("/");
        splitPath.pop();
        gatewayUrl = splitPath.join("/");

        return window.location.origin + path;
    };

    function getAPIUrl(url) {
        url = new URL(url);
        var path =  url.origin + gatewayUrl + url.pathname + url.search;
        return path;
    };

    function fetchCsrfHeader() {
        var response = getSessionDetails();

        if (!csrfEnabled && response['atlas.rest-csrf.enabled']) {
            var str = "" + response['atlas.rest-csrf.enabled'];
            csrfEnabled = (str.toLowerCase() == 'true');
        }

        if (!restCsrfCustomHeader && response["atlas.rest-csrf.custom-header"]) {
            restCsrfCustomHeader = response["atlas.rest-csrf.custom-header"].trim();
        }

        if (restCsrfMethodsToIgnore == 0 && response["atlas.rest-csrf.methods-to-ignore"]) {
            restCsrfMethodsToIgnore = response["atlas.rest-csrf.methods-to-ignore"].split(",");
        }

        if (csrfEnabled) {
            _csrfToken = response['_csrfToken'];
        }
    }

    function setCsrfHeaderToRequest(request) {
        if (csrfEnabled && !restCsrfMethodsToIgnore.includes(request.method)) {
           request.headers[restCsrfCustomHeader] = _csrfToken;
        }
    }

    function getSessionDetails() {
        var response;
        $.ajax({
            async : false,
            method: "GET",
            url: gatewayUrl + "/api/atlas/admin/session",
            dataType: 'json',
            success: function(result){
                response = result;
            }
        });
        return response;
    };
})();