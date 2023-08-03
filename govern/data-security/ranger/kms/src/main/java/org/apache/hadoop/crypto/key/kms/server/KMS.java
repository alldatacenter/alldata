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
package org.apache.hadoop.crypto.key.kms.server;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

import org.apache.hadoop.util.KMSUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.crypto.key.KeyProvider;
import org.apache.hadoop.crypto.key.KeyProvider.KeyVersion;
import org.apache.hadoop.crypto.key.KeyProviderCryptoExtension;
import org.apache.hadoop.crypto.key.KeyProviderCryptoExtension.EncryptedKeyVersion;
import org.apache.hadoop.crypto.key.kms.KMSRESTConstants;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.crypto.key.kms.KMSClientProvider.KMSKeyVersion;
import org.apache.hadoop.crypto.key.kms.KMSClientProvider.KMSEncryptedKeyVersion;
import org.apache.hadoop.crypto.key.kms.server.KMSACLsType.Type;
import org.apache.hadoop.security.token.delegation.web.HttpUserGroupInformation;
import javax.ws.rs.core.UriBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.hadoop.util.KMSUtil.checkNotEmpty;
import static org.apache.hadoop.util.KMSUtil.checkNotNull;
/**
 * Class providing the REST bindings, via Jersey, for the KMS.
 */
@Path(KMSRESTConstants.SERVICE_VERSION)
@InterfaceAudience.Private
public class KMS {
  static final Logger LOG = LoggerFactory.getLogger(KMS.class);

  private static final String KEY_NAME_VALIDATION = "[a-z,A-Z,0-9](?!.*--)(?!.*__)(?!.*-_)(?!.*_-)[\\w\\-\\_]*";
  private static final int    MAX_NUM_PER_BATCH   = 10000;

  public enum KMSOp {
    CREATE_KEY, DELETE_KEY, ROLL_NEW_VERSION, INVALIDATE_CACHE,
    GET_KEYS, GET_KEYS_METADATA,
    GET_KEY_VERSIONS, GET_METADATA, GET_KEY_VERSION, GET_CURRENT_KEY,
    GENERATE_EEK, DECRYPT_EEK, REENCRYPT_EEK, REENCRYPT_EEK_BATCH
  }


  private final KeyProviderCryptoExtension provider;
  private final KMSAudit                   kmsAudit;

  public KMS() throws Exception {
    provider = KMSWebApp.getKeyProvider();
    kmsAudit = KMSWebApp.getKMSAudit();
  }

  @POST
  @Path(KMSRESTConstants.KEYS_RESOURCE)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @SuppressWarnings("unchecked")
  public Response createKey(Map jsonKey, @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> createKey()");
    }

