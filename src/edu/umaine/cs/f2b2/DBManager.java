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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Use this class to get data from the database. This particular manager
 * connects to a Apache Derby database using the embedded driver.
 * 
 * @author Mark Royer
 * 
 */
public class DBManager {

	/**
	 * The directory where the image database will be stored.
	 */
	public static String storageDirectory = System.getProperty("user.home");

	/**
	 * The name of the database. All database related files will be a placed in
	 * this folder, which will be a sub-folder of dbDir.
	 */
	public static String dbName = ".4fb2";

	/**
	 * Fully qualified file path for the database.
	 */
	public static String dbLocation = storageDirectory + File.separator
			+ dbName;

	/**
	 * How the {@link DBManager} will connect to the database.
	 */
	final static String driver = "org.apache.derby.jdbc.EmbeddedDriver";

	/**
	 * Create a new connection to the Derby database.
	 * 
	 * @return A JDBC connection to the Derby database. (Never null)
	 * @throws SQLException
	 *             Thrown if there is a problem creating the connection to the
	 *             database.
	 */
	public static Connection getDerbyConnection() throws SQLException {

		// Make derby put the log file in the same place as the databse. If we
		// don't do this, then it will appear where ever the program is run at.
		System.setProperty("derby.stream.error.file", DBManager.dbLocation
				+ File.separatorChar + "derby.log");

		/*
		 * Connection URL for Derby.
		 */
		String connectionURL = "jdbc:derby:" + dbLocation + ";create=true";

		try {
			// Load the driver
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			/*
			 * In general this will never happen unless the derby jar is not on
			 * the class path.
			 */
			e.printStackTrace();
		}

		// Return the connection
		return DriverManager.getConnection(connectionURL);
	}

	/**
	 * If the tables do not exist create them.
	 * 
	 * @param conn
	 *            The connection to the database. (Not null)
	 * @throws SQLException
	 *             Thrown if there is a problem with the database.
	 */
	public static void createTables(Connection conn) throws SQLException {

		Statement st1 = conn.createStatement();
		ResultSet rs = st1.executeQuery("SELECT tablename FROM sys.systables "
				+ "WHERE tablename='IMAGES'");

		if (!rs.next()) {

			Statement st = conn.createStatement();

			/*
			 * The database is really simple. Just one table containing a path
			 * to the original file, when it was last modified, its width and
			 * height, byte code for the icon, and byte code for the smaller
			 * image.
			 */
			st.execute("CREATE TABLE images "
					+ "(imgpath VARCHAR(32672) PRIMARY KEY, "
					+ "moddate TIMESTAMP NOT NULL, "
					+ "originalWidth INTEGER NOT NULL, "
					+ "originalHeight INTEGER NOT NULL,"
					+ "icon BLOB(100K) NOT NULL, img BLOB(1M))");

		}

		rs.close();
	}

	/**
	 * Save an icon to the database.
	 * 
	 * @param conn
	 *            The database connection. The connection must be open. (Not
	 *            null)
	 * @param filePath
	 *            The fully qualified path to the file. (Not null)
	 * @param moddate
	 *            The most recently modification of the file. (Not null)
	 * @param icon
	 *            The icon that will be saved in the database. (Not null)
	 * @param originalWidth
	 *            The original width of the image in pixels.
	 * @param originalHeight
	 *            The original height of the picture in pixels.
	 * @throws SQLException
	 *             Thrown if there is any kind of exception while saving the
	 *             images' icon to the database.
	 */
	public static void saveIcon(Connection conn, String filePath,
			Timestamp moddate, ImageIcon icon, int originalWidth,
			int originalHeight) throws SQLException {

		boolean exists = fileIsInDB(conn, filePath);

		PreparedStatement ps;

		if (exists) {
			ps = conn
					.prepareStatement("UPDATE images SET icon=?,moddate=?,"
							+ "originalWidth=?,originalHeight=? "
							+ "WHERE imgpath = ?");

		} else {
			ps = conn.prepareStatement("INSERT INTO images "
					+ "(icon,moddate,originalWidth, originalHeight,imgpath) "
					+ "VALUES (?,?,?,?,?)");
		}

		ps.setTimestamp(2, moddate);
		ps.setInt(3, originalWidth);
		ps.setInt(4, originalHeight);
		ps.setString(5, filePath);

		try {

			ByteArrayInputStream bin = toByteArrayInputStream(icon);
			ps.setBinaryStream(1, bin);

			ps.executeUpdate();

		} catch (Exception e) {
			throw new SQLException(e);
		}
	}

