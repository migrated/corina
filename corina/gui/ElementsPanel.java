//
// This file is part of Corina.
// 
// Corina is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// Corina is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with Corina; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// Copyright 2001 Ken Harris <kbh7@cornell.edu>
//

package corina.gui;

import corina.Sample;
import corina.SampleListener;
import corina.SampleEvent;
import corina.Element;
import corina.Metadata;
import corina.Metadata.Field;
import corina.graph.GraphFrame;
import corina.graph.BargraphFrame;
import corina.cross.CrossFrame;
import corina.cross.Sequence;
import corina.cross.GridFrame;
import corina.editor.Editor;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CannotRedoException;

/**
   <p>A JPanel for displaying (and editing) the Elements of a
   List.</p>

   <p>Left to do: finish implementing the popup menu; make the popup
   menu separate, so it can be used as a menubar menu, too; make
   changes to the List dirty the document.</p>

   @author <a href="mailto:kbh7@cornell.edu">Ken Harris</a>
   @version $Id$ */

public class ElementsPanel extends JPanel implements SampleListener {

    // data
    private List elements;
    private Sample sample=null;

    // gui
    private JPopupMenu popup;
    private JTable table;

    // --- SampleListener ----------------------------------------
    public void sampleRedated(SampleEvent e) { }
    public void sampleDataChanged(SampleEvent e) { }
    public void sampleMetadataChanged(SampleEvent e) { }
    public void sampleFormatChanged(SampleEvent e) { }
    public void sampleElementsChanged(SampleEvent e) {
	((AbstractTableModel) table.getModel()).fireTableDataChanged();
    }
    // --- SampleListener ----------------------------------------

    // randomness...
    public void update() {
	((AbstractTableModel) table.getModel()).fireTableDataChanged();
    }

    // --- PopupListener ----------------------------------------
    private class PopupListener extends MouseAdapter {
	public void mousePressed(MouseEvent e) {
	    maybeShowPopup(e);
	}
	public void mouseReleased(MouseEvent e) {
	    maybeShowPopup(e);
	}
	private void maybeShowPopup(MouseEvent e) {
	    if (e.isPopupTrigger() || e.isControlDown())
		popup.show(e.getComponent(), e.getX(), e.getY());
	}
    }
    // --- PopupListener ----------------------------------------

