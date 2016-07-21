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

import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JOptionPane;

/**
 * An object that takes image files and creates icons used in the application.
 * The icons that are created are then stored in the database so that they can
 * quickly be loaded in the future without the need to resize the images, which
 * is a time consuming task. If an icon exists in the database already, that
 * icon is used.
 * 
 * @author Mark Royer
 * 
 */
public class IconLoader implements Runnable {

	/**
	 * Images that need to have icons created or obtained from the database.
	 */
	private Queue<ImageFile> files;

	/**
	 * Reference back to the main program, which will be notified when icons are
	 * updated.
	 */
	private FFB2 ffb2;

	/**
	 * Singleton pattern; we only want to create one of these helper objects.
	 */
	private static IconLoader iconLoader;

	/**
	 * Create an instance to work on the given ffb2 program.
	 * 
	 * @param ffb2
	 *            The main ffb2 program. (Not null)
	 */
	private IconLoader(FFB2 ffb2) {
		this.ffb2 = ffb2;
		files = new LinkedList<ImageFile>();
	}

	/**
	 * Singleton pattern. Users should obtain an instance of the icon loader
	 * through this method.
	 * 
	 * @param ffb2
	 *            The main 4fb2 application panel. (Not null)
	 * @return Reference to an icon loader. (Never null)
	 */
	public static IconLoader getIconLoader(FFB2 ffb2) {
		if (iconLoader == null) {
			iconLoader = new IconLoader(ffb2);
		}
		return iconLoader;
	}

	/**
	 * Invoke this method to start a new thread which will work on creating or
	 * finding icons for files that have been added to the system.
	 */
	public void filesHaveBeenAdd() {
		new Thread(this).start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		/*
		 * Block all other threads from manipulating the files that are in the
		 * queue.
		 */
		synchronized (files) {

			/*
			 * Create and lookup icons for each file in the queue.
			 */
			while (files.size() > 0) {
				ImageFile f = files.remove();

				synchronized (ffb2.getFileModel()) {
					try {
						f.createImageIcon();
					} catch (Exception e) {
						JOptionPane
								.showMessageDialog(
										null,
										"Unable to view file: "
												+ f.getAbsolutePath()
												+ "\nIt will be removed from the zip archive.",
										"Error", JOptionPane.ERROR_MESSAGE);

						ffb2.getFileModel().removeElement(f);

						// e.printStackTrace();
					}

					ffb2.getFileModel().fireContentChanged(f);
				}
			}
		}
	}

	/**
	 * Adds the given file to the list of files to be loaded into the database.
	 * Smaller image sizes of the file will be created.
	 * 
	 * @param file
	 *            The file to be loaded into the database. (Not null)
	 */
	public synchronized void addFile(ImageFile file) {
		synchronized (file) {
			files.add(file);
		}
	}

}
