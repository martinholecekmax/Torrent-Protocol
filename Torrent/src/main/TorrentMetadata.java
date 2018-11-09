package main;

import java.io.Serializable;
import java.sql.Date;

import utils.Utility;

public class TorrentMetadata implements Serializable {
	private static final long serialVersionUID = 1865393834528664485L;
	private String announce;
	private TorrentFileInfo info;
	private String createdBy;
	private Date createdAt;
	private String encoding = "UTF-8";
	private String infoHash;

	/**
	 * Create metadata file that stores information about torrent file. This will
	 * also initialize info hash field from info parameter. To initialize this
	 * field, GenerateChecksum method from Utility class is used which uses MD5
	 * Algorithm to generate hash of the info field.
	 * 
	 * @param announce  - Tracker information
	 * @param info      - Contains List of files and information about the files.
	 * @param createdBy - Name of person that creates this metadata file.
	 * @param createdAt - SQL Date which indicates when this file has been created.
	 * @param encoding  - If this field is null, then default encoding is UTF-8
	 *                  (This field is not implemented)
	 */
	public TorrentMetadata(String announce, TorrentFileInfo info, String createdBy, Date createdAt, String encoding) {
		this.announce = announce;
		this.info = info;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
		if (encoding != null) {
			this.encoding = encoding;
		}
		this.infoHash = Utility.generateChecksum(info);
	}

	public TorrentMetadata(String announce, TorrentFileInfo info, String createdBy, Date createdAt) {
		this(announce, info, createdBy, createdAt, null);
	}

	public String getAnnounce() {
		return announce;
	}

	public TorrentFileInfo getInfo() {
		return info;
	}

	public String getInfoHash() {
		return infoHash;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public String getEncoding() {
		return encoding;
	}

	@Override
	public String toString() {
		return "TorrentMetadata: \nannounce:" + announce + "\ncreatedBy:" + createdBy + "\ncreatedAt:" + createdAt
				+ "\nencoding:" + encoding + "\ninfo: {" + info + "\n}";
	}
}
