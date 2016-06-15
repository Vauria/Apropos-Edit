package com.loverslab.apropos.edit;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import com.loverslab.apropos.edit.AproposLabel.EditingListener;
import com.loverslab.apropos.edit.Model.LabelList;
import com.loverslab.apropos.edit.Model.PerspectiveMap;
import com.loverslab.apropos.edit.Model.StageMap;

@SuppressWarnings("serial")
public class DisplayPanel extends JPanel {
	private View parent;
	private JScrollPane scroll;

	public DisplayPanel( View parent, JScrollPane scroll ) {
		super( true );
		this.parent = parent;
		this.scroll = scroll;
		
		setLayout( new GridBagLayout() );
		
		//addMouseListener( new AproposLabel("",null).new EditingListener() );
		
		System.out.println( this );
	}
	
	public void help() {
		System.out.println( "HALP" );
		add( new JLabel( "HALP" ) );
	}
	
	public void load( StageMap stageMap ) {
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
			//add( stage.display( c, scroll ), c );
			JButton b = new JButton(stage.toString());
			b.addMouseListener( stage.new EditingListener() );
			add( b, c );
			PerspectiveMap persMap = stageMap.get( stage );
			for ( AproposLabel perspec : persMap.keySet() ) {
				c.insets = new Insets( 0, 40, 0, 5 );
				c.gridy++ ;
				add( perspec.display( c, scroll ), c );
				add( new AproposLabelSimple(perspec), c );
				LabelList list = persMap.get( perspec );
				c.insets = new Insets( 0, 70, 0, 5 );
				for ( AproposLabel label : list ) {
					c.gridy++ ;
					add( new AproposLabelSimple(label), c );
				}
			}
		}
		
//		for ( String key : files.keySet() ) {
//			c.insets = new Insets( 25, 10, 5, 10 );
//			c.gridy++ ;
//			c.gridx = 0;
//			c.gridwidth = 1;
//			c.gridheight = 1;
//			jl = new JLabel( key );
//			add( jl, c );
//			TreeMap<JLabel, ArrayList<EditableJLabel>> scene = new TreeMap<JLabel, ArrayList<EditableJLabel>>(
//					new OrderL() );
//			put( jl, scene );
//			System.out.println( key );
//			try ( Reader reader = new InputStreamReader( new FileInputStream( files.get( key ) ) ) ) {
//				Stage s = g.fromJson( reader, Stage.class ).build();
//				if ( s != null ) for ( String per : s.strings.keySet() ) {
//					String[] strings = s.strings.get( per );
//					c.insets = new Insets( 0, 40, 0, 5 );
//					c.gridy++ ;
//					jl = new JLabel( per );
//					add( jl, c );
//					c.insets = new Insets( 0, 70, 0, 5 );
//					ArrayList<EditableJLabel> perspec = new ArrayList<EditableJLabel>();
//					scene.put( jl, perspec );
//					for ( String str : strings ) {
//						c.gridy++ ;
//						ejl = new EditableJLabel( str );
//						add( ejl, c );
//						perspec.add( ejl );
//					}
//					if ( strings.length > 1 | !strings[0].equals( "" ) ) {
//						c.gridy++ ;
//						add( new EditableJLabel( "" ), c );
//					}
//				}
//			}
//			catch ( JsonSyntaxException e ) {
//				System.err.println( "Error parsing " + files.get( key ).getAbsolutePath().replace( db, "\\db\\" ) );
//				System.err.println( e.getMessage() );
//			}
//			catch ( IOException e ) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
		
		c.insets = new Insets( 0, 3, 0, 3 );
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridwidth = 2;
		c.weighty = 1;
		c.gridy++ ;
		c.gridx = 0;
		add( new JSeparator(), c );
		
		revalidate();
	}
	
}
