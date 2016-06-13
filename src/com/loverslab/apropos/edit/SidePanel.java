package com.loverslab.apropos.edit;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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
	
	public SidePanel( View parent ) {
		super( true );
		this.parent = parent;
		
		setLayout( new GridBagLayout() );
		GridBagConstraints c = new GridBagConstraints();
		
		addDBWide( c );
		addAnimLocal( c );
		
		c.insets = new Insets( 0, 3, 0, 3 );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridwidth = 2;
		c.weighty = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( new JSeparator(), c );
		
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
		
		JButton verifyButton = new JButton( "Verify Database" );
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
		
		animationsModel = new DefaultComboBoxModel<String>( new String[] { "FemaleActor", "FemaleActor_Male",
				"FemaleActor_DarkInvestigationsDoggystyle", "FemaleActor_PlaceHolder" } );
		animations = new JComboBox<String>( animationsModel );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets( 0, 3, 0, 3 );
		c.gridy++ ;
		add( animations, c );
		
		positionsModel = new DefaultComboBoxModel<Position>( Position.values() );
		positions = new JComboBox<Position>( positionsModel );
		JCheckBox rapeCheck = new JCheckBox( "Rape", false );
		c.insets = new Insets( 0, 3, 0, 0 );
		c.gridwidth = 1;
		c.gridy++ ;
		add( positions, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 0, 0, 0, 0 );
		c.weightx = 0;
		c.gridx++ ;
		add( rapeCheck, c );
		
		c.gridwidth = 1;
		
		JButton loadButton = new JButton( "Load" );
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
		
		JButton simulateButton = new JButton( "Simulate" );
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
	}
	
	public enum Position {
		Anal, BoobJob, Fisting, FootJob, GangBang, HandJob, MMF, Oral, Vaginal, Unique
	}
	
}