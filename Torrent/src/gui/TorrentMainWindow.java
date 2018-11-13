package gui;
import static utils.Constants.TORRENT_ROOT_LOCATION;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Optional;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import file.FileManager;
import main.DownloadManager;
import main.Job;
import main.Peer;
import main.Server;
import main.TorrentMetadata;
import main.TorrentProcessor;

public class TorrentMainWindow extends JFrame implements ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTable table_jobs;
	private FileManager fileManager;
	private JMenuItem mntmAddTorrent;
	private JMenuItem mntmLoadTorrent;
	private JMenuItem mntmExit;
	private JMenu mnNewMenu;
	private JFileChooser fileChooser = new JFileChooser();
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
				
//
//					boolean loadTorrent = false;
//					loadTorrent = true;
//					if (loadTorrent) {
//						loadTorrent(fileManager, torrentFileName, storeLocation);
//					} else {
//						createTorrent(fileManager, filename, location);
//					}
					
					
					TorrentMainWindow frame = new TorrentMainWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 * @throws IOException 
	 */
	public TorrentMainWindow() throws IOException {
//		String filename = TORRENT_ROOT_LOCATION + "Alpha";
//		String location = TORRENT_ROOT_LOCATION;
//		String torrentFileName = TORRENT_ROOT_LOCATION + "Alpha.temp";
//		String storeLocation = TORRENT_ROOT_LOCATION + "test3\\";

		// Load previous jobs from dat file
//		fileManager.loadJobs();

		// Start Server
		ServerSocket serverSocket = new ServerSocket(0);
		InetAddress localIP = InetAddress.getLocalHost();
		Peer peer = new Peer(Optional.empty(), serverSocket.getInetAddress().toString(), localIP.getHostAddress(),serverSocket.getLocalPort());
		
		// Initialize FileManager
		fileManager = new FileManager(peer);
		
		Server listener = new Server(serverSocket, fileManager);
		Thread thread = new Thread(listener, "Listener thread.");
		thread.start();
		
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 904, 471);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		mnNewMenu = new JMenu("File");
		menuBar.add(mnNewMenu);
		
		mntmAddTorrent = new JMenuItem("Create New Torrent");
		mntmAddTorrent.addActionListener(this);
		mnNewMenu.add(mntmAddTorrent);
		
		JSeparator separator = new JSeparator();
		mnNewMenu.add(separator);
		
		mntmLoadTorrent = new JMenuItem("Load Torrent");
		mntmLoadTorrent.addActionListener(this);
		mnNewMenu.add(mntmLoadTorrent);
		
		JSeparator separator_1 = new JSeparator();
		mnNewMenu.add(separator_1);
		
		mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(this);
		mnNewMenu.add(mntmExit);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);		
		
		String header[] = {"#", "Name", "Size", "Status"};
		DefaultTableModel model = new DefaultTableModel() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int arg0, int arg1) {
				return false;
			}
			
		};
		model.setColumnIdentifiers(header);
		
		table_jobs = new JTable();
		table_jobs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table_jobs.setModel(model);
		DefaultTableCellRenderer renderer = (DefaultTableCellRenderer)table_jobs.getTableHeader().getDefaultRenderer();
		renderer.setHorizontalAlignment(SwingConstants.LEFT);
		JScrollPane scrollPane = new JScrollPane(table_jobs);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		setTable(model);		
		
	}

	private void setTable(DefaultTableModel model) {
		ArrayList<Job> list = fileManager.getJobs();
		Object rowData[] = new Object[4];		
		for (int i = 0; i < list.size(); i++) {
			rowData[0] = model.getRowCount() + 1;
			rowData[1] = list.get(i).getTorrentMetadata().getInfo().getName();
			rowData[2] = list.get(i).getTorrentMetadata().getInfo().getLength();
			rowData[3] = list.get(i).getPercentageDone();	
			model.addRow(rowData);
		}
	}
	
	/**
	 *  Create Metadata file.
	 *  
	 * @param fileManager
	 * @param filename
	 * @param location
	 * @throws IOException
	 */
	private void createTorrent(FileManager fileManager, String filename, String location) throws IOException {
		TorrentProcessor processor = new TorrentProcessor();
		processor.createMetadataFile(fileManager, filename, location, "Martin");
		System.out.println("CREATE TORRENT METADATA");
	}

	/**
	 *  Create DownloadManager - Load torrent Metadata.
	 *  
	 * @param fileManager
	 * @param torrentFileName
	 * @param storeLocation
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void loadTorrent(FileManager fileManager, String torrentFileName, String storeLocation)
			throws IOException, ClassNotFoundException {
		TorrentProcessor processor = new TorrentProcessor();
		TorrentMetadata torrentMetadata = processor.loadMetadataFile(torrentFileName);
		Job job = fileManager.createJob(torrentMetadata, storeLocation);
		DownloadManager downloadManager = new DownloadManager(job, fileManager);
		Thread downloadManagerThread = new Thread(downloadManager, "Download Manager Thread");
		downloadManagerThread.start();
		System.out.println("LOAD TORRENT METADATA");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(mntmExit)) {
			System.exit(0);
		}
		if (e.getSource().equals(mntmAddTorrent)) {
			if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				
			String file = fileChooser.getSelectedFile().getAbsolutePath();
			try {
				createTorrent(fileManager, file, TORRENT_ROOT_LOCATION);
				setTable((DefaultTableModel)table_jobs.getModel());	
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			}
		}
		if (e.getSource().equals(mntmLoadTorrent)) {
			if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				
				String file = fileChooser.getSelectedFile().getAbsolutePath();
				try {
					loadTorrent(fileManager, file, TORRENT_ROOT_LOCATION);
					setTable((DefaultTableModel)table_jobs.getModel());	
				} catch (IOException | ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		
	}
}
