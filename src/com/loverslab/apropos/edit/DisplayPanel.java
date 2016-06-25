package com.loverslab.apropos.edit;

import java.awt.Component;
import java.awt.Container;
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

@SuppressWarnings("serial")
public class DisplayPanel extends JPanel implements LineChangedListener, PopupMenuListener {
	@SuppressWarnings("unused")
	private View parent;
	private JScrollPane scroll;
	private MenuManager menuManager;
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
		
		menuManager = new MenuManager();
		
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
	
	public void sectionRemoved( AproposLabel section ) {
		int distance = 0;
		boolean found = false;
		switch ( section.getDepth() ) {
			case 3:
				for ( AproposLabel pers : stageMap.get( section ).keySet() ) {
					LabelList labelList = stageMap.get( section ).get( pers );
					for ( int i = 0; i < labelList.size() - 1; i++ )
						remove( labelList.get( i ) );
					LabelList replaceList = new LabelList();
					replaceList.add( labelList.get( labelList.size() - 1 ) );
					distance += ( labelList.size() - 1 );
					stageMap.get( section ).put( pers, replaceList );
					for ( AproposLabel stage : stageMap.keySet() ) {
						PerspectiveMap persMap = stageMap.get( stage );
						if ( found ) stage.poke( distance );
						for ( AproposLabel perspec : persMap.keySet() ) {
							LabelList list = persMap.get( perspec );
							if ( found ) {
								perspec.poke( distance );
								for ( AproposLabel label : list ) {
									label.poke( distance );
								}
							}
						}
						if ( !found & stage == section ) found = true;
					}
				}
				break;
			case 4:
				LabelList labelList = stageMap.get( section.getParentLabel() ).get( section );
				for ( int i = 0; i < labelList.size() - 1; i++ )
					remove( labelList.get( i ) );
				LabelList replaceList = new LabelList();
				replaceList.add( labelList.get( labelList.size() - 1 ) );
				distance += ( labelList.size() - 1 );
				stageMap.get( section.getParentLabel() ).put( section, replaceList );
				for ( AproposLabel stage : stageMap.keySet() ) {
					PerspectiveMap persMap = stageMap.get( stage );
					if ( found ) stage.poke( distance );
					for ( AproposLabel perspec : persMap.keySet() ) {
						LabelList list = persMap.get( perspec );
						if ( found ) {
							perspec.poke( distance );
							for ( AproposLabel label : list ) {
								label.poke( distance );
							}
						}
						if ( !found & perspec == section ) found = true;
					}
				}
				break;
			default:
				System.err.println( "Clear was called on a label that shouldn't have clear available" );
				break;
		}
		
		layout.getConstraints( sep ).gridy += distance * 5;
		sep.invalidate();
		
		revalidate();
	}
	
	public void popupMenuTriggered( AproposLabel label, MouseEvent e ) {
		menuManager.show( label, e );
	}
	
	public class MenuManager implements ActionListener {
		// Gonna try this Composition over inheritance stuff all the kids are talking about
		protected AproposLabel invoker;
		private JPopupMenu[] popups = new JPopupMenu[ 6 ];
		private JMenuItem clearItem, removeItem, duplicateItem;
		private JMenu copyToMenu;
		private JMenu[] copyReplaceMenu = new JMenu[ 6 ], copyAppendMenu = new JMenu[ 6 ], stageSubMenus = new JMenu[ stageMap.size() ];
		private JMenuItem[] stageItems = new JMenuItem[ stageMap.size() ], perspectiveItems = new JMenuItem[ 3 ];
		
		public MenuManager() {}
		
		public void show( AproposLabel label, MouseEvent e ) {
			int depth = label.getDepth();
			
			JPopupMenu popup = popups[depth];
			
			if ( popup == null ) {
				popup = new JPopupMenu();
				popups[depth] = popup;
				switch ( depth ) {
					case 0: // Database level
						break;
					case 1: // Folder level
						break;
					case 2: // Position Level
						break;
					case 3: // Stage Level
						popup.add( getClearItem() );
						popup.add( getCopyReplaceMenu( depth ) );
						break;
					case 4: // Perspective Level
						popup.add( getClearItem() );
						popup.add( getCopyReplaceMenu( depth ) );
						break;
					case 5: // Line Level
						popup.add( getRemoveItem() );
						popup.add( getDuplicateItem() );
						break;
					default:
						break;
				}
			}
			
			popup.show( e.getComponent(), e.getX(), e.getY() );
			invoker = label;
		}
		
