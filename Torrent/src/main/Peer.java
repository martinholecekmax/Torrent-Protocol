package main;

import java.util.Optional;
import java.util.UUID;

public class Peer {
	private String peerID;
	private String ipAddress;
	private String localIP;
	private int port;

	public Peer(Optional<String> peerID, String ipAddress, String localIP, int port) {
		this.peerID = peerID.orElse(UUID.randomUUID().toString());
		this.ipAddress = ipAddress;
		this.localIP = localIP;
		this.port = port;
	}
	
	public String getPeerID() {
		return peerID;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getLocalIP() {
		return localIP;
	}

	public void setLocalIP(String localIP) {
		this.localIP = localIP;
	}

	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return ipAddress;
	}

//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
//		result = prime * result + ((localIP == null) ? 0 : localIP.hashCode());
//		result = prime * result + ((peerID == null) ? 0 : peerID.hashCode());
//		result = prime * result + port;
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		Peer other = (Peer) obj;
//		if (ipAddress == null) {
//			if (other.ipAddress != null)
//				return false;
//		} else if (!ipAddress.equals(other.ipAddress))
//			return false;
//		if (localIP == null) {
//			if (other.localIP != null)
//				return false;
//		} else if (!localIP.equals(other.localIP))
//			return false;
//		if (peerID == null) {
//			if (other.peerID != null)
//				return false;
//		} else if (!peerID.equals(other.peerID))
//			return false;
//		if (port != other.port)
//			return false;
//		return true;
//	}
}
