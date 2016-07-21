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

/**
 * Sorts the images by the zip name in a image list.
 * 
 * @author Mark Royer
 * 
 */
public class SortListAction extends AbstractAction {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = -4698365655350402624L;

	/**
	 * The image list that will be sorted.
	 */
	private ImageJList list;

	/**
	 * Create a new {@link SortListAction} that will sort the given image list.
	 * 
	 * @param list
	 *            The list that will be operated on. (Not null)
	 */
	public SortListAction(ImageJList list) {
		super("Sort Images");
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

		model.sort();

	}

}
