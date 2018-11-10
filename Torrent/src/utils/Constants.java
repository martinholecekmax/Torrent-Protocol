package utils;

public class Constants {
	/**	Default torrent location where the files will be stored	 */
	public static String TORRENT_ROOT_LOCATION = System.getProperty("user.home") + "\\Desktop\\";
	/** Extension of the torrent metadata file that is save on the PC. */
	public static final String TORRENT_EXTENSION = ".temp";	
	/** Default size of piece which will be used for splitting of the files (256KB)	 */
	public static final int PIECE_SIZE = 262144;
// 	public static final int PIECE_SIZE = 4; 	// 4 BYTES
	/**	Default tracker URL */
	public static final String ANNOUNCE = "http://martinholecekmax.site/api/announce";	
	/** Default value of interval in which client will contact tracker	 */
	public static final int DEFAULT_INTERVAL = 1800;
	/** Default bandwidth value => 256kB/2s (PIECE_SIZE/2000ms) or 2 Megabits/sec */
//	public static final int DEFAULT_BANDWIDTH = 2000;
	public static final int DEFAULT_BANDWIDTH = 3000;
	/** Maximum number of threads that will be submitted by server */
	public static final int MAX_SERVER_THREADS = 100;
	/** Maximum number of threads that will be submitted by client */
	public static final int MAX_CLIENT_THREADS = 100;
	
	
}