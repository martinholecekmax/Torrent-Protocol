package file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import main.ATorrent;
import main.Job;
import main.Peer;
import main.Piece;
import main.TorrentMetadata;
import tracker.TrackerClient;

/**
 * Create torrent job which is saved into resume.dat file.
 * 
 * resume.dat contains information regarding currently loaded torrent jobs.
 * 
 * @author Maxim
 *
 */
public class FileManager {

	private static final Logger LOGGER = Logger.getLogger(ATorrent.class);

	private ArrayList<Job> jobs = new ArrayList<Job>();

	private FileHandler fileHandler = new FileHandler();
	private Peer peer;

	private Object pieceLock = new Object();
	private Object jobsLock = new Object();

	public FileManager(Peer peer) {
		this.peer = peer;
	}

	public Peer getPeer() {
		return peer;
	}

	public ArrayList<Job> getJobs() {
		return jobs;
	}

	/**
	 * Get Number of assinged jobs.
	 * 
	 * @return integer size of ArrayList jobs.
	 */
	public int size() {
		return jobs.size();
	}

	public boolean contains(Object o) {
		return jobs.contains(o);
	}

	public Job get(int index) {
		return jobs.get(index);
	}

	public Job getJobByMetadata(TorrentMetadata torrentMetadata) {
		for (Job job : jobs) {
			if (job.getTorrentMetadata() == torrentMetadata) {
				return job;
			}
		}
		return null;
	}

	public boolean add(Job e) {
		synchronized (jobsLock) {
			return jobs.add(e);
		}
	}

	public boolean remove(Object o) {
		synchronized (jobsLock) {
			return jobs.remove(o);
		}
	}

	/**
	 * Store Piece received from other peer and save it to the disk where file is
	 * located.
	 * 
	 * @param infoHash - String Hash of the metadata file's info field.
	 * @param piece    - Instance of Piece class which contains data field that has
	 *                 bytes of the file.
	 * @return True if Piece is successfully stored on the disk, otherwise, return
	 *         False.
	 */
	public boolean storePiece(String infoHash, Piece piece) {
		synchronized (pieceLock) {
			for (Job job : jobs) {
				if (job.getTorrentInfoHash().equals(infoHash)) {
					return job.storePiece(piece);
				}
			}
			return false;
		}
	}

	/**
	 * Get piece of the file by its info hash and index.
	 * 
	 * @param infoHash - String Hash of the metadata file's info field.
	 * @param index    - Offset of the file where to get data from.
	 * @return Piece with data if info hash and index are valid, otherwise, return
	 *         piece where data are null.
	 */
	public Piece getPiece(String infoHash, int index) {
		Piece pieceEmpty = new Piece(index, infoHash);
		synchronized (pieceLock) {
			for (Job job : jobs) {
				if (job.getTorrentInfoHash().equals(infoHash)) {
					Piece piece = job.getPiece(index);
					if (piece != null) {
						return piece;
					}
					break;
				}
			}
			return pieceEmpty;
		}
	}

	/**
	 * Create new job if is not in a list, otherwise
	 * 
	 * @param torrentMetadata
	 * @param whereToStore
	 * @return return True if a new directory structure has been created, otherwise,
	 *         return False, if a directories is already in jobs.
	 * @throws IOException
	 */
	public Job createJob(TorrentMetadata torrentMetadata, String whereToStore) throws IOException {
		synchronized (jobsLock) {
			if (!jobExists(torrentMetadata)) {
				ArrayList<File> files = fileHandler.createFileStructure(torrentMetadata, whereToStore);
				Job job = new Job(torrentMetadata.getInfo().getPieces(), files, torrentMetadata);
				add(job);
				saveJobs();
				return job;
			} else {
				return getJobByMetadata(torrentMetadata);
			}
		}
	}

	/**
	 * Check if torrent metadata file is already assinged in jobs.
	 * 
	 * @param torrentMetadata
	 * @return True if metadata file is in jobs ArrayList, otherwise, retrun False.
	 */
	private boolean jobExists(TorrentMetadata torrentMetadata) {
		synchronized (jobsLock) {
			for (Job job : jobs) {
				if (job.getTorrentMetadata().equals(torrentMetadata)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Save jobs into the resume.dat file, that can be loaded after program
	 * terminates and is opened again.
	 * 
	 * @return True if jobs are successfully saved, otherwise, return False.
	 */
	public boolean saveJobs() {
		synchronized (jobsLock) {
			try {
				FileOutputStream fileOutputStream = new FileOutputStream("resume.dat");
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

				objectOutputStream.writeObject(jobs);
				objectOutputStream.close();
				fileOutputStream.close();
				return true;
			} catch (FileNotFoundException e) {
				LOGGER.error("Error, resume.dat file not found.", e);
				return false;
			} catch (IOException e) {
				LOGGER.error("Error, saving jobs into resume.dat file.", e);
				e.printStackTrace();
				return false;
			}
		}
	}

	/**
	 * Loading jobs from resume.dat file which contains previously assinged jobs.
	 * 
	 * @return True if jobs are successfully loaded, otherwise, return False.
	 */
	public boolean loadJobs() {
		synchronized (jobsLock) {
			try {
				FileInputStream fileInputStream = new FileInputStream("resume.dat");
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				ArrayList<Job> jobs = new ArrayList<>();
				Object object = objectInputStream.readObject();
				if (object instanceof ArrayList<?>) {
					ArrayList<?> loadedJobsList = (ArrayList<?>) object;
					if (loadedJobsList.size() > 0) {
						for (int i = 0; i < loadedJobsList.size(); i++) {
							Object loadedObject = loadedJobsList.get(i);
							if (loadedObject instanceof Job) {
								Job loadedJob = (Job) loadedObject;
								if (!jobExists(loadedJob.getTorrentMetadata())) {
									jobs.add(loadedJob);
								}
							}
						}
					}
				}
				if (jobs.size() > 0) {
					this.jobs.addAll(jobs);
				}
				objectInputStream.close();
				fileInputStream.close();
				return true;
			} catch (FileNotFoundException e) {
				LOGGER.error("Error, resume.dat file not found.", e);
				return false;
			} catch (ClassNotFoundException e) {
				LOGGER.error("Error, the object from resume.dat file is not type of Job class.", e);
				return false;
			} catch (IOException e) {
				LOGGER.error("Error, loading jobs from resume.dat file.", e);
				return false;
			}
		}
	}

	/**
	 * Contact Tracker about jobs that has been completed.
	 */
	public void contactTracker() {
		for (Job job : jobs) {
			if (job.isDone()) {
				TrackerClient.getResponse(job.getTorrentMetadata(), peer, job.getStatus());
			}
		}
	}
}
