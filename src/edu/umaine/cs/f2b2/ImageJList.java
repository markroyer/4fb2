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
import static edu.umaine.cs.f2b2.RotateAction.RotateType.RIGHT;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.io.File;

import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;

/**
 * Represents a {@link JList} that contains images.
 * 
 * @author Mark Royer
 * 
 */
public class ImageJList extends JList implements MouseListener {

	/**
	 * For serializing.
	 */
	private static final long serialVersionUID = 6802568356418939446L;

	/**
	 * Pop-up menu that allows user to rotate, remove, and rename images.
	 */
	private JPopupMenu popup;

	/**
	 * The index of the image the last mouse click occurred over.
	 */
	private int mouseOverIndex;

	/**
	 * Creates a new {@link ImageJList} with a pop-up menu.
	 */
	public ImageJList() {
		super(new ImageJListModel());

		// Attach a mouse motion adapter to let us know the mouse is over an
		// item and to show the tip.
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				ImageJList theList = (ImageJList) e.getSource();
				ListModel model = theList.getModel();
				int index = theList.locationToIndex(e.getPoint());
				if (index > -1) {
					theList.setToolTipText(null);
					String text = ((File) model.getElementAt(index))
							.getAbsolutePath();
					theList.setToolTipText(text);
				}
			}
		});

		this.setCellRenderer(new ImageRenderer());

		popup = new JPopupMenu();
		JMenuItem menuItem = new JMenuItem(new RenameImageAction(this));
		menuItem.setToolTipText("Only renames file in archive");
		popup.add(menuItem);
		menuItem = new JMenuItem(new RemoveAction(this));
		menuItem.setToolTipText("Only removes file from archive");
		popup.add(menuItem);
		menuItem = new JMenuItem(new RotateAction(this, LEFT));
		popup.add(menuItem);
		menuItem = new JMenuItem(new RotateAction(this, RIGHT));
		popup.add(menuItem);
		menuItem = new JMenuItem(new SortListAction(this));
		popup.add(menuItem);

		this.addMouseListener(this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JList#getToolTipText(java.awt.event.MouseEvent)
	 */
	public String getToolTipText(MouseEvent e) {
		return super.getToolTipText();
	}

	/**
	 * Show the pop-up menu if the pop-up is triggered.
	 * 
	 * @param e
	 *            The mouse event that occurred. (Not null)
	 */
	private void maybeShowPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked(MouseEvent e) {

		if (e.getClickCount() == 2) {

			try {

				ImageDialog j = new ImageDialog(this.getModel(),
						this.getSelectedIndex());

				j.setLocationRelativeTo(null);
				j.setVisible(true);

			} catch (Exception e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(this,
						"Unable to open image file.", "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseEntered(MouseEvent e) {
		// Nothing to do
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// Nothing to do

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		// Update the last location clicked on by the mouse
		this.mouseOverIndex = this.locationToIndex(e.getPoint());
		maybeShowPopup(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		maybeShowPopup(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JList#getModel()
	 */
	@Override
	public ImageJListModel getModel() {
		return (ImageJListModel) super.getModel();
	}

	/**
	 * Returns the pop-up menu associated with this list.
	 * 
	 * @return The pop-up menu. (Never null)
	 */
	public JPopupMenu getPopup() {
		return popup;
	}

	/**
	 * The last place that the mouse clicked in the list.
	 * 
	 * @return The index of the last mouse click in the list.
	 */
	public int getMouseOverIndex() {
		return mouseOverIndex;
	}

}
