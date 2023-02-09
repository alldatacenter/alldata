/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos;

/**
 * Common COS HTTP header values used throughout the COS Java client.
 */
public interface Headers {

    /*
     * Standard HTTP Headers
     */

    public static final String HOST = "Host";
    public static final String CACHE_CONTROL = "Cache-Control";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_RANGE = "Content-Range";
    public static final String CONTENT_MD5 = "Content-MD5";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LANGUAGE = "Content-Language";
    public static final String DATE = "Date";
    public static final String ETAG = "ETag";
    public static final String LAST_MODIFIED = "Last-Modified";
    public static final String SERVER = "Server";
    public static final String USER_AGENT = "User-Agent";
    public static final String SDK_LOG_DEBUG = "x-cos-sdk-log-debug";
    public static final String FILE_MODE_DIR = "x-cos-file-mode-dir";

    /*
     * Cos HTTP Headers
     */

    /**
     * Prefix for general COS headers: x-cos-
     */
    public static final String COS_PREFIX = "x-cos-";

    /**
     * COS's canned ACL header: x-cos-acl
     */
    public static final String COS_CANNED_ACL = "x-cos-acl";

    /**
     * Cos's alternative date header: x-cos-date
     */
    public static final String COS_ALTERNATE_DATE = "x-cos-date";

    /**
     * part or object crc64
     */
    public static final String COS_HASH_CRC64_ECMA = "x-cos-hash-crc64ecma";

    /**
     * Prefix for COS user metadata: x-cos-meta-
     */
    public static final String COS_USER_METADATA_PREFIX = "x-cos-meta-";

    /**
     * COS's version ID header
     */
    public static final String COS_VERSION_ID = "x-cos-version-id";

    /**
     * COS's Multi-Factor Authentication header
     */
    public static final String COS_AUTHORIZATION = "Authorization";

    /**
     * COS traffic limit header
     */
    public static final String COS_TRAFFIC_LIMIT = "x-cos-traffic-limit";

    /**
     * COS response header for a request's cos request ID
     */
    public static final String REQUEST_ID = "x-cos-request-id";

    /**
     * CI response header for a request's ci request ID
     */
    public static final String CI_REQUEST_ID = "x-ci-request-id";

    /**
     * COS response header for TRACE ID
     */
    public static final String TRACE_ID = "x-cos-trace-id";

    /**
     * COS response header for merge bucket type
     */
    public static final String BUCKET_ARCH = "x-cos-bucket-arch";

    /**
     * COS request header indicating how to handle metadata when copying an object
     */
    public static final String METADATA_DIRECTIVE = "x-cos-metadata-directive";

    /**
     * DevPay token header
     */
    public static final String SECURITY_TOKEN = "x-cos-security-token";

    /**
     * Header describing what class of storage a user wants
     */
    public static final String STORAGE_CLASS = "x-cos-storage-class";

    /**
     * Header for optional server-side encryption algorithm
     */
    public static final String SERVER_SIDE_ENCRYPTION = "x-cos-server-side-encryption";

    /**
     * Header for the encryption algorithm used when encrypting the object with customer-provided keys
     */
    public static final String SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM =
            "x-cos-server-side-encryption-customer-algorithm";

    /**
     * Header for the customer-provided key for server-side encryption
     */
    public static final String SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY =
            "x-cos-server-side-encryption-customer-key";

    public static final String SERVER_SIDE_ENCRYPTION_COS_KMS_KEY_ID =
            "x-cos-server-side-encryption-cos-kms-key-id";

    public static final String SERVER_SIDE_ENCRYPTION_CONTEXT =
            "x-cos-server-side-encryption-context";
    /**
     * Header for the MD5 digest of the customer-provided key for server-side encryption
     */
    public static final String SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5 =
            "x-cos-server-side-encryption-customer-key-MD5";

    /**
     * Header for the encryption algorithm used when encrypting the object with customer-provided keys
     */
    public static final String COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM =
            "x-cos-copy-source-server-side-encryption-customer-algorithm";

    /**
     * Header for the customer-provided key for server-side encryption
     */
    public static final String COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY =
            "x-cos-copy-source-server-side-encryption-customer-key";

    /**
     * Header for the MD5 digest of the customer-provided key for server-side encryption
     */
    public static final String COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5 =
            "x-cos-copy-source-server-side-encryption-customer-key-MD5";

    /**
     * Header for optional object expiration
     */
    public static final String EXPIRATION = "x-cos-expiration";

    /**
     * Header for optional object expiration
     */
    public static final String EXPIRES = "Expires";

    /**
     * ETag matching constraint header for the copy object request
     */
    public static final String COPY_SOURCE_IF_MATCH = "x-cos-copy-source-if-match";

    /**
     * ETag non-matching constraint header for the copy object request
     */
    public static final String COPY_SOURCE_IF_NO_MATCH =
            "x-cos-copy-source-if-none-match";

    /**
     * Unmodified since constraint header for the copy object request
     */
    public static final String COPY_SOURCE_IF_UNMODIFIED_SINCE =
            "x-cos-copy-source-if-unmodified-since";

