package com.loverslab.apropos.edit;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class DisplayPanel extends JPanel {
	
	private View parent;
	
	public DisplayPanel( View parent ) {
		super( true );
		this.parent = parent;
	}
	
}
