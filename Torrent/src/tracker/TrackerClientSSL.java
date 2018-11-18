package tracker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import main.ATorrent;
import main.Peer;
import main.TorrentMetadata;
import main.TorrentStatus;

public class TrackerClientSSL {
	public static void deleteAllPeers() {
//		https://commerce3.derby.ac.uk/~st100344605/api/delete-all
		TrackerClientSSL trackerClient = new TrackerClientSSL();
		trackerClient.jsonGetRequest("https://commerce3.derby.ac.uk/~st100344605/api/delete-all");
	}
	
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
	public static Optional<TrackerResponse> getResponse(TorrentMetadata torrentMetadata, Peer peer,
			TorrentStatus status) {
		try {
			TrackerClientSSL trackerClient = new TrackerClientSSL();
			String params = trackerClient.buildParamsQuery(trackerClient.createParams(torrentMetadata, peer, status));
			return trackerClient.parseResponse(torrentMetadata.getAnnounce(), params);
		} catch (MalformedURLException e) {
			LOGGER.error("Trying to encode URL to UTF-8 failed.", e);
		} catch (IOException e) {
			LOGGER.error("Error, while reading from tracker website.", e);
		} catch (JSONException e) {
			LOGGER.error("Error, parsing json.", e);
		}
		return Optional.empty();
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
	private Optional<TrackerResponse> parseResponse(String announce, String params) throws IOException, JSONException {
		Optional<String> json = jsonGetRequest(announce + params);
		if (json.isPresent()) {			
			ArrayList<Peer> peersList = new ArrayList<>();
			JSONObject jsonObject = new JSONObject(json.get());
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
			TrackerResponse response = new TrackerResponse(peersList, interval);
			return Optional.of(response);
		} else {
			return Optional.empty();
		}
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
		params.put("local_ip", peer.getLocalIP());
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
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public Optional<String> jsonGetRequest(String urlQueryString) {
		try {
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}
			} };

			// Install the all-trusting trust manager

			SSLContext sc = SSLContext.getInstance("SSL");

			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

			String json = "";
			URL url = new URL(urlQueryString);
//			URL url = new URL("https://commerce3.derby.ac.uk/~st100344605/api/announce?peer_id=PEERID123&port=123&info_hash=filehash1&event=stopped&downloaded=0&uploaded=10&left=123");
			URLConnection con = url.openConnection();
			Reader reader = new InputStreamReader(con.getInputStream());
			while (true) {
				int ch = reader.read();
				if (ch == -1) {
					break;
				}
				json = json + (char) ch;
			}
			return Optional.of(json);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error(e);
		} catch (MalformedURLException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		} catch (KeyManagementException e) {
			LOGGER.error(e);
		}
		return Optional.empty();
	}	
}

