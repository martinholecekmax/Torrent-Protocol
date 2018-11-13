package tracker;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import main.ATorrent;
import main.Peer;
import main.TorrentMetadata;
import main.TorrentStatus;

public class TrackerClient {
	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);

	/**
	 * Send announce to a Tracker and get response which contains list of peers and
	 * interval.
	 * 
	 * @param torrentMetadata - Information about torrent.
	 * @param peer            - Peer that sends the response.
	 * @param status          - Status of the torrent.
	 * @return instance of Response class, which contains list of peers and
	 *         interval.
	 * @throws IOException
	 * @throws JSONException
	 */
	public static TrackerResponse getResponse(TorrentMetadata torrentMetadata, Peer peer, TorrentStatus status) {
		TrackerResponse response = null;
		try {
			TrackerClient trackerClient = new TrackerClient();
			String params = trackerClient.buildParamsQuery(trackerClient.createParams(torrentMetadata, peer, status));
			response = trackerClient.parseResponse(torrentMetadata.getAnnounce(), params);
			return response;
		} catch (MalformedURLException e) {
			LOGGER.error("Trying to encode URL to UTF-8 failed.", e);
		} catch (IOException e) {
			LOGGER.error("Error, while reading from tracker website.", e);
		} catch (JSONException e) {
			LOGGER.error("Error, parsing json.", e);
		}
		return null;
	}

	/**
	 * This method will parse response to get list of Peers and interval.
	 * 
	 * @param announce
	 * @param params
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	private TrackerResponse parseResponse(String announce, String params) throws IOException, JSONException {
		String json = jsonGetRequest(announce + params);
		ArrayList<Peer> peersList = new ArrayList<>();
		JSONObject jsonObject = new JSONObject(json);
		int interval = jsonObject.getInt("interval");
		JSONArray peers = jsonObject.getJSONArray("peers");
		String peerID = "";
		String ipAddress = "";
		String localIP = "";
		int port = 0;
		for (int i = 0; i < peers.length(); i++) {
			peerID = peers.getJSONObject(i).getString("peer_id");
			ipAddress = peers.getJSONObject(i).getString("ip");
			localIP = peers.getJSONObject(i).getString("local_ip");
			port = peers.getJSONObject(i).getInt("port");
			Peer peer = new Peer(Optional.of(peerID), ipAddress, localIP, port);
			peersList.add(peer);
		}
		return new TrackerResponse(peersList, interval);
	}

	/**
	 * Create Map object of parameters needed for creation of URL query.
	 * 
	 * @param torrentMetadata
	 * @param peer
	 * @param status
	 * @return Map object containing all parameter.
	 */
	private Map<String, Object> createParams(TorrentMetadata torrentMetadata, Peer peer, TorrentStatus status) {
		Map<String, Object> params = new HashMap<>();
		params.put("peer_id", peer.getPeerID());
		params.put("info_hash", torrentMetadata.getInfoHash());
		params.put("event", status.getEvent());
		params.put("port", peer.getPort());
		params.put("downloaded", status.getDownloaded());
		params.put("uploaded", status.getUploaded());
		params.put("left", status.getLeft());
		return params;
	}

	/**
	 * Try to encode input String into UTF-8.
	 * 
	 * @param input - input string that needs to be encoded.
	 * @return encoded string
	 */
	private String urlEncodeUTF8(String input) {
		try {
			return URLEncoder.encode(input, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	/**
	 * Build query string from parameters that has been stored in map.
	 * 
	 * @param params - Map containing parameters.
	 * @return String of concatonated parameters.
	 * 
	 * @throws MalformedURLException if there has been problem with encoding
	 *                               parameter into URLEncoded string.
	 */
	private String buildParamsQuery(Map<?, ?> params) throws MalformedURLException {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<?, ?> entry : params.entrySet()) {
			if (sb.length() > 0) {
				sb.append("&");
			} else {
				sb.append("?");
			}
			sb.append(String.format("%s=%s", urlEncodeUTF8(entry.getKey().toString()),
					urlEncodeUTF8(entry.getValue().toString())));
		}
		return sb.toString();
	}

	/**
	 * Send GET Request to the tracker website and return json.
	 * 
	 * @param urlQueryString - tracker URL with query parameters
	 * @return json string received from tracker website
	 * @throws IOException
	 */
	private String jsonGetRequest(String urlQueryString) throws IOException {
		String json = null;
		URL url = new URL(urlQueryString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("charset", "utf-8");
		connection.connect();
		switch (connection.getResponseCode()) {
		case HttpURLConnection.HTTP_OK:
			InputStream inStream = connection.getInputStream();
			Scanner scanner = new Scanner(inStream, "UTF-8");
			json = scanner.useDelimiter("\\Z").next();
			scanner.close();
			return json;
		case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
			LOGGER.warn("HTTP Status-Code 504: Gateway Timeout.");			
			break;
		case HttpURLConnection.HTTP_UNAVAILABLE:
			LOGGER.warn("HTTP Status-Code 503: Service Unavailable.");
			break;		
		case 429:
			LOGGER.warn("HTTP Status-Code 429: Too many requests.");
			break;		
		default:
			LOGGER.warn("Response code:" + connection.getResponseCode() + " unknown response code.");
		}
		throw new IOException();
	}
}
