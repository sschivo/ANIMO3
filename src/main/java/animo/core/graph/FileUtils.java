package animo.core.graph;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.session.CySessionManager;

import animo.cytoscape.Animo;

/**
 * The class used for file input/output
 */
public class FileUtils {
	private static File currentDirectory = null;

	/**
	 * Show the File Chooser dialog and return the name of the chosen file. You can choose the file type (ex. ".csv"), the description (ex. "Commma separated files"), and the
	 * parent Component (null is ok: it is just to tell the O.S. to which window the dialog will "belong")
	 * 
	 * @param fileType
	 *            The file type (ex. ".png")
	 * @param description
	 *            The file type description (ex. "Image file")
	 * @param parent
	 *            The parent Component (typically a window. null is ok)
	 * @return The complete (absolute) path of the file selected by the user, or null if the user has selected no file/closed the dialog
	 */
	public static String open(final String fileType, final String description, final String dialogTitle, Component parent) {
		//System.err.println("Inizio dell'open (parent = " + parent + ")");
		CyAppAdapter app = Animo.getCytoscapeApp();
		CySessionManager manager = app.getCySessionManager();
		if (currentDirectory == null && manager.getCurrentSessionFileName() != null) {
			File curSession = new File(manager.getCurrentSessionFileName());
			if (curSession != null && curSession.exists()) {
				currentDirectory = curSession.getParentFile();
			}
		}
		if (currentDirectory == null || !currentDirectory.exists()) {
			currentDirectory = new File(System.getProperty("user.home"));
		}
		if (currentDirectory == null || !currentDirectory.exists()) {
			currentDirectory = new File(System.getProperty("user.dir"));
		}
		if (currentDirectory == null || !currentDirectory.exists()) { //Last resort: just put it "here" if we found no current session
			currentDirectory = new File(".");
		}
		//System.err.println("Siamo adesso nella directory " + currentDirectory.getAbsolutePath());
		JFileChooser chooser = new JFileChooser(currentDirectory);
		if (fileType != null) {
			chooser.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File pathName) {
					if (pathName.getAbsolutePath().endsWith(fileType) || pathName.isDirectory()) {
						return true;
					}
					return false;
				}

				@Override
				public String getDescription() {
					return description;
				}
			});
			//System.err.println("Impostato il file filter " + chooser.getFileFilter());
		} else {
			chooser.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File pathName) {
					return true;
				}

				@Override
				public String getDescription() {
					return description;
				}
			});
		}
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		if (dialogTitle != null) {
			chooser.setDialogTitle(dialogTitle);
		}
		//System.err.println("Ora apro il dialog, eh");
		int result = JFileChooser.CANCEL_OPTION;
		try {
			result = chooser.showOpenDialog(parent);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		//System.err.println("Ecco il risultato: " + result);
		if (result == JFileChooser.APPROVE_OPTION) {
			currentDirectory = chooser.getCurrentDirectory();
			return chooser.getSelectedFile().getAbsolutePath();
		}
		return null;
	}
	
	//Look for a file. First tries user's home, then / (actually, all roots it finds)
	//if isFile = false, we look for a directory instead
	//if strictlyEquals = false, instead of using equals() we use contains and both strings are lower-case
	public static File findFile(String fileName, boolean isFile, boolean strictlyEquals) {
		File result = null;
		String initDir = System.getProperty("user.home");
		File initialDirectory;
		if (initDir != null) {
			initialDirectory = new File(initDir);
		} else {
			initialDirectory = new File(".");
		}
		if (!strictlyEquals) { //We do it once and for all, so we don't need to keep doing it for each comparison
			fileName = fileName.toLowerCase();
		}
		if (initialDirectory.exists()) {
			//System.err.println("First looking for " + fileName + " starting in " + initialDirectory.getAbsolutePath());
			result = findFile(fileName, initialDirectory, isFile, strictlyEquals);
		}
		if (Thread.interrupted()) { //If the user cancelled the search, the interrupt status of this thread is set
			Thread.currentThread().interrupt(); //Because it is a recursive method, we want to keep the interrupted status (which is cleared when checking it), so that every method in the call stack can see it and exit
			return null;
		}
		if (result == null || !result.exists()) {
			File[] roots = FileSystemView.getFileSystemView().getRoots();
			for (File dir : roots) {
				result = findFile(fileName, dir, isFile, strictlyEquals);
				if (result != null) {
					return result;
				}
				if (Thread.interrupted()) { //If the user cancelled the search, the interrupt status of this thread is set
					Thread.currentThread().interrupt(); //Because it is a recursive method, we want to keep the interrupted status (which is cleared when checking it), so that every method in the call stack can see it and exit
					return null;
				}
			}
		}
		return result;
	}
	
	//Recursively look for a file starting from the given directory
	//For isFile and strictlyEquals, see the other function
	public static File findFile(String fileName, File initialDirectory, boolean isFile, boolean strictlyEquals) {
		if (initialDirectory == null || !initialDirectory.exists()
				|| !initialDirectory.isDirectory() || !initialDirectory.canRead()) {
			return null;
		}
		File result = null;
		//System.err.println(initialDirectory.getAbsolutePath());
		File[] contents = initialDirectory.listFiles();
		for (File f : contents) {
			if ((isFile && f.isFile() || !isFile && f.isDirectory())
					&&
				(strictlyEquals && f.getName().equals(fileName) || !strictlyEquals && f.getName().toLowerCase().contains(fileName))) {
				return f;
			}
		}
		if (Thread.interrupted()) { //If the user cancelled the search, the interrupt status of this thread is set
			Thread.currentThread().interrupt(); //Because it is a recursive method, we want to keep the interrupted status (which is cleared when checking it), so that every method in the call stack can see it and exit
			return null;
		}
		for (File f : contents) {
			if (f.isDirectory() && !f.getName().endsWith(".") && !f.getName().endsWith("..")) {
				result = findFile(fileName, f, isFile, strictlyEquals);
				if (result != null) {
					return result;
				}
				if (Thread.interrupted()) { //If the user cancelled the search, the interrupt status of this thread is set
					Thread.currentThread().interrupt(); //Because it is a recursive method, we want to keep the interrupted status (which is cleared when checking it), so that every method in the call stack can see it and exit
					return null;
				}
			}
		}
		return result;
	}
	
	//Find a file which we have a guess of the possible name of a directory containing it
	//The name of the directory will be checked as f.getName().toLowercase().contains(dirName.toLowercase())
	//The file name will be checked exactly as f.getName().equals(fileName)
	//First tries looking in all directories that match the given dirName. That failing, does a bruteforce search for the file
	public static File findFileInDirectory(String fileName, String dirName) {
		File result = null;
		
		dirName = dirName.toLowerCase();
		String initDir = System.getProperty("user.home");
		File initialDirectory;
		if (initDir != null) {
			initialDirectory = new File(initDir);
		} else {
			initialDirectory = new File(".");
		}
		if (initialDirectory.exists()) {
			//System.err.println("First looking for " + fileName + " starting in " + initialDirectory.getAbsolutePath());
			result = findFileInDirectory(fileName, dirName, initialDirectory, false);
		}
		if (Thread.interrupted()) { //If the user cancelled the search, the interrupt status of this thread is set
			Thread.currentThread().interrupt(); //Because it is a recursive method, we want to keep the interrupted status (which is cleared when checking it), so that every method in the call stack can see it and exit
			return null;
		}
		
		if (result == null || !result.exists()) {
			File[] roots = FileSystemView.getFileSystemView().getRoots();
			//System.err.println("Not found. Looking in the roots");
			for (File dir : roots) {
				//System.err.println("Root " + dir);
				result = findFileInDirectory(fileName, dirName, dir, false);
				if (result != null) {
					return result;
				}
				if (Thread.interrupted()) { //If the user cancelled the search, the interrupt status of this thread is set
					Thread.currentThread().interrupt(); //Because it is a recursive method, we want to keep the interrupted status (which is cleared when checking it), so that every method in the call stack can see it and exit
					return null;
				}
			}
		}
		if (result == null) { //If we didn't find the directory which could contain the file, or we didn't find the file in the directory we found, just do a bruteforce search for the file
			//System.err.println("Non trovato file o directory, riparto da zero cercando il file " + fileName);
			result = findFile(fileName, true, true);
		}
		//System.err.println("Ecco il risultato di tanta fatica: " + result);
		return result;
	}
	
	//Look for a file with name fileName, contained somewhere (not necessarily directly) inside a directory
	//with a name that contains dirName (lower case comparison).
	//initialDirectory is the directory where the search starts
	//We use lookingForFile = true when the function is looking for the file, and false to indicate that
	//we are looking for the directory.
	//When we find the directory, we continue the search, looking for fileName, thus lookingForFile = true
	public static File findFileInDirectory(String fileName, String dirName, File initialDirectory, boolean lookingForFile) {
		if (initialDirectory == null || !initialDirectory.exists()
				|| !initialDirectory.isDirectory() || !initialDirectory.canRead()) {
			return null;
		}
		File result = null;
		//System.err.println(initialDirectory.getAbsolutePath());
		File[] contents = initialDirectory.listFiles();
		for (File f : contents) {
			if (lookingForFile && f.isFile() && f.getName().equals(fileName)) {
				//If we were looking for the file, just return it
				return f;
			} else if (!lookingForFile && f.isDirectory() && f.getName().toLowerCase().contains(dirName)) { //If we were looking for the directory, look now for the file
				//System.err.println("Trying to look in matching directory " + f.getAbsolutePath());
				result = findFileInDirectory(fileName, dirName, f, true);
				if (result != null) { //If the file was found in this directory, return it and we are done.
					return result;
				} //Otherwise, we just continue to the next matching directory
				if (Thread.interrupted()) { //If the user cancelled the search, the interrupt status of this thread is set
					Thread.currentThread().interrupt(); //Because it is a recursive method, we want to keep the interrupted status (which is cleared when checking it), so that every method in the call stack can see it and exit
					return null;
				}
			}
		}
		for (File f : contents) {
			if (f.isDirectory() && !f.getName().endsWith(".") && !f.getName().endsWith("..")) {
				result = findFileInDirectory(fileName, dirName, f, lookingForFile);
				if (result != null) { //If we have found the file, just return it
					return result;
				} //Note that whenever result != null we are sure it is the file we were looking for: we never return the directory, but directly start from the directory looking for the file
				if (Thread.interrupted()) { //If the user cancelled the search, the interrupt status of this thread is set
					Thread.currentThread().interrupt(); //Because it is a recursive method, we want to keep the interrupted status (which is cleared when checking it), so that every method in the call stack can see it and exit
					return null;
				}
			}
		}
		return result;
	}
	

	/**
	 * Save what is currently shown on the given Component to a file that the user will choose via the open dialog. The resulting image will be in JPEG format, and will have a size
	 * that is also specified by the user.
	 * 
	 * @param graph
	 *            The component to be saved
	 */
	public static void renderToJPG(Graph graph) {
		String fileName = save(".jpg", "JPEG image", graph);
		if (fileName == null)
			return;
		String widthS = JOptionPane.showInputDialog(Animo.getCytoscape().getJFrame(), "Width of the image",
				graph.getSize().width);
		if (widthS == null)
			return;
		int width = graph.getSize().width;
		try {
			width = Integer.parseInt(widthS);
		} catch (NumberFormatException ex) {
		}
		String heightS = JOptionPane.showInputDialog(Animo.getCytoscape().getJFrame(), "Height of the image",
				graph.getSize().height);
		if (heightS == null)
			return;
		int height = graph.getSize().height;
		try {
			height = Integer.parseInt(heightS);
		} catch (NumberFormatException ex) {
		}
		String zoomS = JOptionPane
				.showInputDialog(Animo.getCytoscape().getJFrame(), "Zoom level", graph.getZoomLevel());
		if (zoomS == null)
			return;
		int zoom = graph.getZoomLevel();
		try {
			int zoomTmp = Integer.parseInt(zoomS);
			if (zoomTmp > 0) {
				zoom = zoomTmp;
			}
		} catch (NumberFormatException ex) {
		}
		int oldZoom = graph.getZoomLevel();
		graph.setZoomLevel(zoom);
		renderToJPG(graph, fileName, width, height);
		graph.setZoomLevel(oldZoom);
	}

	/**
	 * Save what is currently shown on the given Graph to a given .jpg file, rendering it with given size
	 * 
	 * @param graph
	 *            The Graph whose "photograph" is to be saved
	 * @param fileName
	 *            The name of the file in which to save the image
	 * @param width
	 *            Width of the resulting image
	 * @param heigth
	 *            Height of the resulting image
	 */
	public static void renderToJPG(Graph graph, String fileName, int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics imgGraphics = image.createGraphics();
			graph.ensureRedraw();
			graph.paint(imgGraphics, width, height);
			graph.ensureRedraw();
			Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
			if (!iter.hasNext()) {
				throw new IOException("No writer for JPEG format");
			}
			ImageWriter writer = iter.next();
			ImageWriteParam iwp = writer.getDefaultWriteParam();
			iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			String qualityS = JOptionPane
					.showInputDialog(
							Animo.getCytoscape().getJFrame(),
							"Compression quality in [0, 1] (0 = worst quality, highest compression; 1 = best quality, lowest compression)",
							1);
			if (qualityS == null)
				return;
			float quality;
			try {
				quality = Float.parseFloat(qualityS);
			} catch (NumberFormatException ex) {
				quality = 1.0f;
			}
			iwp.setCompressionQuality(quality);
			File f = new File(fileName);
			f.delete();
			FileImageOutputStream output = new FileImageOutputStream(f);
			// ImageIO.write(image, "jpg", f);
			writer.setOutput(output);
			writer.write(image);
			writer.dispose();
			imgGraphics = null;
			image = null;
			JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Graph exported to " + f.getCanonicalPath()
					+ ".", "Operation successful", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
			JOptionPane.showMessageDialog(Animo.getCytoscape().getJFrame(), "Error: " + e.getMessage() + ", caused by "
					+ e.getCause() + ".", "Operation failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Show the save dialog. The workings are the same as the with the open function
	 * 
	 * @param fileType
	 *            The file type (ex. ".png")
	 * @param description
	 *            The file type description (ex. "Image file")
	 * @param parent
	 *            The parent Component (typically a window. null is ok)
	 * @return The complete (absoluite) path of the file selected by the user, or null if the user has selected no file/closed the dialog
	 */
	public static String save(final String fileType, final String description, Component parent) {
		JFileChooser chooser = new JFileChooser(currentDirectory);
		chooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File pathName) {
				if (pathName.getAbsolutePath().endsWith(fileType) || pathName.isDirectory()) {
					return true;
				}
				return false;
			}

			@Override
			public String getDescription() {
				return description;
			}
		});
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		int result = chooser.showSaveDialog(parent);
		if (result == JFileChooser.APPROVE_OPTION) {
			currentDirectory = chooser.getCurrentDirectory();
			String fileName = chooser.getSelectedFile().getAbsolutePath();
			if (!fileName.endsWith(fileType)) {
				fileName += fileType;
			}
			return fileName;
		}
		return null;
	}

	/**
	 * Save what is currently shown on the given Component to a file that the user will choose via the open dialog.
	 * 
	 * @param c
	 *            The component to be saved
	 */
	public static void saveToPNG(Component c) {
		String fileName = save(".png", "PNG image", c);
		if (fileName != null) {
			saveToPNG(c, fileName);
		}
	}

	/**
	 * Save what is currently shown on the given Component to a given .png file
	 * 
	 * @param c
	 *            The component whose "photograph" is to be saved
	 * @param fileName
	 *            The name of the file in which to save the image
	 */
	public static void saveToPNG(Component c, String fileName) {
		try {
			BufferedImage image = new BufferedImage(c.getSize().width, c.getSize().height, BufferedImage.TYPE_INT_RGB);
			Graphics imgGraphics = image.createGraphics();
			c.paint(imgGraphics);
			File f = new File(fileName);
			f.delete();
			ImageIO.write(image, "png", f);
			imgGraphics = null;
			image = null;
		} catch (IOException e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
		}
	}
}
