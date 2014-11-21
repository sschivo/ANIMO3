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
import javax.swing.JFrame;
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
	 * @return The complete (absoluite) path of the file selected by the user, or null if the user has selected no file/closed the dialog
	 */
	public static String open(final String fileType, final String description, Component parent) {
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
		JFrame fintoFrame = null;
		if (parent == null) {
			//Basically, it seems that passing the Animo.getCytoscape().getJFrame() as parent here is not a good idea: for some reason, the open dialog is not displayed and Cytoscape hangs
			fintoFrame = new JFrame("Trying to solve a problem if Cytoscape has just started..");
			fintoFrame.setBounds(0, 0, 1, 1);
			fintoFrame.setVisible(true);
			parent = fintoFrame;
		}
		//System.err.println("Ora apro il dialog, eh");
		int result = JFileChooser.CANCEL_OPTION;
		try {
			result = chooser.showOpenDialog(parent);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		if (fintoFrame != null) {
			fintoFrame.dispose();
		}
		//System.err.println("Ecco il risultato: " + result);
		if (result == JFileChooser.APPROVE_OPTION) {
			currentDirectory = chooser.getCurrentDirectory();
			return chooser.getSelectedFile().getAbsolutePath();
		}
		return null;
	}
	
	//Look for a file. First tries user's home, then / (actually, all roots it finds)
	public static File findFile(String fileName) {
		File result = null;
		String initDir = System.getProperty("user.home");
		File initialDirectory;
		if (initDir != null) {
			initialDirectory = new File(initDir);
		} else {
			initialDirectory = new File(".");
		}
		if (initialDirectory.exists()) {
			result = findFile(fileName, initialDirectory);
		}
		if (result == null || !result.exists()) {
			File[] roots = FileSystemView.getFileSystemView().getRoots();
			for (File dir : roots) {
				result = findFile(fileName, dir);
				if (result != null) {
					return result;
				}
			}
		}
		return result;
	}
	
	//Recursively look for a file starting from the given directory
	public static File findFile(String fileName, File initialDirectory) {
		if (initialDirectory == null || !initialDirectory.exists()
				|| !initialDirectory.isDirectory() || !initialDirectory.canRead()) {
			return null;
		}
		File result = null;
		File[] contents = initialDirectory.listFiles();
		for (File f : contents) {
			if (f.isFile() && f.getName().equals(fileName)) {
				return f;
			}
		}
		for (File f : contents) {
			if (f.isDirectory() && !f.getName().endsWith(".") && !f.getName().endsWith("..")) {
				result = findFile(fileName, f);
				if (result != null) {
					return result;
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
