package com.loverslab.apropos.edit;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

@SuppressWarnings("serial")
public class DisplayPanel extends JPanel implements LineChangedListener, PopupMenuListener {
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
		load( stageMap, true );
	}
	
	public void help() {
		System.out.println( "HALP" );
		add( new JLabel( "HALP" ) );
	}
	
	public void load( StageMap stageMap, boolean resetScroll ) {
		this.stageMap = stageMap;
		
		menuManager = new MenuManager();
		
		removeAll();
		if ( resetScroll ) scroll.getVerticalScrollBar().setValue( 0 );
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
			add( stage.display( layout, this, this, false ), c );
			PerspectiveMap persMap = stageMap.get( stage );
			for ( AproposLabel perspec : persMap.keySet() ) {
				c.insets = new Insets( 0, 40, 0, 5 );
				c.gridy++ ;
				add( perspec.display( layout, this, this, false ), c );
				LabelList list = persMap.get( perspec );
				c.insets = new Insets( 0, 70, 0, 5 );
				for ( AproposLabel label : list ) {
					c.gridy++ ;
					add( label.display( layout, this, this ), c );
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
	
	public void refresh() {
		load( stageMap, false );
	}
	
	public void lineInserted( AproposLabel above ) {
		lineInserted( above, new AproposLabel( "", above.getParentLabel() ) );
	}
	
	public void lineInserted( AproposLabel above, AproposLabel toAdd ) {
		if ( stageMap != null ) {
			LabelList target = stageMap.query( above.getParentLabel() ).labelList;
			int i = target.indexOf( above ) + 1;
			target.add( i, toAdd );
			
			refresh();
			
			toAdd.setHoverState( true );
			toAdd.getTextField().grabFocus();
		}
	}
	
	public void lineRemoved( AproposLabel removed ) {
		if ( stageMap != null ) {
			LabelList target = stageMap.query( removed.getParentLabel() ).labelList;
			target.remove( removed );
			
			refresh();
		}
	}
	
	public void sectionRemoved( AproposLabel section ) {
		switch ( section.getDepth() ) {
			case 3:
				for ( AproposLabel pers : stageMap.get( section ).keySet() ) {
					LabelList labelList = stageMap.get( section ).get( pers );
					for ( int i = labelList.size() - 2; i >= 0; i-- )
						labelList.remove( i );
				}
				break;
			case 4:
				LabelList labelList = stageMap.query( section ).labelList;
				for ( int i = labelList.size() - 2; i >= 0; i-- )
					labelList.remove( i );
				break;
			default:
				System.err.println( "Clear was called on a label that shouldn't have clear available" );
				break;
		}
		
		refresh();
	}
	
	public void stageRemoved( AproposLabel stage ) {
		if ( stage.getDepth() != 3 ) {
			System.err.println( "Stage Remove Called on not a stage: " + stage );
			return;
		}
		if ( stage.getText().indexOf( "Stage" ) == -1 ) {
			parent.handleException( new IllegalStateException( "You can't remove an " + stage.getText() + " Stage" ) );
			return;
		}
		boolean found = false;
		AproposLabel lastStage = null;
		for ( AproposLabel stageLabel : stageMap.keySet() ) {
			if ( found ) {
				String text = stageLabel.getText();
				if ( text.indexOf( "Stage" ) > -1 ) {
					if ( stageLabel.compareTo( lastStage ) > -1 ) lastStage = stageLabel.clone();
					int n = Character.getNumericValue( text.trim().charAt( text.length() - 1 ) );
					stageLabel.setText( text.substring( 0, text.length() - 1 ) + ( n - 1 ) );
				}
			}
			else if ( stageLabel == stage ) {
				found = true;
				if ( stageLabel.compareTo( lastStage ) > -1 ) lastStage = stageLabel.clone();
			}
		}
		System.out.println( lastStage );
		stageMap.remove( stage );
		if ( lastStage != null ) parent.model.deleteStage( lastStage );
		
		refresh();
	}
	
	public void stageAdd( AproposLabel stage, boolean above ) {
		if ( stage.getDepth() != 3 ) {
			System.err.println( "Stage Remove Called on not a stage: " + stage );
			return;
		}
		if ( stage.getText().indexOf( "Intro" ) > -1 && above ) {
			parent.handleException( new IllegalStateException( "You can't add a stage above the " + stage.getText() + " Stage" ) );
			return;
		}
		if ( stage.getText().indexOf( "Orgasm" ) > -1 && !above ) {
			parent.handleException( new IllegalStateException( "You can't add a stage below the " + stage.getText() + " Stage" ) );
			return;
		}
		boolean found = false;
		int i = 0, s = 0;
		AproposLabel[] stageLabels = stageMap.keySet().toArray( new AproposLabel[ 0 ] );
		for ( i = 0; i < stageLabels.length; i++ ) {
			AproposLabel stageLabel = stageLabels[i];
			if ( found ) {
				String text = stageLabel.getText();
				if ( text.indexOf( "Stage" ) > -1 ) {
					int n = Character.getNumericValue( text.trim().charAt( text.length() - 1 ) );
					stageLabel.setText( text.substring( 0, text.length() - 1 ) + ( n + 1 ) );
				}
			}
			else if ( stageLabel == stage ) {
				found = true;
				if ( above ) {
					String text = stageLabel.getText();
					if ( text.indexOf( "Stage" ) > -1 ) {
						int n = Character.getNumericValue( text.trim().charAt( text.length() - 1 ) );
						stageLabel.setText( text.substring( 0, text.length() - 1 ) + ( n + 1 ) );
						s = n;
					}
				}
				else {
					String text = stageLabel.getText();
					if ( text.indexOf( "Stage" ) > -1 ) {
						int n = Character.getNumericValue( text.trim().charAt( text.length() - 1 ) );
						s = n + 1;
					}
				}
			}
		}
		
		AproposLabel parent = stageLabels[0].getParentLabel();
		if ( s == 0 ) {
			if ( stage.getText().indexOf( "Intro" ) > -1 )
				s = 1;
			else if ( stage.getText().indexOf( "Orgasm" ) > -1 ) s = stageLabels.length - 1;
		}
		AproposLabel newKey = new AproposLabel( "Stage " + s, parent );
		
		PerspectiveMap newMap = new PerspectiveMap();
		for ( AproposLabel pers : stageMap.get( stageLabels[0] ).keySet() ) {
			AproposLabel newPers = new AproposLabel( pers.getText(), newKey );
			newMap.put( newPers, new LabelList( Arrays.asList( new AproposLabel[] { new AproposLabel( "", newPers ) } ) ) );
		}
		
		stageMap.put( newKey, newMap );
		
		refresh();
	}
	
	public void copyTo( AproposLabel line, AproposLabel to ) {
		LabelList target = stageMap.query( to ).labelList;
		if ( target.size() > 1 ) {
			target.add( target.size() - 2, new AproposLabel( line.getText(), to ) );
		}
		else {
			target.add( 0, new AproposLabel( line.getText(), to ) );
		}
		refresh();
	}
	
	public void copySection( AproposLabel section, AproposLabel dest, boolean replace ) {
		if ( section == dest ) {
			parent.handleException( new IllegalStateException( "Do you really need to copy to the same section?" ) );
			return;
		}
		switch ( section.getDepth() ) {
			case 3:
				AproposLabel[] sectionPersArray = stageMap.get( section ).keySet().toArray( new AproposLabel[ 3 ] );
				AproposLabel[] destPersArray = stageMap.get( dest ).keySet().toArray( new AproposLabel[ 3 ] );
				for ( int i = 0; i < 3; i++ ) {
					AproposLabel sectionPers = sectionPersArray[i];
					AproposLabel destPers = destPersArray[i];
					LabelList sectionList = stageMap.query( sectionPers ).labelList;
					LabelList destList = stageMap.query( destPers ).labelList;
					if ( replace ) for ( int j = destList.size() - 2; j >= 0; j-- )
						destList.remove( j );
					for ( int j = 0; j < sectionList.size() - 1; j++ )
						destList.add( Math.max( destList.size() - 2, 0 ), new AproposLabel( sectionList.get( j ).getText(), destPers ) );
				}
				break;
			case 4:
				LabelList sectionList = stageMap.query( section ).labelList;
				LabelList destList = stageMap.query( dest ).labelList;
				if ( replace ) for ( int i = destList.size() - 2; i >= 0; i-- )
					destList.remove( i );
				for ( int i = 0; i < sectionList.size() - 1; i++ )
					destList.add( Math.max( destList.size() - 2, 0 ), new AproposLabel( sectionList.get( i ).getText(), dest ) );
				break;
			default:
				System.err.println( "Clear was called on a label that shouldn't have clear available" );
				break;
		}
		
		refresh();
	}
	
	public void popupMenuTriggered( AproposLabel label, MouseEvent e ) {
		menuManager.show( label, e );
	}
	
	public class MenuManager implements ActionListener {
		// Gonna try this Composition over inheritance stuff all the kids are talking about
		protected AproposLabel invoker;
		private JPopupMenu[] popups = new JPopupMenu[ 6 ];
		private JMenuItem clearItem, removeStage, addStageBelow, addStageAbove, removeItem, duplicateItem;
		private String copyTo = "Copy To", copyAppend = "Copy & Append", copyReplace = "Copy & Replace";
		// private JMenu copyToMenu;
		// private JMenu[] copyReplaceMenu = new JMenu[ 6 ], copyAppendMenu = new JMenu[ 6 ], stageSubMenus = new JMenu[ stageMap.size() ];
		// private JMenuItem[] stageItems = new JMenuItem[ stageMap.size() ], perspectiveItems = new JMenuItem[ 3 ];
		
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
						popup.add( getRemoveStageItem() );
						popup.add( getStageAboveItem() );
						popup.add( getStageBelowItem() );
						// Fall through
					case 4: // Perspective Level
						popup.add( getClearItem() );
						popup.add( getCopyMenu( depth, copyAppend ) );
						popup.add( getCopyMenu( depth, copyReplace ) );
						break;
					case 5: // Line Level
						popup.add( getRemoveItem() );
						popup.add( getDuplicateItem() );
						popup.add( getCopyMenu( depth, copyTo ) );
						break;
					default:
						break;
				}
			}
			
			popup.show( e.getComponent(), e.getX(), e.getY() );
			invoker = label;
		}
		
		protected JMenuItem getClearItem() {
			return clearItem = initMenuItem( clearItem, "Clear" );
		}
		
		protected JMenuItem getRemoveItem() {
			return removeItem = initMenuItem( removeItem, "Remove" );
		}
		
		protected JMenuItem getDuplicateItem() {
			return duplicateItem = initMenuItem( duplicateItem, "Duplicate" );
		}
		
		protected JMenuItem getRemoveStageItem() {
			return removeStage = initMenuItem( removeStage, "Remove Stage" );
		}
		
		protected JMenuItem getStageBelowItem() {
			return addStageBelow = initMenuItem( addStageBelow, "Add New Stage Below" );
		}
		
		protected JMenuItem getStageAboveItem() {
			return addStageAbove = initMenuItem( addStageAbove, "Add New Stage Above" );
		}
		
		protected JMenuItem initMenuItem( JMenuItem ref, String text ) {
			if ( ref != null ) return ref;
			ref = new JMenuItem( text );
			ref.addActionListener( this );
			return ref;
		}
		
		protected JMenu getCopyMenu( int depth, String text ) {
			LabelMenu menu = null;
			switch ( depth ) {
				case 3:
					// if ( copyAppendMenu[depth] != null ) return copyAppendMenu[depth];
					menu = new LabelMenu( text );
					for ( int i = 0; i < stageMap.size(); i++ )
						menu.add( getStageItem( i, menu ) );
					return menu;
				case 4:
					// Fall Through
				case 5:
					// if ( copyAppendMenu[depth] != null ) return copyAppendMenu[depth];
					menu = new LabelMenu( text );
					for ( int i = 0; i < stageMap.size(); i++ )
						menu.add( getStageSubMenu( i, menu ) );
					return menu;
				default:
					return null;
			}
		}
		
		protected JMenu getStageSubMenu( int i, LabelMenu parent ) {
			// if ( stageSubMenus[i] != null ) return stageSubMenus[i];
			AproposLabel label = ( stageMap.keySet().toArray( new AproposLabel[ 0 ] ) )[i];
			LabelMenu menu = new LabelMenu( parent, label );
			for ( i = 0; i < 3; i++ )
				menu.add( getPerspectiveItem( i, menu ) );
			return menu;
		}
		
		protected JMenuItem getStageItem( int i, LabelMenu parent ) {
			// if ( stageItems[i] != null ) return stageItems[i];
			AproposLabel label = ( stageMap.keySet().toArray( new AproposLabel[ 0 ] ) )[i];
			JMenuItem item = new LabelMenuItem( parent, label );
			item.addActionListener( this );
			// stageItems[i] = item;
			return item;
		}
		
		protected JMenuItem getPerspectiveItem( int i, LabelMenu parent ) {
			// if ( perspectiveItems[i] != null ) return perspectiveItems[i];
			AproposLabel label = ( stageMap.get( parent.label ).keySet().toArray( new AproposLabel[ 0 ] ) )[i];
			JMenuItem item = new LabelMenuItem( parent, label );
			item.addActionListener( this );
			// perspectiveItems[i] = item;
			return item;
		}
		
		public void actionPerformed( ActionEvent e ) {
			JMenuItem item = (JMenuItem) e.getSource();
			if ( item == clearItem )
				sectionRemoved( invoker );
			else if ( item == removeItem )
				lineRemoved( invoker );
			else if ( item == duplicateItem )
				lineInserted( invoker, invoker.clone() );
			else if ( item == removeStage )
				stageRemoved( invoker );
			else if ( item == addStageBelow )
				stageAdd( invoker, false );
			else if ( item == addStageAbove )
				stageAdd( invoker, true );
			else if ( item instanceof LabelMenuItem ) {
				LabelMenuItem lMI = ( (LabelMenuItem) item );
				LabelMenu root = lMI.parent;
				while ( root.parent != null )
					root = root.parent;
				AproposLabel label = lMI.label;
				if ( root.getText().equals( copyTo ) )
					copyTo( invoker, label );
				else if ( root.getText().equals( copyReplace ) )
					copySection( invoker, label, true );
				else if ( root.getText().equals( copyAppend ) )
					copySection( invoker, label, false );
				else
					System.out.println( "SOL :/ " + item );
			}
			else
				System.out.println( "SOL :/ " + item );
		}
		
		public class LabelMenuItem extends JMenuItem {
			public AproposLabel label;
			public LabelMenu parent;
			
			public LabelMenuItem( LabelMenu parent, AproposLabel label ) {
				super( label.getText() );
				this.parent = parent;
				this.label = label;
			}
			public LabelMenuItem( LabelMenu parent, AproposLabel label, String text ) {
				super( text );
				this.parent = parent;
				this.label = label;
			}
		}
		
		public class LabelMenu extends JMenu {
			public AproposLabel label;
			public LabelMenu parent;
			
			public LabelMenu( LabelMenu parent, AproposLabel label ) {
				super( label.getText() );
				this.parent = parent;
				this.label = label;
			}
			public LabelMenu( LabelMenu parent, AproposLabel label, String text ) {
				super( text );
				this.parent = parent;
				this.label = label;
			}
			public LabelMenu( String text ) {
				super( text );
				this.parent = null;
			}
		}
		
	}
	
}
