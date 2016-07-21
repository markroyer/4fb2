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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Represents an image file in the file system. The image file has additional
 * information about various properties, eg. how it is rotated, its width,
 * height, etc.
 * 
 * @author Mark Royer
 * 
 */
public class ImageFile extends File {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = 2077490448439753126L;

	/**
	 * The name that will be used to represent this image file when it is
	 * zipped.
	 */
	private String zipName;

	/**
	 * The width and height of the icon used to display this image.
	 */
	private static int ICONWIDTH = 100, ICONHEIGHT = 100;

	private final static ImageIcon nullImage = new ImageIcon(Toolkit
			.getDefaultToolkit().getImage(
					ImageFile.class.getResource("missingIcon.gif")));

	/**
	 * An icon for the current image. Very small representation of the image.
	 */
	private ImageIcon smallImage;

	/**
	 * The number of degrees that the image is rotated.
	 */
	private int rotation;

	/**
	 * The width of the image on disk in pixels.
	 */
	private Integer originalWidth;

	/**
	 * The height of the image on disk in pixels.
	 */
	private Integer originalHeight;

	/**
	 * The current amount of rotation the image has had done to it. This can
	 * differ from the amount of rotation, which can be persisted in the
	 * database.
	 */
	private int currentDegrees;

	/**
	 * Create a new {@link ImageFile} from the given file path.
	 * 
	 * @param pathname
	 *            The fully qualified path to the given file. (Not null)
	 */
	public ImageFile(String pathname) {
		super(pathname);
		this.smallImage = nullImage;
		zipName = this.getName();
		rotation = 0;
	}

	/**
	 * Returns the file extension. The text following the period in the name.
	 * 
	 * @param fileName
	 *            The name to get the extension from. (Not null)
	 * @return The extension of the file name not including the period. If there
	 *         is no extension then the empty string is returned. (Never null)
	 */
	public static String getExtension(String fileName) {

		String[] split = fileName.split("[.]");

		// There was no extension just return the empty string.
		if (split.length == 1) {
			return "";
		}

		return split[split.length - 1];
	}

	/**
	 * Returns the icon representation of this image file.
	 * 
	 * @return The icon. (Never null)
	 */
	public ImageIcon getSmallImage() {
		synchronized (smallImage) {
			return smallImage;
		}
	}

	/**
	 * Creates an icon for the current image and places the information into the
	 * database so that it can be quickly retrieved in the future.
	 * 
	 * @return The icon that was created. (Never null)
	 * @throws IOException
	 *             Thrown if there is a problem accessing the image on the file
	 *             system.
	 * @throws SQLException
	 *             Thrown if there was a problem storing the icon information to
	 *             the database.
	 */
	public synchronized ImageIcon createImageIcon() throws IOException,
			SQLException {

		if (this.smallImage != nullImage) {
			rotate(rotation);
		} else {

			Connection conn = DBManager.getDerbyConnection();

			if (DBManager.fileIsInDB(conn, this.getAbsolutePath())) {
				this.smallImage = DBManager.getIcon(conn,
						this.getAbsolutePath());
			} else {

				int width = ICONWIDTH;
				int height = ICONHEIGHT;

				ImageIcon image = new ImageIcon(Toolkit.getDefaultToolkit()
						.getImage(this.getAbsolutePath()));

				originalWidth = image.getIconWidth();
				originalHeight = image.getIconHeight();

				if (image.getIconWidth() > image.getIconHeight()) {
					height = (int) ((image.getIconHeight() / (double) image
							.getIconWidth()) * ICONWIDTH);
				} else if (image.getIconWidth() < image.getIconHeight()) {
					width = (int) ((image.getIconWidth() / (double) image
							.getIconHeight()) * ICONHEIGHT);
				}

				ImageIcon tmpImage = new ImageIcon(image.getImage()
						.getScaledInstance(width, height, Image.SCALE_SMOOTH));

				DBManager.saveIcon(conn, getAbsolutePath(), new Timestamp(
						lastModified()), tmpImage, originalWidth,
						originalHeight);

				this.smallImage = tmpImage;
				// System.out.println("Created small image for "
				// + this.smallImage.toString());
			}

			conn.close();
		}

		return this.smallImage;
	}

