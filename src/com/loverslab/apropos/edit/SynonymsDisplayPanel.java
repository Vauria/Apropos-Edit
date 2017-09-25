package com.loverslab.apropos.edit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

@SuppressWarnings("serial")
public class SynonymsDisplayPanel extends JPanel {
	
	View view;
	JFrame parent;
	private JTabbedPane tabs;
	private DisplayPanel panelSynonyms, panelArousal, panelWearNTear;
	
	public SynonymsDisplayPanel( View view, JFrame parent ) throws HeadlessException {
		super();
		this.view = view;
		this.parent = parent;
		
		initPanels();
	}
	
	public static JScrollPane scrollWrap( Component panel ) {
		JScrollPane pane = new JScrollPane( panel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		pane.getVerticalScrollBar().setUnitIncrement( 16 );
		return pane;
	}
	
	private void initPanels() {
		setLayout( new BorderLayout() );
		
		tabs = new JTabbedPane();
		tabs.addTab( "Synonyms", scrollWrap( initSynonyms() ) );
		tabs.addTab( "Arousal", scrollWrap( initArousal() ) );
		tabs.addTab( "Wear and Tear", scrollWrap( initWearNTear() ) );
		
		add( tabs, BorderLayout.CENTER );
		
		JPanel buttonPanel = new JPanel();
		
		JButton writeButton = new JButton( "Save" );
		writeButton.setToolTipText( "CTRL + S" );
		AbstractAction listenWrite = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {}
		};
		writeButton.addActionListener( listenWrite );
		writeButton.setEnabled( false );
		buttonPanel.add( writeButton );
		
		// Add key listener to allow closing the application with CTRL + W;
		InputMap input = parent.getRootPane().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
		ActionMap action = parent.getRootPane().getActionMap();
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK, true ), "CLOSE" );
		action.put( "CLOSE", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				parent.dispatchEvent( new WindowEvent( parent, WindowEvent.WINDOW_CLOSING ) );
			}
		} );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, true ), "SAVE" );
		action.put( "SAVE", listenWrite );
		
		add( buttonPanel, BorderLayout.PAGE_END );
	}
	
	private JPanel initSynonyms() {
		panelSynonyms = new DisplayPanel( view, null );
		panelSynonyms.load( view.model.synonyms.getSynonymsMap(), false );
		return panelSynonyms;
	}
	
	private JPanel initArousal() {
		panelArousal = new DisplayPanel( view, null );
		panelArousal.load( view.model.synonyms.getArousalMap(), false );
		return panelArousal;
	}
	
	private JPanel initWearNTear() {
		panelWearNTear = new DisplayPanel( view, null );
		panelWearNTear.load( view.model.synonyms.getWearNTear(), false );
		return panelWearNTear;
	}
	
}
