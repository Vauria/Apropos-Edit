package com.loverslab.apropos.edit;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

@SuppressWarnings("serial")
public class View extends JFrame implements ActionListener {
	
	private final String version = "0.6a";
	protected Globals globals;
	protected Model model;
	protected Banner banner;
	protected SidePanel side;
	protected DisplayPanel display;
	JScrollPane displayScroll;
	
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
		catch ( InvocationTargetException | InterruptedException e1 ) {
			e1.printStackTrace();
		}
		
	}
	
	public View() {
		globals = new Globals( new File( "apropos-edit.config" ) );
		globals.read();
		model = new Model();
	}
	
	public void initUI() {
		
		setTitle( "Apropos Edit: " + version );
		positionAndSize();
		initExitActions();
		initPanels();
		
		String defaultDB = globals.getProperty( "locations" ).split( globals.delimiter )[0];
		if ( !defaultDB.equals( "" ) )
			actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, defaultDB ) );
		
		setVisible( true );
	}
	
	/**
	 * Initialise and Create main Panels
	 */
	private void initPanels() {
		JPanel main = (JPanel) getContentPane();
		GridBagLayout gbl = new GridBagLayout();
		main.setLayout( gbl );
		
		GridBagConstraints c = new GridBagConstraints();
		
		banner = new Banner( this );
		banner.addActionListener( this );
		c.insets = new Insets( 0, 0, 0, 0 );
		c.anchor = GridBagConstraints.PAGE_START;
		c.gridheight = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		main.add( banner, c );
		
		side = new SidePanel( this );
		JScrollPane sideScroll = new JScrollPane( side, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		sideScroll.getVerticalScrollBar().setUnitIncrement( 16 );
		sideScroll.setBorder( BorderFactory.createRaisedSoftBevelBorder() );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridwidth = 1;
		c.gridy = 1;
		c.weighty = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.VERTICAL;
		main.add( sideScroll, c );
		
		displayScroll = new JScrollPane( display );
		display = new DisplayPanel( this, displayScroll );
		displayScroll.setViewportView( display );
		displayScroll.getVerticalScrollBar().setUnitIncrement( 16 );
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		main.add( displayScroll, c );
		
		// setGlassPane( new MyGlassPane( null, null, main ));
	}
	
	/**
	 * Get the values written to the Global Properties and adjust window dimensions and location accordingly
	 */
	private void positionAndSize() {
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
			String[] vals = size.split( globals.delimiter );
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
				globals.setProperty( "size",
						isMaximized() ? "max" : getSize().width + globals.delimiter + getSize().height );
				globals.setProperty( "position",
						isMaximized() ? "max" : getLocation().getX() + globals.delimiter + getLocation().getY() );
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
	}
	
	private boolean isMaximized() {
		return getExtendedState() == MAXIMIZED_BOTH;
	}
	
	private void maximize() {
		setExtendedState( MAXIMIZED_BOTH );
	}
	
	public void actionPerformed( ActionEvent e ) {
		model.setDataBase( e.getActionCommand() );
		model.new FolderListFetcher() {
			
			protected void done() {
				try {
					get();
				}
				catch ( InterruptedException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch ( ExecutionException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally {
					side.publishingComplete( true );
				}
				
			}
			
			public void process( List<String> strings ) {
				for ( String s : strings )
					side.publishAnimation( s );
			}
		}.execute();
	}
	
	public void displayPosition( String folder, String animString ) {
		model.new PositionFetcher( folder, animString ) {
			public void doNE() {
				try {
					JFrame frame = new JFrame( animString );
					
					frame.setSize( 1000, 1200 );
					frame.setLocationRelativeTo( View.this );
					
					JScrollPane displayScroll = new JScrollPane( display );
					DisplayPanel display = new DisplayPanel( View.this, displayScroll );
					displayScroll.setViewportView( display );
					displayScroll.getVerticalScrollBar().setUnitIncrement( 16 );
					
					frame.add( displayScroll );
					
					display.load( get() );
					
					frame.setVisible( true );
				}
				catch ( InterruptedException | ExecutionException e ) {
					e.printStackTrace();
				}
			}

			public void done() {
				//JPanel main = (JPanel) getContentPane();
				//GridBagConstraints c = ((GridBagLayout) main.getLayout()).getConstraints( displayScroll );
				
				//remove( displayScroll );
				//displayScroll.setViewportView( new Display() );
				try {
					display.load( get() );
				}
				catch ( InterruptedException | ExecutionException e ) {
					e.printStackTrace();
				}
				//main.add( displayScroll, c );
				//displayScroll.revalidate();
			}
		}.execute();
	}
	
}

class MyGlassPane extends JComponent implements ItemListener {
	private static final long serialVersionUID = -1200592169643692879L;
	Point point;
	
	// React to change button clicks.
	public void itemStateChanged( ItemEvent e ) {
		setVisible( e.getStateChange() == ItemEvent.SELECTED );
	}
	
	protected void paintComponent( Graphics g ) {
		if ( point != null ) {
			g.setColor( Color.red );
			g.fillOval( point.x - 10, point.y - 10, 20, 20 );
		}
	}
	
	public void setPoint( Point p ) {
		point = p;
	}
	
	public MyGlassPane( AbstractButton aButton, JMenuBar menuBar, Container contentPane ) {
		CBListener listener = new CBListener( aButton, menuBar, this, contentPane );
		addMouseListener( listener );
		addMouseMotionListener( listener );
		setVisible( true );
	}
}

/**
 * Listen for all events that our check box is likely to be
 * interested in. Redispatch them to the check box.
 */
class CBListener extends MouseInputAdapter {
	Component liveButton;
	JMenuBar menuBar;
	MyGlassPane glassPane;
	Container contentPane;
	
	public CBListener( Component liveButton, JMenuBar menuBar, MyGlassPane glassPane, Container contentPane ) {
		this.liveButton = liveButton;
		this.menuBar = menuBar;
		this.glassPane = glassPane;
		this.contentPane = contentPane;
	}
	
	public void mouseMoved( MouseEvent e ) {
		redispatchMouseEvent( e, false );
	}
	
	public void mouseDragged( MouseEvent e ) {
		redispatchMouseEvent( e, false );
	}
	
	public void mouseClicked( MouseEvent e ) {
		redispatchMouseEvent( e, false );
	}
	
	public void mouseEntered( MouseEvent e ) {
		redispatchMouseEvent( e, false );
	}
	
	public void mouseExited( MouseEvent e ) {
		redispatchMouseEvent( e, false );
	}
	
	public void mousePressed( MouseEvent e ) {
		redispatchMouseEvent( e, false );
	}
	
	public void mouseReleased( MouseEvent e ) {
		redispatchMouseEvent( e, true );
	}
	
	// A more finished version of this method would
	// handle mouse-dragged events specially.
	private void redispatchMouseEvent( MouseEvent e, boolean repaint ) {
		System.out.println( e );
		Point glassPanePoint = e.getPoint();
		Container container = contentPane;
		Point containerPoint = SwingUtilities.convertPoint( glassPane, glassPanePoint, contentPane );
		if ( containerPoint.y < 0 ) { // we're not in the content pane
		}
		else {
			// The mouse event is probably over the content pane.
			// Find out exactly which component it's over.
			Component component = SwingUtilities.getDeepestComponentAt( container, containerPoint.x, containerPoint.y );
			
			System.out.println( component );
			if ( ( component != null ) && ( component.equals( liveButton ) ) ) {
				// Forward events over the check box.
				Point componentPoint = SwingUtilities.convertPoint( glassPane, glassPanePoint, component );
				component.dispatchEvent( new MouseEvent( component, e.getID(), e.getWhen(), e.getModifiers(),
						componentPoint.x, componentPoint.y, e.getClickCount(), e.isPopupTrigger() ) );
			}
		}
		
		// Update the glass pane if requested.
		if ( repaint ) {
			glassPane.setPoint( glassPanePoint );
			glassPane.repaint();
		}
	}
}
