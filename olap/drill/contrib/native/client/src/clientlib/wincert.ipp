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
#if defined(IS_SSL_ENABLED)

#include <openssl/x509.h>
#include <openssl/ssl.h>

#if defined _WIN32  || defined _WIN64

#include <stdio.h>
#include <windows.h>
#include <wincrypt.h>
#include <cryptuiapi.h>
#include <iostream>
#include <tchar.h>


#pragma comment (lib, "crypt32.lib")
#pragma comment (lib, "cryptui.lib")

#define MY_ENCODING_TYPE  (PKCS_7_ASN_ENCODING | X509_ASN_ENCODING)

inline
int loadSystemTrustStore(const SSL *ssl, std::string& msg) {
    HCERTSTORE hStore;
    PCCERT_CONTEXT pContext = NULL;
    X509 *x509;
	char* stores[] = {
	    "CA",
		"MY",
		"ROOT",
		"SPC"
	};
	int certCount=0;
     
    SSL_CTX * ctx = SSL_get_SSL_CTX(ssl);
    X509_STORE *store = SSL_CTX_get_cert_store(ctx);

	for(int i=0; i<4; i++){
    hStore = CertOpenSystemStore(NULL, stores[i]);

    if (!hStore){
        msg.append("Failed to load store: ").append(stores[i]).append("\n");
        continue;
    }

    while (pContext = CertEnumCertificatesInStore(hStore, pContext)) {
        //uncomment the line below if you want to see the certificates as pop ups
        //CryptUIDlgViewContext(CERT_STORE_CERTIFICATE_CONTEXT, pContext,   NULL, NULL, 0, NULL);

        x509 = NULL;
        x509 = d2i_X509(NULL, (const unsigned char **)&pContext->pbCertEncoded, pContext->cbCertEncoded);
        if (x509) {
            int ret = X509_STORE_add_cert(store, x509);

            //if (ret == 1)
            //    std::cout << "Added certificate " << x509->name << " from " << stores[i] << std::endl;

            X509_free(x509);
            certCount++;
        }
    }

    CertFreeCertificateContext(pContext);
    CertCloseStore(hStore, 0);
	}
	if(certCount==0){
	    msg.append("No certificates found.");
	    return -1;
	}
    return 0;
}

#else // notwindows
inline
int loadSystemTrustStore(const SSL *ssl, std::string& msg) {
    return 0;
}

#endif // WIN32 or WIN64

#endif // SSL_ENABLED