	/**
	 * Return the icon for the image at the given path.
	 * 
	 * @param conn
	 *            The connection to the database. (Not null)
	 * @param filePath
	 *            The fully qualified path to the image file. (Not null)
	 * @return The icon for the image in the database or null if it does not
	 *         exist.
	 * @throws SQLException
	 *             Thrown if there is a problem getting the image icon from the
	 *             database.
	 */
	public static ImageIcon getIcon(Connection conn, String filePath)
			throws SQLException {

		PreparedStatement ps = conn.prepareStatement("SELECT icon FROM images "
				+ "WHERE imgpath = ?");

		ps.setString(1, filePath);

		try {

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {

				ObjectInputStream os = new ObjectInputStream(
						rs.getBinaryStream("icon"));

				ImageIcon result = (ImageIcon) os.readObject();

				rs.close();

				return result;
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new SQLException(e);
		}

		return null;
	}

	/**
	 * Save the resized image from the file system.
	 * 
	 * @param conn
	 *            The connection to the database. (Not null)
	 * @param filePath
	 *            The fully qualified path to the image file. (Not null)
	 * @param moddate
	 *            The last time the image file was modified. (Not null)
	 * @param icon
	 *            The icon for the image. (Not null)
	 * @param extension
	 *            The file type. For example, 'jpg', 'png', etc. (Not null)
	 * @param image
	 *            The scaled image of the original. (Not null)
	 * @param originalWidth
	 *            The width of the unmodified image. (Not null)
	 * @param originalHeight
	 *            The height of the unmodified image. (Not null)
	 * @throws SQLException
	 *             Thrown if there is a problem working with the image file
	 *             and/or the database.
	 */
	public static void saveResizedImage(Connection conn, String filePath,
			Timestamp moddate, ImageIcon icon, String extension,
			BufferedImage image, int originalWidth, int originalHeight)
			throws SQLException {

		boolean exists = fileIsInDB(conn, filePath);

		PreparedStatement ps;

		try {

			if (exists) {
				ps = conn.prepareStatement("UPDATE images SET "
						+ "img=?,moddate=?,originalWidth=?,originalHeight=? "
						+ "WHERE imgpath = ?");

				ByteArrayInputStream bin = toByteArrayInputStream(image,
						extension);
				ps.setBinaryStream(1, bin);
				ps.setTimestamp(2, moddate);
				ps.setInt(3, originalWidth);
				ps.setInt(4, originalHeight);
				ps.setString(5, filePath);

				ps.executeUpdate();

			} else {
				ps = conn.prepareStatement("INSERT INTO images "
						+ "(imgpath,moddate,originalWidth,"
						+ "originalHeight,icon,img) " + "VALUES (?,?,?,?,?,?)");

				ps.setString(1, filePath);
				ps.setTimestamp(2, moddate);
				ps.setInt(3, originalWidth);
				ps.setInt(4, originalHeight);

				ByteArrayInputStream bin = toByteArrayInputStream(icon);
				ps.setBinaryStream(5, bin);

				ByteArrayInputStream bin2 = toByteArrayInputStream(image,
						extension);
				ps.setBinaryStream(6, bin2);

				ps.executeUpdate();

			}

		} catch (IOException e) {
			e.printStackTrace();
			throw new SQLException(e);
		}

	}

	/**
	 * Get the resized image from the database.
	 * 
	 * @param conn
	 *            A connection to the database. (Not null)
	 * @param filePath
	 *            The fully qualified path to the image file including the file
	 *            itself. (Not null)
	 * @return The resized image. (null possible)
	 * @throws SQLException
	 *             Thrown if there is a problem reading the byte stream for the
	 *             image from the database.
	 */
	public static BufferedImage getResizedImage(Connection conn, String filePath)
			throws SQLException {

		PreparedStatement ps = conn.prepareStatement("SELECT img FROM images "
				+ "WHERE imgpath = ?");

		ps.setString(1, filePath);

		ResultSet rs = null;

		try {

			rs = ps.executeQuery();

			// There should be only 1 unique result for the resized image.
			if (rs.next()) {

				BufferedImage result = ImageIO.read(rs.getBinaryStream("img"));

				rs.close();

				return result;
			}

		} catch (IOException e) {
			e.printStackTrace();
			throw new SQLException(e);
		}

		if (rs != null) {
			rs.close();
		}

		return null;
	}

	/**
	 * Creates and writes the serializable object to the returned stream.
	 * 
	 * @param obj
	 *            A serializable object to be written to the stream. (Not null)
	 * @return A ByteArrayInputStream that the given object has been written to.
	 *         (Never null)
	 * @throws IOException
	 *             Thrown if there is a problem writing the serializable object
	 *             to the stream.
	 */
	private static ByteArrayInputStream toByteArrayInputStream(Serializable obj)
			throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(bos);

		os.writeObject(obj);

		ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
		return bin;
	}

