package com.loverslab.apropos.edit;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.loverslab.apropos.edit.Model.FileFilterSearchTerms;
import com.loverslab.apropos.edit.Model.RegexUserSearchTerms;
import com.loverslab.apropos.edit.Model.SimpleUserSearchTerms;
import com.loverslab.apropos.edit.Model.UserSearchTerms;
import com.loverslab.apropos.edit.Model.WWordUserSearchTerms;

import ernieyu.slider.RangeSlider;

/**
 * Left Side Panel to hold all buttons and options for interacting with the database or an animation.
 */
@SuppressWarnings("serial")
public class SidePanel extends JPanel implements DisplayPanelChangedListener {
	
	private View parent;
	private JComboBox<String> animations;
	private ComboBoxModel<String> animationsModel;
	private JComboBox<Position> positions;
	private ComboBoxModel<Position> positionsModel;
	private JCheckBox rapeCheck;
	private JButton simulateButton, duplicatesButton;
	private boolean simulating = false, conflicts = false;
	private AbstractAction listenVerify, listenSynonyms, listenSearch, listenSpecialSearch, listenLoad, listenNWLoad, listenSimulate,
			listenWrite, listenDuplicates, listenCopyNew, listenCopyAppend;
	private ItemListener listenFolder, listenPosition;
	
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
	
	public void setSelectedAnim( String str ) {
		animations.setSelectedItem( str );
	}
	
	public void setSelectedAnim( AproposLabel label ) {
		label = label.getDepth() > 2 ? label.getParentLabel( 2 ) : label;
		String folder = label.getParentLabel().getText();
		String anim = label.getText();
		animations.setSelectedItem( folder );
		positions.setSelectedItem( parent.model.getPosition( folder, anim ) );
		rapeCheck.setSelected( anim.contains( "_Rape" ) );
	}
	
	public void resetButtons() {
		simulating = false;
		simulateButton.setText( "Simulate" );
		conflicts = false;
		duplicatesButton.setText( "Find Duplicates" );
	}
	
	public void setConflicted( boolean b ) {
		resetButtons();
		conflicts = b;
		parent.deSimLabels();
		if ( b ) duplicatesButton.setText( "Resolve Conflicts" );
	}
	
