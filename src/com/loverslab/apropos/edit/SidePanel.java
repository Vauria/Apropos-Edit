package com.loverslab.apropos.edit;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

/**
 * Left Side Panel to hold all buttons and options for interacting with the database or an animation.
 */
@SuppressWarnings("serial")
public class SidePanel extends JPanel {
	
	private View parent;
	private JComboBox<String> animations;
	private ComboBoxModel<String> animationsModel;
	private JComboBox<Position> positions;
	private ComboBoxModel<Position> positionsModel;
	private JCheckBox rapeCheck;
	private JButton simulateButton;
	private boolean simulating;
	private ActionListener listenVerify, listenLoad, listenNWLoad, listenSimulate, listenWrite, listenCopyNew, listenCopyAppend;
	private ItemListener listenFolder;
	
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
		
		// setMinimumSize( new Dimension( animations.getMaximumSize().width, 300 ) );
		// setPreferredSize( new Dimension( animations.getMaximumSize().width, 300 ) );
	}
	
	/**
	 * Adds an Animation to the animations <code>ComboBox</code>.
	 * 
	 * @param str <code>animString</code> to denote the Animation
	 */
	public void publishAnimation( String str ) {
		animations.addItem( str );
	}
	
	/**
	 * @param b Enable the animations box or disable and empty it
	 */
	public void publishingComplete( boolean b ) {
		if ( !b ) animations.removeAllItems();
		animations.setEnabled( b );
	}
	
	/**
	 * Create all the listeners for this panel
	 */
	private void initListeners() {
		listenVerify = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				parent.verifyDatabase();
			}
		};
		listenLoad = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				simulating = false;
				simulateButton.setText( "Simulate" );
				String folder = (String) animations.getSelectedItem();
				String animString = Model.getAnimString( folder, (Position) positions.getSelectedItem(), rapeCheck.isSelected() );
				parent.displayPosition( folder, animString, false );
			}
		};
		listenNWLoad = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String folder = (String) animations.getSelectedItem();
				String animString = Model.getAnimString( folder, (Position) positions.getSelectedItem(), rapeCheck.isSelected() );
				parent.displayPosition( folder, animString, true );
			}
		};
		listenSimulate = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				if ( parent.displayHasLabels() ) {
					simulating = !simulating;
					if ( simulating ) {
						JPanel panel = new JPanel( new GridLayout( 2, 2 ) );
						JTextField activeField = new JTextField( parent.globals.getProperty( "active" ) );
						JTextField primaryField = new JTextField( parent.globals.getProperty( "primary" ) );
						
						panel.add( new JLabel( "Name for Active (Your Partner's Name)" ) );
						panel.add( activeField );
						panel.add( new JLabel( "Name for Primary (Like your PC's Name)" ) );
						panel.add( primaryField );
						
						int result = JOptionPane.showConfirmDialog( parent, panel, "Chose names for {ACTIVE} and {PRIMARY}",
								JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
						
						switch ( result ) {
							case JOptionPane.OK_OPTION:
								simulateButton.setText( "Reset" );
								String active = activeField.getText();
								String primary = primaryField.getText();
								parent.globals.setProperty( "active", active );
								parent.globals.setProperty( "primary", primary );
								parent.simulateLabels( active, primary );
								break;
							default:
								break;
						}
					}
					else {
						simulateButton.setText( "Simulate" );
						parent.deSimLabels();
					}
				}
				else
					parent.handleException( new Exception( "You must load a file before you can Simulate it" ) );
			}
		};
		listenWrite = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				if ( parent.displayHasLabels() )
					parent.writeDisplay();
				else
					parent.handleException( new Exception( "You must load a file before you can write it" ) );
			}
		};
		listenCopyNew = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String newAnim = JOptionPane.showInputDialog( parent,
						new String[] { "Enter the filename for the new position, excluding Stages",
								"E.g. 'FemaleActor_Wolf_Oral', 'FemaleActor_AnubsWolfTest', or 'FemaleActor_NecroDoggy_Rape'" },
						"Copy to New Folder", JOptionPane.QUESTION_MESSAGE );
				if ( newAnim != null && !newAnim.equals( "" ) ) {
					String folder = (String) animations.getSelectedItem();
					String animString = Model.getAnimString( folder, (Position) positions.getSelectedItem(), rapeCheck.isSelected() );
					parent.copyToNew( folder, animString, newAnim );
				}
			}
		};
		listenCopyAppend = new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				
				JComboBox<String> animations = new JComboBox<String>( new DefaultComboBoxModel<String>( new String[ 0 ] ) );
				for ( int i = 0; i < SidePanel.this.animationsModel.getSize(); i++ )
					animations.addItem( SidePanel.this.animations.getItemAt( i ) );
				
				JComboBox<Position> positions = new JComboBox<Position>( new DefaultComboBoxModel<Position>( Position.values() ) );
				
				animations.addItemListener( new ItemListener() {
					public void itemStateChanged( ItemEvent e ) {
						positions.removeAllItems();
						for ( Position position : parent.model.getPositions( (String) animations.getSelectedItem() ) )
							positions.addItem( position );
					}
				});
				
				
				JCheckBox rapeCheck = new JCheckBox( "Rape", false );
				JComponent[] components = new JComponent[] { animations, positions, rapeCheck };
				if ( JOptionPane.showConfirmDialog( parent, components, "Select an Existing Position", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE ) == JOptionPane.OK_OPTION ) {
					String newFolder = (String) animations.getSelectedItem();
					String newAnim = Model.getAnimString( newFolder, (Position) positions.getSelectedItem(), rapeCheck.isSelected() );
					String folder = (String) SidePanel.this.animations.getSelectedItem();
					String animString = Model.getAnimString( folder, (Position) SidePanel.this.positions.getSelectedItem(),
							SidePanel.this.rapeCheck.isSelected() );
					parent.copyAppend( folder, animString, newFolder, newAnim );
				}
			}
		};
		listenFolder = new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED ) {
					positions.removeAllItems();
					for ( Position position : parent.model.getPositions( (String) animations.getSelectedItem() ) )
						positions.addItem( position );
				}
			}
		};
	}
	
	/**
	 * Add interactive options for all the options that affect the entire database
	 */
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
		verifyInfo.setToolTipText( "<html>Reads and then rewrites every JSON file in the selected database<br>"
				+ "so as to ensure uniform formatting across the database in terms of tabs<br>"
				+ "vs spaces, line endings, brackets style and ordering.</html>" );
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
		fixButton.setEnabled( false );
		JLabel fixInfo = new JLabel( "(?)" );
		fixInfo.setToolTipText( "<html>Reads the entire database and attempts to fix simple missing or extra<br>"
				+ "comma errors. Will report any files that could not be automatically fixed, which<br>"
				+ "will need to be repaired manually outside the Apropos Editor</html>" );
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
		brokenSynsButton.setEnabled( false );
		JLabel brokenSynsInfo = new JLabel( "(?)" );
		brokenSynsInfo.setToolTipText( "<html>Displays a list of lines which contain a synonym tag (denoted by <br>"
				+ "a word in all-caps between { and }) that do not have an entry in the <br>" + "synonyms.txt file.</html>" );
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
		suggestSynsButton.setEnabled( false );
		JLabel suggestSynsInfo = new JLabel( "(?)" );
		suggestSynsInfo.setToolTipText( "<html>Reads the entire database and displays every line that contains a word<br>"
				+ "which could be inserted by a synonym tag. Especially useful if you intend<br>"
				+ "to add new tags to an existing database's synonyms file and want existing<br>"
				+ "animation files to take advantage of the new tags. Will take a long time to<br>"
				+ "search the entire database, but will return editable lines as they are found.</html>" );
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
	
	/**
	 * Add interactive options for all the options that only affect one animation
	 */
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
		animations.addItemListener( listenFolder );
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
		loadInfo.setToolTipText( "<html>Loads every stage to be found under the paramaters given by the <br>" + "above options.</html>" );
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
		loadNWInfo.setToolTipText( "<html>Loads every stage to be found under the parameters given by the <br>"
				+ "above options in an external window that opens to the right of the main<br>"
				+ "window, so you can draw inspiration from exisiting files easier</html>" );
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
		
		JButton loadCopyToNew = new JButton( "Copy to New Position" );
		loadCopyToNew.addActionListener( listenCopyNew );
		JLabel loadCopyToNewInfo = new JLabel( "(?)" );
		loadCopyToNewInfo.setToolTipText( "<html>Fetches the files for the animation selected above, and writes<br>"
				+ "to a new folder under the new name given in the dialog window.<br>"
				+ "Will then load the newly writen files immediately ready for editing</html>" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( loadCopyToNew, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( loadCopyToNewInfo, c );
		
		JButton loadCopyToExist = new JButton( "Copy to Existing Position" );
		loadCopyToExist.addActionListener( listenCopyAppend );
		JLabel loadCopyToExistInfo = new JLabel( "(?)" );
		loadCopyToExistInfo.setToolTipText( "<html>Fetches the files for the animation selected above, and adds<br>"
				+ "every line on top of the existing lines for the second animation<br>"
				+ "found by the dialog window. Will then load the newly extended files.</html>" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( loadCopyToExist, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( loadCopyToExistInfo, c );
		
		simulateButton = new JButton( "Simulate" );
		simulateButton.addActionListener( listenSimulate );
		JLabel simulateInfo = new JLabel( "(?)" );
		simulateInfo.setToolTipText( "<html>Replaces all the {TAGS} in the open lines with randomly selected <br>"
				+ "Synonyms and the Active and Primary names you provide, then highlights<br>"
				+ "one line at random from each perspective, simulating how lines could be<br>" + "chosen in-game.</html>" );
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
		writeInfo.setToolTipText(
				"<html>Writes the file loaded in the right display area to the database,<br>" + "overwriting the existing files.</html>" );
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