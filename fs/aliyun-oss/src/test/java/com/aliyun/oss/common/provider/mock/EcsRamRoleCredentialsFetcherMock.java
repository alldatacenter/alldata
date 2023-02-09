/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.provider.mock;

import com.aliyun.oss.common.auth.EcsRamRoleCredentialsFetcher;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.HttpRequest;
import com.aliyuncs.http.HttpResponse;

import java.io.IOException;

public class EcsRamRoleCredentialsFetcherMock extends EcsRamRoleCredentialsFetcher {

    public EcsRamRoleCredentialsFetcherMock(String ossAuthServerHost) {
        super(ossAuthServerHost);
    }

    private static final String NORMAL_METADATA = "{" + "\"AccessKeyId\" : \"STS.EgnR2nX****FAf9uuqjHS8Ddt\","
            + "\"AccessKeySecret\" : \"CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\","
            + "\"Expiration\" : \"2022-11-11T16:10:03Z\","
            + "\"SecurityToken\" : \"CAISigJ1q6Ft5B2yfSjIpKTbGYjatahPg6CtQ0CIkXUkZsd/14HPljz2IHBE****AOEetfs2lW1T6P0TlrRtTtpfTEmBbI569s1WqQW+Z5fT5JHo4LZfhoGoRzB9keMGTIyADd/iRfbxJ92PCTmd5AIRrJ****K9JS/HVbSClZ9gaPkOQwC8dkAoLdxKJwxk2qR4XDmrQp****PxhXfKB0dFoxd1jXgFiZ6y2cqB8BHT/jaYo603392ofsj1NJE1ZMglD4nlhbxMG/CfgHIK2X9j77xriaFIwzDDs+yGDkNZixf8aLqEqIM/dV4hPfdjSvMf8qOtj5t1sffJnoHtzBJAIexOT****FVtcH5xchqAAXp1d/dYv+2L+dJDW+2pm1vACD/UlRk93prPkyuU3zH2wnvXBxEi26QnoQSCA+T1yE2wo41V2mS+LSGYN/PC+2Ml1q+JX5DzKgfGrUPt7kU4FeXJDzGh2YaXRGpO7yERKgAc/NukkDNqthMaHntyTeix08DYBuTT6gd3V8XmN8vF\","
            + "\"Code\" : \"Success\"" + "}";
    private static final String NORMAL_WITHOUT_EXPIRATION_METADATA = "{"
            + "\"AccessKeyId\" : \"STS.EgnR2nX****FAf9uuqjHS8Ddt\","
            + "\"AccessKeySecret\" : \"CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\"," + "\"Code\" : \"Success\""
            + "}";
    private static final String NORMAL_WITHOUT_TOKEN_METADATA = "{"
            + "\"AccessKeyId\" : \"STS.EgnR2nX****FAf9uuqjHS8Ddt\","
            + "\"AccessKeySecret\" : \"CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\","
            + "\"Expiration\" : \"2022-11-11T16:10:03Z\"," + "\"Code\" : \"Success\"" + "}";
    private static final String EXPIRED_METADATA = "{" + "\"AccessKeyId\" : \"STS.EgnR2nX****FAf9uuqjHS8Ddt\","
            + "\"AccessKeySecret\" : \"CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\","
            + "\"Expiration\" : \"2016-11-11T16:10:03Z\","
            + "\"SecurityToken\" : \"CAISigJ1q6Ft5B2yfSjIpKTbGYjatahPg6CtQ0CIkXUkZsd/14HPljz2IHBE****AOEetfs2lW1T6P0TlrRtTtpfTEmBbI569s1WqQW+Z5fT5JHo4LZfhoGoRzB9keMGTIyADd/iRfbxJ92PCTmd5AIRrJ****K9JS/HVbSClZ9gaPkOQwC8dkAoLdxKJwxk2qR4XDmrQp****PxhXfKB0dFoxd1jXgFiZ6y2cqB8BHT/jaYo603392ofsj1NJE1ZMglD4nlhbxMG/CfgHIK2X9j77xriaFIwzDDs+yGDkNZixf8aLqEqIM/dV4hPfdjSvMf8qOtj5t1sffJnoHtzBJAIexOT****FVtcH5xchqAAXp1d/dYv+2L+dJDW+2pm1vACD/UlRk93prPkyuU3zH2wnvXBxEi26QnoQSCA+T1yE2wo41V2mS+LSGYN/PC+2Ml1q+JX5DzKgfGrUPt7kU4FeXJDzGh2YaXRGpO7yERKgAc/NukkDNqthMaHntyTeix08DYBuTT6gd3V8XmN8vF\","
            + "\"Code\" : \"Success\"" + "}";
    private static final String FORMAT_INVALID_METADATA = "{" + "\"AccessKeyId\" : \"STS.EgnR2nX****FAf9uuqjHS8Ddt\","
            + "\"AccessKeySecret\" : \"CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\","
            + "\"Expiration\" : \"2016-11-11T16:10:03Z\"" + "}";

