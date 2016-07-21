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
import javax.swing.JList;

/**
 * Action to remove an image from the list.
 * 
 * @author Mark Royer
 * 
 */
public class RemoveAction extends AbstractAction {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = -4698365655350402624L;

	/**
	 * The {@link JList} that this action will operate on.
	 */
	private ImageJList list;

	/**
	 * Create a new {@link RemoveAction} that will operate on the given list.
	 * 
	 * @param list
	 *            The {@link JList} that this action will operate on. (Not null)
	 */
	public RemoveAction(ImageJList list) {
		super("Remove");
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

		ImageJListModel model = list.getModel();

		// If no image is selected remove the most recent one that was clicked
		// on. For instance, a right click will not select an image, but will
		// still allow a user to remove the image.
		if (list.getSelectedIndex() == -1) {
			model.remove(list.getMouseOverIndex());
		} else {
			// Remove multiple images from the list.
			for (Object o : list.getSelectedValues()) {
				model.removeElement(o);
			}
		}

	}

}
