// package com.example.sbaprs;
import java.net.*;
import java.io.*;
import java.util.*;

/* Test out Android code
 *
 * https://developer.android.com/reference/java/net/HttpURLConnection
 * https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html
 */

public class AprsApiClient {

    private static String hostname;

    private static int port;

    private static int mheard_max = 5;
    AprsApiClient() {
        port = 8000;
        hostname = "aprs.jxqz.org";
    }
    AprsApiClient(String h, int p) {
        hostname = h;
        port = p;
    }

    public static Vector getMheard() {

	String urlString = "http://jxqz.org/~steve/mheard.txt";
        String mheardCsv = null;
        HttpURLConnection connection = null;
	URL url = null;
	StringBuffer response = null;

	Vector<String> v = null;

	try {
		// https://docs.oracle.com/javase/8/docs/api/java/net/URL.html

            	url = new URL(urlString);

	} catch (Exception e) {

		System.err.println("error: " + e.getMessage());
		
		return null;
	}	
        
        try {

	    // https://www.digitalocean.com/community/tutorials/java-httpurlconnection-example-java-http-request-get-post
            
            connection = (HttpURLConnection) url.openConnection();

            if (connection.getResponseCode() == 200) {

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

		String line;

		v = new Vector<>();

		while ((line = in.readLine()) != null)
			v.add(line);

		in.close();

	    }

        } catch (IOException e) {

		System.err.println("error: " + e.getMessage());

        } finally {
            
            connection.disconnect();
        }

	return v;

    }

	public static void main(String[] args) {

		System.out.println("Test Android Code");

		// add try catch : getMheard should throw IOException ?

		System.out.println(getMheard());

	}
}
