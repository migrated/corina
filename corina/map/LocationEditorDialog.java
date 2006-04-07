package corina.map;

import java.awt.Component;
import java.awt.Frame;
import javax.swing.*;

import java.awt.*;
import java.util.StringTokenizer;
import java.awt.event.ActionEvent;

import corina.gui.Layout;
import corina.site.*;
import corina.ui.Builder;
import corina.util.Center;
import corina.util.OKCancel;

public class LocationEditorDialog extends JDialog {	
	private JTextField latdeg;
	private JTextField latmin;
	private JComboBox lathemi;
	private JTextField longdeg;
	private JTextField longmin;
	private JComboBox longhemi;	
	
	private final static String[] N_S = {"N", "S"};
	private final static String[] E_W = {"E", "W"};

	public LocationEditorDialog(Site site, Dialog window) {
		super(window, site.getName(), true);
		
		String location;
		if(site.getLocation() == null)
			location = new String("0,0,N,0,0,E");
		else
			location = site.getLocation().getEasyString();
		
		StringTokenizer tok = new StringTokenizer(location, ",");
		
		latdeg = new JTextField(tok.nextToken());
		latmin = new JTextField(tok.nextToken());
		lathemi = new JComboBox(N_S);
		int latidx = tok.nextToken().charAt(0) == 'S' ? 1 : 0;		
		lathemi.setSelectedIndex(latidx);
		
		longdeg = new JTextField(tok.nextToken());
		longmin = new JTextField(tok.nextToken());
		longhemi = new JComboBox(E_W);
		int longidx = tok.nextToken().charAt(0) == 'S' ? 1 : 0;
		longhemi.setSelectedIndex(longidx);
		
		JPanel line_1 = Layout.flowLayoutL(
				labelOnTopNotKey(" ", new JLabel("Latitude")), 
				strutH(12), 
				labelOnTopNotKey("Degrees", latdeg), 
				strutH(12), 
				labelOnTopNotKey("Minutes", latmin),
				strutH(12), 
				labelOnTopNotKey("Hemisphere", lathemi)
				);
		JPanel line_2 = Layout.flowLayoutL(
				labelOnTopNotKey(" ", new JLabel("Longitude")), 
				strutH(12), 
				labelOnTopNotKey("Degrees", longdeg), 
				strutH(12), 
				labelOnTopNotKey("Minutes", longmin),
				strutH(12), 
				labelOnTopNotKey("Hemisphere", longhemi)
				);
		
		JButton cancel = Builder.makeButton("cancel");
		final JButton ok = Builder.makeButton("ok");
		final JDialog dlg = this;
		final Site _site = site;
		AbstractAction buttonAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// if 'ok' clicked, writeback
				boolean kill = true;
				if (e.getSource() == ok) {
					String value = 
						latdeg.getText() + Location.DEGREE_SIGN + latmin.getText() + "'" + lathemi.getSelectedItem().toString() +
						" " +
						longdeg.getText() + Location.DEGREE_SIGN + longmin.getText() + "'" + longhemi.getSelectedItem().toString();
					
					try {
						Location loc = new Location(value);
						_site.setLocation(loc);
					}
					catch (NumberFormatException nfe) {
						JOptionPane.showMessageDialog(dlg, "Invalid location!");
						kill = false;
					}
				}

				// close this dialog if we're told to kill
				if (kill)
					dispose();
			}
		};
		cancel.addActionListener(buttonAction);
		ok.addActionListener(buttonAction);

		JPanel buttons = Layout.buttonLayout(null, null, cancel, ok);
		buttons.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		JPanel content = Layout.boxLayoutY(line_1, line_2);
		// everything together
		JPanel everything = Layout.borderLayout(null, null, content, null,
				buttons);
		everything.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setContentPane(everything);

		setResizable(false);
		OKCancel.addKeyboardDefaults(ok);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		pack();
		if (window != null)
			Center.center(this, window);
		else
			Center.center(this); // (on screen)
		show();
		
	}
	private static JComponent labelOnTopNotKey(String key, JComponent component) {
		String text = key;

		return Layout.borderLayout(new JLabel(text), null, component, null,
				null);
	}
	private static Component strutH(int width) {
		return Box.createHorizontalStrut(width);
	}
	
}