    private static final String FORMAT_INVALID__WITHOUT_STATUSCODE_METADATA = "{" + "\"AccessKeyId\" : \"STS.EgnR2nX****FAf9uuqjHS8Ddt\","
            + "\"AccessKeySecret\" : \"CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\","
            + "\"Expiration\" : \"2020-11-11T16:10:03Z\","
            + "\"SecurityToken\" : \"CAISigJ1q6Ft5B2yfSjIpKTbGYjatahPg6CtQ0CIkXUkZsd/14HPljz2IHBE****AOEetfs2lW1T6P0TlrRtTtpfTEmBbI569s1WqQW+Z5fT5JHo4LZfhoGoRzB9keMGTIyADd/iRfbxJ92PCTmd5AIRrJ****K9JS/HVbSClZ9gaPkOQwC8dkAoLdxKJwxk2qR4XDmrQp****PxhXfKB0dFoxd1jXgFiZ6y2cqB8BHT/jaYo603392ofsj1NJE1ZMglD4nlhbxMG/CfgHIK2X9j77xriaFIwzDDs+yGDkNZixf8aLqEqIM/dV4hPfdjSvMf8qOtj5t1sffJnoHtzBJAIexOT****FVtcH5xchqAAXp1d/dYv+2L+dJDW+2pm1vACD/UlRk93prPkyuU3zH2wnvXBxEi26QnoQSCA+T1yE2wo41V2mS+LSGYN/PC+2Ml1q+JX5DzKgfGrUPt7kU4FeXJDzGh2YaXRGpO7yERKgAc/NukkDNqthMaHntyTeix08DYBuTT6gd3V8XmN8vF\","
            + "\"NoCode\" : \"Success\"" + "}";

    private static final String FORMAT_FAIL_STATUSCODE_METADATA = "{" + "\"AccessKeyId\" : \"STS.EgnR2nX****FAf9uuqjHS8Ddt\","
            + "\"AccessKeySecret\" : \"CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\","
            + "\"Expiration\" : \"2020-11-11T16:10:03Z\","
            + "\"SecurityToken\" : \"CAISigJ1q6Ft5B2yfSjIpKTbGYjatahPg6CtQ0CIkXUkZsd/14HPljz2IHBE****AOEetfs2lW1T6P0TlrRtTtpfTEmBbI569s1WqQW+Z5fT5JHo4LZfhoGoRzB9keMGTIyADd/iRfbxJ92PCTmd5AIRrJ****K9JS/HVbSClZ9gaPkOQwC8dkAoLdxKJwxk2qR4XDmrQp****PxhXfKB0dFoxd1jXgFiZ6y2cqB8BHT/jaYo603392ofsj1NJE1ZMglD4nlhbxMG/CfgHIK2X9j77xriaFIwzDDs+yGDkNZixf8aLqEqIM/dV4hPfdjSvMf8qOtj5t1sffJnoHtzBJAIexOT****FVtcH5xchqAAXp1d/dYv+2L+dJDW+2pm1vACD/UlRk93prPkyuU3zH2wnvXBxEi26QnoQSCA+T1yE2wo41V2mS+LSGYN/PC+2Ml1q+JX5DzKgfGrUPt7kU4FeXJDzGh2YaXRGpO7yERKgAc/NukkDNqthMaHntyTeix08DYBuTT6gd3V8XmN8vF\","
            + "\"Code\" : \"Fail\"" + "}";

    private static final String FORMAT_INVALID_WITHOUT_AK_METADATA = "{" + "\"NoAccessKeyId\" : \"STS.EgnR2nX****FAf9uuqjHS8Ddt\","
            + "\"AccessKeySecret\" : \"CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\","
            + "\"Expiration\" : \"2020-11-11T16:10:03Z\","
            + "\"SecurityToken\" : \"CAISigJ1q6Ft5B2yfSjIpKTbGYjatahPg6CtQ0CIkXUkZsd/14HPljz2IHBE****AOEetfs2lW1T6P0TlrRtTtpfTEmBbI569s1WqQW+Z5fT5JHo4LZfhoGoRzB9keMGTIyADd/iRfbxJ92PCTmd5AIRrJ****K9JS/HVbSClZ9gaPkOQwC8dkAoLdxKJwxk2qR4XDmrQp****PxhXfKB0dFoxd1jXgFiZ6y2cqB8BHT/jaYo603392ofsj1NJE1ZMglD4nlhbxMG/CfgHIK2X9j77xriaFIwzDDs+yGDkNZixf8aLqEqIM/dV4hPfdjSvMf8qOtj5t1sffJnoHtzBJAIexOT****FVtcH5xchqAAXp1d/dYv+2L+dJDW+2pm1vACD/UlRk93prPkyuU3zH2wnvXBxEi26QnoQSCA+T1yE2wo41V2mS+LSGYN/PC+2Ml1q+JX5DzKgfGrUPt7kU4FeXJDzGh2YaXRGpO7yERKgAc/NukkDNqthMaHntyTeix08DYBuTT6gd3V8XmN8vF\","
            + "\"Code\" : \"Success\"" + "}";

