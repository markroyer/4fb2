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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListModel;

/**
 * A model for keeping track of images in a {@link ImageJListModel}.
 * 
 * @author Mark Royer
 * 
 */
public class ImageJListModel extends DefaultListModel {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = 4198718202539784217L;

	/**
	 * Create a new {@link ImageJListModel}.
	 */
	public ImageJListModel() {
		super();
	}

	/**
	 * Returns true iff there exists a file in this list that has the same zip
	 * file name as the given name.
	 * 
	 * @param zipName
	 *            The name to be checked. (Not null)
	 * @return true iff the exists a file in this list that has the same zip
	 *         file name as the given name.
	 */
	public boolean hasSameName(String zipName) {

		for (int i = 0; i < this.getSize(); i++) {
			if (zipName.equals(((ImageFile) this.get(i)).getZipName()))
				return true;
		}

		return false;
	}

	/**
	 * Notifies listeners that the given {@link ImageFile} has changed.
	 * 
	 * @param file
	 *            The image that was modified. (Not null)
	 */
	public synchronized void fireContentChanged(ImageFile file) {
		int index = this.indexOf(file);
		fireContentsChanged(this, index, index);
	}

	/* (non-Javadoc)
	 * @see javax.swing.AbstractListModel#fireContentsChanged(java.lang.Object, int, int)
	 */
	protected synchronized void fireContentsChanged(Object source, int index0,
			int index1) {
		super.fireContentsChanged(source, index0, index1);
	}

	/* (non-Javadoc)
	 * @see javax.swing.DefaultListModel#get(int)
	 */
	@Override
	public ImageFile get(int index) {
		return (ImageFile) super.get(index);
	}

	/**
	 * Sorts the images that are in the list by zip name.
	 */
	public void sort() {
		// This could be handled using the database...

		List<ImageFile> fileList = new ArrayList<ImageFile>();

		for (int i = 0; i < size(); i++) {
			fileList.add(get(i));
		}

		Collections.sort(fileList, new Comparator<ImageFile>() {
			@Override
			public int compare(ImageFile f1, ImageFile f2) {
				assert f1 != null && f2 != null;
				return f1.getZipName().compareTo(f2.getZipName());
			}

		});
		this.removeAllElements();
		for (Object o : fileList) {
			this.addElement(o);
		}
	}

}
