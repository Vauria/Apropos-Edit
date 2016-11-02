package com.loverslab.apropos.edit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Main class for the application. Handles most of the communication between user and model.
 * 
 */
@SuppressWarnings("serial") // No one Serialises Swing anymore
public class View extends JFrame implements ActionListener {
	
	private final String version = "1.2a";
	protected Globals globals;
	protected Model model;
	protected Banner banner;
	protected SidePanel side;
	protected DisplayPanel display;
	protected JScrollPane displayScroll;
	protected JDialog dialog, dialogLock;
	protected JPanel messagePanel;
	protected JLabel progressLabel;
	protected JProgressBar progressBar;
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
		dialogLock = new JDialog();
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
		side.registerKeybinds( getRootPane().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ), getRootPane().getActionMap() );
		JScrollPane sideScroll = new JScrollPane( side, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		sideScroll.getVerticalScrollBar().setUnitIncrement( 16 );
		sideScroll.setBorder( BorderFactory.createRaisedSoftBevelBorder() );
		main.add( sideScroll, BorderLayout.LINE_START );
		
		displayScroll = new JScrollPane( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
		display = new DisplayPanel( this, displayScroll );
		displayScroll.setViewportView( display );
		displayScroll.getVerticalScrollBar().setUnitIncrement( 16 );
		main.add( displayScroll, BorderLayout.CENTER );
		
		JPanel infoPanel = new JPanel( new GridBagLayout() );
		progressLabel = new JLabel( "Counting Files: 700..." );
		progressBar = new JProgressBar();
		progressBar.setValue( 35 );
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets( 2, 10, 2, 10 );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridx = 0;
		c.gridwidth = 2;
		infoPanel.add( progressBar, c );
		c.gridx = 2;
		c.gridwidth = 1;
		c.weightx = 1;
		infoPanel.add( progressLabel, c );
		main.add( infoPanel, BorderLayout.PAGE_END );
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
				System.exit( 0 );
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
	
	public void ensureOnScreen( Frame frame, Point position, Dimension dimension ) {
		Rectangle bounds = null;
		Point pos = new Point( position.x + dimension.width, position.y );
		Dimension size = new Dimension( dimension );
		double distance = Double.MAX_VALUE;
		for ( GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices() )
			for ( GraphicsConfiguration config : device.getConfigurations() ) {
				Rectangle temp = config.getBounds();
				if ( temp.contains( pos ) ) {
					bounds = temp;
					pos.x = pos.x - size.width;
					distance = -1;
				}
				else if ( temp.getLocation().distance( pos ) < distance ) {
					bounds = temp;
				}
			}
		if ( distance > 0 ) {
			pos.x = pos.x - size.width;
			pos = clampPoint( pos, bounds.getLocation(), new Point( bounds.x + bounds.width, bounds.y + bounds.height ) );
		}
		// Check right bound
		int rightOverflow = pos.x + size.width - bounds.x - bounds.width;
		if ( rightOverflow > 0 ) pos.x = pos.x - rightOverflow;
		
		// Check lower bound
		int lowerOverflow = pos.y + size.height - bounds.y - bounds.height;
		if ( lowerOverflow > 0 ) size = new Dimension( size.width, size.height - lowerOverflow );
		
		frame.setLocation( pos );
		frame.setSize( size );
	}
	
	public Point clampPoint( Point point, Point topLeft, Point bottomRight ) {
		return new Point( Math.max( topLeft.x, Math.min( bottomRight.x, point.x ) ),
				Math.max( topLeft.y, Math.min( bottomRight.y, point.y ) ) );
	}
	
	public boolean displayHasLabels() {
		StageMap map = display.stageMap;
		return map != null ? map.size() != 0 : false;
	}
	
	public boolean displayHasLabels( DisplayPanel display ) {
		StageMap map = display.stageMap;
		return map != null ? map.size() != 0 : false;
	}
	
	public void actionPerformed( ActionEvent e ) {
		model.setDataBase( e.getActionCommand() );
		side.publishingComplete( false );
		model.new FolderListFetcher() {
			
			protected void done() {
				try {
					get();
					side.publishingComplete( true );
					revalidate();
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
					handleException( new Information( "File Reformatting Complete!" ) );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
			}
		}.execute();
	}
	
	public void simulateLabels( String active, String primary ) {
		simulateLabels( display, active, primary );
	}
	
	public void simulateLabels( DisplayPanel display, String active, String primary ) {
		StageMap stageMap = display.stageMap;
		model.new LabelSimulator( stageMap, active, primary ) {
			
			public void process( List<AproposLabel> labels ) {
				for ( AproposLabel label : labels )
					label.simulate();
			}
			
		}.execute();
	}
	
	public void deSimLabels() {
		deSimLabels( display );
	}
	
	protected void deSimLabels( DisplayPanel display ) {
		StageMap stageMap = display.stageMap;
		for ( AproposLabel stage : stageMap.keySet() ) {
			PerspectiveMap persMap = stageMap.get( stage );
			for ( AproposLabel perspec : persMap.keySet() ) {
				LabelList list = persMap.get( perspec );
				for ( AproposLabel label : list ) {
					if ( label.getText().equals( "" ) ) continue;
					label.deSimulate();
				}
			}
		}
	}
	
	public void displayPosition( String folder, String animString, boolean newWindow ) {
		model.new PositionFetcher( folder, animString ) {
			public void done() {
				try {
					StageMap stageMap = get();
					if ( stageMap != null && stageMap.size() != 0 ) {
						if ( newWindow ) {
							final JFrame displayFrame = new JFrame( animString );
							ensureOnScreen( displayFrame, new Point( getLocation().x + getWidth(), getLocation().y ),
									new Dimension( 800, getHeight() ) );
							displayFrame.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
							
							displayFrame.addWindowListener( new WindowAdapter() {
								public void windowDeiconified( WindowEvent e ) {
									View.this.setState( NORMAL );
									for ( JFrame frame : displayFrames ) {
										frame.setState( NORMAL );
									}
								}
							} );
							
							JPanel displayPanel = new JPanel( new BorderLayout() );
							JScrollPane displayNWScroll = new JScrollPane( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
									JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
							DisplayPanel displayNW = new DisplayPanel( null, displayNWScroll );
							displayNW.load( stageMap, true );
							displayNWScroll.setViewportView( displayNW );
							displayNWScroll.getVerticalScrollBar().setUnitIncrement( 16 );
							displayPanel.add( displayNWScroll, BorderLayout.CENTER );
							
							JPanel buttonPanel = new JPanel();
							JButton writeButton = new JButton( "Save" );
							writeButton.setToolTipText( "CTRL + S" );
							AbstractAction listenWrite = new AbstractAction() {
								public void actionPerformed( ActionEvent e ) {
									writeNWDisplay( displayNW );
								}
							};
							writeButton.addActionListener( listenWrite );
							final JButton simulateButton = new JButton( "Simulate" );
							simulateButton.setToolTipText( "CTRL + R (CTRL + SHIFT + R to skip dialog)" );
							AbstractAction listenSimulate = new AbstractAction() {
								public void actionPerformed( ActionEvent e ) {
									View parent = View.this;
									boolean simulating = simulateButton.getText() == "Reset";
									if ( parent.displayHasLabels( displayNW ) ) {
										simulating = !simulating;
										if ( simulating ) {
											JPanel panel = new JPanel( new GridLayout( 2, 2 ) );
											JTextField activeField = new JTextField( parent.globals.getProperty( "active" ) );
											JTextField primaryField = new JTextField( parent.globals.getProperty( "primary" ) );
											
											panel.add( new JLabel( "Name for Active (Your Partner's Name)" ) );
											panel.add( activeField );
											panel.add( new JLabel( "Name for Primary (Like your PC's Name)" ) );
											panel.add( primaryField );
											
											int result = JOptionPane.showConfirmDialog( displayFrame, panel,
													"Chose names for {ACTIVE} and {PRIMARY}", JOptionPane.OK_CANCEL_OPTION,
													JOptionPane.QUESTION_MESSAGE );
											
											switch ( result ) {
												case JOptionPane.OK_OPTION:
													simulateButton.setText( "Reset" );
													String active = activeField.getText();
													String primary = primaryField.getText();
													parent.globals.setProperty( "active", active );
													parent.globals.setProperty( "primary", primary );
													parent.simulateLabels( displayNW, active, primary );
													break;
												default:
													break;
											}
										}
										else {
											simulateButton.setText( "Simulate" );
											parent.deSimLabels( displayNW );
										}
									}
									else
										parent.handleException( new Exception( "You must load a file before you can Simulate it" ) );
								}
							};
							simulateButton.addActionListener( listenSimulate );
							buttonPanel.add( writeButton );
							buttonPanel.add( simulateButton );
							displayPanel.add( buttonPanel, BorderLayout.PAGE_END );
							
							// Add key listener to allow closing the application with CTRL + W;
							InputMap input = displayFrame.getRootPane().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
							ActionMap action = displayFrame.getRootPane().getActionMap();
							input.put( KeyStroke.getKeyStroke( KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK, true ), "CLOSE" );
							action.put( "CLOSE", new AbstractAction() {
								public void actionPerformed( ActionEvent e ) {
									displayFrame.dispatchEvent( new WindowEvent( displayFrame, WindowEvent.WINDOW_CLOSING ) );
								}
							} );
							input.put( KeyStroke.getKeyStroke( KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, true ), "SAVE" );
							action.put( "SAVE", listenWrite );
							input.put( KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true ), "SIMULATE" );
							action.put( "SIMULATE", listenSimulate );
							input.put(
									KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, true ),
									"SIMULATESKIP" );
							action.put( "SIMULATESKIP", new AbstractAction() {
								public void actionPerformed( ActionEvent e ) {
									boolean simulating = simulateButton.getText() == "Reset";
									if ( displayHasLabels( displayNW ) ) {
										simulating = !simulating;
										if ( simulating ) {
											simulateButton.setText( "Reset" );
											simulateLabels( displayNW, globals.getProperty( "active" ), globals.getProperty( "primary" ) );
										}
										else {
											simulateButton.setText( "Simulate" );
											deSimLabels( displayNW );
										}
									}
									else
										handleException( new Exception( "You must load a file before you can Simulate it" ) );
								}
							} );
							
							displayFrames.add( displayFrame );
							displayFrame.setContentPane( displayPanel );
							displayFrame.setVisible( true );
						}
						else
							display.load( stageMap, true );
					}
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
					handleException( new Information( "Write Complete!" ) );
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
					handleException( new Information( "Write Complete!" ) );
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
					side.publishingComplete( false );
					model.new FolderListFetcher() {
						
						protected void done() {
							try {
								get();
								side.publishingComplete( true );
								side.setSelectedAnim( model.extractFolder( newAnim ) );
								revalidate();
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
					display.load( get(), true );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
			}
		}.execute();
	}
	
	public void copyAppend( String folder, String animString, String newFolder, String newAnim ) {
		model.new PositionAppender( folder, animString, newFolder, newAnim ) {
			public void done() {
				try {
					side.publishingComplete( false );
					model.new FolderListFetcher() {
						
						protected void done() {
							try {
								get();
								side.publishingComplete( true );
								side.setSelectedAnim( model.extractFolder( newAnim ) );
								revalidate();
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
					display.load( get(), true );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
			}
		}.execute();
	}
	
	public void handleException( Exception e ) {
		Throwable error = e;
		do
			if ( error instanceof NullPointerException ) {
				// These sorts of exceptions are really dangeroos and can attak at any tiem, so ve must deal vith it.
				
				StringWriter stack = new StringWriter();
				e.printStackTrace( new PrintWriter( stack ) );
				
				JPanel errorPanel = new JPanel( new BorderLayout() );
				JTextArea stackTrace = new JTextArea( stack.toString() );
				stackTrace.setLineWrap( true );
				stackTrace.setWrapStyleWord( true );
				stackTrace.setEditable( false );
				JScrollPane stackScroll = new JScrollPane( stackTrace, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
				stackScroll.setPreferredSize( new Dimension( 600, 200 ) );
				errorPanel.add( new JLabel( e.getMessage() ), BorderLayout.PAGE_START );
				errorPanel.add( stackScroll, BorderLayout.CENTER );
				
				JOptionPane.showMessageDialog( this, errorPanel, e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE );
				
				return;
			}
		while ( ( error = error.getCause() ) != null ); // In case e was a InterruptedException or ExecutionException caused by one of the
														 // above
		exceptionQueue.add( e );
		SwingUtilities.invokeLater( new ExceptionDisplayer() );
	}
	
	private class ExceptionDisplayer implements Runnable {
		public void run() {
			if ( dialog == null ) {
				dialog = dialogLock; // Immediately make dialog non-null so as to claim the creation process
				// It's a really shitty way to simulate synchronisation, and I should really be using an AtomicReference, but lazy
				// Create the messagePanel
				messagePanel = new JPanel();
				messagePanel.setLayout( new BoxLayout( messagePanel, BoxLayout.PAGE_AXIS ) );
				while ( !exceptionQueue.isEmpty() ) {
					Exception e = null;
					try {
						e = exceptionQueue.pop();// Should be thread safe.
					}
					catch ( NoSuchElementException nsee ) {
						// Our paltry attempt at thread safety failed (Another thread popped the element between this thread trying and
						// check the queue was not empty, so re-enter the run (dialog will no longer be null) and then return
						run();
						return;
					}
					displayedExceptions.add( e );
					JLabel label = new JLabel( e.getClass().getSimpleName() + ": " + e.getMessage() );
					messagePanel.add( label, BorderLayout.LINE_START );
				}
				// Create the optionPane with this messagePanel
				JOptionPane optionPane = new JOptionPane( messagePanel, JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION );
				dialog = optionPane.createDialog( View.this, "Messages" );
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