		protected JMenuItem getClearItem() {
			if ( clearItem != null ) return clearItem;
			clearItem = new JMenuItem( "Clear" );
			clearItem.addActionListener( this );
			return clearItem;
		}
		
		protected JMenuItem getRemoveItem() {
			if ( removeItem != null ) return removeItem;
			removeItem = new JMenuItem( "Remove" );
			removeItem.addActionListener( this );
			return removeItem;
		}
		
		protected JMenuItem getDuplicateItem() {
			if ( duplicateItem != null ) return duplicateItem;
			duplicateItem = new JMenuItem( "Duplicate" );
			duplicateItem.addActionListener( this );
			return duplicateItem;
		}
		
		protected JMenu getCopyReplaceMenu( int depth ) {
			JMenu menu = null;
			switch ( depth ) {
				case 3:
					if ( copyReplaceMenu[depth] != null ) return copyReplaceMenu[depth];
					menu = new JMenu( "Copy & Replace" );
					copyReplaceMenu[depth] = menu;
					for ( int i = 0; i < stageMap.size(); i++ )
						menu.add( getStageItem( i ) );
					return menu;
				case 4:
					if ( copyReplaceMenu[depth] != null ) return copyReplaceMenu[depth];
					menu = new JMenu( "Copy & Replace" );
					copyReplaceMenu[depth] = menu;
					return menu;
				default:
					return null;
			}
		}
		
		protected JMenuItem getStageItem( int i ) {
			if ( stageItems[i] != null ) return stageItems[i];
			AproposLabel label = ( stageMap.keySet().toArray( new AproposLabel[ stageMap.size() ] ) )[i];
			JMenuItem item = new JMenuItem( label.getText() );
			item.addActionListener( this );
			stageItems[i] = item;
			return item;
		}
		
		protected JMenuItem getPerspectiveItem( int i ) {
			if ( perspectiveItems[i] != null ) return perspectiveItems[i];
			AproposLabel label = ( stageMap.get( stageMap.firstKey() ).keySet().toArray( new AproposLabel[ stageMap.size() ] ) )[i];
			// Equivalent command for a FolderMap named f. The f of course stands for 'Fuck me' //@formatter:off 
			/*AproposLabel label = ( (AproposLabel[]) f.get( f.keySet().iterator().next() )
					.get( f.get( f.keySet().iterator().next() ).keySet().iterator().next() ).get( f.get( f.keySet().iterator().next() )
							.get( f.get( f.keySet().iterator().next() ).keySet().iterator().next() ).keySet().iterator().next() )
					.keySet().toArray() )[i]; */
			//@formatter:on
			JMenuItem item = new JMenuItem( label.getText() );
			item.addActionListener( this );
			perspectiveItems[i] = item;
			return item;
		}
		
		public void actionPerformed( ActionEvent e ) {
			// Casting galore!
			JMenuItem item = (JMenuItem) e.getSource();
			JMenu menu = null;
			Container parent = item;
			while ( ( parent = parent.getParent() ) != null ) {
				System.out.println( parent );
				if ( parent instanceof JPopupMenu ) menu = (JMenu) ((JPopupMenu) parent).getInvoker();
			}
			AproposLabel label = invoker;
			
			if ( menu == null ) {
				if ( item == clearItem )
					sectionRemoved( label );
				else if ( item == removeItem )
					label.fireLineRemoved( label );
				else if ( item == duplicateItem ) label.fireLineInserted( label, label.clone() );
			}
			else
				System.out.println( menu );
		}
		
		public void weee( AproposLabel label ) {
			switch ( label.getDepth() ) {
				case 3: // Fall Through
				case 4:
					JMenuItem clearItem = new JMenuItem( "Clear" );
					clearItem.addActionListener( new ClearListener( label ) );
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
					duplicateItem.addActionListener( new DuplicateListener( label ) );
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
		
		public class ClearListener implements ActionListener {
			AproposLabel label;
			
			public ClearListener( AproposLabel label ) {
				this.label = label;
			}
			public void actionPerformed( ActionEvent e ) {
				sectionRemoved( label );
			}
			
		}
		
		public class DuplicateListener implements ActionListener {
			AproposLabel label;
			
			public DuplicateListener( AproposLabel label ) {
				this.label = label;
			}
			public void actionPerformed( ActionEvent e ) {
				label.fireLineInserted( label, label.clone() );
			}
			
		}
		
	}
	
}
