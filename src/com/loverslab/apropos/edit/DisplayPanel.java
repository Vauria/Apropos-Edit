package com.loverslab.apropos.edit;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import com.loverslab.apropos.edit.Model.LabelList;
import com.loverslab.apropos.edit.Model.PerspectiveMap;
import com.loverslab.apropos.edit.Model.StageMap;

@SuppressWarnings("serial")
public class DisplayPanel extends JPanel implements LineChangedListener, PopupMenuListener {
	@SuppressWarnings("unused")
	private View parent;
	private JScrollPane scroll;
	protected StageMap stageMap;
	private GridBagLayout layout;
	private JSeparator sep;
	
	public DisplayPanel( View parent, JScrollPane scroll ) {
		super( true );
		this.parent = parent;
		this.scroll = scroll;
		
		layout = new GridBagLayout() {
			public GridBagConstraints getConstraints( Component comp ) {
				return lookupConstraints( comp );
			}
		};
		setLayout( layout );
		
	}
	
	public DisplayPanel( View parent, JScrollPane scroll, StageMap stageMap ) {
		this( parent, scroll );
		load( stageMap );
	}
	
	public void help() {
		System.out.println( "HALP" );
		add( new JLabel( "HALP" ) );
	}
	
	public void load( StageMap stageMap ) {
		this.stageMap = stageMap;
		
		removeAll();
		scroll.getVerticalScrollBar().setValue( 0 );
		GridBagConstraints c = new GridBagConstraints();
		
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridy = 0;
		c.gridx = 0;
		
		for ( AproposLabel stage : stageMap.keySet() ) {
			c.insets = new Insets( 25, 10, 5, 10 );
			c.gridy++ ;
			c.gridx = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			add( stage.display( layout, false ), c );
			stage.addLineChangedListener( this );
			stage.addPopupMenuListener( this );
			PerspectiveMap persMap = stageMap.get( stage );
			for ( AproposLabel perspec : persMap.keySet() ) {
				c.insets = new Insets( 0, 40, 0, 5 );
				c.gridy++ ;
				add( perspec.display( layout, false ), c );
				perspec.addLineChangedListener( this );
				perspec.addPopupMenuListener( this );
				LabelList list = persMap.get( perspec );
				c.insets = new Insets( 0, 70, 0, 5 );
				for ( AproposLabel label : list ) {
					c.gridy++ ;
					add( label.display( layout ), c );
					label.addLineChangedListener( this );
					label.addPopupMenuListener( this );
				}
			}
		}
		
		sep = new JSeparator();
		c.insets = new Insets( 0, 3, 0, 3 );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridwidth = 2;
		c.weighty = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( sep, c );
		
		revalidate();
	}
	
	public void lineInserted( AproposLabel above ) {
		lineInserted( above, new AproposLabel( "", above.getParentLabel() ) );
	}
	
	public void lineInserted( AproposLabel above, AproposLabel toAdd ) {
		if ( stageMap != null ) {
			GridBagConstraints cAbove = above.getGridBagCons();
			GridBagConstraints c;
			boolean found = false;
			LabelList insertList = null;
			for ( AproposLabel stage : stageMap.keySet() ) {
				PerspectiveMap persMap = stageMap.get( stage );
				if ( found ) stage.bump();
				for ( AproposLabel perspec : persMap.keySet() ) {
					LabelList list = persMap.get( perspec );
					if ( found ) perspec.bump();
					for ( AproposLabel label : list ) {
						c = label.getGridBagCons();
						if ( found ) label.bump();
						if ( !found & c.gridy >= cAbove.gridy ) {
							found = true;
							insertList = list;
						}
					}
				}
			}
			
			c = (GridBagConstraints) cAbove.clone();
			c.gridy++ ;
			toAdd.display( layout );
			insertList.add( toAdd );
			add( toAdd, c );
			toAdd.setHoverState( true );
			toAdd.getTextField().grabFocus();
			toAdd.addLineChangedListener( this );
			toAdd.addPopupMenuListener( this );
			
			layout.getConstraints( sep ).gridy++ ;
			sep.invalidate();
			// System.out.println( stageMap );
			
		}
	}
	
