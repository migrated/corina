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

package corina.manip;

import corina.Sample;
import corina.SampleAdapter;
import corina.SampleEvent;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Component;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.AbstractListModel;
import javax.swing.ListCellRenderer;
import javax.swing.BorderFactory;
import javax.swing.AbstractAction;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;

public class ReconcileDialog extends JDialog {

    private Reconcile r;

    ReconcileListModel model; // AbstractListModel

    private Sample s1, s2;

    public ReconcileDialog(Sample s1, Sample s2) {
	setTitle("Reconciliation Messages");

	r = new Reconcile(s1, s2);
	// for n=191, on a PPC G4/500MHz, takes ~12ms for the first reconciliation, 2-3ms for subsequent runs

	this.s1 = s1;
	this.s2 = s2;

	makeButtons();

	model = new ReconcileListModel();
	JList list = new JList(model);

	renderer = new RuleRenderer();
	list.setCellRenderer(renderer);

	// content pane is a borderlayout, by default
	getContentPane().add(buttons, BorderLayout.NORTH);
	getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);

	final SampleAdapter watcher = new SampleAdapter() {
		public void sampleRedated(SampleEvent e) {
		    recompute();
		}
		public void sampleDataChanged(SampleEvent e) {
		    recompute();
		}
	    };

	s1.addSampleListener(watcher);
	s2.addSampleListener(watcher);

	// on close, dispose, and remove sample listeners
	setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	final Sample glue1=s1, glue2=s2;
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    glue1.removeSampleListener(watcher);
		    glue2.removeSampleListener(watcher);
		}
	    });

	pack(); // set preferred size?
	show();
    }

    private void recompute() {
	// recompute reconciliation
	r = new Reconcile(s1, s2);

	// update list
	model.uhoh();

	// update buttons
	setButtonLabels();
    }

    JCheckBox length, trend, percent;
    JPanel buttons;

    private void makeButtons() {
	length = new JCheckBox("", true);
	trend = new JCheckBox("", true);
	percent = new JCheckBox("", true);

	setButtonLabels();

	// length.setIcon(r.lengthIcon);
	// trend.setIcon(r.trendIcon);
	// percent.setIcon(r.percentIcon);

	buttons = new JPanel(new GridLayout(1, 0));
	buttons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

	buttons.add(length);
	buttons.add(trend);
	buttons.add(percent);

	length.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    lengthVisible = !lengthVisible;
		    model.uhoh();
		}
	    });
	trend.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    trendsVisible = !trendsVisible;
		    model.uhoh();
		}
	    });
	percent.addActionListener(new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    percentsVisible = !percentsVisible;
		    model.uhoh();
		}
	    });
    }

    private String nErrors(int n) {
	return (n == 1 ? "1 error" : n+" errors");
    }
    private void setButtonLabels() {
	length.setText("Extra years (" + nErrors(r.length.size()) + ")");
	trend.setText("Trend (" + nErrors(r.trends.size()) + ")");
	percent.setText("3% rule (" + nErrors(r.percents.size()) + ")");
    }

    private boolean lengthVisible=true, trendsVisible=true, percentsVisible=true;

    // show length, then trend, then 3% errors (no, that's not the order they're done on paper!)
    public class ReconcileListModel extends AbstractListModel {
	public Object getElementAt(int index) {
	    if (lengthVisible && index < r.length.size())
		return ((Reconcile.Rule) r.length.get(index));

	    if (lengthVisible)
		index -= r.length.size();

	    if (trendsVisible && index < r.trends.size())
		return ((Reconcile.Rule) r.trends.get(index));

	    if (trendsVisible)
		index -= r.trends.size();

	    return ((Reconcile.Rule) r.percents.get(index));
	}
	public int getSize() {
	    int n = 0;
	    if (lengthVisible)
		n += r.length.size();
	    if (trendsVisible)
		n += r.trends.size();
	    if (percentsVisible)
		n += r.percents.size();
	    return n;
	}
	public void uhoh() {
	    fireContentsChanged(this, 0, model.getSize()-1);
	}
    }

    RuleRenderer renderer;

    class RuleRenderer extends JLabel implements ListCellRenderer {
	RuleRenderer() {
	    setOpaque(true);
	}

	public Component getListCellRendererComponent(JList list, Object value,
						      int index, boolean isSelected, boolean cellHasFocus) {
	    Reconcile.Rule r = (Reconcile.Rule) value;

	    setText(r.toString());
	    setIcon(r.getIcon());

	    setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
	    setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

	    return this;
	}
    }

    /*
      LEFT TO DO:
      -- show icons AND checkboxes (or use togglebuttons?)
      -- make all icons the same height
      -- make text more descriptive
      -- say which samples are being used
      -- s/Extra years/missing years/
      -- one length error per off-year, not one total -- fix reconcile.java
      -- checkboxes should only be clickable to the end of their text, not full 1/3 of window width
      -- (if you then load the C reading, have it use the same sample object (much more work))
      -- if no errors, have one element "No errors", in dimmed list?
    */
}
