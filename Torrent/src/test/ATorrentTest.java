package test;

import org.junit.jupiter.api.Test;

import main.ATorrent;

class ATorrentTest {

	@Test
	void test() {
		ATorrent aTorrent = new ATorrent();
		aTorrent.start(true);
		aTorrent.start(false);
	}

}
