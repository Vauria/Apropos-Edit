package com.loverslab.apropos.edit;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.loverslab.apropos.edit.Model.DatabaseSearch;

public class SearchView extends JPanel implements DisplayPanelContainer, MouseListener, InteractionListener, DisplayPanelChangedNotifier {
	
	private static final long serialVersionUID = -2550756315633568153L;
	DatabaseSearch search;
	int page;
	JPanel cards;
	ScrollSavvyPanel currentPage;
	DisplayPanel currentPanel;
	ArrayList<ScrollSavvyPanel> pages = new ArrayList<ScrollSavvyPanel>();
	HashMap<JPanel, DisplayPanel> displayLookup = new HashMap<JPanel, DisplayPanel>();
	CardLayout cl;
	GridBagConstraints c;
	JButton prev, next;
	JLabel pageNum;
	private View view;
	private String name;
	
	public SearchView( View parent, String name ) {
		this.view = parent;
		this.name = name;
		initPanels();
	}
	
	private void initPanels() {
		setLayout( new BorderLayout() );
		
		JPanel buttons = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		prev = new JButton( "Previous" );
		next = new JButton( "Next" );
		pageNum = new JLabel( "Page 1" );
		pageNum.setHorizontalAlignment( SwingConstants.CENTER );
		
		prev.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				pagePrev();
			}
		} );
		next.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				pageNext();
			}
		} );
		prev.setEnabled( false );
		next.setEnabled( false );
		
		c.anchor = GridBagConstraints.CENTER;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 1;
		buttons.add( prev, c );
		c.gridx++ ;
		buttons.add( pageNum, c );
		c.gridx++ ;
		buttons.add( next, c );
		
		cl = new CardLayout( 0, 0 );
		cards = new JPanel( cl );
		
		addPage( 1 );
		add( buttons, BorderLayout.SOUTH );
		add( cards, BorderLayout.CENTER );
	}
	
	private void addPage( int p ) {
		currentPage = new ScrollSavvyPanel( new GridBagLayout() );
		JScrollPane scroll = new JScrollPane( currentPage, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		currentPage.setScrollPane( scroll );
		currentPage.addMouseListener( this );
		
		c = new GridBagConstraints();
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		
		cards.add( scroll, "PAGE-" + p );
		page = p;
		pages.add( currentPage );
	}
	
	public void addStageMap( StageMap map ) {
		JPanel container = new JPanel( new GridBagLayout() );
		GridBagConstraints con = new GridBagConstraints();
		JLabel name = new JLabel( map.firstKey().getParentLabel().getText() );
		name.setBorder( BorderFactory.createMatteBorder( 1, 0, 0, 0, Color.BLACK ) );
		con.anchor = GridBagConstraints.NORTHWEST;
		con.fill = GridBagConstraints.HORIZONTAL;
		con.weightx = 1;
		con.gridx = 0;
		con.gridy = 0;
		con.insets = new Insets( 0, 0, 2, 0 );
		container.add( name, con );
		
		DisplayPanel display = new DisplayPanel( view, true );
		display.load( map, false );
		con.fill = GridBagConstraints.BOTH;
		con.weighty = 1.0;
		con.insets = new Insets( 0, 0, 0, 0 );
		con.gridy++ ;
		container.add( display, con );
		
		display.addInteractionListener( this );
		displayLookup.put( container, display );
		
		c.gridy++ ;
		currentPage.add( container, c );
		currentPage.revalidate();
	}
	
	/**
	 * Called when the currently loading page has been completed
	 * 
	 * @param searchComplete true if there are no futher pages to be loaded
	 */
	public void pageComplete( boolean searchComplete ) {
		synchronized ( currentPage.getTreeLock() ) {
			setSelected( (JPanel) currentPage.getComponent( 0 ) );
		}
		SwingUtilities.invokeLater( new ResetScroll( currentPage.getScrollPane() ) );
		
		if ( !searchComplete ) {
			if ( page == pages.size() ) {
				next.setEnabled( true );
			}
		}
		else {
			view.setProgress( "", "Search Complete", 100 );
		}
	}
	
	public void pageNext() {
		page++ ;
		// We are on the last generated page, start generating the next page.
		if ( page == pages.size() + 1 ) {
			next.setEnabled( false );
			addPage( page );
			view.setProgress( "Searching: " + name, "Search Page " + page + " Complete", 0 );
			synchronized ( search ) {
				search.notify();
			}
		}
		pages.get( page - 1 ).getScrollPane().getVerticalScrollBar().setValue( 0 );
		cl.next( cards );
		pageNum.setText( "Page " + page );
		prev.setEnabled( true );
	}
	
	public void pagePrev() {
		page-- ;
		if ( page == 1 ) prev.setEnabled( false );
		pages.get( page - 1 ).getScrollPane().getVerticalScrollBar().setValue( 0 );
		cl.previous( cards );
		pageNum.setText( "Page " + page );
		next.setEnabled( true );
	}
	
	public JPanel getJPanel( DisplayPanel panel ) {
		return (JPanel) panel.getParent();
	}
	
	public void setSelected( JPanel panel ) {
		if ( getDisplayPanel() != null ) {
			JPanel old = getJPanel( getDisplayPanel() );
			if ( panel == old ) return;
			old.setBorder( BorderFactory.createEmptyBorder() );
		}
		
		panel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createRaisedBevelBorder(),
				BorderFactory.createMatteBorder( 0, 2, 0, 2, Color.RED ) ) );
		setSelected( displayLookup.get( panel ) );
		fireDisplayPanelChanged( currentPanel );
	}
	
	public void setSelected( DisplayPanel panel ) {
		currentPanel = panel;
	}
	
	public DisplayPanel getDisplayPanel() {
		return currentPanel;
	}
	
	public void mouseClicked( MouseEvent e ) {
		setSelected( (JPanel) ( pages.get( page - 1 ).getComponentAt( e.getPoint() ) ) );
	}
	public void clicked( JComponent source ) {
		setSelected( getJPanel( (DisplayPanel) source ) );
	}
	
	public void addDisplayPanelChangedListener( DisplayPanelChangedListener listener ) {
		listenerList.add( DisplayPanelChangedListener.class, listener );
	}
	public void removeDisplayPanelChangedListener( DisplayPanelChangedListener listener ) {
		listenerList.remove( DisplayPanelChangedListener.class, listener );
	}
	public void fireDisplayPanelChanged( DisplayPanel displayPanel ) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == DisplayPanelChangedListener.class ) {
				( (DisplayPanelChangedListener) listeners[i + 1] ).displayPanelChanged( this, displayPanel );
			}
		}
	}
	
	public void stateChanged( boolean editing, JComponent source ) {}
	public void mousePressed( MouseEvent e ) {}
	public void mouseReleased( MouseEvent e ) {}
	public void mouseEntered( MouseEvent e ) {}
	public void mouseExited( MouseEvent e ) {}
	
}
