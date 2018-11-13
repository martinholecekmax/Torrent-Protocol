package test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

class ATorrentTest {

	@Test
	void test() {
//		ATorrent aTorrent = new ATorrent();
//		aTorrent.start(true);
//		aTorrent.start(false);
		InetAddress ip;
		try {
			ip = InetAddress.getLocalHost();
			System.out.println(ip.getHostAddress());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
