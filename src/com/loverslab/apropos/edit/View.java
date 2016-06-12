package com.loverslab.apropos.edit;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class View extends JFrame {
	
	private final String version = "0.1";
	private Globals globals;
	
	public static void main( String[] args ) {
		// Create and initialise the UI on the EDT (Event Dispatch Thread)
		try {
			SwingUtilities.invokeAndWait( new Runnable() {
				public void run() {
					final View view = new View();
					view.initUI();
				}
			} );
		}
		catch ( InvocationTargetException | InterruptedException e1 ) {
			e1.printStackTrace();
		}
		
	}
	
	public View() {
		globals = new Globals( new File( "apropos-edit.config" ) );
		System.out.println( globals.read() );
	}
	
	public void initUI() {
		setTitle( "Apropos Edit: " + version );
		
		// Set Size
		String size = globals.getProperty( "size" );
		if ( size.equals( "auto" ) | size.equals( "max" ) ) {
			DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
					.getDisplayMode();
			int w = (int) ( dm.getWidth() * 0.8f );
			int h = (int) ( dm.getHeight() * 0.8f );
			setSize( w, h );
			if ( size.equals( "max" ) ) maximize();
		}
		else {
			String[] vals = size.split( "," );
			setSize( (int) Float.parseFloat( vals[0] ), (int) Float.parseFloat( vals[1] ) );
		}
		setMinimumSize( new Dimension( 500, 500 ) );
		
		// Set Position
		String position = globals.getProperty( "position" );
		if ( position.equals( "auto" ) | position.equals( "max" ) ) {
			setLocationRelativeTo( null );
			if ( position.equals( "max" ) ) maximize();
		}
		else {
			String[] vals = position.split( "," );
			setLocation( (int) Float.parseFloat( vals[0] ), (int) Float.parseFloat( vals[1] ) );
		}
		
		// Add exit listener to update Config on Close
		addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
				globals.setProperty( "size", isMaximized() ? "max" : getSize().width + "," + getSize().height );
				globals.setProperty( "position",
						isMaximized() ? "max" : getLocation().getX() + "," + getLocation().getY() );
				globals.write();
				dispose();
			}
		} );
		
		// Add key listener to allow closing the application with CTRL + W;
		final JFrame frame = this;
		getRootPane().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW )
				.put( KeyStroke.getKeyStroke( KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK, true ), "CLOSE" );
		getRootPane().getActionMap().put( "CLOSE", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ) );
			}
		} );
		
		setVisible( true );
	}
	
	protected boolean isMaximized() {
		return getExtendedState() == MAXIMIZED_BOTH;
	}
	
	private void maximize() {
		setExtendedState( MAXIMIZED_BOTH );
	}
	
}
