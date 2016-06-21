package com.loverslab.apropos.edit;

import java.awt.BorderLayout;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Main class for the application. Handles most of the communication between user and model.
 * 
 */
@SuppressWarnings("serial") // No one Serialises Swing anymore
public class View extends JFrame implements ActionListener {
	
	private final String version = "0.9b";
	protected Globals globals;
	protected Model model;
	protected Banner banner;
	protected SidePanel side;
	protected DisplayPanel display;
	protected JScrollPane displayScroll;
	protected JDialog dialog;
	protected JPanel messagePanel;
	protected ArrayList<JFrame> displayFrames = new ArrayList<JFrame>();
	protected volatile LinkedList<Exception> exceptionQueue = new LinkedList<Exception>();
	protected volatile LinkedList<Exception> displayedExceptions = new LinkedList<Exception>();
	
	public static void main( String[] args ) {
		// Create and initialise the UI on the EDT (Event Dispatch Thread)
		final View view = new View();
		try {
			SwingUtilities.invokeAndWait( new Runnable() {
				public void run() {
					view.initUI();
				}
			} );
		}
		catch ( InvocationTargetException | InterruptedException e ) {
			// View initialisation failed, run for the hill
			e.printStackTrace();
			System.exit( ABORT );
		}
		
	}
	
	public View() {
		super();
		globals = new Globals( new File( "apropos-edit.config" ) );
		globals.read();
		model = new Model( this );
	}
	
