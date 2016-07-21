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

import static edu.umaine.cs.f2b2.RotateAction.RotateType.LEFT;

import java.awt.event.ActionEvent;
import java.sql.SQLException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 * Rotates images on a specific list of images.
 * 
 * @author Mark Royer
 * 
 */
public class RotateAction extends AbstractAction {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = 3737608170860232556L;

	/**
	 * Types of rotations.
	 * 
	 * @author Mark Royer
	 * 
	 */
	public enum RotateType {
		LEFT, RIGHT
	};

	/**
	 * The list of images this action performs on.
	 */
	public ImageJList list;

	/**
	 * The type of rotation this action performs. Either left or right.
	 */
	public RotateType rotates;

	/**
	 * Create a new {@link RotateAction} that operates on the given list of
	 * images.
	 * 
	 * @param list
	 *            The list that this action will perform on. (Not null)
	 * @param type
	 *            The type of rotation this action will perform. (Not null)
	 */
	public RotateAction(ImageJList list, RotateType type) {
		super(type == LEFT ? "Rotate Left" : "Rotate Right");
		this.rotates = type;
		this.list = list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {

		final ImageJListModel model = list.getModel();

		int[] selectedIndexes;

		if (list.getSelectedIndex() == -1) {
			selectedIndexes = new int[] { list.getMouseOverIndex() };
		} else {
			selectedIndexes = list.getSelectedIndices();
		}

		/*
		 * For each of the selected images, perform the appropriate rotation.
		 */
		for (int i : selectedIndexes) {

			final ImageFile file = model.get(i);

			try {

				switch (rotates) {
				case LEFT:

					new Thread(new Runnable() {

						public void run() {
							try {
								file.rotateLeft();
							} catch (SQLException e) {
								e.printStackTrace();
							}
							model.fireContentChanged(file);
						}
					}).start();

					break;
				case RIGHT:

					new Thread(new Runnable() {

						public void run() {
							try {
								file.rotateRight();
							} catch (SQLException e) {
								e.printStackTrace();
							}
							model.fireContentChanged(file);
						}
					}).start();

					break;
				}

			} catch (Exception e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(null,
						"Unable to open image file.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}

	}

}
