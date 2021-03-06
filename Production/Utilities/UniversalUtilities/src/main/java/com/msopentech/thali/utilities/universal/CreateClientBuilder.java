/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

THIS CODE IS PROVIDED ON AN *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED,
INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A PARTICULAR PURPOSE,
MERCHANTABLITY OR NON-INFRINGEMENT.

See the Apache 2 License for the specific language governing permissions and limitations under the License.
*/


package com.msopentech.thali.utilities.universal;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.*;
import org.apache.http.conn.scheme.*;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.*;

import java.net.*;
import java.security.*;

/**
 * Ektorp using different client builders for Java and for Android, this class lets us abtract that away so we can
 * build a common test base for both.
 */
public abstract class CreateClientBuilder {
    public static final int timeout = 2 * 60 * 1000; // TODO: This is for giving us time to debug, we should use something
                                                     // shorter in production. And yes this should be an external property.

    /**
     *
     * @param host
     * @param port
     * @param serverPublicKey If null then the server won't be validated
     * @return
     */
    public HttpClient CreateApacheClient(String host, int port, PublicKey serverPublicKey,
                                                  KeyStore clientKeyStore, char[] clientKeyStorePassPhrase, Proxy proxy)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        //  sslSocketFactory
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setUseExpectContinue(params, false); // Work around for bug in TJWS, it doesn't properly
                                                                // support ExpectContinue
        HttpConnectionParams.setSoTimeout(params, timeout);
        HttpConnectionParams.setConnectionTimeout(params, timeout);
        HttpConnectionParams.setTcpNoDelay(params, Boolean.TRUE);
        params.setParameter(ClientPNames.DEFAULT_HOST, new HttpHost(host, port, "https"));

        HttpKeyHttpClient httpKeyHttpClient = new HttpKeyHttpClient(serverPublicKey, clientKeyStore,
                clientKeyStorePassPhrase, proxy, params);
        return httpKeyHttpClient;
    }

    public HttpClient CreateApacheClient(HttpKeyURL httpKeyURL, KeyStore clientKeyStore,
                                         char[] clientKeyStorePassPhrase, Proxy proxy)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return CreateApacheClient(httpKeyURL.getHost(), httpKeyURL.getPort(), httpKeyURL.getServerPublicKey(),
                clientKeyStore, clientKeyStorePassPhrase, proxy);
    }

    abstract public org.ektorp.http.HttpClient CreateEktorpClient(String host, int port, PublicKey serverPublicKey,
                                                                  KeyStore clientKeyStore,
                                                                  char[] clientKeyStorePassPhrase, Proxy proxy)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException;

    public org.ektorp.http.HttpClient CreateEktorpClient(HttpKeyURL httpKeyURL,  KeyStore clientKeyStore,
                                                         char[] clientKeyStorePassPhrase, Proxy proxy)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return CreateEktorpClient(httpKeyURL.getHost(), httpKeyURL.getPort(), httpKeyURL.getServerPublicKey(),
                clientKeyStore, clientKeyStorePassPhrase, proxy);
    }

    abstract public HttpClient extractApacheClientFromThaliCouchDbInstance(ThaliCouchDbInstance thaliCouchDbInstance);

    /**
     * Creates a SSL Socket Factory that will validate that the server presented a cert chain that roots with the
     * key serverPublicKey and will present to the server (if asked) the key stored in clientKeyStore
     * @param serverPublicKey if set to null then any key will be accepted from the server
     * @return
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyManagementException
     * @throws java.security.UnrecoverableKeyException
     * @throws java.security.KeyStoreException
     */
    public static SSLSocketFactory getHttpKeySocketFactory(final PublicKey serverPublicKey, KeyStore clientKeyStore,
                                                    char[] clientKeyStorePassPhrase)
            throws NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException, KeyStoreException {
        return new HttpKeySSLSocketFactory(serverPublicKey, clientKeyStore, clientKeyStorePassPhrase);

        // This is the code we would use if we didn't have to use the outdated Apache code in Android

//        // Adapted from http://stackoverflow.com/questions/2703161/how-to-ignore-ssl-certificate-errors-in-apache-httpclient-4-0 and from configureScheme in StdHttpClient.java in Ektorp
//        SSLContext sslContext = SSLContext.getInstance("TLS");
//        final ThaliPublicKeyComparer thaliPublicKeyComparer = serverPublicKey == null ? null : new ThaliPublicKeyComparer(serverPublicKey);
//
//        TrustManager trustManager = new X509TrustManager() {
//            @Override
//            public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
//                throw new RuntimeException("We should not have gotten a client trusted call, authType was:" + authType);
//            }
//
//            @Override
//            public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
//                //TODO: We actually need to restrict authTypes to known secure ones
//                if (serverPublicKey == null) {
//                    return;
//                }
//                PublicKey rootPublicKey = x509Certificates[x509Certificates.length -1].getPublicKey();
//                if (thaliPublicKeyComparer.KeysEqual(rootPublicKey) == false)
//                {
//                    throw new RuntimeException("Presented server root key does not match expected server root key");
//                }
//            }
//
//            @Override
//            public X509Certificate[] getAcceptedIssuers() {
//                return null;
//            }
//        };
//
//        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//        keyManagerFactory.init(clientKeyStore, clientPassPhrase);
//
//        sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { trustManager }, null);
//
//        return new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }
}
