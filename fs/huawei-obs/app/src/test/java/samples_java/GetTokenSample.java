package samples_java;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy.Type;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.obs.services.internal.Constants.CommonHeaders;

import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class GetTokenSample {
	private static final String endPoint = "https://your-iam-endpoint";

	private static final String userName = "*** Provide your user name ***";

	private static final String passWord = "*** Provide your password ***";

	private static final String domainName = "*** Provide your domain name ***";
	
	private static final long durationSeconds = 900L;
	
	private static String token;
	
	private static final boolean proxyIsable = false;
	
	private static final String proxyHostAddress = "*** Provide your proxy host address ***"; 
	
	private static final int proxyPort = 8080; 
	
	private static final String proxyUser = "*** Provide your proxy user ***";
	
	private static final String proxyPassword = "*** Provide your proxy password ***";

	public static void main(String[] args) {
		getToken();
		
		getSecurityToken(token);
	}

	private static void getToken() {
		Request.Builder builder = new Request.Builder();
		builder.addHeader("Content-Type", "application/json;charset=utf8");
		builder.url(endPoint + "/v3/auth/tokens");
		String mimeType = "application/json";
		/* request body sample
		   {
			"auth": {
				"identity": {
					"methods": ["password"],
					"password": {
						"user": {
							"name": "***userName***",
							"password": "***passWord***",
							"domain": {
								"name": "***domainName***"
							}
						}
					}
				},
				"scope": {
					"domain": {
						"name": "***domainName***"
					}
				}
			}
		  }
		 */
		String content = "{\r\n" + 
				"			\"auth\": {\r\n" + 
				"				\"identity\": {\r\n" + 
				"					\"methods\": [\"password\"],\r\n" + 
				"					\"password\": {\r\n" + 
				"						\"user\": {\r\n" + 
				"							\"name\": \"" + userName + "\",\r\n" + 
				"							\"password\": \"" + passWord + "\",\r\n" + 
				"							\"domain\": {\r\n" + 
				"								\"name\": \"" + domainName + "\"\r\n" + 
				"							}\r\n" + 
				"						}\r\n" + 
				"					}\r\n" + 
				"				},\r\n" + 
				"				\"scope\": {\r\n" + 
				"					\"domain\": {\r\n" + 
				"						\"name\": \"" + domainName + "\"\r\n" + 
				"					}\r\n" + 
				"				}\r\n" + 
				"			}\r\n" + 
				"		  }";
		try {
			builder.post(createRequestBody(mimeType, content));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		try {
			getTokeResponse(builder.build());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void getTokeResponse(Request request) throws IOException {
		Call c = GetHttpClient().newCall(request);
		Response res = c.execute();
		if (res.headers() != null) {
			String header = res.headers().toString();
			if (header == null || header.trim().equals("")) {
				System.out.println("\n");
			} else {
				String subjectToken = res.header("X-Subject-Token").toString();
				System.out.println("the Token :" + subjectToken);
				token = subjectToken;
			}
		}
		res.close();
	}
	
	private static void getSecurityToken(String token) {
		if(token == null) {
			
		}
		Request.Builder builder = new Request.Builder();
		builder.addHeader("Content-Type", "application/json;charset=utf8");
		builder.url(endPoint + "/v3.0/OS-CREDENTIAL/securitytokens");
		String mimeType = "application/json";
		
		/* request body sample
			 {
			    "auth": {
			        "identity": {
			            "methods": [
			                "token"
			            ],
			            "token": {
			                "id": "***yourToken***",
			                "duration-seconds": "***your-duration-seconds***"
			            }
			        }
			    }
			}
		 */
		String content = "{\r\n" + 
				"    \"auth\": {\r\n" + 
				"        \"identity\": {\r\n" + 
				"            \"methods\": [\r\n" + 
				"                \"token\"\r\n" + 
				"            ],\r\n" + 
				"            \"token\": {\r\n" + 
				"                \"id\": \""+ token +"\",\r\n" + 
				"                \"duration-seconds\": \""+ durationSeconds +"\"\r\n" + 
				"\r\n" + 
				"            }\r\n" + 
				"        }\r\n" + 
				"    }\r\n" + 
				"}";
		
		try {
			builder.post(createRequestBody(mimeType, content));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		try {
			getSecurityTokenResponse(builder.build());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void getSecurityTokenResponse(Request request) throws IOException {
		Call c = GetHttpClient().newCall(request);
		Response res = c.execute();
		if (res.body() != null) {
			String content = res.body().string();
			if (content == null || content.trim().equals("")) {
				System.out.println("\n");
			} else {
				System.out.println("Content:" + content + "\n\n");
			}
		} else {
			System.out.println("\n");
		}
		res.close();
	}

	private static OkHttpClient GetHttpClient() {
		X509TrustManager xtm = new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				X509Certificate[] x509Certificates = new X509Certificate[0];
				return x509Certificates;
			}
		};

		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, new TrustManager[] { xtm }, new SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}

		HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
			@Override
			public boolean verify(String arg0, SSLSession arg1) {
				return true;
			}
		};

		OkHttpClient.Builder builder = new OkHttpClient.Builder().followRedirects(false).retryOnConnectionFailure(false)
				.sslSocketFactory(sslContext.getSocketFactory()).hostnameVerifier(DO_NOT_VERIFY).cache(null);
		
		if(proxyIsable) {
			builder.proxy(new java.net.Proxy(Type.HTTP, new InetSocketAddress(proxyHostAddress, proxyPort)));
	        
	        Authenticator proxyAuthenticator = new Authenticator() {
	            @Override public Request authenticate(Route route, Response response) throws IOException {
	                 String credential = Credentials.basic(proxyUser, proxyPassword);
	                 return response.request().newBuilder()
	                     .header(CommonHeaders.PROXY_AUTHORIZATION, credential)
	                     .build();
	            }
	        };
	        builder.proxyAuthenticator(proxyAuthenticator);
		}

        return builder.build();
	}

	private static RequestBody createRequestBody(String mimeType, String content) throws UnsupportedEncodingException {
		return RequestBody.create(MediaType.parse(mimeType), content.getBytes("UTF-8"));
	}
}