	/**
	 * Rotates the current image the specified number of degrees.
	 * 
	 * @param degrees
	 *            The number of degrees the image should be rotated.
	 * @throws SQLException
	 *             Thrown if there is s a problem accessing the image from the
	 *             database.
	 */
	public synchronized void rotate(int degrees) throws SQLException {

		if (degrees != currentDegrees) {
			currentDegrees = degrees;

			Connection conn = DBManager.getDerbyConnection();
			conn.setAutoCommit(false);

			ImageIcon icon = DBManager.getIcon(conn, getAbsolutePath());

			BufferedImage b = new BufferedImage(icon.getIconWidth(),
					icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);

			Graphics2D g = (Graphics2D) b.getGraphics();

			g.drawImage(icon.getImage(), 0, 0, icon.getIconWidth(),
					icon.getIconHeight(), null, null);

			smallImage = new ImageIcon(getScaledInstance(icon.getIconWidth(),
					icon.getIconHeight(), b, rotation));

			conn.commit();
			conn.close();
		}

		System.gc();
	}

	/**
	 * Returns the resized image that will be stored inside the zip file. This
	 * is done lazily, so if the resized image has already been created it will
	 * be loaded from the database. If the resized image has not been created,
	 * then it will be created now and will be stored in the database for future
	 * access.
	 * 
	 * @return The resized image that will be stored in the zip file. (Never
	 *         null)
	 * @throws SQLException
	 *             Thrown if there is a problem accessing the image in the
	 *             database.
	 * @throws IOException
	 *             Thrown if there is a problem accessing the file on disk.
	 */
	public synchronized BufferedImage getResizedImage() throws SQLException,
			IOException {

		Connection conn = DBManager.getDerbyConnection();
		conn.setAutoCommit(false);

		BufferedImage result;

		if (DBManager.resizedImageIsInDB(conn, getAbsolutePath())) {
			result = DBManager.getResizedImage(conn, getAbsolutePath());
		} else {

			BufferedImage image = ImageIO.read(this);

			result = createScaledInstanceMax(image,
					ZipImagesAction.MAXWIDTHORHEIGHT,
					ZipImagesAction.MAXWIDTHORHEIGHT);

			DBManager.saveResizedImage(conn, getAbsolutePath(), new Timestamp(
					lastModified()), getSmallImage(), getExtension(), result,
					image.getWidth(), image.getHeight());
		}

		conn.commit();
		conn.close();

		return result;
	}

	/**
	 * Returns the file extension of this image.
	 * 
	 * @return The file extension of the image. (Never null)
	 */
	public String getExtension() {
		return ImageFile.getExtension(getName());
	}

	/**
	 * Returns an instance of the given image scaled to the given width and
	 * height. The ration of width to height is preserved. The returned image
	 * will not be wider than the specified max width or higher than the
	 * specified max height.
	 * 
	 * @param image
	 *            The image to be scaled. (Not null)
	 * @param maxwidth
	 *            The maximum width the image can be.
	 * @param maxheight
	 *            The maximum height the image can be.
	 * @return The image scaled to now wider than the specified max width and no
	 *         higher than the specified max height. (Never null)
	 */
	public BufferedImage createScaledInstanceMax(BufferedImage image,
			int maxwidth, int maxheight) {

		int width = maxwidth;
		int height = maxheight;

		if (image.getWidth() > maxwidth || image.getHeight() > maxheight) {
			if (image.getWidth() > image.getHeight()) {
				height = (int) ((image.getHeight() / (double) image.getWidth()) * maxwidth);
			} else if (image.getWidth() < image.getHeight()) {
				width = (int) ((image.getWidth() / (double) image.getHeight()) * maxheight);
			}
		} else {
			width = image.getWidth();
			height = image.getHeight();
		}

		return getScaledInstance(width, height, image, 0);
	}