    private static final String FORMAT_INVALID_WITHOUT_SK_METADATA = "{" + "\"AccessKeyId\" : \"STS.EgnR2nX****FAf9uuqjHS8Ddt\","
            + "\"NoAccessKeySecret\" : \"CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\","
            + "\"Expiration\" : \"2020-11-11T16:10:03Z\","
            + "\"SecurityToken\" : \"CAISigJ1q6Ft5B2yfSjIpKTbGYjatahPg6CtQ0CIkXUkZsd/14HPljz2IHBE****AOEetfs2lW1T6P0TlrRtTtpfTEmBbI569s1WqQW+Z5fT5JHo4LZfhoGoRzB9keMGTIyADd/iRfbxJ92PCTmd5AIRrJ****K9JS/HVbSClZ9gaPkOQwC8dkAoLdxKJwxk2qR4XDmrQp****PxhXfKB0dFoxd1jXgFiZ6y2cqB8BHT/jaYo603392ofsj1NJE1ZMglD4nlhbxMG/CfgHIK2X9j77xriaFIwzDDs+yGDkNZixf8aLqEqIM/dV4hPfdjSvMf8qOtj5t1sffJnoHtzBJAIexOT****FVtcH5xchqAAXp1d/dYv+2L+dJDW+2pm1vACD/UlRk93prPkyuU3zH2wnvXBxEi26QnoQSCA+T1yE2wo41V2mS+LSGYN/PC+2Ml1q+JX5DzKgfGrUPt7kU4FeXJDzGh2YaXRGpO7yERKgAc/NukkDNqthMaHntyTeix08DYBuTT6gd3V8XmN8vF\","
            + "\"Code\" : \"Success\"" + "}";

    private String extrnalData = "";

    public enum ResponseCategory {
        Normal, NormalWithoutExpiration, NormalWithoutToken, Expired, FormatInvalid, ServerHalt, Exceptional,
        ExceptionalWithoutStatus, ExceptionalFailStatus, ExceptionalWithoutAK, ExceptionalWithoutSK,
        FromExternal
    };

    @Override
    public HttpResponse send(HttpRequest request) throws IOException {
        HttpResponse response;
        try {
            response = new HttpResponse(buildUrl().toString());
        } catch (ClientException e) {
            throw new IOException("CredentialsFetcher.buildUrl Exception.");
        }

        switch (responseCategory) {
        case Normal:
            response.setStatus(200);
            response.setHttpContent(NORMAL_METADATA.getBytes(), "UTF-8", FormatType.JSON);
            break;
        case NormalWithoutExpiration:
            response.setStatus(200);
            response.setHttpContent(NORMAL_WITHOUT_EXPIRATION_METADATA.getBytes(), "UTF-8", FormatType.JSON);
            break;
        case NormalWithoutToken:
            response.setStatus(200);
            response.setHttpContent(NORMAL_WITHOUT_TOKEN_METADATA.getBytes(), "UTF-8", FormatType.JSON);
            break;
        case Expired:
            response.setStatus(200);
            response.setHttpContent(EXPIRED_METADATA.getBytes(), "UTF-8", FormatType.JSON);
            break;
        case FormatInvalid:
            response.setStatus(200);
            response.setHttpContent(FORMAT_INVALID_METADATA.getBytes(), "UTF-8", FormatType.JSON);
            break;
        case ServerHalt:
            response.setStatus(500);
            response.setHttpContent("".getBytes(), "UTF-8", null);
            break;
        case Exceptional:
            throw new IOException("CredentialsFetcher.send Exception.");
        case ExceptionalWithoutStatus:
            response.setStatus(200);
            response.setHttpContent(FORMAT_INVALID__WITHOUT_STATUSCODE_METADATA.getBytes(), "UTF-8", FormatType.JSON);
            break;
        case ExceptionalFailStatus:
            response.setStatus(200);
            response.setHttpContent(FORMAT_FAIL_STATUSCODE_METADATA.getBytes(), "UTF-8", FormatType.JSON);
            break;
        case ExceptionalWithoutAK:
            response.setStatus(200);
            response.setHttpContent(FORMAT_INVALID_WITHOUT_AK_METADATA.getBytes(), "UTF-8", FormatType.JSON);
            break;
        case ExceptionalWithoutSK:
            response.setStatus(200);
            response.setHttpContent(FORMAT_INVALID_WITHOUT_SK_METADATA.getBytes(), "UTF-8", FormatType.JSON);
            break;
        case FromExternal:
            response.setStatus(200);
            response.setHttpContent(extrnalData.getBytes(), "UTF-8", FormatType.JSON);
            break;
        default:
            break;
        }

        return response;
    }

    public EcsRamRoleCredentialsFetcherMock withResponseCategory(ResponseCategory responseCategory) {
        this.responseCategory = responseCategory;
        return this;
    }

    public void setExternalData(String extrnalData) {
        this.extrnalData = extrnalData;
    }

    private ResponseCategory responseCategory;
}
