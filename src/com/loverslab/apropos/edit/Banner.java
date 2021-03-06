package com.loverslab.apropos.edit;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.EventListenerList;

import sun.swing.FilePane;

@SuppressWarnings("serial")
public class Banner extends JPanel implements ItemListener, ActionListener {
	
	private View parent;
	JLabel label;
	JComboBox<String> locations;
	ComboBoxModel<String> model;
	EventListenerList listenerList = new EventListenerList();;
	
	public Banner( View parent ) {
		super( true );
		this.parent = parent;
		setBorder( BorderFactory.createRaisedSoftBevelBorder() );
		
		setLayout( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		
		label = new JLabel( "Apropos " + Model.fs + "db" + Model.fs + " Location" );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 5, 10, 5, 5 );
		c.gridx = 0;
		add( label, c );
		
		String locConfig = parent.globals.getProperty( "locations" );
		String[] locArr;
		if ( locConfig.equals( "" ) ) {
			locArr = new String[] { "<other>" };
		}
		else {
			String[] locArrO = locConfig.split( parent.globals.delimiter );
			locArr = new String[ locArrO.length + 1 ];
			System.arraycopy( locArrO, 0, locArr, 0, locArrO.length );
			locArr[locArr.length - 1] = "<other>";
		}
		model = new DefaultComboBoxModel<String>( locArr );
		locations = new JComboBox<String>( model );
		locations.addActionListener( this );
		locations.addItemListener( this );
		c.insets = new Insets( 5, 5, 5, 5 );
		c.weightx = 1.0;
		c.gridx++ ;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( locations, c );
		
		JButton refreshButton = new JButton( "Reload" );
		refreshButton.setToolTipText( "Reloads Folders and Synonyms in this Database folder" );
		refreshButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String selection = (String) model.getSelectedItem();
				if ( !selection.equals( "<other>" ) ) fireActionPerformed( selection );
			}
		} );
		JButton deleteButton = new JButton( "Delete" );
		deleteButton.setToolTipText( "Removes this Database folder from the history" );
		deleteButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String selection = (String) model.getSelectedItem();
				if ( !selection.equals( "<other>" ) ) {
					locations.removeItemAt( locations.getSelectedIndex() );
					Globals globals = Banner.this.parent.globals;
					String gLocations = globals.getProperty( "locations" );
					// Rebuild the locations string excluding this entry
					String[] locationsArr = gLocations.split( Pattern.quote( globals.delimiter ) );
					String newLocations = "";
					int found = -1;
					for ( int i = 0; i < locationsArr.length; i++ ) {
						String loc = locationsArr[i];
						if ( found == -1 & loc.equals( selection ) )
							found = i;
						else
							newLocations = newLocations + loc + globals.delimiter;
					}
					// Trim the last delimiter
					newLocations = newLocations.replaceAll( Pattern.quote( globals.delimiter ) + "$", "" );
					globals.setProperty( "locations", newLocations );
				}
			}
		} );
		
		c.weightx = 0;
		c.insets = new Insets( 5, 1, 5, 1 );
		c.gridx++ ;
		add( refreshButton, c );
		c.gridx++ ;
		add( deleteButton, c );
	}
	
	public void addActionListener( ActionListener listener ) {
		listenerList.add( ActionListener.class, listener );
	}
	
	public void removeActionListener( ActionListener listener ) {
		listenerList.remove( ActionListener.class, listener );
	}
	
	void fireActionPerformed( String command ) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		ActionEvent e = null;
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == ActionListener.class ) {
				// Lazily create the event:
				if ( e == null ) {
					e = new ActionEvent( this, ActionEvent.ACTION_PERFORMED, command );
				}
				( (ActionListener) listeners[i + 1] ).actionPerformed( e );
			}
		}
	}
	
	/**
	 * For when there is only one item, the <other> item, no new item can be selected, and so no new items can be added
	 */
	public void actionPerformed( ActionEvent e ) {
		if ( locations.getItemCount() != 1 ) return; // This method needs to be prevented from firing if the itemStateChange listener is
													 // active
		String selected = (String) model.getSelectedItem();
		if ( selected.equals( "<other>" ) ) {
			itemStateChanged( new ItemEvent( locations, ItemEvent.ITEM_STATE_CHANGED, selected, ItemEvent.SELECTED ) );
		}
	}
	
	public void itemStateChanged( ItemEvent e ) {
		if ( e.getStateChange() == ItemEvent.SELECTED ) {
			String selected = (String) model.getSelectedItem();
			if ( selected.equals( "<other>" ) ) {
				SwingUtilities.invokeLater( new Runnable() {
					public void run() {
						JSystemFileChooser fileChooser = new JSystemFileChooser();
						fileChooser.setAcceptAllFileFilterUsed( false );
						fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
						try { // Set the default directory to the one the code was launched from
							String spoofLoc = parent.globals.getProperty( "spoofloc" );
							if ( spoofLoc != null )
								fileChooser.setCurrentDirectory( new File( spoofLoc ) );
							else
								fileChooser.setCurrentDirectory(
										new File( ClassLoader.getSystemClassLoader().getResource( "." ).toURI().getPath() ) );
						}
						catch ( URISyntaxException ex ) {
							parent.handleException( ex );
							ex.printStackTrace();
						}
						int result = fileChooser.showOpenDialog( Banner.this );
						
						if ( result == JFileChooser.APPROVE_OPTION ) {
							File chosenFile = fileChooser.getSelectedFile();
							// Update locations in config file
							String oldLoc = parent.globals.getProperty( "locations" );
							parent.globals.setProperty( "locations",
									oldLoc + ( ( oldLoc.split( parent.globals.delimiter ).length > 0 & !oldLoc.equals( "" ) )
											? parent.globals.delimiter : "" ) + chosenFile.getAbsolutePath() );
							locations.insertItemAt( chosenFile.getAbsolutePath(), locations.getItemCount() - 1 );
							locations.setSelectedIndex( locations.getItemCount() - 2 );
							// Above call will trigger another itemStateChange event, which we will rely on to trigger the Database Change
							return;
						}
						else {
							// Check there's actually another element to check, otherwise we'll just reopen the FileChooser.
							// Again and again.
							// Not that that isn't hilarious of course. PLEASE CHOSE A DIRECTORY I BEG YOU
							if ( locations.getItemCount() > 1 ) locations.setSelectedIndex( 0 );
							return;
						}
					}
				} );
				// The FileChooser dialog blocks the running thread until the user makes their input. But this is an event, and thus we're
				// on the EDT. Suspected cause of fuckery.
			}
			else {
				String newSelection = (String) model.getSelectedItem();
				fireActionPerformed( newSelection );
			}
		}
	}
	
	public class JSystemFileChooser extends JFileChooser {
		public void updateUI() {
			LookAndFeel old = UIManager.getLookAndFeel();
			try {
				UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
			}
			catch ( Throwable ex ) {
				old = null;
			}
			
			super.updateUI();
			
			if ( old != null ) {
				FilePane filePane = findFilePane( this );
				if ( filePane == null ) return;
				filePane.setViewType( FilePane.VIEWTYPE_DETAILS );
				filePane.setViewType( FilePane.VIEWTYPE_LIST );
				
				Color background = UIManager.getColor( "Label.background" );
				setBackground( background );
				setOpaque( true );
				
				try {
					UIManager.setLookAndFeel( old );
				}
				catch ( UnsupportedLookAndFeelException ignored ) {} // shouldn't get here
			}
		}
		
		private FilePane findFilePane( Container parent ) {
			for ( Component comp : parent.getComponents() ) {
				if ( FilePane.class.isInstance( comp ) ) { return (FilePane) comp; }
				if ( comp instanceof Container ) {
					Container cont = (Container) comp;
					if ( cont.getComponentCount() > 0 ) {
						FilePane found = findFilePane( cont );
						if ( found != null ) { return found; }
					}
				}
			}
			
			return null;
		}
	}
	
}