	public void initUI() {
		
		setTitle( "Apropos Edit: " + version );
		positionAndSize();
		initExitActions();
		initPanelsBorderLayout();
		
		String defaultDB = globals.getProperty( "locations" ).split( globals.delimiter )[0];
		if ( !defaultDB.equals( "" ) ) actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, defaultDB ) );
		
		setVisible( true );
	}
	
	/**
	 * Initialise and Create main Panels
	 */
	private void initPanelsBorderLayout() {
		JPanel main = (JPanel) getContentPane();
		BorderLayout border = new BorderLayout();
		main.setLayout( border );
		
		banner = new Banner( this );
		banner.addActionListener( this );
		main.add( banner, BorderLayout.PAGE_START );
		
		side = new SidePanel( this );
		JScrollPane sideScroll = new JScrollPane( side, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		sideScroll.getVerticalScrollBar().setUnitIncrement( 16 );
		sideScroll.setBorder( BorderFactory.createRaisedSoftBevelBorder() );
		main.add( sideScroll, BorderLayout.LINE_START );
		
		displayScroll = new JScrollPane( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		display = new DisplayPanel( this, displayScroll );
		displayScroll.setViewportView( display );
		displayScroll.getVerticalScrollBar().setUnitIncrement( 16 );
		main.add( displayScroll, BorderLayout.CENTER );
	}
	
	/**
	 * Get the values written to the Global Properties and adjust window dimensions and location accordingly
	 */
	private void positionAndSize() {
		// Set Size
		String size = globals.getProperty( "size" );
		if ( size.equals( "auto" ) | size.equals( "max" ) ) {
			DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
			int w = (int) ( dm.getWidth() * 0.8f );
			int h = (int) ( dm.getHeight() * 0.8f );
			setSize( w, h );
			if ( size.equals( "max" ) ) maximize();
		}
		else {
			String[] vals = size.split( globals.delimiter );
			setSize( (int) Float.parseFloat( vals[0] ), (int) Float.parseFloat( vals[1] ) );
		}
		// setMinimumSize( new Dimension( 500, 500 ) );
		
		// Set Position
		String position = globals.getProperty( "position" );
		if ( position.equals( "auto" ) | position.equals( "max" ) ) {
			setLocationRelativeTo( null );
			if ( position.equals( "max" ) ) maximize();
		}
		else {
			String[] vals = position.split( globals.delimiter );
			setLocation( (int) Float.parseFloat( vals[0] ), (int) Float.parseFloat( vals[1] ) );
		}
	}
	
	/**
	 * Set up exit listener to write config file on close and bind CTRL + W to close
	 */
	private void initExitActions() {
		// Add exit listener to update Config on Close
		addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
				globals.setProperty( "size", isMaximized() ? "max" : getSize().width + globals.delimiter + getSize().height );
				globals.setProperty( "position", isMaximized() ? "max" : getLocation().getX() + globals.delimiter + getLocation().getY() );
				globals.write();
				dispose();
			}
			public void windowIconified( WindowEvent e ) {
				for ( JFrame frame : displayFrames )
					frame.setState( ICONIFIED );
			}
			public void windowDeiconified( WindowEvent e ) {
				for ( JFrame frame : displayFrames )
					frame.setState( NORMAL );
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
	}
	
	private boolean isMaximized() {
		return getExtendedState() == MAXIMIZED_BOTH;
	}
	
	private void maximize() {
		setExtendedState( MAXIMIZED_BOTH );
	}
	
	public void actionPerformed( ActionEvent e ) {
		model.setDataBase( e.getActionCommand() );
		side.publishingComplete( false );
		model.new FolderListFetcher() {
			
			protected void done() {
				try {
					get();
					side.publishingComplete( true );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
				
			}
			
			public void process( List<String> strings ) {
				for ( String s : strings )
					side.publishAnimation( s );
			}
		}.execute();
	}
	
	public void verifyDatabase() {
		model.new DatabaseRebuilder() {
			public void done() {
				try {
					get();
					System.out.println( "Files reformatted" );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
			}
		}.execute();
	}
	
	public void displayPosition( String folder, String animString, boolean newWindow ) {
		model.new PositionFetcher( folder, animString ) {
			public void done() {
				try {
					if ( newWindow ) {
						JFrame displayFrame = new JFrame( animString );
						displayFrame.setSize( 800, getHeight() );
						displayFrame.setLocation( new Point( getLocation().x + getWidth(), getLocation().y ) );
						displayFrame.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
						
						JPanel displayPanel = new JPanel( new BorderLayout() );
						JScrollPane displayNWScroll = new JScrollPane( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
								JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
						DisplayPanel displayNW = new DisplayPanel( null, displayNWScroll );
						displayNW.load( get() );
						displayNWScroll.setViewportView( displayNW );
						displayNWScroll.getVerticalScrollBar().setUnitIncrement( 16 );
						displayPanel.add( displayNWScroll, BorderLayout.CENTER );
						
						JPanel writePanel = new JPanel();
						JButton writeButton = new JButton( "Write" );
						writeButton.addActionListener( new ActionListener() {
							public void actionPerformed( ActionEvent e ) {
								writeNWDisplay( displayNW );
							}
						} );
						writePanel.add( writeButton );
						displayPanel.add( writePanel, BorderLayout.PAGE_END );
						
						displayFrames.add( displayFrame );
						displayFrame.setContentPane( displayPanel );
						displayFrame.setVisible( true );
					}
					else
						display.load( get() );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
			}
		}.execute();
	}
	
	public void writeDisplay() {
		model.new PositionWriter( display.stageMap ) {
			public void done() {
				try {
					get();
					System.out.println( "Written!" );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
			}
		}.execute();
	}
	
	public void writeNWDisplay( DisplayPanel display ) {
		model.new PositionWriter( display.stageMap ) {
			public void done() {
				try {
					get();
					System.out.println( "Written!" );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
			}
		}.execute();
	}
	
	public void copyToNew( String folder, String animString, String newAnim ) {
		model.new PositionCopier( folder, animString, newAnim ) {
			public void done() {
				try {
					display.load( get() );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
			}
		}.execute();
	}
	
	public void copyAppend( String folder, String animString, String newFolder, String newAnim ) {
		System.out.println( "Unimplemented" );
	}
	
	public void handleException( Exception e ) {
		exceptionQueue.add( e );
		SwingUtilities.invokeLater( new ExceptionDisplayer() );
	}
	
	private class ExceptionDisplayer implements Runnable {
		public void run() {
			if ( dialog == null ) {
				// Create the messagePanel
				messagePanel = new JPanel();
				messagePanel.setLayout( new BoxLayout( messagePanel, BoxLayout.PAGE_AXIS ) );
				while ( !exceptionQueue.isEmpty() ) {
					Exception e = exceptionQueue.pop();// Should be thread safe.
					displayedExceptions.add( e );
					JLabel label = new JLabel( e.getClass().getSimpleName() + ": " + e.getMessage() );
					messagePanel.add( label, BorderLayout.LINE_START );
				}
				// Create the optionPane with this messagePanel
				JOptionPane optionPane = new JOptionPane( messagePanel, JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION );
				dialog = optionPane.createDialog( View.this, "Errors Occured" );
				dialog.setLocationRelativeTo( View.this );
				dialog.pack();
				dialog.setVisible( true ); // Blocks until the user selects an input, and during this time new exceptions can be added to
											 // it's panel
				
				int result = optionPane.getValue() == null ? JOptionPane.CLOSED_OPTION : ( (Integer) optionPane.getValue() ).intValue();
				dialog.dispose();
				dialog = null;
				switch ( result ) {
					case JOptionPane.OK_OPTION:
						displayedExceptions.clear();
						break;
					default:
						while ( !displayedExceptions.isEmpty() ) {
							exceptionQueue.add( displayedExceptions.pop() );
						}
						break;
				}
			}
			else {
				while ( !exceptionQueue.isEmpty() ) {
					Exception e = exceptionQueue.pop();// Should be thread safe.
					displayedExceptions.add( e );
					JLabel label = new JLabel( e.getClass().getSimpleName() + ": " + e.getMessage() );
					messagePanel.add( label, BorderLayout.LINE_START );
				}
				dialog.pack();
			}
		}
	}
	
}