	try {
      KMSWebApp.getAdminCallsMeter().mark();

      final UserGroupInformation user = HttpUserGroupInformation.get();
      final String               name = (String) jsonKey.get(KMSRESTConstants.NAME_FIELD);

      checkNotEmpty(name, KMSRESTConstants.NAME_FIELD);

      validateKeyName(name);

      assertAccess(Type.CREATE, user, KMSOp.CREATE_KEY, name, request.getRemoteAddr());

      final String cipher      = (String) jsonKey.get(KMSRESTConstants.CIPHER_FIELD);
      final String material    = (String) jsonKey.get(KMSRESTConstants.MATERIAL_FIELD);
      final int    length      = (jsonKey.containsKey(KMSRESTConstants.LENGTH_FIELD)) ? (Integer) jsonKey.get(KMSRESTConstants.LENGTH_FIELD) : 0;
      final String description = (String) jsonKey.get(KMSRESTConstants.DESCRIPTION_FIELD);

      if (LOG.isDebugEnabled()) {
	      LOG.debug("Creating key: name={}, cipher={}, keyLength={}, description={}", name, cipher, length, description);
      }

      Map<String, String> attributes = (Map<String, String>) jsonKey.get(KMSRESTConstants.ATTRIBUTES_FIELD);

      if (material != null) {
        assertAccess(Type.SET_KEY_MATERIAL, user, KMSOp.CREATE_KEY, name, request.getRemoteAddr());
      }

      final KeyProvider.Options options = new KeyProvider.Options(KMSWebApp.getConfiguration());

      if (cipher != null) {
        options.setCipher(cipher);
      }

      if (length != 0) {
        options.setBitLength(length);
      }

      options.setDescription(description);
      options.setAttributes(attributes);

      KeyVersion keyVersion = user.doAs((PrivilegedExceptionAction<KeyVersion>) () -> {
        final KeyVersion ret;

        if (material != null) {
          ret = provider.createKey(name, Base64.decodeBase64(material), options);
        } else {
          ret = provider.createKey(name, options);
        }

        provider.flush();

        return ret;
      });

      kmsAudit.ok(user, KMSOp.CREATE_KEY, name, "UserProvidedMaterial:" + (material != null) + " Description:" + description);

      if (!KMSWebApp.getACLs().hasAccess(Type.GET, user, request.getRemoteAddr())) {
        keyVersion = removeKeyMaterial(keyVersion);
      }

      Map    json       = KMSUtil.toJSON(keyVersion);
      String requestURL = KMSMDCFilter.getURL();
      int    idx        = requestURL.lastIndexOf(KMSRESTConstants.KEYS_RESOURCE);

      requestURL = requestURL.substring(0, idx);

      return Response.created(getKeyURI(KMSRESTConstants.SERVICE_VERSION, name))
                     .type(MediaType.APPLICATION_JSON)
                     .header("Location", getKeyURI(requestURL, name)).entity(json).build();
    } catch (Exception e) {
      LOG.error("Exception in createKey.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== createKey()");
      }
    }
  }

  @DELETE
  @Path(KMSRESTConstants.KEY_RESOURCE + "/{name:.*}")
  public Response deleteKey(@PathParam("name") final String name, @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> deleteKey({})", name);
    }

    try {
      KMSWebApp.getAdminCallsMeter().mark();

      UserGroupInformation user = HttpUserGroupInformation.get();

      assertAccess(Type.DELETE, user, KMSOp.DELETE_KEY, name, request.getRemoteAddr());

      checkNotEmpty(name, "name");

      user.doAs((PrivilegedExceptionAction<Void>) () -> {
        provider.deleteKey(name);

        provider.flush();

        return null;
      });

      kmsAudit.ok(user, KMSOp.DELETE_KEY, name, "");

      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Exception in deleteKey.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== deleteKey({})", name);
      }
    }
  }

  @POST
  @Path(KMSRESTConstants.KEY_RESOURCE + "/{name:.*}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response rolloverKey(@PathParam("name") final String name, Map jsonMaterial, @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> rolloverKey({})", name);
    }

    try {
      KMSWebApp.getAdminCallsMeter().mark();

      UserGroupInformation user = HttpUserGroupInformation.get();

      assertAccess(Type.ROLLOVER, user, KMSOp.ROLL_NEW_VERSION, name, request.getRemoteAddr());

      checkNotEmpty(name, "name");

      final String material = (String) jsonMaterial.get(KMSRESTConstants.MATERIAL_FIELD);

      if (material != null) {
        assertAccess(Type.SET_KEY_MATERIAL, user, KMSOp.ROLL_NEW_VERSION, name, request.getRemoteAddr());
      }

      KeyVersion keyVersion = user.doAs((PrivilegedExceptionAction<KeyVersion>) () -> {
        final KeyVersion ret;

        if (material != null) {
          ret = provider.rollNewVersion(name, Base64.decodeBase64(material));
        } else {
          ret = provider.rollNewVersion(name);
        }

        provider.flush();

        return ret;
      });

      kmsAudit.ok(user, KMSOp.ROLL_NEW_VERSION, name, "UserProvidedMaterial:" + (material != null) + " NewVersion:" + keyVersion.getVersionName());

      if (!KMSWebApp.getACLs().hasAccess(Type.GET, user, request.getRemoteAddr())) {
        keyVersion = removeKeyMaterial(keyVersion);
      }

      Map json = KMSUtil.toJSON(keyVersion);

      return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
    } catch (Exception e) {
      LOG.error("Exception in rolloverKey.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== rolloverKey({})", name);
      }
    }
  }

  @POST
  @Path(KMSRESTConstants.KEY_RESOURCE + "/{name:.*}/" + KMSRESTConstants.INVALIDATECACHE_RESOURCE)
  public Response invalidateCache(@PathParam("name") final String name) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> invalidateCache({})", name);
    }

    try {
      KMSWebApp.getAdminCallsMeter().mark();

      checkNotEmpty(name, "name");

      UserGroupInformation user = HttpUserGroupInformation.get();

      assertAccess(Type.ROLLOVER, user, KMSOp.INVALIDATE_CACHE, name);

      user.doAs((PrivilegedExceptionAction<Void>) () -> {
        provider.invalidateCache(name);

        provider.flush();

        return null;
      });

      kmsAudit.ok(user, KMSOp.INVALIDATE_CACHE, name, "");

      return Response.ok().build();
    } catch (Exception e) {
      LOG.error("Exception in invalidateCache for key name {}.", name, e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== invalidateCache({})", name);
      }
    }
  }

  @GET
  @Path(KMSRESTConstants.KEYS_METADATA_RESOURCE)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getKeysMetadata(@QueryParam(KMSRESTConstants.KEY) List<String> keyNamesList, @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> getKeysMetadata()");
    }

    try {
      KMSWebApp.getAdminCallsMeter().mark();

      final UserGroupInformation user     = HttpUserGroupInformation.get();
      final String[]             keyNames = keyNamesList.toArray(new String[0]);

      assertAccess(Type.GET_METADATA, user, KMSOp.GET_KEYS_METADATA, request.getRemoteAddr());

      KeyProvider.Metadata[] keysMeta = user.doAs((PrivilegedExceptionAction<KeyProvider.Metadata[]>) () -> provider.getKeysMetadata(keyNames));
      Object                 json     = KMSServerJSONUtils.toJSON(keyNames, keysMeta);

      kmsAudit.ok(user, KMSOp.GET_KEYS_METADATA, "");

      return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
    } catch (Exception e) {
      LOG.error("Exception in getKeysmetadata.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== getKeysMetadata()");
      }
    }
  }

  @GET
  @Path(KMSRESTConstants.KEYS_NAMES_RESOURCE)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getKeyNames(@Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> getKeyNames()");
    }

    try {
      KMSWebApp.getAdminCallsMeter().mark();

      UserGroupInformation user = HttpUserGroupInformation.get();

      assertAccess(Type.GET_KEYS, user, KMSOp.GET_KEYS, request.getRemoteAddr());

      List<String> json = user.doAs((PrivilegedExceptionAction<List<String>>) provider::getKeys);

      kmsAudit.ok(user, KMSOp.GET_KEYS, "");

      return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
    } catch (Exception e) {
      LOG.error("Exception in getkeyNames.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== getKeyNames()");
      }
    }
  }

  @GET
  @Path(KMSRESTConstants.KEY_RESOURCE + "/{name:.*}")
  public Response getKey(@PathParam("name") String name, @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> getKey({})", name);
    }

    try {
      return getMetadata(name, request);
    } catch (Exception e) {
      LOG.error("Exception in getKey.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== getKey({})", name);
      }
    }
  }

  @GET
  @Path(KMSRESTConstants.KEY_RESOURCE + "/{name:.*}/" + KMSRESTConstants.METADATA_SUB_RESOURCE)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMetadata(@PathParam("name") final String name, @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> getMetadata({})", name);
    }

    try {
      UserGroupInformation user = HttpUserGroupInformation.get();

      checkNotEmpty(name, "name");

      KMSWebApp.getAdminCallsMeter().mark();

      assertAccess(Type.GET_METADATA, user, KMSOp.GET_METADATA, name, request.getRemoteAddr());

      KeyProvider.Metadata metadata = user.doAs((PrivilegedExceptionAction<KeyProvider.Metadata>) () -> provider.getMetadata(name));
      Object               json     = KMSServerJSONUtils.toJSON(name, metadata);

      kmsAudit.ok(user, KMSOp.GET_METADATA, name, "");

      return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
    } catch (Exception e) {
      LOG.error("Exception in getMetadata.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== getMetadata({})", name);
      }
    }
  }

  @GET
  @Path(KMSRESTConstants.KEY_RESOURCE + "/{name:.*}/" + KMSRESTConstants.CURRENT_VERSION_SUB_RESOURCE)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCurrentVersion(@PathParam("name") final String name, @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> getCurrentVersion({})", name);
    }

    try {
      UserGroupInformation user = HttpUserGroupInformation.get();

      checkNotEmpty(name, "name");

      KMSWebApp.getKeyCallsMeter().mark();

      assertAccess(Type.GET, user, KMSOp.GET_CURRENT_KEY, name, request.getRemoteAddr());

      KeyVersion keyVersion = user.doAs((PrivilegedExceptionAction<KeyVersion>) () -> provider.getCurrentKey(name));

      Object json = KMSUtil.toJSON(keyVersion);

      kmsAudit.ok(user, KMSOp.GET_CURRENT_KEY, name, "");

      return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
    } catch (Exception e) {
      LOG.error("Exception in getCurrentVersion.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== getCurrentVersion({})", name);
      }
    }
  }

  @GET
  @Path(KMSRESTConstants.KEY_VERSION_RESOURCE + "/{versionName:.*}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getKeyVersion(@PathParam("versionName") final String versionName, @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> getKeyVersion({})", versionName);
    }

    try {
      UserGroupInformation user = HttpUserGroupInformation.get();

      checkNotEmpty(versionName, "versionName");

      KMSWebApp.getKeyCallsMeter().mark();

      assertAccess(Type.GET, user, KMSOp.GET_KEY_VERSION, request.getRemoteAddr());

      KeyVersion keyVersion = user.doAs((PrivilegedExceptionAction<KeyVersion>) () -> provider.getKeyVersion(versionName));

      if (keyVersion != null) {
        kmsAudit.ok(user, KMSOp.GET_KEY_VERSION, keyVersion.getName(), "");
      }

      Object json = KMSUtil.toJSON(keyVersion);

      return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
    } catch (Exception e) {
      LOG.error("Exception in getKeyVersion.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== getKeyVersion({})", versionName);
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @GET
  @Path(KMSRESTConstants.KEY_RESOURCE + "/{name:.*}/" + KMSRESTConstants.EEK_SUB_RESOURCE)
  @Produces(MediaType.APPLICATION_JSON)
  public Response generateEncryptedKeys(@PathParam("name") final String name,
                                        @QueryParam(KMSRESTConstants.EEK_OP) String edekOp,
                                        @DefaultValue("1") @QueryParam(KMSRESTConstants.EEK_NUM_KEYS) final int numKeys,
                                        @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> generateEncryptedKeys(name={}, eekOp={}, numKeys={})", name, edekOp, numKeys);
    }

    try {
      UserGroupInformation user = HttpUserGroupInformation.get();

      checkNotEmpty(name, "name");

      checkNotNull(edekOp, "eekOp");

      Object retJSON;

      if (edekOp.equals(KMSRESTConstants.EEK_GENERATE)) {
        assertAccess(Type.GENERATE_EEK, user, KMSOp.GENERATE_EEK, name,request.getRemoteAddr());

        final List<EncryptedKeyVersion> retEdeks = new LinkedList<>();

        try {
          user.doAs((PrivilegedExceptionAction<Void>) () -> {
            for (int i = 0; i < numKeys; i++) {
              retEdeks.add(provider.generateEncryptedKey(name));
            }

            return null;
          });
        } catch (Exception e) {
          LOG.error("Exception in generateEncryptedKeys:", e);

          throw new IOException(e);
        }

        kmsAudit.ok(user, KMSOp.GENERATE_EEK, name, "");

        retJSON = new ArrayList();

        for (EncryptedKeyVersion edek : retEdeks) {
          ((ArrayList) retJSON).add(KMSUtil.toJSON(edek));
        }
      } else {
        StringBuilder error = new StringBuilder("IllegalArgumentException Wrong ");

        error.append(KMSRESTConstants.EEK_OP)
             .append(" value, it must be ")
             .append(KMSRESTConstants.EEK_GENERATE)
             .append(" or ")
             .append(KMSRESTConstants.EEK_DECRYPT);

        LOG.error(error.toString());

        throw new IllegalArgumentException(error.toString());
      }

      KMSWebApp.getGenerateEEKCallsMeter().mark();

      return Response.ok().type(MediaType.APPLICATION_JSON).entity(retJSON).build();
    } catch (Exception e) {
      LOG.error("Exception in generateEncryptedKeys.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== generateEncryptedKeys(name={}, eekOp={}, numKeys={})", name, edekOp, numKeys);
      }
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @POST
  @Path(KMSRESTConstants.KEY_RESOURCE + "/{name:.*}/" + KMSRESTConstants.REENCRYPT_BATCH_SUB_RESOURCE)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON )
  public Response reencryptEncryptedKeys(@PathParam("name") final String name, final List<Map> jsonPayload) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> reencryptEncryptedKeys(name={}, count={})", name, (jsonPayload != null ? jsonPayload.size() : 0));
    }

    try {
      final Stopwatch sw = Stopwatch.createStarted();

      checkNotEmpty(name, "name");
      checkNotNull(jsonPayload, "jsonPayload");

      final UserGroupInformation user = HttpUserGroupInformation.get();

      KMSWebApp.getReencryptEEKBatchCallsMeter().mark();

      if (jsonPayload.size() > MAX_NUM_PER_BATCH) {
        LOG.warn("Payload size {} too big for reencryptEncryptedKeys from" + " user {}.", jsonPayload.size(), user);
      }

      assertAccess(Type.GENERATE_EEK, user, KMSOp.REENCRYPT_EEK_BATCH,name);

      final List<EncryptedKeyVersion> ekvs = KMSUtil.parseJSONEncKeyVersions(name, jsonPayload);

      Preconditions.checkArgument(ekvs.size() == jsonPayload.size(),"EncryptedKey size mismatch after parsing from json");

      for (EncryptedKeyVersion ekv : ekvs) {
        Preconditions.checkArgument(name.equals(ekv.getEncryptionKeyName()),"All EncryptedKeys must be under the given key name " + name);
      }

      user.doAs((PrivilegedExceptionAction<Void>) () -> {
        provider.reencryptEncryptedKeys(ekvs);

        return null;
      });

      List retJSON = new ArrayList<>(ekvs.size());

      for (EncryptedKeyVersion ekv: ekvs) {
        retJSON.add(KMSUtil.toJSON(ekv));
      }

      kmsAudit.ok(user, KMSOp.REENCRYPT_EEK_BATCH, name,"reencrypted " + ekvs.size() + " keys");

      if (LOG.isDebugEnabled()) {
        LOG.debug("reencryptEncryptedKeys {} keys for key {} took {}", jsonPayload.size(), name, sw.stop());
      }

      return Response.ok().type(MediaType.APPLICATION_JSON).entity(retJSON).build();
    } catch (Exception e) {
      LOG.error("Exception in reencryptEncryptedKeys.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== reencryptEncryptedKeys(name={}, count={})", name, (jsonPayload != null ? jsonPayload.size() : 0));
      }
    }
  }

  @SuppressWarnings("rawtypes")
  @POST
  @Path(KMSRESTConstants.KEY_VERSION_RESOURCE + "/{versionName:.*}/" + KMSRESTConstants.EEK_SUB_RESOURCE)
  @Produces(MediaType.APPLICATION_JSON)
  public Response handleEncryptedKeyOp(@PathParam("versionName") final String versionName,
                                       @QueryParam(KMSRESTConstants.EEK_OP) String eekOp,
                                       Map jsonPayload, @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> handleEncryptedKeyOp(versionName={}, eekOp={})", versionName, eekOp);
    }

    try {
      UserGroupInformation user = HttpUserGroupInformation.get();

      checkNotEmpty(versionName, "versionName");
      checkNotNull(eekOp, "eekOp");

      final String keyName        = (String) jsonPayload.get(KMSRESTConstants.NAME_FIELD);
      final String ivStr          = (String) jsonPayload.get(KMSRESTConstants.IV_FIELD);
      final String encMaterialStr = (String) jsonPayload.get(KMSRESTConstants.MATERIAL_FIELD);

      checkNotNull(ivStr, KMSRESTConstants.IV_FIELD);

      final byte[] iv = Base64.decodeBase64(ivStr);

      checkNotNull(encMaterialStr, KMSRESTConstants.MATERIAL_FIELD);

      final byte[] encMaterial = Base64.decodeBase64(encMaterialStr);

      Object retJSON;

      if (eekOp.equals(KMSRESTConstants.EEK_DECRYPT)) {
        KMSWebApp.getDecryptEEKCallsMeter().mark();

        assertAccess(Type.DECRYPT_EEK, user, KMSOp.DECRYPT_EEK, keyName, request.getRemoteAddr());

        KeyVersion retKeyVersion = user.doAs((PrivilegedExceptionAction<KeyVersion>) () -> {
          KMSEncryptedKeyVersion ekv = new KMSEncryptedKeyVersion(keyName, versionName, iv, KeyProviderCryptoExtension.EEK, encMaterial);

          return provider.decryptEncryptedKey(ekv);
        });

        retJSON = KMSUtil.toJSON(retKeyVersion);

        kmsAudit.ok(user, KMSOp.DECRYPT_EEK, keyName, "");
      } else if (eekOp.equals(KMSRESTConstants.EEK_REENCRYPT)) {
        KMSWebApp.getReencryptEEKCallsMeter().mark();

        assertAccess(Type.GENERATE_EEK, user, KMSOp.REENCRYPT_EEK, keyName);

        EncryptedKeyVersion retEncryptedKeyVersion = user.doAs((PrivilegedExceptionAction<EncryptedKeyVersion>) () -> {
          KMSEncryptedKeyVersion ekv = new KMSEncryptedKeyVersion(keyName,versionName, iv, KeyProviderCryptoExtension.EEK, encMaterial);

          return provider.reencryptEncryptedKey(ekv);
        });

        retJSON = KMSUtil.toJSON(retEncryptedKeyVersion);

        kmsAudit.ok(user, KMSOp.REENCRYPT_EEK, keyName, "");
      } else {
        StringBuilder error = new StringBuilder("IllegalArgumentException Wrong ");

        error.append(KMSRESTConstants.EEK_OP)
             .append(" value, it must be ")
             .append(KMSRESTConstants.EEK_GENERATE)
             .append(" or ")
             .append(KMSRESTConstants.EEK_DECRYPT);

        LOG.error(error.toString());

        throw new IllegalArgumentException(error.toString());
      }

      return Response.ok().type(MediaType.APPLICATION_JSON).entity(retJSON).build();
    } catch (Exception e) {
      LOG.error("Exception in handleEncryptedKeyOp.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== handleEncryptedKeyOp(versionName={}, eekOp={})", versionName, eekOp);
      }
    }
  }

  @GET
  @Path(KMSRESTConstants.KEY_RESOURCE + "/{name:.*}/" + KMSRESTConstants.VERSIONS_SUB_RESOURCE)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getKeyVersions(@PathParam("name") final String name, @Context HttpServletRequest request) throws Exception {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> getKeyVersions({})", name);
    }

    try {
      UserGroupInformation user = HttpUserGroupInformation.get();

      checkNotEmpty(name, "name");

      KMSWebApp.getKeyCallsMeter().mark();

      assertAccess(Type.GET, user, KMSOp.GET_KEY_VERSIONS, name, request.getRemoteAddr());

      List<KeyVersion> ret  = user.doAs((PrivilegedExceptionAction<List<KeyVersion>>) () -> provider.getKeyVersions(name));
      Object           json = KMSServerJSONUtils.toJSON(ret);

      kmsAudit.ok(user, KMSOp.GET_KEY_VERSIONS, name, "");

      return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
    } catch (Exception e) {
      LOG.error("Exception in getKeyVersions.", e);

      throw e;
    } finally {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<== getKeyVersions({})", name);
      }
    }
   }

  private void assertAccess(Type aclType, UserGroupInformation ugi, KMSOp operation, String clientIp) throws AccessControlException {
    KMSWebApp.getACLs().assertAccess(aclType, ugi, operation, null, clientIp);
  }

  private void assertAccess(Type aclType, UserGroupInformation ugi, KMSOp operation, String key, String clientIp) throws AccessControlException {
    KMSWebApp.getACLs().assertAccess(aclType, ugi, operation, key, clientIp);
  }

  private void validateKeyName(String name) {
    Pattern pattern = Pattern.compile(KEY_NAME_VALIDATION);
    Matcher matcher = pattern.matcher(name);

    if (!matcher.matches()) {
      throw new IllegalArgumentException("Key Name : " + name + ", should start with alpha/numeric letters and can have special characters - (hypen) or _ (underscore)");
    }
  }

  private static KeyVersion removeKeyMaterial(KeyVersion keyVersion) {
    return new KMSKeyVersion(keyVersion.getName(), keyVersion.getVersionName(), null);
  }

  private static URI getKeyURI(String domain, String keyName) {
    return UriBuilder.fromPath("{a}/{b}/{c}").build(domain, KMSRESTConstants.KEY_RESOURCE, keyName);
  }
}
