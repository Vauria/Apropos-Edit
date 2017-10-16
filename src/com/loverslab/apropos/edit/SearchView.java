package com.loverslab.apropos.edit;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

public class SearchView implements DisplayPanelContainer {
	
	int page;
	JPanel main, cards, currentpage;
	CardLayout cl;
	GridBagConstraints c;
	JButton prev, next;
	JLabel pagenum;
	private View parent;
	
	public SearchView( View parent ) {
		this.parent = parent;
		initPanels();
	}
	
	private void initPanels() {
		main = new JPanel( new BorderLayout() );
		
		JPanel buttons = new JPanel( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		prev = new JButton( "Previous" );
		next = new JButton( "Next" );
		pagenum = new JLabel( "Page 1" );
		pagenum.setHorizontalAlignment( SwingConstants.CENTER );
		
		c.anchor = GridBagConstraints.CENTER;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 1;
		buttons.add( prev, c );
		c.gridx++ ;
		buttons.add( pagenum, c );
		c.gridx++ ;
		buttons.add( next, c );
		
		cl = new CardLayout( 0, 0 );
		cards = new JPanel( cl );
		
		addPage( 1 );
		main.add( buttons, BorderLayout.SOUTH );
		main.add( cards, BorderLayout.CENTER );
	}
	
	private void addPage( int p ) {
		currentpage = new ScrollSavvyPanel( new GridBagLayout() );
		JScrollPane scroll = new JScrollPane( currentpage, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		c = new GridBagConstraints();
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		
		cards.add( scroll, "PAGE-" + p );
		page = p;
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
		con.insets = new Insets( 3, 0, 2, 0 );
		container.add( name, con );
		
		DisplayPanel display = new DisplayPanel( parent, true );
		display.load( map, false );
		con.fill = GridBagConstraints.BOTH;
		con.weighty = 1.0;
		con.insets = new Insets( 0, 0, 0, 0 );
		con.gridy++ ;
		container.add( display, con );
		
		c.gridy++ ;
		currentpage.add( container, c );
		currentpage.revalidate();
	}
	
	public DisplayPanel getDisplayPanel() {
		return null;
	}
	
}
