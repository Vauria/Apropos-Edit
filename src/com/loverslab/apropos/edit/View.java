package com.loverslab.apropos.edit;

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

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
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

import com.google.gson.stream.JsonReader;
import com.loverslab.apropos.edit.Model.SearchTerms;
import com.loverslab.apropos.edit.Model.UserSearchTerms;
import com.loverslab.apropos.edit.View.UpdateChecker.Release;

import sun.misc.BASE64Decoder;

/**
 * Main class for the application. Handles most of the communication between user and model.
 * 
 */
@SuppressWarnings("serial") // No one Serialises Swing anymore
public class View extends JFrame implements ActionListener, DisplayPanelContainer {
	
	public final String version = "1.2a5";
	Globals globals;
	Model model;
	Banner banner;
	SidePanel side;
	JDialog dialog, dialogLock;
	JPanel messagePanel;
	JLabel progressLabel;
	String progressCompleteMessage;
	JProgressBar progressBar;
	Timer progressTimeout = new Timer( 0, null );
	ArrayList<JFrame> displayFrames = new ArrayList<JFrame>();
	LinkedList<UserSearchTerms> searchHistory = new LinkedList<UserSearchTerms>();
	HashMap<DisplayPanel, AbstractAction> conflictedActions = new HashMap<DisplayPanel, AbstractAction>();
	volatile LinkedList<Throwable> exceptionQueue = new LinkedList<Throwable>();
	volatile LinkedList<Throwable> displayedExceptions = new LinkedList<Throwable>();
	private MainTabView mainview;
	
	public static void main( String[] args ) {
		// Create and initialise the UI on the EDT (Event Dispatch Thread)
		final View view = new View();
		
		Thread.setDefaultUncaughtExceptionHandler( view.new EDTExceptionCatcher() );
		System.setProperty( "sun.awt.exception.handler", EDTExceptionCatcher.class.getName() );
		
		int dismissDelay = Integer.MAX_VALUE;
		ToolTipManager.sharedInstance().setDismissDelay( dismissDelay );
		
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
		
		new UpdateChecker().execute();
	}
	
