package main;

enum Event {
	STARTED, COMPLETED, STOPPED
}

public class TorrentStatus {
	private Event event;
	private int downloaded;
	private int uploaded;
	private long left;

	/**
	 * Create Torrent Status with default values. Default values of all variables
	 * are zero.
	 * 
	 */
	public TorrentStatus() {
		this.event = Event.STARTED;
		this.downloaded = 0;
		this.uploaded = 0;
		this.left = 0;
	}

	public TorrentStatus(Event event, int downloaded, int uploaded, long left) {
		setEvent(event);
		this.downloaded = downloaded;
		this.uploaded = uploaded;
		this.left = left;
	}

	/**
	 * Get string representation of event. Default return value is Started.
	 * 
	 * @return String value "started", "stopped" or "completed"
	 */
	public String getEvent() {
		switch (event) {
		case STARTED:
			return "started";
		case STOPPED:
			return "stopped";
		case COMPLETED:
			return "completed";
		default:
			return "started";
		}
	}

	/**
	 * Set event to either Started, Stopped or Completed. Default value is Started.
	 * 
	 * @param event - Started, Stopped or Completed
	 */
	public void setEvent(Event event) {
		this.event = event;
	}

	public int getDownloaded() {
		return downloaded;
	}

	public void addDownloaded(int downloaded) {
		this.downloaded += downloaded;
	}

	public void setDownloaded(int downloaded) {
		this.downloaded = downloaded;
	}

	public int getUploaded() {
		return uploaded;
	}

	public void setUploaded(int uploaded) {
		this.uploaded = uploaded;
	}

	public long getLeft() {
		return left;
	}

	public void decreaseLeft(long left) {
		if (this.left - left >= 0) {
			this.left -= left;
		}
		this.left = 0;
	}

	public void setLeft(long left) {
		this.left = left;
	}
}
