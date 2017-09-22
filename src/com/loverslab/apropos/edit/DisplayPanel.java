package com.loverslab.apropos.edit;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import com.google.gson.stream.MalformedJsonException;

@SuppressWarnings("serial")
public class DisplayPanel extends JPanel implements LineChangedListener, PopupMenuListener, Scrollable, DisplayPanelContainer {
	private View parent;
	private JScrollPane scroll;
	private MenuManager menuManager;
	StageMap stageMap;
	private JSeparator sep;
	
	public DisplayPanel( View parent, JScrollPane scroll ) {
		super( true );
		this.parent = parent;
		this.scroll = scroll;
		
		setLayout( new GridBagLayout() );
		
	}
	
	public DisplayPanel( View parent, JScrollPane scroll, StageMap stageMap ) {
		this( parent, scroll );
		load( stageMap, true );
	}
	
	public void load( StageMap stageMap, boolean resetScroll ) {
		this.stageMap = stageMap;
		
		menuManager = new MenuManager();
		
		removeAll();
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
			add( stage.display( this, this, false ), c );
			PerspectiveMap persMap = stageMap.get( stage );
			if ( persMap == null ) System.out.println( stageMap );
			for ( AproposLabel perspec : persMap.keySet() ) {
				c.insets = new Insets( 0, 40, 0, 5 );
				c.gridy++ ;
				add( perspec.display( this, this, false ), c );
				LabelList list = persMap.get( perspec );
				c.insets = new Insets( 0, 70, 0, 5 );
				for ( AproposLabel label : list ) {
					c.gridy++ ;
					add( label.display( this, this, parent.model.synonymsLengths ), c );
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
		
		if ( resetScroll ) SwingUtilities.invokeLater( new ResetScroll( scroll ) );
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
	
	public void lineUpdated( AproposLabel label ) {
		label.setSimulateString( parent.model.insert( label.getText() ) );
		label.simulate();
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
	
	public void toClipboard( AproposLabel invoker ) {
		parent.new ClipboardWriter( stageMap.query( invoker ).map ).execute();
		
	}
	
	public void fromClipboard( AproposLabel invoker ) {
		AproposMap map = stageMap.query( invoker ).map;
		parent.new ClipboardReader( map ) {
			public void done() {
				try {
					get();
					refresh();
				}
				catch ( InterruptedException | ExecutionException e ) {
					try {
						throw e.getCause();
					}
					catch ( MalformedJsonException e1 ) {
						parent.handleException( new MalformedJsonException( "Invalid JSON" ) );
					}
					catch ( StringIndexOutOfBoundsException e1 ) {
						parent.handleException( new IllegalArgumentException( "Clipboard is Empty" ) );
					}
					catch ( Throwable t ) {
						parent.handleException( t );
						t.printStackTrace();
					}
				}
			}
		}.execute();
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
		AproposLabel lastStage = null; // Queue to hold key/value parings that need to be re-inserted into the StageMap
		AproposLabel[] stageLabels = stageMap.keySet().toArray( new AproposLabel[ 0 ] );
		int i = 0;
		for ( i = 0; i < stageLabels.length; i++ ) {
			AproposLabel stageLabel = stageLabels[i];
			if ( found ) {
				String text = stageLabel.getText();
				if ( text.indexOf( "Stage" ) > -1 ) {
					if ( stageLabel.compareTo( lastStage ) > -1 ) lastStage = stageLabel.clone();
					int n = Integer.valueOf( text.replace( "Stage ", "" ) );
					stageMap.modifyKey( stageLabel, text.replaceAll( " [1-9][0-9]*", " " + ( n - 1 ) ) );
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
					int n = Integer.valueOf( text.replace( "Stage ", "" ) );
					stageMap.modifyKey( stageLabel, text.replaceAll( " [1-9][0-9]*", " " + ( n + 1 ) ) );
				}
			}
			else if ( stageLabel == stage ) {
				found = true;
				if ( above ) {
					String text = stageLabel.getText();
					if ( text.indexOf( "Stage" ) > -1 ) {
						int n = Integer.valueOf( text.replace( "Stage ", "" ) );
						stageMap.modifyKey( stageLabel, text.replaceAll( " [1-9][0-9]*", " " + ( n + 1 ) ) );
						s = n;
					}
				}
				else {
					String text = stageLabel.getText();
					if ( text.indexOf( "Stage" ) > -1 ) {
						int n = Integer.valueOf( text.replace( "Stage ", "" ) );
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
		AproposLabel newLine = Model.perspectiveShift( new AproposLabel( line.getText(), to ), line.getParentLabel(), to );
		if ( target.size() > 1 ) {
			target.add( target.size() - 1, newLine );
		}
		else {
			target.add( 0, newLine );
		}
		System.out.println( target );
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
						destList.add( Math.max( destList.size() - 1, 0 ), new AproposLabel( sectionList.get( j ).getText(), destPers ) );
				}
				break;
			case 4:
				LabelList sectionList = Model.perspectiveShift( stageMap.query( section ).labelList, section, dest );
				LabelList destList = stageMap.query( dest ).labelList;
				if ( replace ) for ( int i = destList.size() - 2; i >= 0; i-- )
					destList.remove( i );
				for ( int i = 0; i < sectionList.size() - 1; i++ )
					destList.add( Math.max( destList.size() - 1, 0 ), new AproposLabel( sectionList.get( i ).getText(), dest ) );
				break;
			default:
				System.err.println( "Clear was called on a label that shouldn't have clear available" );
				break;
		}
		
		if ( !replace ) parent.setConflicted( stageMap.query( dest ).map.checkDuplicates(), this );
		refresh();
	}
	
	public void popupMenuTriggered( AproposLabel label, MouseEvent e ) {
		if ( !stageMap.isConflicted() ) menuManager.show( label, e );
	}
	
	public class MenuManager implements ActionListener {
		// Gonna try this Composition over inheritance stuff all the kids are talking about
		AproposLabel invoker;
		private JPopupMenu[] popups = new JPopupMenu[ 6 ];
		private JMenuItem clearItem, removeStage, addStageBelow, addStageAbove, removeItem, duplicateItem, copyItem, pasteItem;
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
				// popups[depth] = popup; TODO: Determine if this GC avoidance is even needed
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
						popup.add( getCopyItem() );
						popup.add( getPasteItem() );
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
		
		JMenuItem getClearItem() {
			return clearItem = initMenuItem( clearItem, "Clear" );
		}
		
		JMenuItem getRemoveItem() {
			return removeItem = initMenuItem( removeItem, "Remove" );
		}
		
		JMenuItem getDuplicateItem() {
			return duplicateItem = initMenuItem( duplicateItem, "Duplicate" );
		}
		
		JMenuItem getCopyItem() {
			return copyItem = initMenuItem( copyItem, "Copy" );
		}
		
		JMenuItem getPasteItem() {
			return pasteItem = initMenuItem( pasteItem, "Paste" );
		}
		
		JMenuItem getRemoveStageItem() {
			return removeStage = initMenuItem( removeStage, "Remove Stage" );
		}
		
		JMenuItem getStageBelowItem() {
			return addStageBelow = initMenuItem( addStageBelow, "Add New Stage Below" );
		}
		
		JMenuItem getStageAboveItem() {
			return addStageAbove = initMenuItem( addStageAbove, "Add New Stage Above" );
		}
		
		JMenuItem initMenuItem( JMenuItem ref, String text ) {
			if ( ref != null ) return ref;
			ref = new JMenuItem( text );
			ref.addActionListener( this );
			return ref;
		}
		
		JMenu getCopyMenu( int depth, String text ) {
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
		
		JMenu getStageSubMenu( int i, LabelMenu parent ) {
			// if ( stageSubMenus[i] != null ) return stageSubMenus[i];
			AproposLabel label = ( stageMap.keySet().toArray( new AproposLabel[ 0 ] ) )[i];
			LabelMenu menu = new LabelMenu( parent, label );
			for ( i = 0; i < 3; i++ )
				menu.add( getPerspectiveItem( i, menu ) );
			return menu;
		}
		
		JMenuItem getStageItem( int i, LabelMenu parent ) {
			// if ( stageItems[i] != null ) return stageItems[i];
			AproposLabel label = ( stageMap.keySet().toArray( new AproposLabel[ 0 ] ) )[i];
			JMenuItem item = new LabelMenuItem( parent, label );
			item.addActionListener( this );
			// stageItems[i] = item;
			return item;
		}
		
		JMenuItem getPerspectiveItem( int i, LabelMenu parent ) {
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
			else if ( item == copyItem )
				toClipboard( invoker );
			else if ( item == pasteItem )
				fromClipboard( invoker );
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
	public DisplayPanel getDisplayPanel() {
		return this;
	}
	
}

class ScrollableDisplayPanel extends JScrollPane implements DisplayPanelContainer {
	private static final long serialVersionUID = 7178177870076629501L;
	private DisplayPanel display;
	
	public ScrollableDisplayPanel( View parent ) {
		super( VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER );
		display = new DisplayPanel( parent, this );
		setViewportView( display );
	}
	
	public DisplayPanel getDisplayPanel() {
		return display;
	}
}

class ResetScroll implements Runnable {
	JScrollPane scrollpane;
	
	public ResetScroll( JScrollPane scrollpane ) {
		this.scrollpane = scrollpane;
	}
	
	public void run() {
		scrollpane.getVerticalScrollBar().setValue( 0 );
	}
}