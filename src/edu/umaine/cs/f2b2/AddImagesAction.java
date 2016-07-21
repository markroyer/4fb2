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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileFilter;

/**
 * Action that loads an image from the file system, puts it in the database and
 * adds it to the JList.
 * 
 * @author Mark Royer
 * 
 */
public class AddImagesAction extends AbstractAction {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = 2857899041556398358L;

	/**
	 * The main program.
	 */
	private FFB2 f2b2;

	/**
	 * The last directory images were loaded from.
	 */
	private File lastDirectory;

	/**
	 * Filter for only *.jpg, *.png, and *.gif files.
	 */
	static ImageFilter imageFilter = new ImageFilter();

	/**
	 * Create a new {@link AddImagesAction} for the {@link FFB2} program.
	 * 
	 * @param f2b2
	 *            The main {@link FFB2} program. (Not null)
	 */
	public AddImagesAction(FFB2 f2b2) {
		super("Add Images");
		this.f2b2 = f2b2;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {

		final JFileChooser fileChooser = new JFileChooser(getLastDirectory());
		fileChooser.setFileFilter(imageFilter);
		fileChooser.setMultiSelectionEnabled(true);

		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.setVisible(true);
		int answer = fileChooser.showDialog(f2b2, "Add Images");

		if (answer == JFileChooser.APPROVE_OPTION) {

			// We'll add the files in the background so that the rest of the
			// application doesn't lock up.
			new Thread(new Runnable() {

				public void run() {
					addFilesToList(fileChooser.getSelectedFiles());

					if (fileChooser.getSelectedFile().getParent() != null) {
						lastDirectory = new File(fileChooser.getSelectedFile()
								.getParent());
					}

				}
			}).start();
		}
	}

	/**
	 * Add the chosen files to the list. This method will create a progress bar
	 * and will update it while the images are being loaded. To make sure the
	 * progress bar is updated, it should be run in a separate thread.
	 * 
	 * @param files
	 *            The files to be added to the list. (Not null)
	 */
	private void addFilesToList(File[] files) {

		List<File> imageFiles = getImageFiles(new ArrayList<File>(), files, 0,
				1);

		int iCount = imageFiles.size();
		ProgressMonitor pm = new ProgressMonitor(null, null, "Loaded 0/"
				+ iCount, 0, iCount);

		Collections.sort(imageFiles);

		for (int i = 0; i < iCount; i++) {

			File f = imageFiles.get(i);

			if (!f2b2.addFile(new ImageFile(f.getAbsolutePath()))) {
				pm.close();
				break;
			}

			pm.setProgress(i + 1);
			pm.setNote("Loaded " + (i + 1) + "/" + iCount);
			if (pm.isCanceled()) {
				break;
			}
		}

		f2b2.imagesHaveBeenAdded();
	}

	/**
	 * Recursively add image files and image files in sub-folders to result. For
	 * each file in the files array, if it is a folder and and depth hasn't
	 * exceeded max depth, add it to the result list.
	 * 
	 * @param result
	 *            The image files. (Not null)
	 * @param files
	 *            The files to look for images. (Not null)
	 * @param depth
	 *            The current recursion depth. Initialize this to 0 for the
	 *            current working directory.
	 * @param maxDepth
	 *            The maximum recursion depth.
	 * @return The image files in the files and folders given. (Never null)
	 */
	private List<File> getImageFiles(List<File> result, File[] files,
			int depth, int maxDepth) {

		for (File f : files) {
			if (f.isFile() && imageFilter.accept(f)) {
				result.add(f);
			} else if (f.isDirectory() && depth < maxDepth) {
				getImageFiles(result, f.listFiles(), depth + 1, maxDepth);
			}
		}

		return result;
	}

	/**
	 * Returns the most recent directory that the user entered.
	 * 
	 * @return Returns the default directory if no directory has been chosen.
	 */
	public File getLastDirectory() {

		if (lastDirectory == null) {

			String desktopDir = System.getProperty("user.home")
					+ File.separator + "Desktop";

			lastDirectory = new File(desktopDir);

		}

		return lastDirectory;
	}

	/**
	 * A filter used for showing only *.gif, *.jpeg, *.jpg, and *.png files when
	 * browsing the file system.
	 * 
	 * @author mroyer
	 * 
	 */
	public static class ImageFilter extends FileFilter {

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
		 */
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}

			String[] split = f.getName().split("[.]");

			if (split.length > 0) {
				String extension = split[split.length - 1];

				if (extension != null) {
					if (extension.equalsIgnoreCase("gif")
							|| extension.equalsIgnoreCase("jpeg")
							|| extension.equalsIgnoreCase("jpg")
							|| extension.equalsIgnoreCase("png")) {
						return true;
					} else {
						return false;
					}
				}
			}

			// The file was not an accepted image file
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return "Accepted Images";
		}
	}

}
