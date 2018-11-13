package test;

import java.util.Optional;

import file.FileManager;
import main.Peer;

class JobHandlerTest {
	void test() {
		Optional<String> optional = Optional.of("");
		Peer peer = new Peer(optional,"","", 2);
		FileManager jobHandler = new FileManager(peer);
		boolean saved = false;
		saved = jobHandler.saveJobs();
		System.out.println(saved);
		saved = jobHandler.loadJobs();
		System.out.println(saved);
	}
}