	public void displayPanelChanged( DisplayPanelChangedNotifier parent, DisplayPanel panel ) {
		resetButtons();
		if ( panel != null && panel.stageMap != null && panel.stageMap.size() != 0 ) {
			if ( panel.stageMap.isConflicted() ) {
				conflicts = true;
				duplicatesButton.setText( "Resolve Conflicts" );
			}
			if ( panel.stageMap.firstLine().getSimulateState() == true ) {
				simulating = true;
				simulateButton.setText( "Reset" );
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	public boolean selectionIsValid( JComboBox a, JComboBox b ) {
		return a.getSelectedIndex() != -1 & b.getSelectedIndex() != -1;
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
		
		JButton synonymsButton = new JButton( "Synonyms Editor" );
		synonymsButton.addActionListener( listenSynonyms );
		JLabel synonymsInfo = new JLabel( "(?)" );
		synonymsInfo.setToolTipText( "<html>Opens a separate window that allows you to view and edit this database's<br>"
				+ "three synonyms files, Synonyms.txt, Arousal_Descriptors.txt and<br>" + "WearAndTear_Descriptors.txt</html>" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1.0;
		c.gridy++ ;
		c.gridx = 0;
		add( synonymsButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( synonymsInfo, c );
		
		JButton searchButton = new JButton( "Database Search" );
		searchButton.addActionListener( listenSearch );
		JLabel searchInfo = new JLabel( "(?)" );
		searchInfo.setToolTipText( "<html>Opens a separate window that allows you to view and edit this database's<br>"
				+ "three synonyms files, Synonyms.txt, Arousal_Descriptors.txt and<br>" + "WearAndTear_Descriptors.txt</html>" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1.0;
		c.gridy++ ;
		c.gridx = 0;
		add( searchButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( searchInfo, c );
		
		JButton brokenSynsButton = new JButton( "Special Searches" );
		brokenSynsButton.addActionListener( listenSpecialSearch );
		JLabel brokenSynsInfo = new JLabel( "(?)" );
		brokenSynsInfo.setToolTipText( "<html>Provides access to a number of custom searches, designed to<br>"
				+ "aid in maintaining and cleaning up your database.</html>" );
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
		animations = new JComboBox<String>( animationsModel ) {
			// Change methods so folders are stored internally with a shorthand
			public void addItem( String item ) {
				super.addItem( Model.shorten( item ) );
			}
			public Object getSelectedItem() {
				return Model.expand( (String) super.getSelectedItem() );
			}
			public String getItemAt( int index ) {
				return Model.expand( super.getItemAt( index ) );
			}
			public void setSelectedItem( Object anObject ) {
				super.setSelectedItem( Model.shorten( (String) anObject ) );
			}
		};
		animations.addItemListener( listenFolder );
		animations.setEnabled( false );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = new Insets( 0, 3, 0, 3 );
		c.gridy++ ;
		add( animations, c );
		
		positionsModel = new DefaultComboBoxModel<Position>( Position.values() );
		positions = new JComboBox<Position>( positionsModel );
		positions.addItemListener( listenPosition );
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
		
		JButton loadButton = new JButton( "Open" );
		loadButton.setToolTipText( "CTRL + O" );
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
		
		JButton loadNWButton = new JButton( "Open in New Window" );
		loadNWButton.setToolTipText( "CTRL + SHIFT + O" );
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
		loadCopyToNew.setToolTipText( "CTRL + N" );
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
		loadCopyToExist.setToolTipText( "CTRL + SHIFT + N" );
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
		simulateButton.setToolTipText( "CTRL + R (CTRL + SHIFT + R to skip dialog)" );
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
		
		JButton writeButton = new JButton( "Save" );
		writeButton.setToolTipText( "CTRL + S" );
		writeButton.addActionListener( listenWrite );
		JLabel writeInfo = new JLabel( "(?)" );
		writeInfo.setToolTipText(
				"<html>Saves the file loaded in the right display area to the database,<br>" + "overwriting the existing files.</html>" );
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
		
		duplicatesButton = new JButton( "Find Duplicates" );
		duplicatesButton.addActionListener( listenDuplicates );
		JLabel duplicatesInfo = new JLabel( "(?)" );
		duplicatesInfo.setToolTipText(
				"<html>Shows all lines that may be duplicates of another, letting you chose<br>" + "which ones you want to keep.</html>" );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.insets = insButton;
		c.weightx = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( duplicatesButton, c );
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = insHelp;
		c.weightx = 0;
		c.gridx++ ;
		add( duplicatesInfo, c );
	}
	
	/**
	 * Create all the listeners for this panel
	 */
	private void initListeners() {
		listenVerify = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				int result = JOptionPane.showOptionDialog( parent,
						"Do you want to sort the individual lines in each file alphabetically?\nDoing so allows for easier database merging, but the 1st and 2nd person lines will no longer have the same order.",
						"Enable Line Sorting?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						new String[] { "Sort", "Don't Sort" }, "Don't Sort" );
				switch ( result ) {
					case JOptionPane.OK_OPTION:
						parent.verifyDatabase( true );
						break;
					case JOptionPane.NO_OPTION:
						parent.verifyDatabase( false );
						break;
					default:
						break;
				}
				
			}
		};
		listenSynonyms = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				parent.openSynonymsEditor();
			}
		};
		listenSearch = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				SearchDialog dialog = new SearchDialog( parent );
				dialog.show();
			}
		};
		listenSpecialSearch = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				SpecialSearchDialog dialog = new SpecialSearchDialog( parent );
				dialog.show();
			}
		};
		listenLoad = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				if ( !selectionIsValid( animations, positions ) ) {
					parent.handleException( new Exception( "The Selection is invalid, try a different Folder/Position Combination" ) );
					return;
				}
				resetButtons();
				String folder = (String) animations.getSelectedItem();
				String animString = Model.getAnimString( folder, (Position) positions.getSelectedItem(), rapeCheck.isSelected() );
				parent.displayPosition( folder, animString, false );
			}
		};
		listenNWLoad = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				if ( !selectionIsValid( animations, positions ) ) {
					parent.handleException( new Exception( "The Selection is invalid, try a different Folder/Position Combination" ) );
					return;
				}
				String folder = (String) animations.getSelectedItem();
				String animString = Model.getAnimString( folder, (Position) positions.getSelectedItem(), rapeCheck.isSelected() );
				parent.displayPosition( folder, animString, true );
			}
		};
		listenSimulate = new AbstractAction() {
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
		listenDuplicates = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				if ( parent.displayHasLabels() ) {
					boolean c = !conflicts;
					resetButtons();
					conflicts = c;
					parent.deSimLabels();
					if ( conflicts ) {
						if ( parent.checkDuplicates( parent.getDisplayPanel() ) )
							duplicatesButton.setText( "Resolve Conflicts" );
						else {
							conflicts = false;
							parent.handleException( new Information( "No Duplicates Found" ) );
						}
					}
					else {
						parent.resolveConflicts( parent.getDisplayPanel() );
					}
				}
				else
					parent.handleException( new Exception( "You must load a file before you can check it for duplicates" ) );
			}
		};
		listenWrite = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				if ( parent.displayHasLabels() )
					parent.writeDisplay();
				else
					parent.handleException( new Exception( "You must load a file before you can write it" ) );
			}
		};
		listenCopyNew = new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				if ( !selectionIsValid( animations, positions ) ) {
					parent.handleException( new Exception( "The Selection is invalid, try a different Folder/Position Combination" ) );
					return;
				}
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
		listenCopyAppend = new AbstractAction() {
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
				} );
				
				JCheckBox rapeCheck = new JCheckBox( "Rape", false );
				
				positions.addItemListener( new ItemListener() {
					public void itemStateChanged( ItemEvent e ) {
						if ( e.getStateChange() == ItemEvent.SELECTED ) {
							String folder = (String) animations.getSelectedItem();
							String animString = Model.getAnimString( folder, (Position) positions.getSelectedItem(), false );
							int rape = parent.model.hasRape( folder, animString );
							rapeCheck.setSelected( rape == 2 );
							rapeCheck.setEnabled( rape > 2 || rape < 1 );
						}
					}
				} );
				
				JComponent[] components = new JComponent[] { animations, positions, rapeCheck };
				if ( JOptionPane.showConfirmDialog( parent, components, "Select an Existing Position", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE ) == JOptionPane.OK_OPTION ) {
					if ( !selectionIsValid( animations, positions ) ) {
						parent.handleException( new Exception( "The Selection is invalid, try a different Folder/Position Combination" ) );
						return;
					}
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
		listenPosition = new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED ) {
					String folder = (String) animations.getSelectedItem();
					String animString = Model.getAnimString( folder, (Position) positions.getSelectedItem(), false );
					int rape = parent.model.hasRape( folder, animString );
					rapeCheck.setSelected( rape == 2 );
					rapeCheck.setEnabled( rape > 2 || rape < 1 );
				}
			}
			
		};
	}
	
	public void registerKeybinds( InputMap input, ActionMap action ) {
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK, true ), "SYNONYMS" );
		action.put( "SYNONYMS", listenSynonyms );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK, true ), "OPEN" );
		action.put( "OPEN", listenLoad );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, true ), "OPENNW" );
		action.put( "OPENNW", listenNWLoad );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK, true ), "COPYNEW" );
		action.put( "COPYNEW", listenCopyNew );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, true ), "COPYAPPEND" );
		action.put( "COPYAPPEND", listenCopyAppend );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, true ), "SAVE" );
		action.put( "SAVE", listenWrite );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK, true ), "SIMULATE" );
		action.put( "SIMULATE", listenSimulate );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, true ), "SIMULATESKIP" );
		action.put( "SIMULATESKIP", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				if ( parent.displayHasLabels() ) {
					simulating = !simulating;
					if ( simulating ) {
						simulateButton.setText( "Reset" );
						parent.simulateLabels( parent.globals.getProperty( "active" ), parent.globals.getProperty( "primary" ) );
					}
					else {
						simulateButton.setText( "Simulate" );
						parent.deSimLabels();
					}
				}
				else
					parent.handleException( new Exception( "You must load a file before you can Simulate it" ) );
			}
		} );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK, true ), "GRABFOLDER" );
		action.put( "GRABFOLDER", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				animations.grabFocus();
			}
		} );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK, true ), "GRABPOSITION" );
		action.put( "GRABPOSITION", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				positions.grabFocus();
			}
		} );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK, true ), "GRABRAPE" );
		action.put( "GRABRAPE", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				rapeCheck.grabFocus();
			}
		} );
		input.put( KeyStroke.getKeyStroke( KeyEvent.VK_F5, 0, true ), "REFRESH" );
		action.put( "REFRESH", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				if ( !parent.displayHasLabels() ) return;
				StageMap map = parent.getDisplayPanel().stageMap;
				setSelectedAnim( map.firstKey() );
			}
		} );
	}
	
}

