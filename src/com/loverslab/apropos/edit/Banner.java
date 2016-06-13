package com.loverslab.apropos.edit;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.EventListenerList;

import sun.swing.FilePane;

@SuppressWarnings("serial")
public class Banner extends JPanel implements ActionListener {
	
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
		
		label = new JLabel( "Apropos \\db\\ Location" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets( 5, 10, 5, 5 );
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
		c.insets = new Insets( 5, 5, 5, 5 );
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( locations, c );
	}
	
	public void addActionListener( ActionListener listener ) {
		listenerList.add( ActionListener.class, listener );
	}
	
	public void removeActionListener( ActionListener listener ) {
		listenerList.remove( ActionListener.class, listener );
	}
	
	protected void fireActionPerformed( String command ) {
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
	
	public void actionPerformed( ActionEvent e ) {
		String selected = (String) model.getSelectedItem();
		if ( selected.equals( "<other>" ) ) {
			JSystemFileChooser fileChooser = new JSystemFileChooser();
			fileChooser.setAcceptAllFileFilterUsed( false );
			fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
			fileChooser.setCurrentDirectory( new File( "" ) );
			int result = fileChooser.showOpenDialog( this );
			
			if ( result == JFileChooser.APPROVE_OPTION ) {
				File chosenFile = fileChooser.getSelectedFile();
				// Update locations in config file
				String oldLoc = parent.globals.getProperty( "locations" );
				parent.globals.setProperty( "locations",
						( ( oldLoc.split( parent.globals.delimiter ).length > 0 & !oldLoc.equals( "" ) )
								? parent.globals.delimiter : "" ) + chosenFile.getAbsolutePath() );
				locations.insertItemAt( chosenFile.getAbsolutePath(), locations.getItemCount() - 1 );
				locations.setSelectedIndex( locations.getItemCount() - 2 );
			}
			
		}
		String newSelection = (String) model.getSelectedItem();
		if ( !selected.equals( newSelection ) ) {
			fireActionPerformed( newSelection );
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
