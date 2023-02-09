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

/**
 * <p>
 * Specifies constants that define COS storage classes. The standard storage class
 * is the default storage class.
 * </p>
 * <p>
 * Qcloud COS offers multiple storage classes for different customers' needs. The
 * <code>STANDARD</code> storage class is the default storage class, and means that
 * redundant copies of data will be stored in different locations.
 * </p>
 */
public enum StorageClass {

    /**
     * The default COS class. This storage class
     * is recommended for critical, non-reproducible data.  The standard
     * storage class is a highly available and highly redundant storage option
     * provided for an affordable price.
     */
    Standard("Standard"),

    /**
     * Standard_IA
     */
    Standard_IA("Standard_IA"),

    /**
     * Archive
     */
    Archive("Archive"),

    /**
     * Deep_Archive
     */
    Deep_Archive("Deep_Archive"),

    /**
     * Intelligent_Tiering
     */
    Intelligent_Tiering("Intelligent_Tiering"),

    /**
     *Maz_Standard
     */
    Maz_Standard("Maz_Standard"),

    /**
     *Maz_Standard_IA
     */
     Maz_Standard_IA("Maz_Standard_IA"),

    /**
     * Maz_Archive
     */
    Maz_Archive("Maz_Archive"),

    /**
     * Maz_Deep_Archive
     */
    Maz_Deep_Archive("Maz_Deep_Archive"),

    /**
     * Maz_Intelligent_Tiering
     */
    Maz_Intelligent_Tiering("Maz_Intelligent_Tiering");

    /**
     * Returns the Qcloud COS {@link StorageClass} enumeration value representing the
     * specified Qcloud COS <code>StorageClass</code> ID string.
     * If the specified string doesn't map to a known Qcloud COS storage class,
     * an <code>IllegalArgumentException</code> is thrown.
     *
     * @param cosStorageClassString The Qcloud COS storage class ID string.
     * @return The Qcloud COS <code>StorageClass</code> enumeration value representing the
     * specified Qcloud COS storage class ID.
     * @throws IllegalArgumentException If the specified value does not map to one of the known
     *                                  Qcloud COS storage classes.
     */
    public static StorageClass fromValue(String cosStorageClassString) throws IllegalArgumentException {
        for (StorageClass storageClass : StorageClass.values()) {
            if (storageClass.toString().compareToIgnoreCase(cosStorageClassString) == 0) return storageClass;
        }

        throw new IllegalArgumentException(
                "Cannot create enum from " + cosStorageClassString + " value!");
    }

    private final String storageClassId;

    private StorageClass(String id) {
        this.storageClassId = id;
    }

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return storageClassId;
    }

}
