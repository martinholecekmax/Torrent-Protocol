package tracker;

import java.util.ArrayList;

import main.Peer;

public class TrackerResponse {
	private ArrayList<Peer> peers;
	private int interval;
	
	public TrackerResponse(ArrayList<Peer> peers, int interval) {
		this.peers = new ArrayList<>(peers);
		this.interval = interval;
	}

	public ArrayList<Peer> getPeers() {
		return peers;
	}

	public int getInterval() {
		return interval;
	}	
}