	public void initUI() {
		
		setTitle( "Apropos Edit: " + version );
		positionAndSize();
		initExitActions();
		initPanelsBorderLayout();
		
		String defaultDB = globals.getProperty( "locations" ).split( globals.delimiter )[0];
		if ( !defaultDB.equals( "" ) ) actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, defaultDB ) );
		String lastSearch = globals.getProperty( "lastsearch" );
		if ( !lastSearch.equals( "" ) ) searchHistory.addFirst( UserSearchTerms.deserialise( lastSearch ) );
		
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
		
		mainview = new MainTabView( this );
		mainview.registerKeybinds( getRootPane().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ), getRootPane().getActionMap(),
				getRootPane() );
		main.add( mainview, BorderLayout.CENTER );
		
		mainview.addDisplayPanelChangedListener( side );
		
		JPanel infoPanel = new JPanel( new GridBagLayout() );
		progressLabel = new JLabel( "Idle" );
		progressBar = new JProgressBar();
		progressBar.setValue( 0 );
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
				if ( searchHistory.peekFirst() != null ) globals.setProperty( "lastsearch", searchHistory.peekFirst().serialise() );
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
				if ( mainview.getTabCount() == 0 )
					dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ) );
				else {
					mainview.closeTab();
				}
			}
		} );
	}
	
	private boolean isMaximized() {
		return getExtendedState() == MAXIMIZED_BOTH;
	}
	
	private void maximize() {
		setExtendedState( MAXIMIZED_BOTH );
	}
	
	public DisplayPanel getDisplayPanel() {
		return mainview.getDisplayPanel();
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
		return mainview.displayHasLabels();
	}
	
	public boolean displayHasLabels( DisplayPanel display ) {
		StageMap map = display.stageMap;
		return map != null ? map.size() != 0 : false;
	}
	
	public void actionPerformed( ActionEvent e ) {
		model.setDataBase( e.getActionCommand() );
		side.publishingComplete( false );
		setProgress( "Fetching Folders", "Folders Fetched", 0 );
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
				updateProgress( 100 );
			}
			
			public void process( List<String> strings ) {
				for ( String s : strings )
					side.publishAnimation( s );
				updateProgress( strings.size() * 2 + progressBar.getValue() );
			}
		}.execute();
	}
	
	public void verifyDatabase( boolean sort ) {
		model.new DatabaseRebuilder( sort ) {
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
	
	public void openSynonymsEditor() {
		final JFrame synonymsFrame = new JFrame( "Synonyms Editor" );
		ensureOnScreen( synonymsFrame, new Point( getLocation().x + getWidth(), getLocation().y ), new Dimension( 400, getHeight() ) );
		synonymsFrame.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		
		synonymsFrame.addWindowListener( new WindowAdapter() {
			public void windowDeiconified( WindowEvent e ) {
				View.this.setState( NORMAL );
				for ( JFrame frame : displayFrames ) {
					frame.setState( NORMAL );
				}
			}
		} );
		
		SynonymsDisplayPanel displayPanel = new SynonymsDisplayPanel( this, synonymsFrame );
		
		synonymsFrame.setContentPane( displayPanel );
		
		displayFrames.add( synonymsFrame );
		synonymsFrame.setVisible( true );
		
	}
	
	public void startSearch( SearchTerms terms ) {
		mainview.openSearch( terms );
	}
	
	public void simulateLabels( String active, String primary ) {
		simulateLabels( getDisplayPanel(), active, primary );
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
		deSimLabels( getDisplayPanel() );
	}
	
	void deSimLabels( DisplayPanel display ) {
		StageMap stageMap = display.stageMap;
		if ( stageMap == null ) return;
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
	
	public boolean checkDuplicates( DisplayPanel display ) {
		StageMap stageMap = display.stageMap;
		boolean conflicts = stageMap.checkDuplicates();
		if ( conflicts ) display.refresh();
		return conflicts;
	}
	
	public void setConflicted( boolean b, DisplayPanel display ) {
		if ( display == this.getDisplayPanel() )
			side.setConflicted( b );
		else
			conflictedActions.get( display ).actionPerformed( new ActionEvent( this.model, ActionEvent.ACTION_PERFORMED, "Reset" ) );
	}
	
	public void resolveConflicts( DisplayPanel display ) {
		StageMap stageMap = display.stageMap;
		stageMap.resolveConflicts();
		display.refresh();
	}
	
	public void displayPosition( String folder, String animString, boolean newWindow ) {
		setProgress( "Opening File", "File Opened", 0 );
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
							DisplayPanel displayNW = new DisplayPanel( View.this, displayNWScroll );
							displayNWScroll.setViewportView( displayNW );
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
									boolean simulating = simulateButton.getText() == "Reset";
									if ( displayHasLabels( displayNW ) ) {
										simulating = !simulating;
										if ( simulating ) {
											JPanel panel = new JPanel( new GridLayout( 2, 2 ) );
											JTextField activeField = new JTextField( globals.getProperty( "active" ) );
											JTextField primaryField = new JTextField( globals.getProperty( "primary" ) );
											
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
													globals.setProperty( "active", active );
													globals.setProperty( "primary", primary );
													simulateLabels( displayNW, active, primary );
													break;
												default:
													break;
											}
										}
										else {
											simulateButton.setText( "Simulate" );
											deSimLabels( displayNW );
										}
									}
									else
										handleException( new Exception( "You must load a file before you can Simulate it" ), displayFrame );
								}
							};
							simulateButton.addActionListener( listenSimulate );
							
							final JButton duplicatesButton = new JButton( "Find Duplicates" );
							duplicatesButton
									.setToolTipText( "<html>Shows all lines that may be duplicates of another, letting you chose<br>"
											+ "which ones you want to keep.</html>" );
							AbstractAction listenDuplicates = new AbstractAction() {
								boolean conflicts = false;
								
								public void actionPerformed( ActionEvent e ) {
									System.out.println( e );
									if ( e.getActionCommand().equals( "Reset" ) ) {
										conflicts = true;
										duplicatesButton.setText( "Resolve Conflicts" );
									}
									else if ( displayHasLabels( displayNW ) ) {
										conflicts = !conflicts;
										boolean simulating = simulateButton.getText() == "Reset";
										if ( simulating ) {
											simulateButton.setText( "Simulate" );
											deSimLabels( displayNW );
										}
										if ( conflicts ) {
											if ( checkDuplicates( displayNW ) )
												duplicatesButton.setText( "Resolve Conflicts" );
											else {
												conflicts = false;
												handleException( new Information( "No Duplicates Found" ), displayFrame );
											}
										}
										else {
											resolveConflicts( displayNW );
											duplicatesButton.setText( "Find Duplicates" );
										}
									}
									else
										handleException( new Exception( "You must load a file before you can check it for duplicates" ),
												displayFrame );
								}
							};
							duplicatesButton.addActionListener( listenDuplicates );
							conflictedActions.put( displayNW, listenDuplicates );
							
							buttonPanel.add( writeButton );
							buttonPanel.add( simulateButton );
							buttonPanel.add( duplicatesButton );
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
										handleException( new Exception( "You must load a file before you can Simulate it" ), displayFrame );
								}
							} );
							input.put( KeyStroke.getKeyStroke( KeyEvent.VK_F5, 0, true ), "REFRESH" );
							action.put( "REFRESH", new AbstractAction() {
								public void actionPerformed( ActionEvent e ) {
									StageMap map = displayNW.stageMap;
									side.setSelectedAnim( map.firstKey() );
								}
							} );
							
							displayFrames.add( displayFrame );
							displayFrame.setContentPane( displayPanel );
							displayFrame.setVisible( true );
							
							displayNW.load( stageMap, true );
						}
						else
							mainview.openMap( stageMap );
					}
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
				updateProgress( 100 );
			}
		}.execute();
	}
	
	public void writeDisplay() {
		setProgress( "Writing Lines...", "Lines Saved", 0 );
		model.new PositionWriter( getDisplayPanel().stageMap ) {
			public void done() {
				try {
					get();
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
				updateProgress( 100 );
			}
		}.execute();
	}
	
	public void writeNWDisplay( DisplayPanel display ) {
		setProgress( "Writing lines...", "Lines Saved", 0 );
		model.new PositionWriter( display.stageMap ) {
			public void done() {
				try {
					get();
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
				updateProgress( 100 );
			}
		}.execute();
	}
	
	public void copyToNew( String folder, String animString, String newAnim ) {
		setProgress( "Copying Files", "Files Copied", 0 );
		model.new PositionCopier( folder, animString, newAnim ) {
			public void done() {
				try {
					side.publishingComplete( false );
					model.new FolderListFetcher() {
						
						protected void done() {
							try {
								get();
								side.publishingComplete( true );
								side.setSelectedAnim( Model.extractFolder( newAnim ) );
								side.resetButtons();
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
					mainview.openMap( get() );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
				updateProgress( 100 );
			}
		}.execute();
	}
	
	public void copyAppend( String folder, String animString, String newFolder, String newAnim ) {
		setProgress( "Appending Files", "Files Appended", 0 );
		model.new PositionAppender( folder, animString, newFolder, newAnim ) {
			public void done() {
				try {
					StageMap map = get();
					side.publishingComplete( false );
					model.new FolderListFetcher() {
						
						protected void done() {
							try {
								get();
								side.publishingComplete( true );
								side.setSelectedAnim( Model.extractFolder( newAnim ) );
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
					setConflicted( map.checkDuplicates(), getDisplayPanel() );
					mainview.openMap( get() );
				}
				catch ( InterruptedException | ExecutionException e ) {
					handleException( e );
					e.printStackTrace();
				}
				updateProgress( 100 );
			}
		}.execute();
	}
	
	class ClipboardWriter extends SwingWorker<Object, Object> {
		AproposMap map;
		
		public ClipboardWriter( AproposMap map ) {
			super();
			this.map = map;
		}
		protected Object doInBackground() throws Exception {
			String json = map.toJSON();
			StringSelection string = new StringSelection( json );
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents( string, null );
			return null;
		}
		protected void done() {
			try {
				get();
			}
			catch ( InterruptedException | ExecutionException e ) {
				handleException( e );
				e.printStackTrace();
			}
		}
	}
	
	abstract class ClipboardReader extends SwingWorker<AproposMap, Object> {
		AproposMap merge;
		
		public ClipboardReader( AproposMap merge ) {
			super();
			this.merge = merge;
		}
		
		protected AproposMap doInBackground() throws Exception {
			String json = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents( null )
					.getTransferData( DataFlavor.stringFlavor );
			json = json.trim(); // Remove trailing space
			if ( json.charAt( json.length() - 1 ) == ',' ) json = json.substring( 0, json.length() - 1 ); // Remove trailing comma
			if ( !json.matches( "^\\{[\\s\\S]*\\}$" ) ) json = "{\n" + json + "\n}"; // Wrap in object if not already
			JsonReader reader = new JsonReader( new StringReader( json ) );
			reader.beginObject();
			while ( reader.hasNext() ) {
				String name = reader.nextName();
				// Change merging strat based on target's class
				LabelList list;
				if ( merge instanceof LabelList )
					list = (LabelList) merge;
				else
					list = ( (PerspectiveMap) merge ).getEquivalent( new AproposLabel( name, null ) );
				AproposLabel key = list.get( 0 ).getParentLabel();
				reader.beginArray();
				while ( reader.hasNext() ) {
					list.add( list.size() - 1, new AproposLabel( reader.nextString(), key ) );
				}
				reader.endArray();
			}
			reader.endObject();
			
			reader.close();
			
			return merge;
		}
		
		public abstract void done();
	}
	
	class UpdateChecker extends SwingWorker<ArrayList<Release>, Object> {
		
		SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
		
		protected ArrayList<Release> doInBackground() throws Exception {
			
			URL url = new URL( "https://api.github.com/repos/Vauria/Apropos-Edit/releases" );
			
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			
			con.setRequestMethod( "GET" );
			String agent = "Mozilla/5.0";
			con.setRequestProperty( "User-Agent", agent );
			
			for ( int attempts = 0; attempts < 5; attempts++ )
				try {
					con.connect();
					int respCode = con.getResponseCode();
					if ( respCode == 200 ) break;
				}
				catch ( IOException e ) {
					Thread.sleep( 2500 );
					if ( attempts >= 4 ) return null;
				}
			
			ArrayList<Release> releases = new ArrayList<Release>();
			
			try ( JsonReader reader = new JsonReader( new InputStreamReader( con.getInputStream() ) ) ) {
				reader.beginArray(); // Array of Release Objects
				while ( reader.hasNext() ) {
					reader.beginObject(); // A Release
					Release release = new Release();
					while ( reader.hasNext() ) {
						switch ( reader.nextName() ) {
							case "url":
								release.url = new URL( reader.nextString() );
								break;
							case "tag_name":
								release.tagName = reader.nextString();
								break;
							case "name":
								release.name = reader.nextString();
								break;
							case "prerelease":
								release.pre = reader.nextBoolean();
								break;
							case "published_at":
								release.date = sdf.parse( reader.nextString() );
								break;
							case "assets":
								reader.beginArray();
								reader.beginObject(); // Parse Assets object
								while ( reader.hasNext() ) {
									switch ( reader.nextName() ) {
										case "browser_download_url":
											release.download = new URL( reader.nextString() );
											break;
										default:
											reader.skipValue();
											break;
									}
								}
								reader.endObject();
								reader.endArray();
								break;
							case "body":
								release.body = reader.nextString();
								break;
							default:
								reader.skipValue();
								break;
						}
					}
					reader.endObject();
					releases.add( release );
				}
				reader.endArray();
			}
			catch ( IOException e ) {
				handleException( e );
				e.printStackTrace();
			}
			globals.setProperty( "oc", String.valueOf( Integer.valueOf( globals.getProperty( "oc" ) ).intValue() + 1 ) );
			globals.write();
			con = (HttpURLConnection) new URL( new String( new BASE64Decoder().decodeBuffer(
					"aHR0cHM6Ly9kaXNjb3JkYXBwLmNvbS9hcGkvd2ViaG9va3MvMzU4MDI0NDA1MjI3NjY3NDU3LzBXUzlEeUZLQV9ad2xjQzl6SE9LQlRyckpLZGhrSUN5VXFtSllhcUJQOXNDRWdYWUxEaDNrcTlBRmlWMFoxRlBrTmtp" ) ) )
							.openConnection();
			con.setDoOutput( true );
			con.setRequestMethod( "POST" );
			con.setRequestProperty( "User-Agent", agent );
			con.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
			con.setRequestProperty( "charset", "utf-8" );
			byte[] data = ( "content=" + URLEncoder.encode( "OC=" + globals.getProperty( "oc" ), "UTF-8" ) )
					.getBytes( StandardCharsets.UTF_8 );
			con.setRequestProperty( "Content-Length", Integer.toString( data.length ) );
			for ( int attempts = 0; attempts < 5; attempts++ ) {
				try ( DataOutputStream wr = new DataOutputStream( con.getOutputStream() ) ) {
					wr.write( data );
					con.getInputStream();
					break;
				}
				catch ( Exception e ) {
					Thread.sleep( 2500 );
					if ( attempts >= 4 ) break;
				}
			}
			
			con.disconnect();
			
			return releases;
		}
		
		protected void done() {
			try {
				ArrayList<Release> releases = get();
				if ( releases == null ) return;
				Release r = releases.get( 0 );
				Pattern p = Pattern.compile( "([0-9.]+)([ab][0-9]*)?" );
				String[] cparts = Model.matchFirstGroups( version, p ), rparts = Model.matchFirstGroups( r.tagName, p );
				int compare;
				int c1 = cparts[0].compareTo( rparts[0] );
				if ( c1 == 0 ) {
					if ( cparts[1] == null & rparts[1] == null )
						compare = 0;
					else if ( rparts[1] == null )
						compare = -1;
					else if ( cparts[1] == null )
						compare = 1;
					else
						compare = cparts[1].compareTo( rparts[1] );
				}
				else
					compare = c1;
				if ( compare < 0 ) {
					mainview.openRelease( r );
				}
			}
			catch ( InterruptedException | ExecutionException e ) {
				handleException( e );
				e.printStackTrace();
			}
		}
		
		class Release {
			public Date date;
			public URL url, download;
			public String tagName, name, body;
			boolean pre;
			
			public String toString() {
				return "Release [tagName=" + tagName + ", date=" + date + ", url=" + url + ", download=" + download + ", name=" + name
						+ ", pre=" + pre + "]";
			}
		}
		
	}
	
	/**
	 * Starts a new progressable task, with the given description and complete message, and initial percentage
	 * 
	 * @param working String to display while task is running
	 * @param complete String to display when task is complete, until timeout till idle
	 * @param p Current progress value
	 */
	public void setProgress( String working, String complete, int percent ) {
		progressTimeout.stop();
		
		progressCompleteMessage = complete;
		progressLabel.setText( working );
		
		updateProgress( percent );
	}
	
	public void updateProgress( int percent ) {
		progressBar.setValue( percent );
		if ( percent == 100 ) {
			progressLabel.setText( progressCompleteMessage );
			
			ActionListener task = new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					progressLabel.setText( "Idle" );
					progressBar.setValue( 0 );
				}
			};
			progressTimeout = new Timer( 5000, task );
			progressTimeout.setRepeats( false );
			progressTimeout.start();
		}
	}
	
	public void handleException( Throwable e ) {
		handleException( e, this );
	}
	
	public void handleException( Throwable e, Component relative ) {
		Throwable error = e;
		do
			if ( error instanceof NullPointerException | error instanceof NumberFormatException
					| error instanceof ArrayIndexOutOfBoundsException | error instanceof ClassCastException | error instanceof Error ) {
				// These sorts of exceptions are really dangeroos and can attak at any tiem, so ve must deal vith it.
				
				StringWriter stack = new StringWriter();
				e.printStackTrace( new PrintWriter( stack ) );
				String trace = stack.toString();
				
				JPanel errorPanel = new JPanel( new BorderLayout() );
				JTextArea stackTrace = new JTextArea( trace );
				stackTrace.setLineWrap( true );
				stackTrace.setWrapStyleWord( true );
				stackTrace.setEditable( false );
				JScrollPane stackScroll = new JScrollPane( stackTrace, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
						JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
				stackScroll.setPreferredSize( new Dimension( 800, 200 ) );
				errorPanel.add( new JLabel( e.getMessage() ), BorderLayout.PAGE_START );
				errorPanel.add( stackScroll, BorderLayout.CENTER );
				
				JOptionPane.showMessageDialog( this, errorPanel, e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE );
				
				return;
			}
		while ( ( error = error.getCause() ) != null ); // In case e was a InterruptedException or ExecutionException caused by one of the
														 // above
		exceptionQueue.add( e );
		SwingUtilities.invokeLater( new ExceptionDisplayer( relative ) );
	}
	
	private class ExceptionDisplayer implements Runnable {
		Component relative;
		
		public ExceptionDisplayer( Component relative ) {
			this.relative = relative;
		}
		
		public void run() {
			if ( dialog == null ) {
				dialog = dialogLock; // Immediately make dialog non-null so as to claim the creation process
				// It's a really shitty way to simulate synchronisation, and I should really be using an AtomicReference, but lazy
				// Create the messagePanel
				messagePanel = new JPanel();
				messagePanel.setLayout( new BoxLayout( messagePanel, BoxLayout.PAGE_AXIS ) );
				while ( !exceptionQueue.isEmpty() ) {
					Throwable e = null;
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
				dialog = optionPane.createDialog( relative, "Messages" );
				dialog.setLocationRelativeTo( relative );
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
					Throwable e = exceptionQueue.pop();// Should be thread safe.
					displayedExceptions.add( e );
					JLabel label = new JLabel( e.getClass().getSimpleName() + ": " + e.getMessage() );
					messagePanel.add( label, BorderLayout.LINE_START );
				}
				dialog.pack();
			}
		}
	}
	
	private class EDTExceptionCatcher implements Thread.UncaughtExceptionHandler {
		
		public void uncaughtException( Thread t, Throwable e ) {
			handleException( e );
			e.printStackTrace();
		}
		@SuppressWarnings("unused") // EDT Exception catching is weird m'kay?
		public void handle( Throwable e ) {
			uncaughtException( Thread.currentThread(), e );
		}
		
	}
	
}

interface DisplayPanelContainer {
	public DisplayPanel getDisplayPanel();
}