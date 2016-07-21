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
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.umaine.cs.f2b2.ImageDialog.NextImageAction.Direction;

/**
 * A simple dialog for presenting images to the user for review. The dialog
 * consists of a single image with previous and next buttons.
 * 
 * @author Mark Royer
 * 
 */
public class ImageDialog extends JDialog {

	/**
	 * For serialization.
	 */
	private static final long serialVersionUID = -1434681033958766051L;

	/**
	 * The current image's index in the list of images.
	 */
	private int imageIndex;

	/**
	 * A label where the current image is displayed.
	 */
	private JLabel imageHolder;

	/**
	 * Label used for displaying the current index out of the maximum number of
	 * images. For example, 10/20 images.
	 */
	private JLabel outOfLabel;

	/**
	 * The model used for storing the images.
	 */
	private ImageJListModel model;

	/**
	 * Create a new image dialog with the given model and the index of the
	 * active image to be displayed.
	 * 
	 * @param model
	 *            The list of images. (Not null)
	 * @param selectedIndex
	 *            The selected image to display.
	 * @throws Exception
	 *             Thrown if there is a problem loading the image.
	 */
	public ImageDialog(ImageJListModel model, int selectedIndex)
			throws Exception {

		imageIndex = selectedIndex;

		imageHolder = new JLabel();

		this.model = model;
		outOfLabel = new JLabel(getOutOfLabel());

		setImage(selectedIndex);

		this.setLayout(new BorderLayout());

		imageHolder.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				ImageDialog.this.setVisible(false);
				ImageDialog.this.dispose();
			}

		});

		Dimension imSize = new Dimension(800, 800);
		imageHolder.setPreferredSize(imSize);
		imageHolder.setToolTipText("Click to close image.");

		JScrollPane sp = new JScrollPane(imageHolder);
		// sp.setPreferredSize(imSize);
		this.add(sp, BorderLayout.CENTER);

		JButton prevImage = new JButton(new NextImageAction("Previous Image",
				Direction.PREVIOUS, this));
		prevImage.setToolTipText("Display the previous image in the list.");
		JButton nextImage = new JButton(new NextImageAction("Next Image",
				Direction.NEXT, this));
		nextImage.setToolTipText("Display the next image in the list.");
		JPanel bottom = new JPanel();

		bottom.add(prevImage);
		bottom.add(nextImage);
		bottom.add(outOfLabel);

		this.add(bottom, BorderLayout.PAGE_END);

		this.pack();
	}

	/**
	 * Returns a string representing the current index of the image being
	 * displayed out of all the images. For example, (10/20).
	 * 
	 * @return
	 */
	public String getOutOfLabel() {
		return " ( " + (imageIndex + 1) + " / " + model.getSize() + " ) ";
	}

	/**
	 * Updates the displayed image to the image with the given index in the list
	 * of images.
	 * 
	 * @param selectedIndex
	 *            The index of the image to display.
	 * @throws SQLException
	 *             Thrown if there is a problem getting the image from the
	 *             database.
	 * @throws IOException
	 *             Thrown if there is a problem getting the path information for
	 *             the image.
	 */
	private void setImage(int selectedIndex) throws SQLException, IOException {
		ImageFile file = model.get(selectedIndex);

		int width = file.getOriginalWidth();
		int height = file.getOriginalHeight();

		ImageIcon icon = new ImageIcon(file.getRotatedImage());

		String name = (file).getAbsolutePath();

		imageHolder.setIcon(icon);

		this.setTitle("File:" + name + " (" + width + ", " + height + ") to "
				+ file.getZipName() + " (" + icon.getIconWidth() + ","
				+ icon.getIconHeight() + ")");

		this.outOfLabel.setText(getOutOfLabel());
	}

	/**
	 * Action to display the next or previous image.
	 * 
	 * @author Mark Royer
	 * 
	 */
	protected static class NextImageAction extends AbstractAction {

		/**
		 * For serializing.
		 */
		private static final long serialVersionUID = -5439155043009912394L;

		/**
		 * Direction to display the next image. Previous is backward and next is
		 * forward.
		 * 
		 * @author Mark Royer
		 * 
		 */
		enum Direction {
			PREVIOUS, NEXT
		};

		/**
		 * The direction this action will move the active image.
		 */
		private Direction dir;

		/**
		 * The dialog tha is displaying the images.
		 */
		private ImageDialog dialog;

		/**
		 * Create an action to display the next or previous image in the list.
		 * 
		 * @param name
		 *            The name of the action. (Not null)
		 * @param direction
		 *            The direction the action will move. (Not null)
		 * @param dialog
		 *            The dialog displaying the images. (Not null)
		 */
		public NextImageAction(String name, Direction direction,
				ImageDialog dialog) {
			super(name);
			dir = direction;
			this.dialog = dialog;
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {

			// Check to make sure there are images to display.
			if (dialog.model.size() < 1) {
				dialog.setVisible(false);
				dialog.dispose();
				return;
			}
			
			/*
			 * Display the previous or next image in the list.
			 */
			switch (dir) {
			case PREVIOUS:
				if (dialog.imageIndex > 0) {
					dialog.imageIndex--;
				} else if (dialog.imageIndex == 0) {
					// Wrap around to the last image.
					dialog.imageIndex = dialog.model.size() - 1;
				}
				break;
			case NEXT:
				if (dialog.imageIndex < dialog.model.size() - 1) {
					dialog.imageIndex++;
				} else if (dialog.imageIndex == dialog.model.size() - 1) {
					// Wrap around to the first image.
					dialog.imageIndex = 0;
				}
			}
			try {
				dialog.setImage(dialog.imageIndex);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

	}

}
