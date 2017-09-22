package com.loverslab.apropos.edit;

import java.awt.AWTKeyStroke;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

@SuppressWarnings("serial")
public class MainTabView extends JTabbedPane implements DisplayPanelContainer {
	
	private static final long serialVersionUID = 7461908750515160476L;
	private View parent;
	
	public MainTabView( View parent ) {
		super( TOP, SCROLL_TAB_LAYOUT );
		this.parent = parent;
	}
	
	public DisplayPanel getDisplayPanel() {
		return ( (DisplayPanelContainer) getComponentAt( getSelectedIndex() ) ).getDisplayPanel();
	}
	
	public void openMap( StageMap map ) {
		ScrollableDisplayPanel display = new ScrollableDisplayPanel( parent );
		addTab( Model.shorten( map.firstKey().getParentLabel().getText() ), display );
		setSelectedIndex( getTabCount() - 1 );
		display.getDisplayPanel().load( map, true );
	}
	
	public void openSearch() {}
	
	public void registerKeybinds( InputMap input, ActionMap action, java.awt.Container root ) {
		KeyStroke next = KeyStroke.getKeyStroke( "ctrl TAB" ), prev = KeyStroke.getKeyStroke( "ctrl shift TAB" );
		
		Set<AWTKeyStroke> forwardKeys = new HashSet<AWTKeyStroke>(
				root.getFocusTraversalKeys( KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS ) );
		Set<AWTKeyStroke> backwardKeys = new HashSet<AWTKeyStroke>(
				root.getFocusTraversalKeys( KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS ) );
		forwardKeys.remove( next );
		backwardKeys.remove( prev );
		root.setFocusTraversalKeys( KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardKeys );
		root.setFocusTraversalKeys( KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardKeys );
		
		input.put( next, "NEXTTAB" );
		action.put( "NEXTTAB", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				cycleForward();
			}
		} );
		input.put( prev, "PREVTAB" );
		action.put( "PREVTAB", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				cycleBackward();
			}
		} );
	}
	
	public void cycleForward() {
		if ( getTabCount() == 0 ) return;
		int ind = getSelectedIndex() + 1;
		if ( ind >= getTabCount() ) ind = 0;
		setSelectedIndex( ind );
	}
	
	public void cycleBackward() {
		if ( getTabCount() == 0 ) return;
		int ind = getSelectedIndex() - 1;
		if ( ind < 0 ) ind = getTabCount() - 1;
		setSelectedIndex( ind );
	}
	
}
