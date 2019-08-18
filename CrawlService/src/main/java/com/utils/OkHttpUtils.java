package com.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

import javax.net.ssl.*;

public class OkHttpUtils {
	private static OkHttpClient okHttpClient = null;

	private static void setProxyInfo(OkHttpClient.Builder builder, String hostName, Integer port) {
		if (hostName!=null && null != port && 0 != port) {
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostName, port));
			builder.proxy(proxy);
		}
	}

	/**
	 * @param proxyHostName
	 * @param proxyPort
	 * @return
	 */
	private static OkHttpClient getProxyHttpClient(String proxyHostName, Integer proxyPort) {
		if (null == okHttpClient) {
			OkHttpClient.Builder builder = new OkHttpClient.Builder();
			builder.addInterceptor(new Interceptor() {
				@Override
				public Response intercept(Chain chain) throws IOException {
					//log.info("GzipRequestInterceptor　chain.request().toString():"+chain.request().toString());
					Request request = chain.request()
							.newBuilder()
							.build();
					//log.info("GzipRequestInterceptor　request.toString():"+request.toString());
					return chain.proceed(request);
				}
			});
			builder.connectionPool(new ConnectionPool(2, 2, TimeUnit.MINUTES));
			//设置连接超时时间
			builder.connectTimeout(1, TimeUnit.MINUTES)
					.readTimeout(1, TimeUnit.MINUTES)
					.writeTimeout(1, TimeUnit.MINUTES)
					//设置信任所有SSL证书
					.sslSocketFactory(createSSLSocketFactory(), mMyTrustManager)
					.hostnameVerifier(new TrustAllHostnameVerifier());
			//设置代理,需要替换 (只针对获取数据)
			setProxyInfo(builder, proxyHostName, proxyPort);
			//cookie管理器

			OkHttpClient buildClient = builder.build();
			return buildClient;
		} else {
			return okHttpClient;
		}
	}

	public static Response  postSync(Request request ){
		InetAddress inetAddress = null;
		try {
			inetAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		String ip = inetAddress.getHostAddress();
		OkHttpClient client = getProxyHttpClient(ip, 8888);
		Call call = client.newCall(request);

		try {
			Response response = call.execute();
			return response;

		} catch (IOException e) {
			return null;
		}
	}


    private static void requestBuilderWithHeaders(Request.Builder builder, Map<String, String> headers) {
        if (null != headers) {
            headers.forEach((key, value) -> {
                if (key!=null) {
                    builder.addHeader(key, value);
                }
            });
        }
    }

    /**
     * 发送get 请求
     *
     * @param url
     * @param headers
     * @return
     */
    public static String httpGetWithProxy(String url, Map<String, String> headers) {

        Request.Builder requestBuilder = new Request.Builder();
        //设置header
        requestBuilderWithHeaders(requestBuilder, headers);

        Request cookieRequest = requestBuilder
                .url(url)
                .get()
                .build();

        try {

            InetAddress inetAddress = null;
            try {
                inetAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            String ip = inetAddress.getHostAddress();
            OkHttpClient client = getProxyHttpClient(ip, 8888);
            Response response = client.newCall(cookieRequest).execute();
            ResponseBody body = response.body();

            if (response.isSuccessful() && null != body) {
                String responseString = body.string();
                return responseString;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
	
	public static Response postSync(RequestBean poiRequest) throws IOException {
		//System.out.println(poiRequest.requestUrl+"\r\n" + (poiRequest.requestBody));
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String ip = inetAddress.getHostAddress();
        OkHttpClient client = getProxyHttpClient(ip, 8888);
		final Request request = new Request.Builder().url(poiRequest.requestUrl)
				.post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), poiRequest.requestBody))
				.build();
		Call call = client.newCall(request);
        return call.execute();
		
		
	}

	private static MyTrustManager mMyTrustManager;

	private static SSLSocketFactory createSSLSocketFactory() {
		SSLSocketFactory ssfFactory = null;
		try {
			mMyTrustManager = new MyTrustManager();
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, new TrustManager[]{mMyTrustManager}, new SecureRandom());
			ssfFactory = sc.getSocketFactory();
		} catch (Exception ignored) {
			ignored.printStackTrace();
		}

		return ssfFactory;
	}

	//实现X509TrustManager接口
	public static class MyTrustManager implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws
				CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}

	//实现HostnameVerifier接口
	private static class TrustAllHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}


}
