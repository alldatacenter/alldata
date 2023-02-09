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


package com.qcloud.cos.model;

import java.io.Serializable;
import java.util.Map;

/**
 * <p>
 * An extension of {@link GetObjectRequest} to allow additional encryption material description to
 * be specified on a per-request basis. In particular, {@link EncryptedGetObjectRequest} is only
 * recognized by {@link COSEncryptionClient}.
 * </p>
 * <p>
 * If {@link EncryptedGetObjectRequest} is used against the non-encrypting
 * {@link COSEncryptionClient}, the additional attributes will be ignored.
 * </p>
 * <p>
 * The additional material description must not conflict with the existing one saved in COS or else
 * will cause the get request to fail fast later on.
 */
public class EncryptedGetObjectRequest extends GetObjectRequest implements Serializable {
    /**
     * Used to retrieve the COS encrypted object via instruction file with an explicit suffix.
     * Applicable only if specified (which means non-null and non-blank.)
     */
    private String instructionFileSuffix;
    /**
     * True if the retrieval of the encrypted object expects the CEK to have been key-wrapped;
     * Default is false.
     * <p>
     * Note, however, that if {@link CryptoMode#StrictAuthenticatedEncryption} is in use, key
     * wrapping is always expected for the CEK regardless.
     */
    private boolean keyWrapExpected;

    public EncryptedGetObjectRequest(String bucketName, String key) {
        this(bucketName, key, null);
    }

    public EncryptedGetObjectRequest(String bucketName, String key, String versionId) {
        super(bucketName, key, versionId);
        setKey(key);
        setVersionId(versionId);
    }

    public EncryptedGetObjectRequest(COSObjectId cosObjectId) {
        super(cosObjectId);
    }


    public String getInstructionFileSuffix() {
        return instructionFileSuffix;
    }

    /**
     * Explicitly sets the suffix of an instruction file to be used to retrieve the COS encrypted
     * object. Typically this is for more advanced use cases where multiple crypto instruction files
     * have been created for the same COS object. Each instruction file contains the same CEK
     * encrypted under a different KEK, the IV, and other meta information (aka material
     * description).
     * 
     * @param instructionFileSuffix suffix of the instruction file to be used.
     * 
     * @see COSEncryptionClient#putInstructionFile(PutInstructionFileRequest)
     */
    public void setInstructionFileSuffix(String instructionFileSuffix) {
        this.instructionFileSuffix = instructionFileSuffix;
    }

    /**
     * Fluent API to explicitly sets the suffix of an instruction file to be used to retrieve the COS
     * encrypted object. Typically this is for more advanced use cases where multiple crypto
     * instruction files have been created for the same COS object. Each instruction file contains
     * the same CEK encrypted under a different KEK, the IV, and other meta information (aka
     * material description).
     * 
     * @param instructionFileSuffix suffix of the instruction file to be used.
     * 
     * @see COSEncryptionClient#putInstructionFile(PutInstructionFileRequest)
     */
    public EncryptedGetObjectRequest withInstructionFileSuffix(String instructionFileSuffix) {
        this.instructionFileSuffix = instructionFileSuffix;
        return this;
    }

    /**
     * Returns true if key wrapping is expected; false otherwise. Note, however, that if
     * {@link CryptoMode#StrictAuthenticatedEncryption} or KMS is in use, key wrapping is always
     * expected for the CEK regardless.
     */
    public boolean isKeyWrapExpected() {
        return keyWrapExpected;
    }

    /**
     * @param keyWrapExpected true if key wrapping is expected for the CEK; false otherwse. Note,
     *        however, that if {@link CryptoMode#StrictAuthenticatedEncryption} or KMS is in use,
     *        key wrapping is always expected for the CEK regardless.
     *        <p>
     *        If keyWrapExpected is set to true but the CEK is found to be not key-wrapped, it would
     *        cause a {@link KeyWrapException} to be thrown.
     */
    public void setKeyWrapExpected(boolean keyWrapExpected) {
        this.keyWrapExpected = keyWrapExpected;
    }

    /**
     * Fluent API for {@link #setKeyWrapExpected(boolean)}.
     */
    public EncryptedGetObjectRequest withKeyWrapExpected(boolean keyWrapExpected) {
        this.keyWrapExpected = keyWrapExpected;
        return this;
    }
}
