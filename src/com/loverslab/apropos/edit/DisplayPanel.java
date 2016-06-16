package com.loverslab.apropos.edit;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import com.loverslab.apropos.edit.Model.LabelList;
import com.loverslab.apropos.edit.Model.PerspectiveMap;
import com.loverslab.apropos.edit.Model.StageMap;

@SuppressWarnings("serial")
public class DisplayPanel extends JPanel implements LineInsertedListener {
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
			add( stage.display( layout ), c );
			stage.addLineInsertedListener( this );
			PerspectiveMap persMap = stageMap.get( stage );
			for ( AproposLabel perspec : persMap.keySet() ) {
				c.insets = new Insets( 0, 40, 0, 5 );
				c.gridy++ ;
				add( perspec.display( layout ), c );
				perspec.addLineInsertedListener( this );
				LabelList list = persMap.get( perspec );
				c.insets = new Insets( 0, 70, 0, 5 );
				for ( AproposLabel label : list ) {
					c.gridy++ ;
					add( label.display( layout ), c );
					label.addLineInsertedListener( this );
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
		// add( sep, c );
		
		revalidate();
	}
	
	public void lineInserted( AproposLabel above ) {
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
			AproposLabel toAdd = new AproposLabel( "", above.getParentLabel() ).display( layout );
			insertList.add( toAdd );
			add( toAdd, c );
			toAdd.setHoverState( true );
			toAdd.getTextField().grabFocus();
			toAdd.addLineInsertedListener( this );
			
			layout.getConstraints( sep ).gridy++ ;
			// System.out.println( stageMap );
			
			revalidate();
		}
	}
	
}
