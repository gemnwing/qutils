package net.njcp.service.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

public class QHttpUtil {

	private final static String USER_AGENT = "Mozilla/5.0";

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
			if ( args[i + 1] instanceof List ) {
				List<?> values = (List<?>) args[i + 1];
				for ( Object value : values ) {
					urlWithParams.append(firstArg ? "?" : "&").append(key).append("=").append((value == null) ? "" : value);
				}
			} else {
				Object value = args[i + 1];
				urlWithParams.append(firstArg ? "?" : "&").append(key).append("=").append((value == null) ? "" : value);
			}
			firstArg = false;
		}

		QLog.debug("Request url:" + urlWithParams);
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
		String retStr = null;
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();
		HttpPost post = new HttpPost(url);

		// add header
		post.setHeader("User-Agent", USER_AGENT);

		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();

		for ( int i = 0; i < args.length; i += 2 ) {
			if ( args[i] == null || i + 1 >= args.length ) {
				continue;
			}
			String key = args[i].toString();
			if ( args[i + 1] instanceof List ) {
				List<?> values = (List<?>) args[i + 1];
				for ( Object value : values ) {
					urlParameters.add(new BasicNameValuePair(key, (value == null) ? "" : value.toString()));
				}
			} else {
				Object value = args[i + 1];
				urlParameters.add(new BasicNameValuePair(key, (value == null) ? "" : value.toString()));
			}
		}

		post.setEntity(new UrlEncodedFormEntity(urlParameters));

		HttpResponse response = client.execute(post);
		QLog.debug("\nSending 'POST' request to URL : " + url);
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
		QLog.setDebugFlag(true);

		String urlGet = "http://localhost:8080/script-caller-service/webapi/run";

		QLog.println(sendGet(urlGet, "analyid", 1234, "app", "test", "serv", "python", "args", new ArrayList<String>() {
			{
				add("3");
				add("4");
			}
		}, "timeout", 500));

		// String urlPost = "http://localhost:8080/script-caller-service/webapi/post";
		// QLog.println(sendPost(urlPost));

	}

}