@SuppressWarnings("serial")
class SearchDialog extends AbstractAction implements ItemListener, DocumentListener, ChangeListener {
	
	private View parent;
	private UserSearchTerms terms;
	private JFrame frame;
	private JComboBox<UserSearchTerms> searchHistory;
	private JTextField searchField, filterPath;
	private JLabel searchDescription, filterStageLabel;
	private JRadioButton searchModeSimple, searchModeWWord, searchModeRegex;
	private JCheckBox caseSens;
	private JCheckBox filterPersp1, filterPersp2, filterPersp3;
	private JRadioButton filterNoRape, filterOnlyRape, filterRapeBoth;
	private RangeSlider filterStageLimits;
	private static String[] filterStageValues = Model.UserSearchTerms.stageValues;
	private static String[] searchDescriptions = new String[] { "Searches for the literal string given",
			"Searches for words, | seperating them and * as wildcard", "Provides the full power of Java's Pattern engine" };
	
	public SearchDialog( View parent ) {
		this.parent = parent;
		
		frame = new JFrame( "Database Search" );
		
		JPanel panel = new JPanel( new BorderLayout() ), termsPanel = new JPanel( new GridBagLayout() ),
				filterPanel = new JPanel( new GridBagLayout() );
		termsPanel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 10, 5, 2, 5 ),
				BorderFactory.createRaisedBevelBorder() ) );
		filterPanel.setBorder( BorderFactory.createEmptyBorder( 0, 5, 20, 5 ) );
		
		GridBagConstraints c = new GridBagConstraints();
		
		searchHistory = new JComboBox<UserSearchTerms>();
		searchHistory.setEditable( true );
		searchHistory.addItemListener( this::comboStageChanged );
		searchField = ( (JTextField) searchHistory.getEditor().getEditorComponent() );
		searchField.addActionListener( this );
		searchDescription = new JLabel();
		searchDescription.setBorder( BorderFactory.createEmptyBorder( 1, 1, 1, 1 ) );
		searchModeSimple = new JRadioButton( "Simple" );
		searchModeSimple.addItemListener( this );
		searchModeWWord = new JRadioButton( "Whole Word" );
		searchModeWWord.addItemListener( this );
		searchModeRegex = new JRadioButton( "Regex" );
		searchModeRegex.addItemListener( this::regexStateChanged );
		searchModeRegex.addItemListener( this );
		ButtonGroup searchBG = new ButtonGroup();
		searchBG.add( searchModeSimple );
		searchBG.add( searchModeWWord );
		searchBG.add( searchModeRegex );
		caseSens = new JCheckBox( "Case Sensitive", true );
		searchModeSimple.setSelected( true );
		
		filterPath = new JTextField();
		filterPath.addActionListener( this );
		JTextArea pathFilterDesc = new JTextArea();
		pathFilterDesc.setText( "If text is entered here, animation names and folders will be checked before"
				+ " opening, allowing you to limit the search to a subset of files, like a new unique animation"
				+ " or just FemaleActor_Male, for example." );
		pathFilterDesc.setEditable( false );
		pathFilterDesc.setFont( searchDescription.getFont() );
		pathFilterDesc.setBackground( searchDescription.getBackground() );
		pathFilterDesc.setWrapStyleWord( true );
		pathFilterDesc.setLineWrap( true );
		pathFilterDesc.setRows( 4 );
		filterPersp1 = new JCheckBox( "1st Person", true );
		filterPersp2 = new JCheckBox( "2nd Person", true );
		filterPersp3 = new JCheckBox( "3rd Person", true );
		filterNoRape = new JRadioButton( "No Rape" );
		filterOnlyRape = new JRadioButton( "Only Rape" );
		filterRapeBoth = new JRadioButton( "Both" );
		ButtonGroup rapeBG = new ButtonGroup();
		rapeBG.add( filterNoRape );
		rapeBG.add( filterOnlyRape );
		rapeBG.add( filterRapeBoth );
		filterRapeBoth.setSelected( true );
		filterStageLabel = new JLabel();
		filterStageLabel.setHorizontalAlignment( JLabel.CENTER );
		filterStageLimits = new RangeSlider( 0, 6 );
		filterStageLimits.setValue( 0 );
		filterStageLimits.addChangeListener( this ); // Add the listener between set events so it only fires once c:
		filterStageLimits.setUpperValue( 6 );
		
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridwidth = 4;
		c.gridy = 0;
		c.gridx = 0;
		termsPanel.add( searchHistory, c );
		c.gridy++ ;
		termsPanel.add( searchDescription, c );
		c.gridy++ ;
		c.gridwidth = 1;
		termsPanel.add( searchModeSimple, c );
		c.gridx++ ;
		termsPanel.add( searchModeWWord, c );
		c.gridx++ ;
		termsPanel.add( searchModeRegex, c );
		c.gridx++ ;
		termsPanel.add( caseSens, c );
		
		c = new GridBagConstraints();
		
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 4;
		filterPanel.add( pathFilterDesc, c );
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 0;
		c.weightx = 1.0;
		c.gridy++ ;
		filterPanel.add( filterPath, c );
		c.weightx = 0;
		c.gridwidth = 1;
		c.gridy++ ;
		filterPanel.add( filterPersp1, c );
		c.gridy++ ;
		filterPanel.add( filterPersp2, c );
		c.gridy++ ;
		filterPanel.add( filterPersp3, c );
		c.gridy = 2;
		c.gridx++ ;
		filterPanel.add( filterNoRape, c );
		c.gridy++ ;
		filterPanel.add( filterOnlyRape, c );
		c.gridy++ ;
		filterPanel.add( filterRapeBoth, c );
		c.insets = new Insets( 1, 0, 0, 0 );
		c.anchor = GridBagConstraints.CENTER;
		c.gridy = 2;
		c.gridx++ ;
		c.gridwidth = 2;
		c.weightx = 1.0;
		filterPanel.add( filterStageLimits, c );
		c.insets = new Insets( 0, 0, 0, 0 );
		c.gridy++ ;
		filterPanel.add( filterStageLabel, c );
		
		panel.add( termsPanel, BorderLayout.NORTH );
		panel.add( filterPanel, BorderLayout.WEST );
		
		if ( parent.searchHistory.size() > 0 ) {
			Iterator<UserSearchTerms> iterator = parent.searchHistory.iterator();
			while ( iterator.hasNext() )
				searchHistory.addItem( iterator.next() );
			searchHistory.setSelectedIndex( 0 );
		}
		
		frame.getRootPane().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW )
				.put( KeyStroke.getKeyStroke( KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK, true ), "CLOSE" );
		frame.getRootPane().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0, true ),
				"CLOSE" );
		frame.getRootPane().getActionMap().put( "CLOSE", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ) );
			}
		} );
		frame.getRootPane().getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0, true ),
				"CONFIRM" );
		frame.getRootPane().getActionMap().put( "CONFIRM", this );
		
		frame.setContentPane( panel );
	}
	
	public void show() {
		// frame.setSize( 600, 300 );
		frame.pack();
		frame.setLocationRelativeTo( parent );
		frame.setVisible( true );
	}
	
	public void actionPerformed( ActionEvent e ) {
		if ( searchModeRegex.isSelected() && searchDescription.getForeground().equals( Color.RED ) ) {
			final Timer timer = new Timer( 70, null );
			timer.addActionListener( new ActionListener() {
				int flashed = 0;
				
				public void actionPerformed( ActionEvent e ) {
					flashed++ ;
					if ( flashed % 2 == 1 )
						searchDescription.setBorder( BorderFactory.createMatteBorder( 1, 1, 1, 1, Color.RED ) );
					else
						searchDescription.setBorder( BorderFactory.createEmptyBorder( 1, 1, 1, 1 ) );
					if ( flashed == 6 ) timer.stop();
				}
			} );
			timer.start();
			return;
		}
		frame.setVisible( false );
		terms = getTerms();
		parent.searchHistory.addFirst( terms );
		Set<UserSearchTerms> set = new HashSet<UserSearchTerms>();
		Iterator<UserSearchTerms> it = parent.searchHistory.iterator();
		while ( it.hasNext() ) {
			UserSearchTerms next = it.next();
			if ( !set.add( next ) ) it.remove();
		}
		while ( parent.searchHistory.size() > 10 )
			parent.searchHistory.removeLast();
		parent.startSearch( terms );
		frame.dispose();
	}
	
	public void itemStateChanged( ItemEvent e ) {
		if ( e.getStateChange() == ItemEvent.SELECTED ) {
			int searchMode = searchModeSimple.isSelected() ? 0 : ( searchModeWWord.isSelected() ? 1 : 2 );
			searchDescription.setText( searchDescriptions[searchMode] );
			
		}
	}
	/**
	 * Regex Selected Listener
	 */
	public void regexStateChanged( ItemEvent e ) {
		switch ( e.getStateChange() ) {
			case ItemEvent.SELECTED:
				searchField.getDocument().addDocumentListener( this );
				fieldChanged();
				break;
			case ItemEvent.DESELECTED:
				searchField.getDocument().removeDocumentListener( this );
				searchDescription.setForeground( Color.BLACK );
				break;
		}
	}
	/**
	 * Combo Box Selected Listener
	 */
	public void comboStageChanged( ItemEvent e ) {
		if ( e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof UserSearchTerms ) {
			SwingUtilities.invokeLater( () -> { // Resolve these events first otherwise default behaviour overwrites resetting searchField
				setState( (UserSearchTerms) e.getItem() );
			} );
		}
	}
	
	public void fieldChanged() {
		String regex = searchField.getText();
		try {
			Pattern.compile( regex );
			searchDescription.setText( searchDescriptions[2] );
			searchDescription.setForeground( Color.BLACK );
		}
		catch ( PatternSyntaxException e ) {
			String error = e.getLocalizedMessage();
			searchDescription.setText( error );
			searchDescription.setForeground( Color.RED );
		}
	}
	
	public void insertUpdate( DocumentEvent e ) {
		fieldChanged();
	}
	public void removeUpdate( DocumentEvent e ) {
		fieldChanged();
	}
	public void changedUpdate( DocumentEvent e ) {}
	
	public void stateChanged( ChangeEvent e ) {
		filterStageLabel
				.setText( filterStageValues[filterStageLimits.getValue()] + " - " + filterStageValues[filterStageLimits.getUpperValue()] );
	}
	
	private void setState( UserSearchTerms terms ) {
		searchField.setText( terms.search );
		switch ( terms.searchMode ) {
			case 0:
				searchModeSimple.setSelected( true );
				break;
			case 1:
				searchModeWWord.setSelected( true );
				break;
			case 2:
				searchModeRegex.setSelected( true );
				break;
		}
		caseSens.setSelected( terms.caseSens );
		filterPath.setText( terms.pathSub );
		filterPersp1.setSelected( terms.first );
		filterPersp2.setSelected( terms.second );
		filterPersp3.setSelected( terms.third );
		switch ( terms.rapeMode ) {
			case 0:
				filterNoRape.setSelected( true );
				break;
			case 1:
				filterOnlyRape.setSelected( true );
				break;
			case 2:
				filterRapeBoth.setSelected( true );
				break;
		}
		filterStageLimits.setValue( terms.lowerBoundInt );
		filterStageLimits.setUpperValue( terms.upperBoundInt );
	}
	
	private UserSearchTerms getTerms() {
		int searchMode = searchModeSimple.isSelected() ? 0 : ( searchModeWWord.isSelected() ? 1 : 2 );
		
		UserSearchTerms terms = null;
		switch ( searchMode ) {
			case 0:
				terms = new SimpleUserSearchTerms();
				break;
			case 1:
				terms = new WWordUserSearchTerms();
				break;
			case 2:
				terms = new RegexUserSearchTerms();
				break;
		}
		terms.caseSens = caseSens.isSelected();
		terms.setSearchString( searchField.getText() );
		terms.setPathSub( filterPath.getText() );
		terms.setStages( filterStageLimits.getValue(), filterStageLimits.getUpperValue() );
		terms.setRapes( filterNoRape.isSelected(), filterOnlyRape.isSelected(), filterRapeBoth.isSelected() );
		terms.setPerspectives( filterPersp1.isSelected(), filterPersp2.isSelected(), filterPersp3.isSelected() );
		terms.generateName();
		
		return terms;
	}
	
}

