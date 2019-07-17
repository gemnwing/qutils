package net.njcp.service.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.util.MultiValueMap;

import net.njcp.service.util.I18N;

public class QHttpUtil {

	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String CHARSET = I18N.tr("UTF-8");
	private final static int MAX_DEBUG_STRING_LENGTH = 50;

	private static String toUrlString(Object str) {
		if ( str == null ) {
			return "";
		} else {
			try {
				return URLEncoder.encode(String.valueOf(str), CHARSET);
			} catch ( UnsupportedEncodingException e ) {
				return String.valueOf(str);
			}
		}
	}

	public static class LinkedMultiValueMap<K, V> extends LinkedHashMap<K, List<V>> implements MultiValueMap<K, V> {
		private static final long serialVersionUID = 1L;

		@Override
		public void add(K key, V value) {
			List<V> values = containsKey(key) ? get(key) : new ArrayList<V>();
			values.add(value);
			put(key, values);
		}

		@Override
		public V getFirst(K key) {
			if ( !containsKey(key) || get(key).size() == 0 ) {
				return null;
			}
			return get(key).get(0);
		}

		@Override
		public void set(K key, V value) {
			List<V> values = new ArrayList<V>();
			values.add(value);
			put(key, values);
		}

		@Override
		public void setAll(Map<K, V> values) {
			for ( K key : values.keySet() ) {
				set(key, values.get(key));
			}
		}

		@Override
		public Map<K, V> toSingleValueMap() {
			LinkedHashMap<K, V> retMap = new LinkedHashMap<K, V>();
			for ( K key : keySet() ) {
				retMap.put(key, getFirst(key));
			}
			return retMap;
		}

	}

	public static class ParameterMap extends LinkedMultiValueMap<String, String> {
		private static final long serialVersionUID = 1L;

		public void add(String key, Object value) {
			add(key, value == null ? null : String.valueOf(value));
		}

		public String getSingleValue(String key, String defaultValue) {
			List<String> values = get(key);
			if ( values != null && !values.isEmpty() ) {
				for ( String value : values ) {
					if ( values != null ) {
						return value;
					}
				}
			}
			return defaultValue;
		}

		public static ParameterMap parse(String paramStr) {
			ParameterMap queryParameters = new ParameterMap();
			if ( paramStr == null || paramStr.length() == 0 ) {
				return queryParameters;
			}
			int s = 0;
			do {
				final int e = paramStr.indexOf('&', s);
				if ( e == -1 ) {
					decodeQueryParam(queryParameters, paramStr.substring(s));
				} else if ( e > s ) {
					decodeQueryParam(queryParameters, paramStr.substring(s, e));
				}
				s = e + 1;
			} while ( s > 0 && s < paramStr.length() );
			return queryParameters;
		}

		private static void decodeQueryParam(ParameterMap params, String param) {
			try {
				final int equals = param.indexOf('=');
				if ( equals > 0 ) {
					String key = URLDecoder.decode(param.substring(0, equals), I18N.tr("UTF-8"));
					String value = URLDecoder.decode(param.substring(equals + 1), I18N.tr("UTF-8"));
					params.add(key, value);
				} else if ( equals == 0 ) {
					// no key declared, ignore
				} else if ( param.length() > 0 ) {
					String key = URLDecoder.decode(param, I18N.tr("UTF-8"));
					params.add(key, "");
				}
			} catch ( final UnsupportedEncodingException ex ) {
				// This should never occur
				throw new IllegalArgumentException(ex);
			}
		}
	}

	public static MultiValueMap<String, String> decodeQuery(String q) {
		return ParameterMap.parse(q);
	}

	// HTTP GET request
	public static HttpResponse sendGet(String url, Object... args) throws Exception {
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		// add request header
		StringBuilder urlWithParams = buildUrlWithArgs(url, args);

		HttpGet request = new HttpGet(urlWithParams.toString());
		request.addHeader("User-Agent", USER_AGENT);
		request.addHeader("charset", I18N.tr("UTF-8"));

		QLog.debug(I18N.tr("Sending 'GET' request to URL: {0}", urlWithParams));

		HttpResponse response = client.execute(request);

		if ( QLog.isDebugMode() ) {
			QLog.debug(I18N.tr("Response Code: {0}", response.getStatusLine().getStatusCode()));
			QLog.debug(I18N.tr("Response content:\n{0}", QStringUtil.cutStringWithEllipsis(getContent(response).replaceAll("\n", ""), MAX_DEBUG_STRING_LENGTH - 3)));
		}
		return response;

	}

