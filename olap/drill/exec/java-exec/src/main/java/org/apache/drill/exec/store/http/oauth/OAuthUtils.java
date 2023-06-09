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

package org.apache.drill.exec.store.http.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.store.security.oauth.OAuthTokenCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OAuthUtils {
  private static final Logger logger = LoggerFactory.getLogger(OAuthUtils.class);

  /**
   * Crafts a POST request to obtain an access token.
   * @param credentialsProvider A credential provider containing the clientID, clientSecret and authorizationCode
   * @param authorizationCode The authorization code from the OAuth2.0 enabled API
   * @param callbackURL  The callback URL. For our purposes this is obtained from the incoming Drill request as it all goes to the same place.
   * @return A Request Body to obtain an access token
   */
  public static RequestBody getPostRequest(CredentialsProvider credentialsProvider, String authorizationCode, String callbackURL) {
    return new FormBody.Builder()
      .add("grant_type", "authorization_code")
      .add("client_id", credentialsProvider.getCredentials().get(OAuthTokenCredentials.CLIENT_ID))
      .add("client_secret", credentialsProvider.getCredentials().get(OAuthTokenCredentials.CLIENT_SECRET))
      .add("redirect_uri", callbackURL)
      .add("code", authorizationCode)
      .build();
  }

  /**
   * Crafts a POST request for refreshing an access token when a refresh token is present.
   * @param credentialsProvider A credential provider containing the clientID, clientSecret and refreshToken
   * @param refreshToken The refresh token
   * @return A Request Body with the correct parameters for obtaining an access token
   */
  public static RequestBody getPostRequestForTokenRefresh(CredentialsProvider credentialsProvider, String refreshToken) {
    return new FormBody.Builder()
      .add("grant_type", "refresh_token")
      .add("client_id", credentialsProvider.getCredentials().get(OAuthTokenCredentials.CLIENT_ID))
      .add("client_secret", credentialsProvider.getCredentials().get(OAuthTokenCredentials.CLIENT_SECRET))
      .add("refresh_token", refreshToken)
      .build();
  }

  /**
   * Helper method for building the access token URL.
   * @param credentialsProvider The credentialsProvider containing all the OAuth pieces.
   * @return The URL string for obtaining an Auth Code.
   */
  public static String buildAccessTokenURL(CredentialsProvider credentialsProvider) {
    return credentialsProvider.getCredentials().get(OAuthTokenCredentials.TOKEN_URI);
  }

  /**
   * Crafts a POST request to obtain an access token.  This method should be used for the initial call
   * to the OAuth API when you are exchanging the authorization code for an access token.
   * @param credentialsProvider The credentialsProvider containing the client_id, client_secret, and auth_code.
   * @param authenticationCode The authentication code from the API.
   * @return A request to obtain the access token.
   */
  public static Request getAccessTokenRequest(CredentialsProvider credentialsProvider, String authenticationCode, String callbackURL) {
    return new Request.Builder()
      .url(buildAccessTokenURL(credentialsProvider))
      .header("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(getPostRequest(credentialsProvider, authenticationCode, callbackURL))
      .build();
  }


  /**
   * Crafts a POST request to obtain an access token.  This method should be used for the additional calls
   * to the OAuth API when you are refreshing the access token. The refresh token must be populated for this
   * to be successful.
   * @param credentialsProvider The credential provider containing the client_id, client_secret, and refresh token.
   * @param refreshToken The OAuth2.0 refresh token
   * @return A request to obtain the access token.
   */
  public static Request getAccessTokenRequestFromRefreshToken(CredentialsProvider credentialsProvider, String refreshToken) {
    String tokenURI = credentialsProvider.getCredentials().get(OAuthTokenCredentials.TOKEN_URI);
    logger.debug("Requesting new access token with refresh token from {}", tokenURI);
    return new Request.Builder()
      .url(tokenURI)
      .header("Content-Type", "application/json")
      .addHeader("Accept", "application/json")
      .post(getPostRequestForTokenRefresh(credentialsProvider, refreshToken))
      .build();
  }

  /**
   * This function is called in after the user has obtained an OAuth Authorization Code.
   * It returns a map of any tokens returned which should be an access_token and an optional
   * refresh_token.
   * @param client The OkHTTP3 client.
   * @param request The finalized Request to obtain the tokens.  This request should be a POST request
   *                containing a client_id, client_secret, authorization code, and grant type.
   * @return a Map of any tokens returned.
   */
  public static Map<String, String> getOAuthTokens(OkHttpClient client, Request request) {
    String accessToken;
    String refreshToken;
    Map<String, String> tokens = new HashMap<>();
    Response response = null;

    try {
      response = client.newCall(request).execute();
      String responseBody = response.body().string();

      if (!response.isSuccessful()) {
        throw UserException.connectionError()
          .message("Error obtaining access tokens: ")
          .addContext(response.message())
          .addContext("Response code: " + response.code())
          .addContext(response.body().string())
          .build(logger);
      }

      logger.debug("Response: {}", responseBody);
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> parsedJson = mapper.readValue(responseBody, Map.class);

      if (parsedJson.containsKey("access_token")) {
        accessToken = (String) parsedJson.get("access_token");
        tokens.put(OAuthTokenCredentials.ACCESS_TOKEN, accessToken);
        logger.debug("Successfully added access token");
      } else {
        // Something went wrong here.
        throw UserException.connectionError()
          .message("Error obtaining access token.")
          .addContext(parsedJson.toString())
          .build(logger);
      }

      // Some APIs will return an access token AND a refresh token at the same time. In that case,
      // we will get both tokens and store them in a HashMap.  The refresh token is used when the
      // access token expires.
      if (parsedJson.containsKey("refresh_token")) {
        refreshToken = (String) parsedJson.get("refresh_token");
        tokens.put(OAuthTokenCredentials.REFRESH_TOKEN, refreshToken);
      }
      return tokens;

    } catch (NullPointerException | IOException e) {
      throw UserException.connectionError()
        .message("Error refreshing access OAuth2 access token. " + e.getMessage())
        .build(logger);
    } finally {
      response.close();
    }
  }
}
