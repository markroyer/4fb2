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

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Determines how an image is rendered inside of a {@link JList}.
 * 
 * @author Mark Royer
 * 
 */
public class ImageRenderer extends JLabel implements ListCellRenderer {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = -3910672117374162584L;

	/**
	 * Creates a new {@link ImageRenderer} that is left aligned and vertically
	 * centered.
	 */
	public ImageRenderer() {
		setOpaque(true);
		setHorizontalAlignment(LEFT);
		setVerticalAlignment(CENTER);
	}

	/**
	 * This method finds the image and text corresponding to the selected value
	 * and returns the label that is set up to display the text and image.
	 */
	/* (non-Javadoc)
	 * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
	 */
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {

		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		} else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}

		ImageFile file = ((ImageFile) value);
		String fileName = file.getZipName();
		Icon icon = file.getSmallImage();
		setIcon(icon);
		setText(fileName);
		setFont(list.getFont());

		return this;
	}

}
