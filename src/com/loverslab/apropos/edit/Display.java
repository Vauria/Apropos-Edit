package com.loverslab.apropos.edit;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.loverslab.apropos.edit.Prototype.Stage;

@SuppressWarnings("serial")
class Display<T extends JPanel> extends JPanel {
	
	JPanel panel;
	JMenuBar menu;
	JMenu file;
	GridBagLayout gbl;
	TreeMap<JLabel, TreeMap<JLabel, ArrayList<T>>> data = new TreeMap<JLabel, TreeMap<JLabel, ArrayList<T>>>(
			new Prototype().new OrderL() );
	
	public static void main( String[] args ) {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		try {
			SwingUtilities.invokeAndWait( new Runnable() {
				public void run() {
					JFrame frame = new JFrame( "I'M FUCKING DONE" );
					frame.setSize( 1000, 1200 );
					
					frame.setContentPane( new JScrollPane( new Display<AproposLabelSimple>() ) );
					( (JScrollPane) frame.getContentPane() ).getVerticalScrollBar().setUnitIncrement( 16 );
					
					frame.setVisible( true );
				}
			} );
		}
		catch ( InvocationTargetException | InterruptedException e1 ) {
			e1.printStackTrace();
		}
	}
	
	public Display() {
		gbl = new GridBagLayout() {
			public GridBagConstraints getConstraints( Component comp ) {
				return lookupConstraints( comp );
			}
		};
		setLayout( gbl );
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0d;
		
		JLabel jl = null;
		T ejl = null;
		
		TreeMap<String, File> files = new TreeMap<String, File>();
			files.put( "Intro", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation.txt") );
			files.put( "Stage 1", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation_Stage1.txt") );
			files.put( "Stage 2", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation_Stage2.txt") );
			files.put( "Stage 3", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation_Stage3.txt") );
			files.put( "Stage 4", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation_Stage4.txt") );
			files.put( "Orgasm", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation_Orgasm.txt") );
		
		for ( String key : files.keySet() ) {
			c.insets = new Insets( 25, 10, 5, 10 );
			c.gridy++ ;
			c.gridx = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			jl = new JLabel( key );
			add( jl, c );
			TreeMap<JLabel, ArrayList<T>> scene = new TreeMap<JLabel, ArrayList<T>>( new Prototype().new OrderL() );
			data.put( jl, scene );
			System.out.println( key );
			try ( Reader reader = new InputStreamReader( new FileInputStream( files.get( key ) ) ) ) {
				Gson g = new Gson();
				Stage s = g.fromJson( reader, Stage.class ).build();
				if ( s != null ) for ( String per : s.strings.keySet() ) {
					String[] strings = s.strings.get( per );
					c.insets = new Insets( 0, 40, 0, 5 );
					c.gridy++ ;
					jl = new JLabel( per );
					add( jl, c );
					c.insets = new Insets( 0, 70, 0, 5 );
					ArrayList<T> perspec = new ArrayList<T>();
					scene.put( jl, perspec );
					for ( String str : strings ) {
						c.gridy++ ;
						ejl = (T) new AproposLabelSimple( str );
						add( ejl, c );
						perspec.add( ejl );
					}
					if ( strings.length > 1 | !strings[0].equals( "" ) ) {
						c.gridy++ ;
						add( (T) new AproposLabelSimple( "" ), c );
					}
				}
			}
			catch ( JsonSyntaxException e ) {
					System.err.println( "Error parsing " + files.get( key ).getAbsolutePath().replace( "C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\", "\\db\\" ) );
				System.err.println( e.getMessage() );
			}
			catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//revalidate();
		
	}
	
	public void insert( JComponent add, JComponent above ) {
		int sceneY = -1, persY = -1;
		Component sceneMarker = null, persMarker = null;
		GridBagConstraints point = gbl.getConstraints( above );
		for ( Component comp : getComponents() ) {
			GridBagConstraints c = gbl.getConstraints( comp );
			if ( c.gridy > point.gridy ) {
				c.gridy++ ;
				comp.invalidate();
			}
			else {
				/*
				 * System.out.println(
				 * "In: " + c.insets.left + ", Y: " + c.gridy + ", : " + ( ( comp instanceof T )
				 * ? ( (T) comp ).getText() : ( (JLabel) comp ).getText() ) );
				 */
				if ( c.insets.left == 10 & c.gridy > sceneY ) {
					sceneY = c.gridy;
					sceneMarker = comp;
				}
				if ( c.insets.left == 40 & c.gridy > persY ) {
					persY = c.gridy;
					persMarker = comp;
				}
			}
		}
		GridBagConstraints a = (GridBagConstraints) point.clone();
		a.gridy++ ;
		add( add, a );
		JLabel scene = ( (JLabel) sceneMarker );
		JLabel pers = ( (JLabel) persMarker );
		T old = (T) above;
		System.out.println( "Adding \"" + old.toString() + "\" to " + scene.getText() + ": " + pers.getText() );
		data.get( scene ).get( pers ).add( old );
		
		for ( JLabel k1 : data.keySet() ) {
			System.out.println( k1.getText() + ": " + data.get( k1 ).size() );
			for ( JLabel k2 : data.get( k1 ).keySet() ) {
				System.out.println( "\t" + k2.getText() + ": " + data.get( k1 ).get( k2 ).size() );
				for ( T s : data.get( k1 ).get( k2 ) )
					System.out.println( "\t\t" + s.toString() );
			}
		}
	}
	
}