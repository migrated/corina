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

package corina.cross;

import corina.Element;
import corina.Sample;
import corina.graph.GraphFrame;
import corina.gui.SaveableDocument;
import corina.gui.PrintableDocument;
import corina.gui.HasPreferences;
import corina.prefs.PrefsDialog;
import corina.prefs.Prefs;
import corina.gui.XFrame;
import corina.gui.XMenubar;
import corina.gui.FileDialog;
import corina.gui.WindowMenu;
import corina.util.Platform;
import corina.gui.UserCancelledException;
import corina.util.Overwrite;

import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;

import java.io.File;
import java.io.IOException;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

public class GridFrame extends XFrame
                    implements SaveableDocument, PrintableDocument, HasPreferences {
    // gui
    private JTable output;

    // data
    private Grid grid=null;

    // i18n
    private static ResourceBundle msg = ResourceBundle.getBundle("TextBundle");

    // saving -- (this seems like it should be higher...)
    private String filename=null;

    // saveabledocument
    public boolean isSaved() { // don't worry about deleting grids, for now
	return true; // fixme
    }
    /*
      REFACTOR: make Grid Saveable(?), and move all of this up to XFrame, which
      i should rename as DocumentFrame(?).  it's the same for samples, grids, graphs, ...
     */
    public void save() {
        // check filename
        if (filename == null) {
            try {
                filename = FileDialog.showSingle("Save");
            } catch (UserCancelledException uce) {
                return; // this should return FAILURE, too -- solution: save() throws UCE
            }

            // try up here, try down there.  can these be merged?  (but there's an if-stmt...)
            
            // check for already-exists
            if (new File(filename).exists() && Overwrite.overwrite(filename))
                return; // should return FAILURE -- how?
        }

        // save!
        try {
            grid.save(filename);
        } catch (IOException ioe) {
            // error!
            JOptionPane.showMessageDialog(null,
                                          "Error: " + ioe.getMessage(),
                                          "Error saving",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    public String getDocumentTitle() {
        return msg.getString("grid") + ": " + filename;
    }
    public void setFilename(String fn) {
        filename = fn;
    }
    public String getFilename() {
        return filename;
    }

    // table model for the grid
    static class GridTableModel extends AbstractTableModel {
        private Grid grid;
        public GridTableModel(Grid grid) {
            this.grid = grid;
        }
        public int getColumnCount() {
            return (grid == null ? 0 : grid.size()+1);
        }
        public int getRowCount() {
            return (grid == null ? 0 : grid.size()+1);
        }
        public Object getValueAt(int row, int col) {
            return grid.getCells()[row][col];
        }
    }

    private static float scale = Float.parseFloat(System.getProperty("corina.grid.scale", "1.0"));

    // cell renderer
    static class GridRenderer extends JComponent implements TableCellRenderer {

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            // set myself, return myself
            cell = (Grid.Cell) value;
            return this;
        }

        private Grid.Cell cell;
        public void paintComponent(Graphics g) {
            // set font: get original font, and scale it
            Font origFont = (System.getProperty("corina.grid.font")==null ? g.getFont() : Font.getFont("corina.grid.font"));
            Font scaledFont = new Font(origFont.getName(),
                                       origFont.getStyle(),
                                       (int) (origFont.getSize() * scale));
//            System.out.println("new font t=" + System.currentTimeMillis()); -- for debugging
            g.setFont(scaledFont);
            // FIXME: new font each time seems even MORE inefficient!
            // better: in refresh() just do component.setFont(...), and i'll pick it up automatically, right?

	    // ((Graphics2D) g).scale(scale, scale);
	    // FIXME: agh!  just call g2.scale(scale, scale) -- grid cells shouldn't have to
	    // scale themselves.  but scale() doesn't scale the text as nicely as i do, so
	    // until i figure that out, i'll keep passing in my own scale.

            // call the printing method (REFACTOR: rename method?  it's not just for printing, anymore...)
            cell.print((Graphics2D) g, 0, 0, scale);
        }
    }

    // PrintableDocument
    public int getPrintingMethod() {
        return PrintableDocument.PAGEABLE;
    }
    public Pageable makePageable(PageFormat pf) {
	return grid.makeHardcopy(pf);
    }
    public Printable makePrintable(PageFormat pf) {
        return null;
    }
    public String getPrintTitle() {
        return msg.getString("crossdating_grid");
    }

    private void initTable() {
        output = new JTable(new GridTableModel(grid));

        // cell-selection only
        output.setRowSelectionAllowed(false);

        // set cell height/width from Grid
        output.setRowHeight((int) (Grid.getCellHeight()*scale) + 2);
        for (int i=0; i<output.getColumnCount(); i++)
            output.getColumnModel().getColumn(i).setPreferredWidth((int) (Grid.getCellWidth()*scale) + 2);

        // no top-header
        output.setTableHeader(null);
        output.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // renderer -- use same as for printer
        output.setDefaultRenderer(Object.class, new GridRenderer());
        output.setShowGrid(false);

	// respond to double-clicks
	output.addMouseListener(new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
		    if (e.getClickCount() == 2) {
			// get the (row,col) of the click
			int row = output.rowAtPoint(e.getPoint());
			int col = output.columnAtPoint(e.getPoint());

			// figure out what samples are there (REFACTOR: LoD says this should be in grid)
			Element e1 = (Element) grid.getFiles().get(row-1);
			Element e2 = (Element) grid.getFiles().get(col-1);

			// make a graph
			List list = new ArrayList(2);
			list.add(e1);
			list.add(e2);
			new GraphFrame(list);
		    }
		}
	    });

        // put the table in a scroller
        JScrollPane scroller = new JScrollPane(output);
        getContentPane().add(scroller, BorderLayout.CENTER);
        // REFACTOR: extract GridPanel, GridFrame; then stuffing the panel into a CrossFrame is trivial.
        // OR: it's just a JTable, right?  would GridComponent (extends JTable) be better?
    }
    
    // used by elementspanel -- shortcut for new gridframe(new grid(list))
    public GridFrame(List s) {
        grid = new Grid(s);
        grid.run(); // change cursor to WAIT?
        init();
    }

    // used by canopener
    public GridFrame(Grid g) {
        grid = g;
        // move grid.run() to init(), and make run() a no-op if already run?
        init();
    }

    // never used -- could be used by xmenubar, but isn't
    public GridFrame() {
        try {
            // get args
            List samples = FileDialog.showMulti(msg.getString("grid"));

            grid = new Grid(samples);
            grid.run(); // change cursor to WAIT?
            init();
        } catch (UserCancelledException uce) {
            dispose();
            return;
        }
    }

    void init() {
        setTitle(msg.getString("crossdating_grid"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initTable();

        setJMenuBar(new XMenubar(this, makeMenus()));

        pack();
        setSize(new Dimension(640, 480));
        show();
    }

    private JMenu[] makeMenus() {
	// menubar
	JMenu view = new XMenubar.XMenu("View", 'V');

	JMenuItem graph = new XMenubar.XMenuItem("Graph All");
	graph.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    new GraphFrame(grid.getFiles());
		}
	    });
	view.add(graph);

	// ---
	view.addSeparator();

        // NEED NEW ABSTRACTION: ZOOM
        // -- placard component -- override jscrollpane?  jscrollbar?
        // -- menuitems: zoom in, zoom out, normal/100%, specific values (50, 75, 100, 125, 150, 200, 400?)
        // -- (consistent everywhere!)
        // -- "other..." value (dialog) for other types of user-zooms (like drag-area)
        // --

        // also, the grid should have the option of showing page-breaks (horiz+vert).
        // view -> {show,hide} page breaks.
        // would it be easier to make gridcomponent not-a-jtable, then?
        
        // zoom in
        AbstractAction zoomIn = new AbstractAction("Zoom In") {
            public void actionPerformed(ActionEvent e) {
                // increase by 0.1
                scale += 0.1;

                // set property
                System.setProperty("corina.grid.scale", Float.toString(scale));

                // tell 'em -- use Preferences.updateAll()?
                refreshFromPreferences();

                // save new pref
		Prefs.save();
            }
        };
        zoomIn.putValue(Action.MNEMONIC_KEY, new Integer('I'));
        zoomIn.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(XMenubar.macize("control UP")));
        view.add(new XMenubar.XMenuItem(zoomIn));

        // zoom out
        AbstractAction zoomOut = new AbstractAction("Zoom Out") {
            public void actionPerformed(ActionEvent e) {
                // decrease by 0.1
                scale -= 0.1;

                // set property
                System.setProperty("corina.grid.scale", Float.toString(scale));

                // tell 'em -- use Preferences.updateAll()?
                refreshFromPreferences();

                // save new pref
		Prefs.save();
            }
        };
        zoomOut.putValue(Action.MNEMONIC_KEY, new Integer('O'));
        zoomOut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(XMenubar.macize("control DOWN")));
        view.add(new XMenubar.XMenuItem(zoomOut));

        // REFACTOR: WindowMenu should be automatic; move up to XFrame or XMenubar or some such
        if (Platform.isMac)
            return new JMenu[] { view, new WindowMenu(this) };
        else
            return new JMenu[] { view };
    }

    // HasPreferences
    public void refreshFromPreferences() {
        // re-read scale
        scale = Float.parseFloat(System.getProperty("corina.grid.scale", "1.0"));

        // reset sizes
        output.setRowHeight((int)(Grid.getCellHeight()*scale) + 2);
        for (int i=0; i<output.getColumnCount(); i++)
            output.getColumnModel().getColumn(i).setPreferredWidth((int)(Grid.getCellWidth()*scale) + 2);

        // redraw?  sure.
        repaint();
    }
}