	public static String getContent(HttpResponse response) {
		StringBuilder retSb = new StringBuilder();
		try {
			Charset charset = null;
			try {
				for ( HeaderElement element : response.getHeaders("Content-Type")[0].getElements() ) {
					NameValuePair nv = element.getParameterByName("charset");
					if ( nv != null ) {
						charset = Charset.forName(nv.getValue());
					}
					break;
				}
			} catch ( Throwable t ) {
				charset = null;
			}
			if ( charset == null ) {
				charset = Charset.defaultCharset();
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), charset));
			String line = null;
			while ( (line = br.readLine()) != null ) {
				retSb.append(line).append("\n");
			}
		} catch ( Throwable t ) {
			QLog.error(I18N.tr("Failed to get content from response."), t);
		}
		return retSb.toString().replaceAll("\n$", "");
	}

	// HTTP POST request
	public static HttpResponse sendPost(String url, Object... args) throws Exception {
		return processPostRequest(url, null, null, args);
	}

	// HTTP POST request
	public static HttpResponse sendPostWithHeadersAndEntities(String url, Map<?, ?> headers, Map<?, ?> entities, Object... args) throws Exception {
		return processPostRequest(url, headers, entities, args);
	}

	public static HttpResponse sendPostWithHeadersAndSingleEntity(String url, Map<?, ?> headers, String entity, Object... args) throws Exception {
		return processPostRequest(url, headers, entity, args);
	}

	private static HttpResponse processPostRequest(String url, Map<?, ?> headers, Object entityParam, Object... args) throws Exception {
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		StringBuilder urlWithParams = buildUrlWithArgs(url, args);
		HttpPost post = new HttpPost(urlWithParams.toString());
		// add header
		post.setHeader("User-Agent", USER_AGENT);
		if ( headers != null && !headers.isEmpty() ) {
			for ( Object key : headers.keySet() ) {
				String value = String.valueOf(headers.get(key));
				post.addHeader(String.valueOf(key), value);
				QLog.debug(I18N.tr("Adding a header<{0}={1}>", key, value));
			}
		}
		// post.addHeader("charset", "UTF-8");
		HttpEntity entity = null;
		if ( entityParam instanceof Map<?, ?> ) {
			Map<?, ?> entities = (Map<?, ?>) entityParam;
			List<NameValuePair> entityPair = new ArrayList<NameValuePair>();
			for ( Object key : entities.keySet() ) {
				String value = String.valueOf(entities.get(key));
				entityPair.add(new BasicNameValuePair(String.valueOf(key), value));
				QLog.debug(I18N.tr("Adding a pair of entity<{0}={1}>", key, QStringUtil.cutStringWithEllipsis(value, MAX_DEBUG_STRING_LENGTH - 3)));
			}
			entity = new UrlEncodedFormEntity(entityPair);
		} else {
			QLog.debug(I18N.tr("Adding a single string entity<{0}>", QStringUtil.cutStringWithEllipsis(entityParam, MAX_DEBUG_STRING_LENGTH - 3)));
			entity = new StringEntity(String.valueOf(entityParam), I18N.tr("UTF-8"));
		}

		post.setEntity(entity);

		QLog.debug(I18N.tr("Sending 'POST' request to URL: {0}", urlWithParams));

		HttpResponse response = client.execute(post);

		if ( QLog.isDebugMode() ) {
			QLog.debug(I18N.tr("Response Code: {0}", response.getStatusLine().getStatusCode()));
			QLog.debug(I18N.tr("Response content:\n{0}", QStringUtil.cutStringWithEllipsis(getContent(response).replaceAll("\n", ""), MAX_DEBUG_STRING_LENGTH - 3)));
		}

		return response;

	}

	private static void patchUrlWithParamMap(StringBuilder paramUrl, LinkedHashMap<String, List<String>> paramMap) {
		if ( !paramMap.isEmpty() ) {
			for ( String key : paramMap.keySet() ) {
				List<String> values = paramMap.get(key);
				for ( String value : values ) {
					paramUrl.append(paramUrl.length() == 0 ? "?" : "&").append(key).append("=").append(toUrlString(value));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static StringBuilder buildUrlWithArgs(String url, Object... args) {
		StringBuilder urlSb = new StringBuilder();
		boolean kvSwitch = true;
		Object key = null;
		for ( int i = 0; i < args.length; i++ ) {
			if ( args[i] instanceof LinkedHashMap<?, ?> ) {
				try {
					patchUrlWithParamMap(urlSb, (LinkedHashMap<String, List<String>>) args[i]);
				} catch ( Throwable t ) {
				}
			} else {
				if ( kvSwitch ) {
					// key
					key = args[i];
					if ( key == null || i + 1 >= args.length ) {
						continue;
					}
					key = toUrlString(key);
				} else {
					// value
					if ( args[i] instanceof List ) {
						List<?> values = (List<?>) args[i];
						for ( Object value : values ) {
							urlSb.append(urlSb.length() == 0 ? "?" : "&").append(key).append("=").append(toUrlString(value));
						}
					} else {
						Object value = args[i];
						urlSb.append(urlSb.length() == 0 ? "?" : "&").append(key).append("=").append(toUrlString(value));
					}
				}
				kvSwitch = !kvSwitch;
			}
		}
		return new StringBuilder(url.replaceFirst("/$", "")).append(urlSb);
	}

	public static void main(String[] args) throws Exception {
		// QLog.setDebugFlag(true);
		//
		// String urlGet = "http://localhost:8080/script-caller-service/webapi/run";
		//
		// QLog.println(sendGet(urlGet, "analyid", 1234, "app", "test", "serv", "python", "args", new ArrayList<String>() {
		// {
		// add("3");
		// add("4");
		// }
		// }, "timeout", 500));
		StringBuilder sb = new StringBuilder();
		sb = buildUrlWithArgs("http://abc/xyz/", "a", new ParameterMap() {
			{
				add("x", 10);
				add("y", 11);
				add("z", 12);
			}
		}, 1, "b", new ArrayList<String>() {
			{
				add("i");
				add("ii");
				add("iii");
			}
		//
		}, "c", 3, "d", 4 );
		//
		System.out.println(sb );
		// String urlPost = "http://localhost:8080/script-caller-service/webapi/post";
		// QLog.println(sendPost(urlPost));

	}

}
