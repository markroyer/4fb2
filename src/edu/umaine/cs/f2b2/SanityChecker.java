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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.imageio.ImageIO;

/**
 * Used to check the database for files that have been updated on the file
 * system.
 * 
 * @author Mark Royer
 * 
 */
public class SanityChecker { // implements Runnable {

	/**
	 * Checks the database to see if the files have been modified on the file
	 * system.
	 */
	public void checkDatabaseFileReferences() {
		Connection conn;
		try {
			conn = DBManager.getDerbyConnection();
			conn.setAutoCommit(false);
			checkDatabaseFileReferences(conn);
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks images in the database and updates their icons if they have been
	 * modified in the file system since they were created in the database.
	 * 
	 * @param conn
	 *            A connection to the database. (Not null)
	 * @throws SQLException
	 *             Thrown if there is a problem connecting to the database.
	 * @throws IOException
	 *             Thrown if there is a problem reading the image file from the
	 *             disk.
	 */
	public void checkDatabaseFileReferences(Connection conn)
			throws SQLException, IOException {

		Statement st = conn.createStatement();

		DBManager.createTables(conn);

		ResultSet rs = st.executeQuery("SELECT imgpath, moddate FROM images");

		/*
		 * For each image in the database check to see if it has been altered in
		 * the file system. If it has, update the image icon in the database.
		 */
		while (rs.next()) {
			File file = new File(rs.getString("imgpath"));
			Timestamp moddate = rs.getTimestamp("moddate");

			if (!file.exists()) {
				DBManager.removeImageFile(conn, file.getAbsolutePath());
			} else if (moddate.getTime() < file.lastModified()) {
				// Update the icon to reflect the change in the image

				ImageFile imageFile = new ImageFile(file.getAbsolutePath());

				BufferedImage buff = ImageIO.read(imageFile);

				DBManager.saveResizedImage(conn, imageFile.getAbsolutePath(),
						new Timestamp(imageFile.lastModified()),
						imageFile.getSmallImage(), imageFile.getExtension(),
						imageFile.getResizedImage(), buff.getWidth(),
						buff.getHeight());
			}
		}
	}
}