	/**
	 * Creates and writes the given image to the returned input stream.
	 * 
	 * @param image
	 *            The image to write to the stream. (Not null)
	 * @param extension
	 *            The image type. (Not null)
	 * @return A stream that the image was written to. (Never null)
	 * @throws IOException
	 *             Thrown if there was a problem writing to the stream.
	 */
	private static ByteArrayInputStream toByteArrayInputStream(
			RenderedImage image, String extension) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		ImageIO.write(image, extension, bos);

		return new ByteArrayInputStream(bos.toByteArray());
	}

	/**
	 * Returns true iff the file path exists in the database.
	 * 
	 * @param conn
	 *            A connection to the database. (Not null)
	 * @param filePath
	 *            A fully qualified path of the file. (Not null)
	 * @return Returns true iff the file path exists in the database.
	 * @throws SQLException
	 *             Thrown if there is a problem with the database.
	 */
	public static boolean fileIsInDB(Connection conn, String filePath)
			throws SQLException {

		PreparedStatement ps = conn.prepareStatement("SELECT COUNT(imgpath) "
				+ "FROM images WHERE imgpath = ?");
		ps.setString(1, filePath);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {

			int result = rs.getInt(1);
			rs.close();

			return result == 1;
		} else {

			rs.close();
			return false;
		}
	}

	/**
	 * Destroys all of the tables in the database.
	 * 
	 * @param conn
	 *            A connection to the database. (Not null)
	 * @throws SQLException
	 *             Thrown if there is a problem with the database.
	 */
	public static void destroyTables(Connection conn) throws SQLException {

		Statement st = conn.createStatement();

		st.execute("DROP TABLE images");

	}

	/**
	 * Looks to see if there is a resized image in the database that matches the
	 * given file.
	 * 
	 * @param conn
	 *            A connection to the database. (Not null)
	 * @param filePath
	 *            A fully qualified path to the image file. (Not null)
	 * @return true iff a resized image with the given path name was found in
	 *         the database.
	 * @throws SQLException
	 *             Thrown if there is a problem with the database.
	 */
	public static boolean resizedImageIsInDB(Connection conn, String filePath)
			throws SQLException {

		PreparedStatement ps = conn.prepareStatement("SELECT img "
				+ "FROM images WHERE imgpath = ?");
		ps.setString(1, filePath);

		ResultSet rs = ps.executeQuery();

		if (rs.next()) {

			Blob result = rs.getBlob(1);
			rs.close();

			return result != null;
		} else {
			rs.close();

			return false;
		}

	}

	/**
	 * Removes image data from the database that matches the given file path.
	 * 
	 * @param conn
	 *            A connection to the database. (Not null)
	 * @param filePath
	 *            A fully qualified path to the image file. (Not null)
	 * @throws SQLException
	 *             Thrown if there is a problem with the database.
	 */
	public static void removeImageFile(Connection conn, String filePath)
			throws SQLException {

		PreparedStatement ps = conn
				.prepareStatement("DELETE FROM images WHERE imgpath = ?");

		ps.setString(1, filePath);

		ps.executeUpdate();
	}

	/**
	 * Returns the image dimensions from the database.
	 * 
	 * @param conn
	 *            A connection to the databse. (Not null)
	 * @param absolutePath
	 *            A fully qualified path to the image file. (Not null)
	 * @return The original dimensions of the image. (Never null)
	 * @throws SQLException
	 *             Thrown if there is a problem with the database.
	 */
	public static Dimension getOriginalImageDimensions(Connection conn,
			String absolutePath) throws SQLException {

		PreparedStatement ps = conn.prepareStatement("SELECT "
				+ "originalWidth, originalHeight "
				+ "FROM images WHERE imgpath = ?");

		ps.setString(1, absolutePath);

		ResultSet rs = ps.executeQuery();

		Dimension result = null;

		if (rs.next()) {
			result = new Dimension(rs.getInt(1), rs.getInt(2));
		}

		rs.close();

		return result;
	}

	/**
	 * @return A fully qualified path of the database's location in the file
	 *         system. (Never null)
	 */
	public static String getDatabaseLocation() {
		return dbLocation;
	}
}
