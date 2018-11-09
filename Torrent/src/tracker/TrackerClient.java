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
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import main.Peer;
import main.TorrentMetadata;
import main.TorrentStatus;

public class TrackerClient {	
	/**
	 * Send announce to a Tracker and get response which contains list of peers and interval.
	 * 
	 * @param torrentMetadata - Information about torrent.
	 * @param peer - Peer that sends the response.
	 * @param status - Status of the torrent.
	 * @return instance of Response class, which contains list of peers and interval.
	 * @throws IOException
	 * @throws JSONException
	 */
	public static TrackerResponse getResponse(TorrentMetadata torrentMetadata, Peer peer, TorrentStatus status)
			throws IOException, JSONException {
		TrackerClient trackerClient = new TrackerClient();
		String params = trackerClient.buildParamsQuery(trackerClient.createParams(torrentMetadata, peer, status));
		TrackerResponse response = trackerClient.parseResponse(torrentMetadata.getAnnounce(), params);
		return response;
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
		String peerIPAddress = "";
		int peerPort = 0;
		for (int i = 0; i < peers.length(); i++) {
			peerID = peers.getJSONObject(i).getString("peer_id");
			peerIPAddress = peers.getJSONObject(i).getString("ip");
			peerPort = peers.getJSONObject(i).getInt("port");
			Peer peer = new Peer(peerID, peerIPAddress, peerPort);
			peersList.add(peer);
		}
		return new TrackerResponse(peersList, interval);
	}

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

	private String urlEncodeUTF8(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(e);
		}
	}

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
		InputStream inStream = connection.getInputStream();
		Scanner scanner = new Scanner(inStream, "UTF-8");
		json = scanner.useDelimiter("\\Z").next();
		scanner.close();
		return json;
	}
}