@SuppressWarnings("serial")
class SpecialSearchDialog extends AbstractAction implements DocumentListener {
	
	View view;
	JFrame frame;
	JTabbedPane tabbedPane;
	JSlider lengthSlider;
	JTextField pathFilterField, synsInputField;
	JLabel synsTagCount;
	
	public SpecialSearchDialog( View parent ) {
		view = parent;
		frame = new JFrame( "Special Search" );
		JPanel panel = new JPanel( new BorderLayout() );
		tabbedPane = new JTabbedPane();
		tabbedPane.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 10, 5, 2, 5 ),
				BorderFactory.createRaisedBevelBorder() ) );
		
		GridBagConstraints c;
		JLabel description;
		
		JPanel brokenSyns = new JPanel( new GridBagLayout() );
		description = new JLabel();
		description.setText( "<html>Searches for anything suspected to be a synonym tag, but isn't functional "
				+ "either due to missing brackets, typos, or a missing definition for that "
				+ "synonym. Will attempt to suggest a correct replacement.</html>" );
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets( 3, 3, 3, 3 );
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 1;
		c.gridy = 0;
		c.gridx = 0;
		brokenSyns.add( description, c );
		tabbedPane.addTab( "Broken Synonyms", brokenSyns );
		
		JPanel suggestSyns = new JPanel( new GridBagLayout() );
		description = new JLabel();
		description.setText( "<html>Searches for any lines that contain a word which could be inserted by a "
				+ "synonym tag. Especially useful if you have added new synonyms and want to "
				+ "update existing files to take advantage of them.</html>" );
		c.gridwidth = 2;
		suggestSyns.add( description, c );
		JLabel synsInputLabel = new JLabel( "<html>Enter any number of {TAGS} separated by '|', like \"{CUMS}|{CUMMING}\"<br>"
				+ "Or leave empty to use every available synonym." );
		c.insets = new Insets( 1, 3, 0, 3 );
		c.gridy++ ;
		suggestSyns.add( synsInputLabel, c );
		synsTagCount = new JLabel( "Valid Tags: " );
		synsTagCount.setBorder( BorderFactory.createEmptyBorder( 1, 1, 1, 1 ) );
		synsTagCount.setMinimumSize( new Dimension( 95, 18 ) );
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets( 2, 3, 2, 2 );
		c.gridwidth = 1;
		c.gridy++ ;
		c.weightx = 0;
		suggestSyns.add( synsTagCount, c );
		synsInputField = new JTextField();
		synsInputField.getDocument().addDocumentListener( this );
		fieldChanged();
		c.insets = new Insets( 1, 0, 1, 3 );
		c.gridx++ ;
		c.weightx = 1;
		suggestSyns.add( synsInputField, c );
		tabbedPane.addTab( "Suggest Synonyms", suggestSyns );
		
		JPanel findDuplicates = new JPanel( new GridBagLayout() );
		description = new JLabel();
		description.setText( "<html>Will check every perspective for lines that are equal or sufficiently "
				+ "similiar, displaying each match so you can decide to keep one, both, or edit them.</html>" );
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets( 3, 3, 3, 3 );
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 1;
		c.gridy = 0;
		c.gridx = 0;
		findDuplicates.add( description, c );
		tabbedPane.addTab( "Find Duplicates", findDuplicates );
		
		JPanel findLongLines = new JPanel( new GridBagLayout() );
		description = new JLabel();
		description.setText( "<html>Finds any line where the percentage chance of it being "
				+ "cut off in-game is greater than the specified percentage. Unless these lines are too long because of the "
				+ "unpredictable {PRIMARY} or {ACTIVE} tags, they should be shortened.</html>" );
		JLabel percentage = new JLabel( "% Chance of Cutoff:" ), value = new JLabel();
		value.setMinimumSize( new Dimension( 50, 10 ) );
		lengthSlider = new JSlider( 0, 100 );
		lengthSlider.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				float per = (float) lengthSlider.getValue() / 100f;
				Color c = new Color( 1f, 1 - per, 0f );
				lengthSlider.setBorder( BorderFactory.createMatteBorder( 0, 1, 1, 5, c ) );
				value.setText( lengthSlider.getValue() + "%" );
			}
		} );
		lengthSlider.setValue( 0 );
		c.gridwidth = 3;
		findLongLines.add( description, c );
		c.anchor = GridBagConstraints.CENTER;
		c.gridwidth = 1;
		c.gridy++ ;
		c.weightx = 0;
		findLongLines.add( percentage, c );
		c.gridx++ ;
		c.weightx = 1;
		c.insets = new Insets( 3, 15, 3, 15 );
		findLongLines.add( lengthSlider, c );
		c.gridx++ ;
		c.weightx = 0;
		c.insets = new Insets( 3, 3, 3, 3 );
		findLongLines.add( value, c );
		tabbedPane.addTab( "Find Long Lines", findLongLines );
		
		JPanel fileFilter = new JPanel( new GridBagLayout() );
		fileFilter.setBorder( BorderFactory.createEmptyBorder( 0, 5, 20, 5 ) );
		description = new JLabel();
		description.setText( "<html>If text is entered here, animation names and folders will be checked before"
				+ " opening, allowing you to limit the search to a subset of files, like a new unique animation"
				+ " or just FemaleActor_Male, for example." );
		pathFilterField = new JTextField();
		JButton start = new JButton( "Start" );
		start.addActionListener( this );
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets( 3, 3, 3, 3 );
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;
		c.gridy = 0;
		c.gridx = 0;
		fileFilter.add( description, c );
		c.gridy++ ;
		fileFilter.add( pathFilterField, c );
		c.weightx = 0;
		c.weighty = 0.6;
		c.fill = GridBagConstraints.VERTICAL;
		c.gridx++ ;
		c.gridy = 0;
		c.gridheight = 2;
		fileFilter.add( start, c );
		
		JRootPane root = frame.getRootPane();
		InputMap inputMap = root.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK, true ), "CLOSE" );
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0, true ), "CLOSE" );
		ActionMap actionMap = root.getActionMap();
		actionMap.put( "CLOSE", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				frame.dispatchEvent( new WindowEvent( frame, WindowEvent.WINDOW_CLOSING ) );
			}
		} );
		inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0, true ), "CONFIRM" );
		actionMap.put( "CONFIRM", this );
		KeyStroke next = KeyStroke.getKeyStroke( "ctrl TAB" ), prev = KeyStroke.getKeyStroke( "ctrl shift TAB" );
		Set<AWTKeyStroke> forwardKeys = new HashSet<AWTKeyStroke>(
				root.getFocusTraversalKeys( KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS ) );
		Set<AWTKeyStroke> backwardKeys = new HashSet<AWTKeyStroke>(
				root.getFocusTraversalKeys( KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS ) );
		forwardKeys.remove( next );
		backwardKeys.remove( prev );
		root.setFocusTraversalKeys( KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardKeys );
		root.setFocusTraversalKeys( KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardKeys );
		inputMap.put( next, "NEXTTAB" );
		actionMap.put( "NEXTTAB", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				int ind = tabbedPane.getSelectedIndex() + 1;
				if ( ind >= tabbedPane.getTabCount() ) ind = 0;
				tabbedPane.setSelectedIndex( ind );
			}
		} );
		inputMap.put( prev, "PREVTAB" );
		actionMap.put( "PREVTAB", new AbstractAction() {
			public void actionPerformed( ActionEvent e ) {
				int ind = tabbedPane.getSelectedIndex() - 1;
				if ( ind < 0 ) ind = tabbedPane.getTabCount() - 1;
				tabbedPane.setSelectedIndex( ind );
			}
		} );
		
		panel.add( tabbedPane, BorderLayout.CENTER );
		panel.add( fileFilter, BorderLayout.SOUTH );
		
		frame.setContentPane( panel );
	}
	
	public void show() {
		frame.setSize( 600, 300 );
		frame.setLocationRelativeTo( view );
		frame.setVisible( true );
	}
	
	public void actionPerformed( ActionEvent e ) {
		FileFilterSearchTerms terms;
		switch ( tabbedPane.getSelectedIndex() ) {
			case 0:
				terms = new Model.BrokenSynonymsFinder( view.model.synonyms );
				break;
			case 1:
				if ( synsTagCount.getForeground().equals( Color.RED ) ) {
					final Timer timer = new Timer( 70, null );
					timer.addActionListener( new ActionListener() {
						int flashed = 0;
						
						public void actionPerformed( ActionEvent e ) {
							flashed++ ;
							if ( flashed % 2 == 1 )
								synsTagCount.setBorder( BorderFactory.createMatteBorder( 1, 1, 1, 1, Color.RED ) );
							else
								synsTagCount.setBorder( BorderFactory.createEmptyBorder( 1, 1, 1, 1 ) );
							if ( flashed == 6 ) timer.stop();
						}
					} );
					timer.start();
					return;
				}
				String input = synsInputField.getText();
				String[] keys = input.length() == 0 ? view.model.synonyms.keySet().toArray( new String[ 0 ] ) : input.split( "\\|" );
				terms = new Model.SynonymsSuggester( view.model.synonyms, keys );
				break;
			case 2:
				terms = new Model.DupeFinder();
				break;
			case 3:
				terms = new Model.LongLineFinder( view.model.synonymsLengths, ( (float) lengthSlider.getValue() ) / 100f );
				break;
			default:
				return;
		}
		if ( !pathFilterField.getText().equals( "" ) ) terms.setPathSub( pathFilterField.getText() );
		frame.setVisible( false );
		view.startSearch( terms );
		frame.dispose();
	}
	
	public void fieldChanged() {
		String input = synsInputField.getText();
		SynonymsMap synonyms = view.model.synonyms;
		int count = 0;
		boolean error = false;
		if ( input.length() == 0 ) {
			count = synonyms.keySet().size();
		}
		else {
			String[] keys = input.split( "\\|" );
			for ( String key : keys ) {
				if ( synonyms.containsKey( key ) )
					count++ ;
				else
					error = true;
			}
		}
		synsTagCount.setText( "Valid Tags: " + count );
		synsTagCount.setForeground( error ? Color.RED : Color.BLACK );
	}
	
	public void insertUpdate( DocumentEvent e ) {
		fieldChanged();
	}
	public void removeUpdate( DocumentEvent e ) {
		fieldChanged();
	}
	public void changedUpdate( DocumentEvent e ) {}
	
}