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

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 * Action to change the name of the image as saved in the zip file.
 * 
 * @author Mark Royer
 * 
 */
public class RenameImageAction extends AbstractAction {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = 588566981188695110L;

	/**
	 * The list that the rename will be operating on.
	 */
	private ImageJList list;

	/**
	 * Create a new {@link RenameImageAction} that operates on the given list.
	 * 
	 * @param list
	 *            A list of images that the rename action will operate on. (Not
	 *            null)
	 */
	public RenameImageAction(ImageJList list) {
		super("Rename Image");
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
		ImageFile file = (ImageFile) list.getModel().elementAt(
				list.getMouseOverIndex());

		String result = JOptionPane.showInputDialog(list,
				"Type the new name that will appear in the zip file.",
				file.getZipName());

		// If the entered name is canceled or empty ignore the rename.
		if (result != null && result != "") {

			ImageFile newFile = new ImageFile(result);

			while (list.getModel().hasSameName(newFile.getZipName())) {
				String ans = JOptionPane.showInputDialog(
						"Already have a file named + '" + result
								+ "'.\nPlease select another name.", result);

				if (ans == null || ans == "") {
					return;
				}

				result = ans;
				newFile = new ImageFile(result);
			}

			while (list.getModel().hasSameName(newFile.getZipName())) {
				String ans = JOptionPane.showInputDialog(
						"Already have a file named + '" + result
								+ "'.\nPlease select another name.", result);

				if (ans == null || ans == "") {
					return;
				}

				result = ans;
			}

			if (AddImagesAction.imageFilter.accept(newFile)) {
				file.setZipName(result);
			} else {

				JOptionPane.showMessageDialog(
						list,
						"File type \""
								+ ImageFile.getExtension(newFile.getZipName())
								+ "\" not supported.", "Bad extension",
						JOptionPane.ERROR_MESSAGE);

			}
		}

		list.repaint();
	}

}
