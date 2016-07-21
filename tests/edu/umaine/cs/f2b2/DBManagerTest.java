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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that the database connections work properly.
 * 
 * @author Mark Royer
 * 
 */
public class DBManagerTest {

	/**
	 * Connection to the database.
	 */
	private Connection conn;

	/**
	 * An image file to use for testing.
	 */
	private ImageFile imageFile = new ImageFile(this.getClass()
			.getResource("meAtMtDesert.jpg").getPath());

	/**
	 * Make sure the database is ready for testing.
	 * 
	 * @throws Exception
	 *             Thrown if there is a problem setting up the database.
	 */
	@Before
	public void setUp() throws Exception {

		File f = new File(DBManager.getDatabaseLocation());

		// Make sure we have a clean directory to work with
		if (f.exists())
			deleteDirectory(f);

		conn = DBManager.getDerbyConnection();
		DBManager.createTables(conn);
	}

	/**
	 * Remove all of the test database directory.
	 * 
	 * @throws Exception
	 *             Thrown if there is a problem removing the database.
	 */
	@After
	public void tearDown() throws Exception {
		DBManager.destroyTables(conn);
		conn.close();
		deleteDirectory(new File(DBManager.dbLocation));

	}

	/**
	 * Make sure the database is correctly created.
	 */
	@Test
	public void ensureDatabaseCreated() {

		assertTrue(new File(DBManager.dbLocation).exists());

	}

	/**
	 * Make sure image icons are correctly created in the database.
	 * 
	 * @throws SQLException
	 *             Thrown if there is a problem connecting to the database.
	 * @throws IOException
	 *             Thrown if there is a problem reading the image file from the
	 *             file system.
	 */
	@Test
	public void testFileIsInDB() throws SQLException, IOException {

		assertFalse(DBManager.fileIsInDB(conn, imageFile.getAbsolutePath()));

		DBManager.saveIcon(conn, imageFile.getAbsolutePath(), new Timestamp(
				imageFile.lastModified()), imageFile.createImageIcon(),
				imageFile.getOriginalWidth(), imageFile.getOriginalHeight());

		assertTrue(DBManager.fileIsInDB(conn, imageFile.getAbsolutePath()));

	}

	/**
	 * Ensures that an icon can be properly retrieved from the database.
	 * 
	 * @throws SQLException
	 *             Thrown if there is a problem connecting to the database.
	 * @throws IOException
	 *             Thrown if there is a problem getting the image information
	 *             from the file system.
	 */
	@Test
	public void testGetIcon() throws SQLException, IOException {

		imageFile.createImageIcon();

		DBManager.saveIcon(conn, imageFile.getAbsolutePath(), new Timestamp(
				imageFile.lastModified()), imageFile.createImageIcon(),
				imageFile.getOriginalWidth(), imageFile.getOriginalHeight());

		assertTrue(DBManager.fileIsInDB(conn, imageFile.getAbsolutePath()));

		ImageIcon icon = DBManager.getIcon(conn, imageFile.getAbsolutePath());

		assertEquals(100, icon.getIconWidth());
		assertEquals(77, icon.getIconHeight());

	}

	/**
	 * Ensures that the image is properly resized from the database.
	 * 
	 * @throws SQLException
	 *             Thrown if there is a problem connecting to the database.
	 * @throws IOException
	 *             Thrown if there is a problem getting the image information
	 *             from the file system.
	 */
	@Test
	public void testGetResizedImage() throws SQLException, IOException {

		BufferedImage oImage = ImageIO.read(imageFile);

		DBManager.saveResizedImage(conn, imageFile.getAbsolutePath(),
				new Timestamp(imageFile.lastModified()), imageFile
						.getSmallImage(), imageFile.getExtension(), imageFile
						.createScaledInstanceMax(oImage,
								ZipImagesAction.MAXWIDTHORHEIGHT,
								ZipImagesAction.MAXWIDTHORHEIGHT), oImage
						.getWidth(), oImage.getHeight());

		assertTrue(DBManager.fileIsInDB(conn, imageFile.getAbsolutePath()));
		assertTrue(DBManager.resizedImageIsInDB(conn,
				imageFile.getAbsolutePath()));

		BufferedImage image = DBManager.getResizedImage(conn,
				imageFile.getAbsolutePath());

		assertEquals(300, image.getWidth());
		assertEquals(233, image.getHeight());

	}

	/**
	 * 
	 * Ensures that a resized image is properly saved to the database.
	 * 
	 * @throws SQLException
	 *             Thrown if there is a problem connecting to the database.
	 * @throws IOException
	 *             Thrown if there is a problem getting the image information
	 *             from the file system.
	 */
	@Test
	public void testSaveResizedImage() throws SQLException, IOException {

		assertTrue(imageFile.exists());

		assertFalse(DBManager.resizedImageIsInDB(conn,
				imageFile.getAbsolutePath()));

		BufferedImage image = ImageIO.read(imageFile);

		DBManager.saveResizedImage(conn, imageFile.getAbsolutePath(),
				new Timestamp(imageFile.lastModified()), imageFile
						.getSmallImage(), imageFile.getExtension(), imageFile
						.createScaledInstanceMax(image,
								ZipImagesAction.MAXWIDTHORHEIGHT,
								ZipImagesAction.MAXWIDTHORHEIGHT), image
						.getWidth(), image.getHeight());

		assertTrue(DBManager.resizedImageIsInDB(conn,
				imageFile.getAbsolutePath()));

	}

	/**
	 * Recursively deletes the given directory from the file system.
	 * 
	 * @param path
	 *            The directory to delete. (Not null)
	 * @return true iff the directory was successfully deleted.
	 */
	static public boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

}