    /**
     * Modified since constraint header for the copy object request
     */
    public static final String COPY_SOURCE_IF_MODIFIED_SINCE =
            "x-cos-copy-source-if-modified-since";

    /**
     * Range header for the get object request
     */
    public static final String RANGE = "Range";

    /**
     * Range header for the copy part request
     */
    public static final String COPY_PART_RANGE = "x-cos-copy-source-range";

    /**
     * Modified since constraint header for the get object request
     */
    public static final String GET_OBJECT_IF_MODIFIED_SINCE = "If-Modified-Since";

    /**
     * Unmodified since constraint header for the get object request
     */
    public static final String GET_OBJECT_IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    /**
     * ETag matching constraint header for the get object request
     */
    public static final String GET_OBJECT_IF_MATCH = "If-Match";

    /**
     * ETag non-matching constraint header for the get object request
     */
    public static final String GET_OBJECT_IF_NONE_MATCH = "If-None-Match";

    /**
     * Encrypted symmetric key header that is used in the envelope encryption mechanism
     */
    public static final String CRYPTO_KEY = "x-cos-key";

    public static final String APPEND_OBJECT_NEXT_POSISTION =
            "x-cos-next-append-position";
    /**
     * Encrypted symmetric key header that is used in the Authenticated
     * Encryption (AE) cryptographic module. Older versions of COS encryption
     * client with encryption-only capability would not be able to recognize
     * this AE key, and therefore will be prevented from mistakenly decrypting
     * ciphertext in AE format.
     */
    public static final String CRYPTO_KEY_V2 = "x-cos-key-v2";

    /**
     * Initialization vector (IV) header that is used in the symmetric and envelope encryption mechanisms
     */
    public static final String CRYPTO_IV = "x-cos-iv";

    /**
     * JSON-encoded description of encryption materials used during encryption
     */
    public static final String MATERIALS_DESCRIPTION = "x-cos-matdesc";

    /**
     * Instruction file header to be placed in the metadata of instruction files
     */
    public static final String CRYPTO_INSTRUCTION_FILE = "x-cos-crypto-instr-file";

    /**
     * Header for the original, unencrypted size of an encrypted object
     */
    public static final String UNENCRYPTED_CONTENT_LENGTH = "x-cos-unencrypted-content-length";

    /**
     * Header for the optional original unencrypted Content MD5 of an encrypted object
     */
    public static final String UNENCRYPTED_CONTENT_MD5 = "x-cos-unencrypted-content-md5";

    /**
     * Header in the request and response indicating the QCLOUD Key Management
     * System key id used for Server Side Encryption.
     */
    public static final String SERVER_SIDE_ENCRYPTION_QCLOUD_KMS_KEYID =
            "x-cos-server-side-encryption-qcloud-kms-key-id";

    /**
     * Header for optional redirect location of an object
     */
    public static final String REDIRECT_LOCATION = "x-cos-website-redirect-location";

    /**
     * Header for the optional restore information of an object
     */
    public static String RESTORE = "x-cos-restore";

    /**
     * Header for the optional delete marker information of an object
     */
    public static String DELETE_MARKER = "x-cos-delete-marker";

    /**
     * Key wrapping algorithm such as "AESWrap" and "RSA/ECB/OAEPWithSHA-256AndMGF1Padding".
     */
    public static final String CRYPTO_KEYWRAP_ALGORITHM = "x-cos-wrap-alg";
    /**
     * Content encryption algorithm, such as "AES/GCM/NoPadding".
     */
    public static final String CRYPTO_CEK_ALGORITHM = "x-cos-cek-alg";
    /**
     * Tag length applicable to authenticated encrypt/decryption.
     */
    public static final String CRYPTO_TAG_LENGTH = "x-cos-tag-len";

    /**
     * Region where the bucket is located. This header is returned only in HEAD bucket and ListObjects response.
     */
    public static final String COS_BUCKET_REGION = "x-cos-bucket-region";

    public static final String PIC_OPERATIONS = "Pic-Operations";

    // Client Side encryption header used in all COS SDK

    public static final String ENCRYPTION_KEY = "client-side-encryption-key";
    public static final String ENCRYPTION_START = "client-side-encryption-start";
    public static final String ENCRYPTION_CEK_ALG = "client-side-encryption-cek-alg";
    public static final String ENCRYPTION_WRAP_ALG = "client-side-encryption-wrap-alg";
    public static final String ENCRYPTION_MATDESC = "client-side-encryption-matdesc";
    public static final String ENCRYPTION_UNENCRYPTED_CONTENT_LENGTH =
            "client-side-encryption-unencrypted-content-length";
    public static final String ENCRYPTION_UNENCRYPTED_CONTENT_MD5 = "client-side-encryption-unencrypted-content-md5";
    public static final String ENCRYPTION_DATA_SIZE = "client-side-encryption-data-size";
    public static final String ENCRYPTION_PART_SIZE = "client-side-encryption-part-size";
}