package jpstrack.upload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.darwinsys.io.FileIO;

/**
 * Support for uploading GPX Trace files to OSM.
 * Refer to http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.2
 * @author Ian Darwin
 */
public class Upload {

	private static final String API_CREATE_URL = "/api/0.6/gpx/create";
	private final static String BOUNDARY = "ANOTHER_GREAT_OSM_TRACE_FILE";
	static boolean debug = false;


	/** Handle the HTTP POST conversation */
	public static UploadResponse converse(String host, int port, String userName, String password, String postBody) throws IOException {
		UploadResponse ret = new UploadResponse();
		URL url = new URL("http", host, port, API_CREATE_URL);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		final String auth = "Basic " + new String(Base64.encodeBytes((userName + ":" + password).getBytes()));
		conn.setRequestProperty("Authorization", auth);
		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
		if (debug) {
			System.out.println("--- About to send POST with these Request Headers: ---");
			for (String s : conn.getRequestProperties().keySet()) {
				System.out.println(s + ": " + conn.getRequestProperty(s));
			}
		}
		conn.setDoInput(true);	// we want POST!
		conn.setDoOutput(true);
		conn.setAllowUserInteraction(true);

		conn.connect();
		
		final OutputStream outputStream = conn.getOutputStream();
		outputStream.write(postBody.getBytes());
		outputStream.close();

		ret.status = conn.getResponseCode();
		
		// The response from this REST is a single line with the GPX ID
		BufferedReader in = new BufferedReader(
				new InputStreamReader(
						conn.getInputStream()));
		String line = null;
		if ((line = in.readLine()) != null) {
			ret.gpxId = Long.parseLong(line);
		}
		if (in != null) {
			in.close();
		}
		return ret;
	}

	/** Get the GPX file and other parameters into shape for POSTing */
	public static String encodePostBody(String description, TraceVisibility visibility, File gpxFile) throws IOException {
		StringBuffer body = new StringBuffer();
		body.append(encodePlainTextPart("description", description, true));
		body.append(encodePlainTextPart("visibility", visibility.toString().toLowerCase(), false));
		body.append(encodePlainTextPart("tags", "test", false));

		body.append("\r\n--" + BOUNDARY + "\r\n");
		body.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + gpxFile.getName() + "\"\r\n");
		body.append("Content-Type: application/gpx+xml\r\n");
		body.append("\r\n");

		body.append(FileIO.readerToString(new FileReader(gpxFile)));

		body.append("\r\n--" + BOUNDARY + "--\r\n");
		return body.toString();
	}

	private static String encodePlainTextPart(String partName, String partBody, boolean first) {
		return (first? "" : "\r\n") + "--" + BOUNDARY + "\r\n"
				+ "Content-Disposition: form-data; name=\"" + partName + "\"\r\n\r\n" 
				+ partBody;
	}

	public static void setDebug(boolean debug) {
		Upload.debug = debug;
	}
}
