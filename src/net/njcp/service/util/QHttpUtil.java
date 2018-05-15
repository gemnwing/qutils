package net.njcp.service.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

public class QHttpUtil {

	private final static String USER_AGENT = "Mozilla/5.0";
	private final static String CHARSET = "UTF-8";

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

	// HTTP GET request
	public static String sendGet(String url, Object... args) throws Exception {
		String retStr = null;
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		// add request header
		StringBuilder urlWithParams = new StringBuilder(url);
		boolean firstArg = true;
		for ( int i = 0; i < args.length; i += 2 ) {
			Object key = args[i];
			if ( key == null || i + 1 >= args.length ) {
				continue;
			}
			key = toUrlString(key);
			if ( args[i + 1] instanceof List ) {
				List<?> values = (List<?>) args[i + 1];
				for ( Object value : values ) {
					urlWithParams.append(firstArg ? "?" : "&").append(key).append("=").append((value == null) ? "" : toUrlString(value));
				}
			} else {
				Object value = args[i + 1];
				urlWithParams.append(firstArg ? "?" : "&").append(key).append("=").append((value == null) ? "" : toUrlString(value));
			}
			firstArg = false;
		}

		HttpGet request = new HttpGet(urlWithParams.toString());
		request.addHeader("User-Agent", USER_AGENT);

		HttpResponse response = client.execute(request);

		QLog.debug("Sending 'GET' request to URL : " + urlWithParams);
		QLog.debug("Response Code : " + response.getStatusLine().getStatusCode());

		BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

		String line = null;
		while ( (line = br.readLine()) != null ) {
			retStr = (retStr == null ? "" : retStr) + line + "\n";
		}

		return retStr.replaceAll("\n$", "");

	}

	// HTTP POST request
	public static String sendPost(String url, Object... args) throws Exception {
		return processPostRequest(url, null, null, args);
	}

	// HTTP POST request
	public static String sendPostWithHeadersAndEntities(String url, Map<?, ?> headers, Map<?, ?> entities, Object... args) throws Exception {
		return processPostRequest(url, headers, entities, args);
	}

	public static String sendPostWithHeadersAndSingleEntity(String url, Map<?, ?> headers, String entity, Object... args) throws Exception {
		return processPostRequest(url, headers, entity, args);
	}

	private static String processPostRequest(String url, Map<?, ?> headers, Object entityParam, Object... args) throws Exception {
		String retStr = null;
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		StringBuilder urlWithParams = new StringBuilder(url);
		boolean firstArg = true;
		for ( int i = 0; i < args.length; i += 2 ) {
			Object key = args[i];
			if ( key == null || i + 1 >= args.length ) {
				continue;
			}
			key = toUrlString(key);
			if ( args[i + 1] instanceof List ) {
				List<?> values = (List<?>) args[i + 1];
				for ( Object value : values ) {
					urlWithParams.append(firstArg ? "?" : "&").append(key).append("=").append((value == null) ? "" : toUrlString(value));
				}
			} else {
				Object value = args[i + 1];
				urlWithParams.append(firstArg ? "?" : "&").append(key).append("=").append((value == null) ? "" : toUrlString(value));
			}
			firstArg = false;
		}

		HttpPost post = new HttpPost(urlWithParams.toString());
		// add header
		post.setHeader("User-Agent", USER_AGENT);
		if ( headers != null && !headers.isEmpty() ) {
			for ( Object key : headers.keySet() ) {
				String value = String.valueOf(headers.get(key));
				post.addHeader(String.valueOf(key), value);
				QLog.debug("Adding a header<" + key + "=" + value + ">");
			}
		}
		HttpEntity entity = null;
		if ( entityParam instanceof Map<?, ?> ) {
			Map<?, ?> entities = (Map<?, ?>) entityParam;
			List<NameValuePair> entityPair = new ArrayList<NameValuePair>();
			for ( Object key : entities.keySet() ) {
				String value = String.valueOf(entities.get(key));
				entityPair.add(new BasicNameValuePair(String.valueOf(key), value));
				QLog.debug("Adding a pair of entity<" + key + "=" + value + ">");
			}
			entity = new UrlEncodedFormEntity(entityPair);
		} else {
			QLog.debug("Adding a single string entity<" + QStringUtil.cutStringWithEllipsis(entityParam, 47) + ">");
			entity = new StringEntity(String.valueOf(entityParam));
		}

		post.setEntity(entity);
		HttpResponse response = client.execute(post);
		QLog.debug("\nSending 'POST' request to URL : " + urlWithParams);
		QLog.debug("Post parameters : " + post.getEntity());
		QLog.debug("Response Code : " + response.getStatusLine().getStatusCode());

		BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

		String line = null;
		while ( (line = br.readLine()) != null ) {
			retStr = (retStr == null ? "" : retStr) + line + "\n";
		}

		return retStr.replaceAll("\n$", "");

	}

	@SuppressWarnings("serial")
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
		System.out.println(
				java.net.URLEncoder.encode("a< >b", "UTF-8")
		//
		);
		// String urlPost = "http://localhost:8080/script-caller-service/webapi/post";
		// QLog.println(sendPost(urlPost));

	}

}