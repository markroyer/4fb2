/**
 * Copyright (C) Mar 31, 2010 Mark Royer
 *
 * This file is part of 4fb2.
 *
 * 4fb2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * 4fb2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with 4fb2.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.umaine.cs.f2b2;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

/**
 * Create a zip file from all of the images in the image list.
 * 
 * @author Mark Royer
 * 
 */
public class ZipImagesAction extends AbstractAction {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = -8425665901262828152L;

	/**
	 * Object that references the images to be zipped.
	 */
	private FFB2 f2b2;

	/**
	 * The maximum width of the images that will be zipped.
	 */
	public static int MAXWIDTHORHEIGHT = 800;

	/**
	 * Creates a new {@link ZipImagesAction} that will zip files listed by the
	 * given f2b2 application.
	 * 
	 * @param f2b2
	 *            The application containing the images to be zipped. (Not null)
	 */
	public ZipImagesAction(FFB2 f2b2) {
		super("Zip Images");
		this.f2b2 = f2b2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent arg0) {

		String desktopDir = System.getProperty("user.home") + File.separator
				+ "Desktop";

		int answer = JFileChooser.APPROVE_OPTION;
		boolean canSaveFile = false;

		/*
		 * Until the user approves the save or we can't save any more try to
		 * save.
		 */
		while (answer == JFileChooser.APPROVE_OPTION && !canSaveFile) {

			final JFileChooser sf = new JFileChooser(new File(desktopDir));

			answer = sf.showSaveDialog(f2b2);

			if (answer == JFileChooser.APPROVE_OPTION) {

				File selectedFile = sf.getSelectedFile();

				if (selectedFile.exists()
						|| new File(selectedFile.getAbsolutePath() + ".zip")
								.exists()) {
					int answer2 = JOptionPane.showConfirmDialog(f2b2, "File "
							+ selectedFile.getAbsolutePath()
							+ " already exists.\r\n"
							+ "Are you sure you want to continue?");

					if (answer2 == JOptionPane.YES_OPTION) {
						canSaveFile = true;
					}

				} else {
					canSaveFile = true;
				}

				if (canSaveFile) {

					/*
					 * Perform the actual save in a seperate thread so that the
					 * GUI doesn't lock up.
					 */
					new Thread(new Runnable() {

						public void run() {

							saveFile(sf.getSelectedFile());

						}
					}).start();
				}

			}
		}

	}

	/**
	 * Save the images to the given file.
	 * 
	 * @param file
	 *            The file that the images will be zipped to. (Not null)
	 */
	private void saveFile(File file) {

		try {
			File savedFile = saveFilesTo(ensureExtension(file), f2b2.getFiles());

			JOptionPane.showMessageDialog(f2b2, "Successfully saved images to "
					+ savedFile.getAbsolutePath());

		} catch (Exception e) {

			JOptionPane.showMessageDialog(f2b2, "Unable to save images to "
					+ file.getAbsolutePath(), "Error",
					JOptionPane.ERROR_MESSAGE);

			e.printStackTrace();
		}

	}

	/**
	 * Save all of the given files to given zip file.
	 * 
	 * @param fileName
	 *            The zip file. (Not null)
	 * @param files
	 *            The image files to save. (Not null)
	 * @return The file that the images were zipped to. (Never null)
	 * @throws IOException
	 *             Thrown if there is a problem writing the zip file to the
	 *             disk.
	 * @throws SQLException
	 *             Thrown if there is a problem accessing the images from the
	 *             database.
	 */
	private File saveFilesTo(File fileName, List<ImageFile> files)
			throws IOException, SQLException {

		byte[] buf = new byte[1024];

		ZipOutputStream out = new ZipOutputStream(
				new FileOutputStream(fileName));

		// Create a subdirectory in the zip file that has the same name as the
		// zip file.
		String[] split = fileName.getName().split("[.]");
		StringBuffer str = new StringBuffer(split[0]);
		for (int i = 1; i < split.length - 1; i++) {
			str.append(split[i]);
		}

		String subDirectory = str.toString();

		int iCount = files.size();
		ProgressMonitor pm = new ProgressMonitor(null, "Saving to "
				+ fileName.getName(), "Compressed 0/" + iCount, 0, iCount);
		pm.setMillisToDecideToPopup(0);

		/*
		 * Add each image to the zip file.
		 */
		for (int i = 0; i < iCount; i++) {

			ImageFile file = files.get(i);

			// Add ZIP entry to output stream.
			out.putNextEntry(new ZipEntry(subDirectory + "/"
					+ file.getZipName()));

			ByteArrayOutputStream bas = new ByteArrayOutputStream();
			BufferedImage image = file.getRotatedImage();
			ImageIO.write(image, ImageFile.getExtension(file.getZipName()), bas);
			byte[] data = bas.toByteArray();

			ByteArrayInputStream in = new ByteArrayInputStream(data);

			// Transfer bytes from the file to the ZIP file
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}

			// Complete the entry
			out.closeEntry();
			in.close();

			pm.setProgress(i + 1);
			pm.setNote("Compressed " + (i + 1) + "/" + iCount);
			if (pm.isCanceled()) {
				break;
			}

		}

		// Complete the ZIP file
		out.close();

		return fileName;
	}

	/**
	 * Ensures that the given file has the proper zip extension. If the file
	 * does not have a zip extension then one is added.
	 * 
	 * @param selectedFile
	 *            The file to make sure there is a zip extension. (Not null)
	 * @return The file updated to have zip extension. (Never null)
	 */
	private File ensureExtension(File selectedFile) {

		String[] split = selectedFile.getName().split("[.]");

		if (split.length < 0 || split.length > 0
				&& !"zip".equalsIgnoreCase(split[split.length - 1])) {
			selectedFile = new File(selectedFile.getParent() + File.separator
					+ selectedFile.getName() + ".zip");
		}

		return selectedFile;
	}
}