	public void lineRemoved( AproposLabel removed ) {
		if ( stageMap != null ) {
			GridBagConstraints cRemoved = removed.getGridBagCons();
			GridBagConstraints c;
			boolean found = false;
			LabelList removeList = null;
			for ( AproposLabel stage : stageMap.keySet() ) {
				PerspectiveMap persMap = stageMap.get( stage );
				if ( found ) stage.boop();
				for ( AproposLabel perspec : persMap.keySet() ) {
					LabelList list = persMap.get( perspec );
					if ( found ) perspec.boop();
					for ( AproposLabel label : list ) {
						c = label.getGridBagCons();
						if ( found ) label.boop();
						if ( !found & c.gridy >= cRemoved.gridy ) {
							found = true;
							removeList = list;
						}
					}
				}
			}
			
			remove( removed );
			removeList.remove( removed );
			
			layout.getConstraints( sep ).gridy-- ;
			sep.invalidate();
			
			revalidate();
		}
	}
	
	public void popupMenuTriggered( AproposLabel label, MouseEvent e ) {
		new RightClickMenu( label ).show( e.getComponent(), e.getX(), e.getY() );
	}
	
	public class RightClickMenu extends JPopupMenu {
		
		public RightClickMenu( AproposLabel label ) {
			super();
			switch ( label.getDepth() ) {
				case 0:
					break;
				case 1:
					break;
				case 2:
					break;
				case 3:
				case 4:
					JMenuItem clearItem = new JMenuItem( "Clear" );
					add( clearItem );
					JMenu copyReplace = new JMenu( "Copy & Replace" );
					JMenu copyAppend = new JMenu( "Copy & Append" );
					add( copyReplace );
					add( copyAppend );
					if ( stageMap != null ) {
						for ( AproposLabel stage : stageMap.keySet() ) {
							PerspectiveMap persMap = stageMap.get( stage );
							JMenu stageMenuRep = new JMenu( stage.getText() );
							JMenu stageMenuApp = new JMenu( stage.getText() );
							copyReplace.add( stageMenuRep );
							copyAppend.add( stageMenuApp );
							for ( AproposLabel perspec : persMap.keySet() ) {
								if ( perspec != label ) {
									JMenuItem persItemRep = new JMenuItem( perspec.getText() );
									JMenuItem persItemApp = new JMenuItem( perspec.getText() );
									stageMenuRep.add( persItemRep );
									stageMenuApp.add( persItemApp );
								}
							}
						}
					}
					break;
				case 5:
					JMenuItem removeItem = new JMenuItem( "Remove" );
					removeItem.addActionListener( new RemoveListener( label ) );
					add( removeItem );
					JMenuItem duplicateItem = new JMenuItem( "Duplicate" );
					add( duplicateItem );
					JMenu copyTo = new JMenu( "Copy To" );
					add( copyTo );
					AproposLabel perspecSource = label.getParentLabel();
					if ( stageMap != null ) {
						for ( AproposLabel stage : stageMap.keySet() ) {
							PerspectiveMap persMap = stageMap.get( stage );
							JMenu stageMenu = new JMenu( stage.getText() );
							copyTo.add( stageMenu );
							for ( AproposLabel perspec : persMap.keySet() ) {
								if ( perspec != perspecSource ) {
									JMenuItem persItem = new JMenuItem( perspec.getText() );
									stageMenu.add( persItem );
								}
							}
						}
					}
					break;
				default:
					break;
			}
		}
		
	}
	
	public class RemoveListener implements ActionListener {
		AproposLabel label;
		
		public RemoveListener( AproposLabel label ) {
			this.label = label;
		}
		public void actionPerformed( ActionEvent e ) {
			label.fireLineRemoved( label );
		}
		
	}
	
}
