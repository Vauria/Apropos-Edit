package com.loverslab.apropos.edit;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import com.loverslab.apropos.edit.Model.DatabaseSearch;
import com.loverslab.apropos.edit.Model.SearchTerms;
import com.loverslab.apropos.edit.View.UpdateChecker.Release;

@SuppressWarnings("serial")
public class MainTabView extends JTabbedPane
		implements DisplayPanelContainer, ChangeListener, DisplayPanelChangedNotifier, DisplayPanelChangedListener {
	
	private static final long serialVersionUID = 7461908750515160476L;
	private View parent;
	
	public MainTabView( View parent ) {
		super( TOP, SCROLL_TAB_LAYOUT );
		this.parent = parent;
		addChangeListener( this );
	}
	
	public DisplayPanel getDisplayPanel() {
		if ( getTabCount() == 0 ) return null;
		try {
			DisplayPanelContainer con = (DisplayPanelContainer) getComponentAt( getSelectedIndex() );
			return con.getDisplayPanel();
		}
		catch ( ClassCastException e ) {
			return null;
		}
	}
	
	public boolean displayHasLabels() {
		if ( getTabCount() == 0 ) return false;
		String title = getTitleAt( getSelectedIndex() );
		if ( title.contains( "Update Available" ) ) return false;
		DisplayPanel displayPanel = getDisplayPanel();
		if ( displayPanel == null ) return false;
		StageMap map = displayPanel.stageMap;
		return map.size() != 0;
	}
	
	public void openMap( StageMap map ) {
		ScrollableDisplayPanel display = new ScrollableDisplayPanel( parent );
		addTab( Model.shorten( map.firstKey().getParentLabel().getText() ), display );
		setSelectedIndex( getTabCount() - 1 );
		display.getDisplayPanel().load( map, true );
	}
	
	public void openSearch( SearchTerms terms ) {
		SearchView search = new SearchView( parent, terms.name );
		addTab( "Search:" + terms.name, search );
		setSelectedIndex( getTabCount() - 1 );
		search.addDisplayPanelChangedListener( this );
		
		DatabaseSearch databaseSearch = parent.model.new DatabaseSearch( terms, search );
		search.search = databaseSearch;
		databaseSearch.execute();
	}
	
	public void openRelease( Release release ) {
		ScrollSavvyPanel panel = new ScrollSavvyPanel( new BorderLayout() );
		
		JLabel title = new JLabel( release.name );
		title.setFont( title.getFont().deriveFont( 40f ) );
		panel.add( title, BorderLayout.NORTH );
		
		Parser parser = Parser.builder().build();
		Node document = parser.parse( "<html>" + release.body + "</html>" );
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		String html = renderer.render( document );
		
		JLabel description = new JLabel( html );
		description.setBorder( BorderFactory.createEmptyBorder( 0, 15, 0, 15 ) );
		panel.add( description, BorderLayout.CENTER );
		
		JPanel download = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTH;
		JButton button = new JButton( "Download Now" );
		final JProgressBar progress = new JProgressBar();
		download.add( button, c );
		c.gridy = 1;
		download.add( progress, c );
		panel.add( download, BorderLayout.SOUTH );
		
		button.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				new SwingWorker<Object, Integer>() {
					protected Object doInBackground() throws Exception {
						try {
							HttpURLConnection con = (HttpURLConnection) release.download.openConnection();
							con.connect();
							if ( con.getResponseCode() / 100 != 2 )
								JOptionPane.showMessageDialog( parent, "Connection could not be established.\nTry again later?" );
							long size = con.getContentLengthLong();
							System.out.println( size );
							String fileName = release.download.getFile();
							System.out.println( fileName );
							RandomAccessFile jar = new RandomAccessFile( fileName.substring( fileName.lastIndexOf( '/' ) + 1 ), "rw" );
							long downloaded = 0l;
							InputStream stream = con.getInputStream();
							while ( true ) {
								byte[] buffer = new byte[ ( ( size - downloaded ) < 1024 ) ? (int) ( size - downloaded ) : 1024 ];
								int read = stream.read( buffer );
								if ( read == -1 ) break;
								jar.write( buffer, 0, read );
								downloaded += read;
								publish( (int) ( downloaded * 100 / size ) );
							}
							jar.close();
							stream.close();
						}
						catch ( IOException e ) {
							e.printStackTrace();
						}
						return null;
					}
					protected void process( List<Integer> chunks ) {
						progress.setValue( chunks.get( chunks.size() - 1 ) );
					}
					protected void done() {
						progress.setValue( 100 );
						JOptionPane.showMessageDialog( parent,
								"Download Complete!\nYou can now close the editor and find the new version in the same folder." );
					}
				}.execute();
				
			}
		} );
		
		JScrollPane scroll = new JScrollPane( panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		addTab( "Update Available: " + release.tagName, scroll );
		setSelectedIndex( getTabCount() - 1 );
	}
	
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
	
	public void closeTab() {
		String title = getTitleAt( getSelectedIndex() );
		if ( title.startsWith( "Search:" ) ) ( (SearchView) getSelectedComponent() ).search.stop();
		remove( getSelectedIndex() );
	}
	
	public void stateChanged( ChangeEvent e ) {
		if ( getTabCount() != 0 ) fireDisplayPanelChanged( getDisplayPanel() );
	}
	
	public void displayPanelChanged( DisplayPanelChangedNotifier parent, DisplayPanel panel ) {
		fireDisplayPanelChanged( parent, panel );
	}
	
	public void addDisplayPanelChangedListener( DisplayPanelChangedListener listener ) {
		listenerList.add( DisplayPanelChangedListener.class, listener );
	}
	public void removeDisplayPanelChangedListener( DisplayPanelChangedListener listener ) {
		listenerList.remove( DisplayPanelChangedListener.class, listener );
	}
	public void fireDisplayPanelChanged( DisplayPanelChangedNotifier parent, DisplayPanel displayPanel ) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == DisplayPanelChangedListener.class ) {
				( (DisplayPanelChangedListener) listeners[i + 1] ).displayPanelChanged( parent, displayPanel );
			}
		}
	}
	public void fireDisplayPanelChanged( DisplayPanel displayPanel ) {
		fireDisplayPanelChanged( this, displayPanel );
	}
	
}

class ScrollSavvyPanel extends JPanel implements Scrollable {
	private static final long serialVersionUID = 3447298266512731802L;
	private JScrollPane pane;
	
	public ScrollSavvyPanel() {
		super();
	}
	public ScrollSavvyPanel( boolean isDoubleBuffered ) {
		super( isDoubleBuffered );
	}
	public ScrollSavvyPanel( LayoutManager layout, boolean isDoubleBuffered ) {
		super( layout, isDoubleBuffered );
	}
	public ScrollSavvyPanel( LayoutManager layout ) {
		super( layout );
	}
	public JScrollPane getScrollPane() {
		return pane;
	}
	public void setScrollPane( JScrollPane pane ) {
		this.pane = pane;
	}
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}
	public int getScrollableUnitIncrement( Rectangle visibleRect, int orientation, int direction ) {
		return 16;
	}
	public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ) {
		return 64;
	}
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}