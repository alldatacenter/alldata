/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.kafka.authorizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public final class KafkaTestUtils {
    
    public static String createAndStoreKey(String subjectName, String issuerName, BigInteger serial, String keystorePassword,
    		String keystoreAlias, String keyPassword, KeyStore trustStore) throws Exception {
    	
    	// Create KeyPair
    	KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    	keyPairGenerator.initialize(2048, new SecureRandom());
    	KeyPair keyPair = keyPairGenerator.generateKeyPair();
    	
    	Date currentDate = new Date();
    	Date expiryDate = new Date(currentDate.getTime() + 365L * 24L * 60L * 60L * 1000L);
    	
    	// Create X509Certificate
    	X509v3CertificateBuilder certBuilder =
    			new X509v3CertificateBuilder(new X500Name(RFC4519Style.INSTANCE, issuerName), serial, currentDate, expiryDate, 
    					new X500Name(RFC4519Style.INSTANCE, subjectName), SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
    	ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
    	X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certBuilder.build(contentSigner));
    	
    	// Store Private Key + Certificate in Keystore
    	KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    	keystore.load(null, keystorePassword.toCharArray());
    	keystore.setKeyEntry(keystoreAlias, keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[] {certificate});
    	
    	File keystoreFile = File.createTempFile("kafkakeystore", ".jks");
    	try (OutputStream output = new FileOutputStream(keystoreFile)) {
    		keystore.store(output, keystorePassword.toCharArray());
    	}
    	
    	// Now store the Certificate in the truststore
    	trustStore.setCertificateEntry(keystoreAlias, certificate);
    	
    	return keystoreFile.getPath();
    	
    }

	static void createSomeTopics(Properties adminProps) {
		try (AdminClient adminClient = AdminClient.create(adminProps)) {
			adminClient.createTopics(Arrays.asList(
					new NewTopic("test", 1, (short) 1),
					new NewTopic("dev", 1, (short) 1)
			));
		}
	}
}
