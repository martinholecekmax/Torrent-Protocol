package test;

import file.FileManager;
import main.Peer;

class JobHandlerTest {
	void test() {
		Peer peer = new Peer("", 2);
		FileManager jobHandler = new FileManager(peer);
		boolean saved = false;
		saved = jobHandler.saveJobs();
		System.out.println(saved);
		saved = jobHandler.loadJobs();
		System.out.println(saved);
	}
}