	/**
	 * Scales the given image to the width and height specified. The image is
	 * also rotated by the specified number of degrees.
	 * 
	 * @param width
	 *            The width in pixels.
	 * @param height
	 *            The height in pixels.
	 * @param image
	 *            The image to scale. (Not null)
	 * @param rotation
	 *            The amount in degrees to rotate the image.
	 * @return The scaled and rotated image. (Never null)
	 */
	public BufferedImage getScaledInstance(final int width, final int height,
			BufferedImage image, int rotation) {

		AffineTransform at = AffineTransform.getRotateInstance(Math
				.toRadians(rotation));

		BufferedImage b = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);

		if (rotation == 90) {
			at.translate(0, -height);
			b = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);
		} else if (rotation == 180) {
			at.translate(-width, -height);
		} else if (rotation == 270) {
			at.translate(-width, 0);
			b = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);
		} // No translation

		at.scale(width / ((double) image.getWidth()),
				height / ((double) image.getHeight()));

		Graphics2D g = (Graphics2D) b.getGraphics();

		g.drawRenderedImage(image, at);

		return b;
	}

	/**
	 * Rotates the image 90 degrees counter clockwise.
	 * 
	 * @throws SQLException
	 *             Thrown if there is a problem accessing the image in the
	 *             database.
	 */
	public void rotateLeft() throws SQLException {
		rotation = rotation - 90;

		if (rotation < 0) {
			rotation = 270;
		}

		rotate(rotation);
	}

	/**
	 * Rotates the image 90 degrees to clockwise.
	 * 
	 * @throws SQLException
	 *             Thrown if there is a problem accessing the image in the
	 *             database.
	 */
	public void rotateRight() throws SQLException {
		rotation = (rotation + 90) % 360;
		rotate(rotation);
	}

	/**
	 * Returns the name that will be used to represent this image in the zip
	 * file. If one hasn't been specified then the original name is used.
	 * 
	 * @return The name used to represent this image in the zip file. (Never
	 *         null)
	 */
	public String getZipName() {
		return zipName;
	}

	/**
	 * Sets the name to be used to represent this image file in the zip file.
	 * 
	 * @param zipName
	 *            The name to use for representing this image in the zip file.
	 *            (Not null)
	 */
	public void setZipName(String zipName) {
		this.zipName = zipName;
	}

	/**
	 * Returns the original width of the image.
	 * 
	 * @return The original width of the image.
	 * @throws SQLException
	 *             Thrown if there is a problem accessing the image information
	 *             in the database.
	 */
	public int getOriginalWidth() throws SQLException {
		if (originalWidth == null) {
			getDimensionsFromDb();
		}
		return originalWidth;
	}

	/**
	 * Returns the original height of the image.
	 * 
	 * @return The original height of the image.
	 * @throws SQLException
	 *             Thrown if there is a problem accessing the image information
	 *             in the database.
	 */
	public int getOriginalHeight() throws SQLException {
		if (originalHeight == null) {
			getDimensionsFromDb();
		}

		return originalHeight;
	}

	/**
	 * Updates this images original dimensions of the image from the database.
	 * 
	 * @throws SQLException
	 *             Thrown if there is s a problem getting the dimensions from
	 *             the database.
	 */
	private void getDimensionsFromDb() throws SQLException {
		Connection conn = DBManager.getDerbyConnection();
		Dimension d = DBManager.getOriginalImageDimensions(conn,
				getAbsolutePath());

		originalWidth = (int) d.getWidth();
		originalHeight = (int) d.getHeight();
	}

	/**
	 * Returns the current image resized and rotated.
	 * 
	 * @return The current image resized and rotated. (Never null)
	 * @throws SQLException
	 *             Thrown if there is a problem accessing the database.
	 * @throws IOException
	 *             Thrown if there is a problem accessing the image on disk.
	 */
	public BufferedImage getRotatedImage() throws SQLException, IOException {

		BufferedImage image = this.getResizedImage();

		return getScaledInstance(image.getWidth(), image.getHeight(), image,
				rotation);
	}
}