    // --- DropAdder ----------------------------------------
    private class DropLoader implements DropTargetListener {
	public void dragEnter(DropTargetDragEvent event) {
	    event.acceptDrag(DnDConstants.ACTION_MOVE);
	}
	public void dragOver(DropTargetDragEvent event) { } // do nothing
	public void dragExit(DropTargetEvent event) { } // do nothing
	public void dropActionChanged(DropTargetDragEvent event) { } // do nothing
	public void drop(DropTargetDropEvent event) {
	    try {
		Transferable transferable = event.getTransferable();

		// we accept only filelists
		if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
		    event.acceptDrop(DnDConstants.ACTION_MOVE);
		    Object o = transferable.getTransferData(DataFlavor.javaFileListFlavor);
		    List l = (List) o; // a List of Files

		    for (int i=0; i<l.size(); i++) {
			elements.add(new Element(((File) l.get(i)).getPath()));
			// fire update?
		    }
		    event.getDropTargetContext().dropComplete(true);
		} else {
		    event.rejectDrop();
		}
	    } catch (IOException ioe) {
		// handle error?
		event.rejectDrop();
	    } catch (UnsupportedFlavorException ufe) {
		// handle error?
		event.rejectDrop();
	    }
	}
    }
    // --- DropAdder ----------------------------------------

    // --- ContextPopup ----------------------------------------
    private class ContextPopup extends JPopupMenu {
	public ContextPopup() {
	    // Open
	    JMenuItem open = new JMenuItem("Open");
	    open.addActionListener(new AbstractAction() {
		    public void actionPerformed(ActionEvent ae) {
			// get selected element
			int i = table.getSelectedRow();
			Element e = (Element) elements.get(i);

			// load it
			Sample s=null;
			try {
			    s = new Sample(e.getFilename());
			} catch (IOException ioe) {
			    JOptionPane.showMessageDialog(null,
							  "Can't open this file: " + ioe.getMessage(),
							  "Error Loading Sample",
							  JOptionPane.ERROR_MESSAGE);
			    return;
			}

			// open it
			new Editor(s);
		    }
		});
	    super.add(open);

	    // ---
	    addSeparator();

	    // Grid from all
	    JMenuItem allGrid = new JMenuItem("Grid from all");
	    allGrid.addActionListener(new AbstractAction() {
		    public void actionPerformed(ActionEvent ae) {
			new GridFrame(elements);
		    }
		});
	    super.add(allGrid);

	    // Crossdate all
	    JMenuItem allCross = new JMenuItem("Crossdate all");
	    allCross.addActionListener(new AbstractAction() {
		    public void actionPerformed(ActionEvent ae) {
			new CrossFrame(new Sequence(elements, elements));
		    }
		});
	    super.add(allCross);

	    // ---
	    addSeparator();

	    // Change directory
	    JMenuItem changeDir = new JMenuItem("Change directory...");
	    changeDir.addActionListener(new AbstractAction() {
		    public void actionPerformed(ActionEvent ae) {
			// figure out what the base directory is for the samples
			String prefix = ((Element) elements.get(0)).getFilename();
			for (int i=1; i<elements.size(); i++) {

			    // crop prefix by directories until it really is a prefix
			    while (!((Element) elements.get(i)).getFilename().startsWith(prefix)) {
				int slash = prefix.lastIndexOf(File.separatorChar);
				if (slash == -1) {
				    prefix = "";
				    break;
				}
				prefix = prefix.substring(0, slash+1);
			    }
			}

			// ask user what the new directory will be
			String target = (String) JOptionPane.showInputDialog(null,
									     "Enter a new directory:",
									     "Choose new directory",
									     JOptionPane.QUESTION_MESSAGE,
									     null, /* ??? */
									     null,
									     prefix);

			// user aborted
			if (target == null || target.equals(prefix))
			    return;

			// make sure target now holds a prefix which ends with File.separator
			if (!target.endsWith(File.separator))
			    target += File.separatorChar;

			// change all filenames to the new directory (s/prefix/target/).
			// elements are now immutable, so make a new one from the old name.
			for (int i=0; i<elements.size(); i++) {
			    Element oldEl = (Element) elements.get(i);
			    String newFilename = target + oldEl.getFilename().substring(prefix.length());
			    Element newEl = new Element(newFilename, oldEl.isActive());
			    elements.set(i, newEl);
			}

			// fire event so table gets changed
			((AbstractTableModel) table.getModel()).fireTableDataChanged();
		    }
		});
	    super.add(changeDir);
	}
    }
    // --- ContextPopup ----------------------------------------

    // given: list of elements
    public ElementsPanel(List el) {
	this(null, el);
    }

    // given: editor
    public ElementsPanel(Editor e) {
	this(e.getSample(), e.getSample().elements);
    }

    // given: Sample, and list of Elements (well, this never happens,
    // but it's the common subset.)
    private ElementsPanel(Sample s, List el) {
        // boilerplate
        setLayout(new BorderLayout());

	// data
	if (s == null) {
	    elements = el;
	} else {
	    this.sample = s;
	    if (sample.elements == null)
		sample.elements = new ArrayList();
	    elements = sample.elements;
	}

        // table
        table = new JTable();
	setView(VIEW_FILENAMES); // initial view
	addClickToSort();
        if (elements != null && elements.size() > 0)
            table.setRowSelectionInterval(0, 0);
        JScrollPane scroller = new JScrollPane(table,
                                               ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                               ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(scroller, BorderLayout.CENTER);

        // popup, mouselistener for it
	popup = new ContextPopup();
        MouseListener popupListener = new PopupListener();
        addMouseListener(popupListener); // for clicking on empty space below the entries
        table.addMouseListener(popupListener); // for clicking on table entries

        // drag-n-drop
        DropLoader dropLoader = new DropLoader();
        DropTarget dt1 = new DropTarget(this, dropLoader);
        DropTarget dt2 = new DropTarget(table, dropLoader);
    }

    public void removeSelectedRows() {
	// save backup -- this is only a shallow copy (but that's good)
	final List save = (List) ((ArrayList) elements).clone();

	final int rows[] = table.getSelectedRows();
	int deleted = 0; // number of rows already deleted
	for (int i=0; i<rows.length; i++) { // remove those rows
	    elements.remove(rows[i] - deleted);
	    deleted++;
	}

	// update
	if (sample != null) {
	    sample.setModified();
	    sample.fireSampleElementsChanged();
	    sample.fireSampleMetadataChanged(); // so title gets updated (modified-flag)

	    // add undoable edit -- not the most efficient way, but not bad, either.
	    sample.postEdit(new AbstractUndoableEdit() {
		    List before=save, after;
		    public void undo() throws CannotUndoException {
			after = (List) ((ArrayList) elements).clone();
			elements.clear();
			elements.addAll(before);
			update();
		    }
		    public void redo() throws CannotRedoException {
			elements.clear();
			elements.addAll(after);
			update();
		    }
		    public boolean canRedo() {
			return true;
		    }
		    public String getPresentationName() {
			return "Remove"; // (rows.length==1 ?  "Remove Element" : "Remove Elements");
		    }
		});
	}

	// System.out.println("updating...");
	// ((AbstractTableModel) table.getModel()).fireTableStructureChanged();
	AbstractTableModel tm = (AbstractTableModel) table.getModel();
	tm.fireTableChanged(new TableModelEvent(tm)); // , TableModelEvent.HEADER_ROW));
	update();

	// what the fuck is going on here?  why why oh why?
    }

    private List fields;
    public final static int VIEW_FILENAMES = 0;
    public final static int VIEW_STANDARD = 1;
    public final static int VIEW_ALL = 2;
    public void setView(int view) {
        switch (view) {
            case VIEW_FILENAMES:
                fields = new ArrayList();
                table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                break;
            case VIEW_STANDARD:
                fields = new ArrayList(Metadata.preview.length);
                for (int i=0; i<Metadata.preview.length; i++)
                    fields.add(Metadata.preview[i]);
                table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                break;
            case VIEW_ALL:
                fields = new ArrayList(Metadata.fields.length);
                for (int i=0; i<Metadata.fields.length; i++)
                    fields.add(Metadata.fields[i]);
                table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                break;
            default:
                throw new IllegalArgumentException();
        }

        table.setModel(new ElementsTableModel(elements, fields));

	// renderer
	table.getColumnModel().getColumn(0).setCellRenderer(new ElementsTableModel.FilenameRenderer(table));
	/*
	table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
		public Component getTableCellRendererComponent(JTable table, Object value,
							       boolean isSelected, boolean hasFocus,
							       int row, int column) {
		    Component c = super.getTableCellRendererComponent(table, value,
								      isSelected, hasFocus,
								      row, column);
		    Color back = (isSelected ? table.getSelectionBackground() : table.getBackground());

		    if (row % 2 == 0)
			back = new Color(back.getRed() - 16,
					 back.getGreen() - 16,
					 back.getBlue());
		    c.setBackground(back);
		    return c;
		}
	    });
	*/

	// editor
	final JPanel panel = new JPanel(new BorderLayout());
	final JCheckBox chx = new JCheckBox();
	final JLabel lab = new JLabel();
	panel.add(chx, BorderLayout.WEST);
	panel.add(lab, BorderLayout.CENTER);
	lab.setFont(table.getFont());
	chx.setForeground(table.getForeground());
	lab.setForeground(table.getForeground());
	chx.setBackground(table.getBackground());
	lab.setBackground(table.getBackground());
	lab.setOpaque(true);
	chx.setOpaque(true);
	table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(chx) {
		public Component getTableCellEditorComponent(JTable table, Object value,
							     boolean isSelected,
							     int row, int column) {
		    Element e = (Element) value;
		    chx.setSelected(e.active);
		    lab.setText(new File(e.getFilename()).getName()); // filename only (not fq)

		    Color fore = (isSelected ? table.getSelectionForeground() : table.getForeground());
		    Color back = (isSelected ? table.getSelectionBackground() : table.getBackground());

		    /*
		    // light-blue-ish
		    if (row % 2 == 0)
			back = new Color(back.getRed() - 16,
					 back.getGreen() - 16,
					 back.getBlue());
		    */

		    chx.setForeground(fore);
		    lab.setForeground(fore);
		    chx.setBackground(back);
		    lab.setBackground(back);

		    return panel;
		}
	    });

	// set columns: for each column use popup if suggested values present
	for (int i=0; i<fields.size(); i++)
	    if (((Field) fields.get(i)).values != null) {
		JComboBox popup = new JComboBox(((Field) fields.get(i)).values);
		popup.setEditable(true);
		table.getColumnModel().getColumn(i+2).setCellEditor(new DefaultCellEditor(popup));
	    }
    }

    // this method is unbelievably ugly, thanks to java's lack of
    // closures.  *sigh*
    private int lastSortCol = -1;
    private void addClickToSort() {
	table.getTableHeader().addMouseListener(new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
		    if (e.getClickCount() != 1)
			return;
		    int col = table.getColumnModel().getColumnIndexAtX(e.getX());

		    switch (col) {
		    case 0: // filename
			if (lastSortCol==col && ((Element) elements.get(0)).filename.compareTo(((Element) elements.get(elements.size()-1)).filename)<0)
			    Collections.sort(elements, new Comparator() { // reverse
				    public int compare(Object o1, Object o2) {
					return -((Element) o1).filename.compareTo(((Element) o2).filename);
				    }
				});
			else
			    Collections.sort(elements, new Comparator() { // normal
				    public int compare(Object o1, Object o2) {
					return ((Element) o1).filename.compareTo(((Element) o2).filename);
				    }
				});
			break;

		    case 1: // range
			if (lastSortCol==col && ((Element) elements.get(0)).range.compareTo(((Element) elements.get(elements.size()-1)).range)<0)
			    Collections.sort(elements, new Rangesorter(true));
			else
			    Collections.sort(elements, new Rangesorter(false));
			break;

		    default: // details
                        final String key = ((Field) fields.get(col-2)).variable;
			Object v0 = ((Element) elements.get(0)).details.get(key);
			Object vn = ((Element) elements.get(elements.size()-1)).details.get(key);
			if (lastSortCol==col && ((Comparable) v0).compareTo((Comparable) vn)<0)
			    Collections.sort(elements, new Metasorter(key, true));
			else
			    Collections.sort(elements, new Metasorter(key, false));
			/*
			Object v0 = ((Element) elements.get(0)).details.get(key);
			Object vn = ((Element) elements.get(elements.size()-1)).details.get(key);
			if (lastSortCol==col && ((Comparable) v0).compareTo((Comparable) vn)<0)
			    Collections.sort(elements, new Comparator() { // reverse
				    public int compare(Object o1, Object o2) {
					Object v1 = ((Element) o1).details.get(key);
					Object v2 = ((Element) o2).details.get(key);
					return -((Comparable) v1).compareTo((Comparable) v2);
				    }
				});
			else
			    Collections.sort(elements, new Comparator() { // normal
				    public int compare(Object o1, Object o2) {
					Object v1 = ((Element) o1).details.get(key);
					Object v2 = ((Element) o2).details.get(key);
					return ((Comparable) v1).compareTo((Comparable) v2);
				    }
				});
			*/
		    }

		    lastSortCol = col;
		}
	    });
    }

    // the sorts!  (if you see gosling, kick him for me for not giving java closures)
    private static class Rangesorter implements Comparator { // by range
	private boolean rev;
	public Rangesorter(boolean reverse) {
	    rev = reverse;
	}
	public int compare(Object o1, Object o2) {
	    int x = ((Element) o1).range.compareTo(((Element) o2).range);
	    // what about nulls?
	    return (rev ? -x : x);
	}
    }
    private static class Metasorter implements Comparator { // by meta field
	private boolean rev;
	private String field;
	public Metasorter(String field, boolean reverse) {
	    rev = reverse;
	    this.field = field;
	}
	public int compare(Object o1, Object o2) {
	    Object v1 = ((Element) o1).details.get(field); // what about null HERE?
	    Object v2 = ((Element) o2).details.get(field);
	    if (v1==null && v2!=null) // deal with nulls ... ick
		return +1;
	    else if (v1!=null && v2==null)
		return -1;
	    else if (v1==null && v2==null)
		return 0;
	    int x = ((Comparable) v1).compareTo((Comparable) v2);
	    // what about nulls?
	    return (rev ? -x : x);
	}
    }
}
