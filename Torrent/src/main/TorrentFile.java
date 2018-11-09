package main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TorrentFile implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<String> relativePath;
	private long length;

	public TorrentFile(List<String> relativePath, long length) {
		this.relativePath = new ArrayList<>(relativePath);
		this.length = length;
	}

	public List<String> getPath() {
		return relativePath;
	}

	public long getLength() {
		return length;
	}

	@Override
	public String toString() {
		return "\n   path: { " + relativePath + " }\n   length:" + length;
	}
}