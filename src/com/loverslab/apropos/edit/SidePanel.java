package com.loverslab.apropos.edit;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

@SuppressWarnings("serial")
public class SidePanel extends JPanel {
	
	private View parent;
	private JComboBox<String> animations;
	private ComboBoxModel<String> animationsModel;
	private JComboBox<Position> positions;
	private ComboBoxModel<Position> positionsModel;
	private JCheckBox rapeCheck;
	private ActionListener listenVerify, listenLoad, listenNWLoad, listenSimulate, listenWrite;
	
	public SidePanel( View parent ) {
		super( true );
		this.parent = parent;
		
		setLayout( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		
		initListeners();
		addDBWide( c );
		addAnimLocal( c );
		
		c.insets = new Insets( 0, 3, 0, 3 );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridwidth = 2;
		c.weighty = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( new JSeparator(), c );
		
		//setMinimumSize( new Dimension( animations.getMaximumSize().width, 300 ) );
		//setPreferredSize( new Dimension( animations.getMaximumSize().width, 300 ) );
	}
	
	public void publishAnimation( String str ) {
		animations.addItem( str );
	}
	
	public void publishingComplete( boolean b ) {
		if(!b) animations.removeAllItems();
		animations.setEnabled( b );
	}
	
	private void initListeners() {
		listenVerify = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				parent.verifyDatabase();
			}
		};
		
		listenLoad = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String folder = (String) animations.getSelectedItem();
				String animString = Model.getAnimString( folder, (Position) positions.getSelectedItem(),
						rapeCheck.isSelected() );
				parent.displayPosition( folder, animString, false );
			}
		};
		
		listenNWLoad = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String folder = (String) animations.getSelectedItem();
				String animString = Model.getAnimString( folder, (Position) positions.getSelectedItem(),
						rapeCheck.isSelected() );
				parent.displayPosition( folder, animString, true );
			}
		};
		
		listenSimulate = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				System.out.println( "Yeah, I'll get right on it" );
			}
		};
		
		listenWrite = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				parent.writeDisplay();
			}
		};
	}
	
	private void addDBWide( GridBagConstraints c ) {
		JLabel dbWide = new JLabel( "Database Wide" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets( 3, 2, 0, 5 );
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.weightx = 1.0;
		add( dbWide, c );
		
		c.insets = new Insets( 0, 3, 0, 3 );
		c.weighty = 0;
		c.gridy++ ;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( new JSeparator( JSeparator.HORIZONTAL ), c );
		
		Insets insButton = new Insets( 0, 5, 0, 0 );
		Insets insHelp = new Insets( 0, 3, 0, 5 );
		
		JButton verifyButton = new JButton( "Unify Formating" );
		verifyButton.addActionListener( listenVerify );
		JLabel verifyInfo = new JLabel( "(?)" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.gridy++ ;
		add( verifyButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( verifyInfo, c );
		
		JButton fixButton = new JButton( "Fix Comma Errors" );
		JLabel fixInfo = new JLabel( "(?)" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1.0;
		c.gridy++ ;
		c.gridx = 0;
		add( fixButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( fixInfo, c );
		
		JButton brokenSynsButton = new JButton( "List Broken Synonyms" );
		JLabel brokenSynsInfo = new JLabel( "(?)" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1.0;
		c.gridy++ ;
		c.gridx = 0;
		add( brokenSynsButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( brokenSynsInfo, c );
		
		JButton suggestSynsButton = new JButton( "Suggest Synonyms" );
		JLabel suggestSynsInfo = new JLabel( "(?)" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1.0;
		c.gridy++ ;
		c.gridx = 0;
		add( suggestSynsButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( suggestSynsInfo, c );
	}
	
	private void addAnimLocal( GridBagConstraints c ) {
		JLabel animLocal = new JLabel( "Animation Options" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets( 3, 2, 0, 5 );
		c.gridx = 0;
		c.gridy++ ;
		c.gridwidth = 2;
		c.weightx = 1.0;
		add( animLocal, c );
		
		c.insets = new Insets( 0, 3, 0, 3 );
		c.weighty = 0;
		c.gridy++ ;
		c.fill = GridBagConstraints.HORIZONTAL;
		add( new JSeparator( JSeparator.HORIZONTAL ), c );
		
		Insets insButton = new Insets( 0, 5, 0, 0 );
		Insets insHelp = new Insets( 0, 3, 0, 5 );
		
		animationsModel = new DefaultComboBoxModel<String>( new String[ 0 ] );
		animations = new JComboBox<String>( animationsModel );
		animations.setEnabled( false );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets( 0, 3, 0, 3 );
		c.gridy++ ;
		add( animations, c );
		
		positionsModel = new DefaultComboBoxModel<Position>( Position.values() );
		positions = new JComboBox<Position>( positionsModel );
		rapeCheck = new JCheckBox( "Rape", false );
		c.insets = new Insets( 0, 3, 0, 0 );
		c.gridwidth = 1;
		c.gridy++ ;
		add( positions, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 0, 0, 0, 0 );
		c.weightx = 0;
		c.gridx++ ;
		add( rapeCheck, c );
		
		JButton loadButton = new JButton( "Load" );
		loadButton.addActionListener( listenLoad );
		JLabel loadInfo = new JLabel( "(?)" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( loadButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( loadInfo, c );
		
		JButton loadNWButton = new JButton( "Load in New Window" );
		loadNWButton.addActionListener( listenNWLoad );
		JLabel loadNWInfo = new JLabel( "(?)" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( loadNWButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( loadNWInfo, c );
		
		JButton simulateButton = new JButton( "Simulate" );
		simulateButton.addActionListener( listenSimulate );
		JLabel simulateInfo = new JLabel( "(?)" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( simulateButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( simulateInfo, c );
		
		JButton writeButton = new JButton( "Write" );
		writeButton.addActionListener( listenWrite );
		JLabel writeInfo = new JLabel( "(?)" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( writeButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( writeInfo, c );
	}
	
}