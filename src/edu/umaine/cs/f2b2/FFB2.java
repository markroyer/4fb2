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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.SplashScreen;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * The entrance point to the 4fb2 program. The program itself is contained
 * inside of a JPanel so that it may be embedded inside of another application
 * if desired.
 * 
 * @author Mark Royer
 * 
 */
public class FFB2 extends JPanel {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = 386279452358088867L;

	/**
	 * Create a F2b2 instance as a stand-alone instance.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		/*
		 * How long the splash image should be shown in milliseconds.
		 */
		final int splashTime = 2000;

		createSplashScreen(splashTime);

		JFrame window = new JFrame("4FB2");
		window.getContentPane().add(new FFB2());
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.validate();
		window.pack();
		window.setLocationRelativeTo(null);
		window.setVisible(true);

	}

	/**
	 * List of image files to be shrunk and zipped.
	 */
	private ImageJList fileJList;

	/**
	 * Add files to the list.
	 */
	protected AddImagesAction addImagesAction;

	/**
	 * Shrink and zip the images.
	 */
	protected ZipImagesAction zipImagesAction;

	/**
	 * An object that is responsible for loading or creating icons for the
	 * images.
	 */
	private IconLoader iconLoader;

	/**
	 * Create a new F2b2 instance that contains a list of files and an add files
	 * button and a save button.
	 */
	public FFB2() {

		try {

			// We want to make sure that the image files in the database have
			// not been altered in the file system since the last time the
			// program has looked at them.
			new SanityChecker().checkDatabaseFileReferences();

			Connection conn = DBManager.getDerbyConnection();
			conn.setAutoCommit(false);
			DBManager.createTables(conn);
			conn.commit();
			conn.close();
		} catch (SQLException e) {
			// Not expected to occur unless there is a problem with the
			// libraries.
			e.printStackTrace();
		}

		iconLoader = IconLoader.getIconLoader(this);
		new Thread(iconLoader).start();

		JPanel upperPanel = new JPanel(new GridLayout(1, 1));
		JPanel lowerPanel = new JPanel();

		fileJList = new ImageJList();

		JScrollPane scrollPane = new JScrollPane(fileJList);

		upperPanel.add(scrollPane);

		JButton addImagesJButton = new JButton(new AddImagesAction(this));
		JButton zipImagesJButton = new JButton(new ZipImagesAction(this));

		lowerPanel.add(addImagesJButton);
		lowerPanel.add(zipImagesJButton);

		this.setLayout(new BorderLayout(0, 5));

		this.add(upperPanel, BorderLayout.CENTER);
		this.add(lowerPanel, BorderLayout.SOUTH);

		this.setPreferredSize(new Dimension(500, 500));

	}

	/**
	 * Add the given file to the list of files to be resized and zipped.
	 * 
	 * @param f
	 *            An image file from the file system. (Not null)
	 * @return true iff the file is added to the list of images.
	 */
	public boolean addFile(final ImageFile f) {

		String result = f.getName();

		while (fileJList.getModel().hasSameName(f.getZipName())) {
			String ans = JOptionPane.showInputDialog(
					"There is already a file named '" + result
							+ "'.\nPlease rename the zip file name.", result);

			if (ans == null || ans == "") {
				return false;
			}

			result = ans;
			f.setZipName(result);
		}

		synchronized (fileJList.getModel()) {
			fileJList.getModel().addElement(f);
			iconLoader.addFile(f);
		}

		return true;
	}

	/**
	 * Returns a list of all of the images that the user has chosen to resize
	 * and zip.
	 * 
	 * @return The files the user has chosen to save. (Never null)
	 */
	public List<ImageFile> getFiles() {

		List<ImageFile> files = new ArrayList<ImageFile>();

		Enumeration<?> e = ((DefaultListModel) fileJList.getModel()).elements();

		while (e.hasMoreElements()) {
			files.add((ImageFile) e.nextElement());
		}

		return files;

	}

	/**
	 * Paints the splash image to the screen for the given amount of time.
	 * 
	 * @param splashTime
	 *            The amount of time to show the splash image in milliseconds.
	 */
	public static void createSplashScreen(int splashTime) {

		final SplashScreen splash = SplashScreen.getSplashScreen();
		if (splash == null) {
			return;
		}
		Graphics2D g = splash.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_DITHERING,
				RenderingHints.VALUE_DITHER_ENABLE);

		try {
			Thread.sleep(splashTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Returns the model that is used for storing the images.
	 * 
	 * @return The model used for managing the images. (Never null)
	 */
	public ImageJListModel getFileModel() {
		return this.fileJList.getModel();
	}

	/**
	 * Starts a new thread to update and create image icons for the newly added
	 * images.
	 */
	public void imagesHaveBeenAdded() {
		IconLoader.getIconLoader(this).filesHaveBeenAdd();
	}
}
