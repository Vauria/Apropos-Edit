package com.loverslab.apropos.edit;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

@SuppressWarnings("serial")
public class Prototype {
	
	// Variables
	//String db = "C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\";
	String db = "E:\\User Files\\Dumps\\Workspace\\Apropos Diffing\\dbOfficial\\";
	String outputDir = db + "Combined\\";
	String folder = "FemaleActor_SexLabAggrMissionary";
	String active = "Vilkas";
	String primary = "Kaylee";
	String path = db + folder + "\\";
	String name;
	Map<String, File> files;
	Map<String, List<String>> synonyms;
	Position pos = Position.Unique;
	boolean rape = false;
	int p /* erspective */ = 1;
	Gson g = new Gson();
	
	public static void main( String[] args ) throws Exception {
		Prototype ae = new Prototype();
		ae.init();
		ae.findPositions();
		// ae.synonyms();
		// ae.findBrokenReferences();
		
		if ( ae.files.size() > 0 )
			// ae.combine();
			// ae.simulate();
			//ae.display();
			;
		else
			System.out.println( "No files found" );
		
	}
	
	public void init() {
		name = folder + ( pos != Position.Unique ? "_" + pos.name() : "" ) + ( rape ? "_Rape" : "" );
		files = new TreeMap<String, File>( new Order() );
		File file = new File( path + name + ".txt" );
		if ( file.exists() ) files.put( "Intro", file );
		int i = 1;
		while ( ( file = new File( path + name + "_Stage" + i + ".txt" ) ).exists() ) {
			files.put( "Stage " + i, file );
			i++ ;
		}
		file = new File( path + name + "_Orgasm.txt" );
		if ( file.exists() ) files.put( "Orgasm", file );
		System.out.println( files );
	}
	
	public void synonyms() {
		synonyms = new TreeMap<String, List<String>>();
		File file = new File( db + "Synonyms.txt" );
		try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
			reader.beginObject();
			while ( reader.hasNext() ) {
				String key = reader.nextName();
				List<String> list = new ArrayList<String>();
				reader.beginArray();
				while ( reader.hasNext() )
					list.add( reader.nextString() );
				reader.endArray();
				System.out.println( key + ":" + concatenate( list.toArray( new String[ 0 ] ), ", " ) );
				synonyms.put( key, list );
			}
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		catch ( IllegalStateException e ) {
			System.err.println( "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) );
			System.err.println( e.getMessage() );
		}
		file = new File( db + "Arousal_Descriptors.txt" );
		try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
			reader.beginObject();
			while ( reader.hasNext() ) {
				String key = reader.nextName();
				List<String> list = new ArrayList<String>();
				reader.beginObject();
				while ( reader.hasNext() ) {
					reader.nextName();
					reader.beginArray();
					while ( reader.hasNext() )
						list.add( reader.nextString() );
					reader.endArray();
				}
				reader.endObject();
				System.out.println( key + ":" + concatenate( list.toArray( new String[ 0 ] ), ", " ) );
				synonyms.put( key, list );
			}
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		catch ( IllegalStateException e ) {
			System.err.println( "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) );
			System.err.println( e.getMessage() );
		}
		file = new File( db + "WearAndTear_Descriptors.txt" );
		try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
			reader.beginObject();
			reader.nextName();
			List<String> list = new ArrayList<String>();
			reader.beginObject();
			while ( reader.hasNext() ) {
				reader.nextName();
				reader.beginArray();
				while ( reader.hasNext() )
					list.add( reader.nextString() );
				reader.endArray();
			}
			reader.endObject();
			System.out.println( "{WT}" + ":" + concatenate( list.toArray( new String[ 0 ] ), ", " ) );
			synonyms.put( "{WTVAGINAL}", list );
			synonyms.put( "{WTORAL}", list );
			synonyms.put( "{WTANAL}", list );
		}
		catch ( IllegalStateException e ) {
			System.err.println( "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) );
			System.err.println( e.getMessage() );
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		synonyms.put( "{PRIMARY}", new ArrayList<String>() {
			{
				add( primary );
			}
		} );
		synonyms.put( "{ACTIVE}", new ArrayList<String>() {
			{
				add( active );
			}
		} );
	}
	
	public String insert( String str ) {
		if ( synonyms.size() == 0 ) return str;
		Random r = new Random();
		for ( String key : synonyms.keySet() ) {
			List<String> list = synonyms.get( key );
			if ( list.size() == 0 ) continue;
			String synonym = list.get( list.size() == 1 ? 0 : r.nextInt( list.size() ) );
			str = str.replace( key, synonym );
		}
		return str;
	}
	
	public void simulate() {
		Random r = new Random();
		for ( String key : files.keySet() ) {
			try ( Reader reader = new InputStreamReader( new FileInputStream( files.get( key ) ) ) ) {
				Stage s = g.fromJson( reader, Stage.class );
				if ( s.first.length == 0 ) continue;
				String str = s.first[s.first.length == 1 ? 0 : r.nextInt( s.first.length - 1 )];
				System.out.println( key + ": " + insert( str ) );
			}
			catch ( IOException e ) {
				e.printStackTrace();
			}
		}
	}
	
	public void findBrokenReferences() {
		ReferenceChecker checker = new ReferenceChecker();
		try {
			Files.walkFileTree( Paths.get( db ), checker );
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		checker.results();
	}
	
	public void findPositions() {
		PositionFinder finder = new PositionFinder();
		try {
			Files.walkFileTree( Paths.get( db ), finder );
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		finder.results();
	}
	
	public void combine() {
		File output = new File( outputDir + name + "_All.txt" );
		new File( outputDir ).mkdirs();
		try ( JsonWriter writer = new JsonWriter( new FileWriter( output ) ) ) {
			writer.setIndent( "    " );
			writer.beginObject();
			for ( String key : files.keySet() ) {
				writer.name( key );
				writer.beginArray();
				System.out.println( key );
				try ( Reader reader = new InputStreamReader( new FileInputStream( files.get( key ) ) ) ) {
					Stage s = g.fromJson( reader, Stage.class );
					String[] strings = ( p == 1 ? s.first : ( p == 2 ? s.second : s.third ) );
					for ( String str : strings )
						writer.value( str );
				}
				writer.endArray();
			}
			writer.endObject();
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
	}
	
	public void display() {
		new Display();
	}
	
	public static String concatenate( String[] array, String sep ) {
		StringBuilder builder = new StringBuilder();
		for ( int i = 0; i < array.length; i++ ) {
			if ( i > 0 ) builder.append( sep );
			builder.append( array[i] );
		}
		return builder.toString();
	}
	
	class Stage {
		@SerializedName("1st Person")
		String[] first;
		@SerializedName("2nd Person")
		String[] second;
		@SerializedName("3rd Person")
		String[] third;
		Map<String, String[]> strings;
		
		public Stage( String[] first, String[] second, String[] third ) {
			super();
			this.first = first;
			this.second = second;
			this.third = third;
		}
		
		public Stage build() {
			strings = new TreeMap<String, String[]>();
			strings.put( "1st Person", first );
			strings.put( "2nd Person", second );
			strings.put( "3rd Person", third );
			return this;
		}
	}
	
	class ReferenceChecker extends SimpleFileVisitor<Path> {
		
		int checked;
		int errors;
		Set<String> missing = new TreeSet<String>();
		
		public FileVisitResult visitFile( Path file, BasicFileAttributes attr ) {
			if ( file.toFile().getName().endsWith( ".txt" ) ) {
				try ( Reader reader = new InputStreamReader( new FileInputStream( file.toFile() ) ) ) {
					Stage s = g.fromJson( reader, Stage.class );
					if ( s == null ) {
						System.err.println( "File is empty: " + file.toFile().getAbsolutePath().replace( db, "\\db\\" ) );
						return FileVisitResult.CONTINUE;
					}
					checked++ ;
					if ( s.first != null ) for ( String str : s.first )
						check( str, file );
					if ( s.second != null ) for ( String str : s.second )
						check( str, file );
					if ( s.third != null ) for ( String str : s.third )
						check( str, file );
				}
				catch ( IOException e ) {
					e.printStackTrace();
				}
				catch ( JsonSyntaxException e ) {
					System.err.println( "Error parsing " + file.toFile().getAbsolutePath().replace( db, "\\db\\" ) );
					System.err.println( e.getMessage() );
				}
			}
			return FileVisitResult.CONTINUE;
		}
		
		public void check( String str, Path file ) {
			Pattern p = Pattern.compile( "\\{[A-Z]*\\}" );
			Matcher m = p.matcher( str );
			while ( m.find() ) {
				String match = m.group();
				if ( !synonyms.containsKey( match ) ) {
					System.out.println( "Unused Tag " + match + " in " + file.toFile().getAbsolutePath().replace( db, "\\db\\" ) + ", \""
							+ str + "\"" );
					missing.add( match );
					errors++ ;
				}
			}
		}
		
		public void results() {
			System.out.println( "Tags undefined in Synonym files: " + missing.toString() );
			System.out.println( errors + " errors found. " + checked + " files total" );
		}
		
	}
	
	class PositionFinder extends SimpleFileVisitor<Path> {
		
		Set<String> positions = new TreeSet<String>();
		
		public FileVisitResult visitFile( Path path, BasicFileAttributes attr ) {
			File file = path.toFile();
			if ( file.getName().endsWith( ".txt" ) && !file.getParentFile().getName().equals( "dbOfficial" ) ) {
				String name = file.getName().replace( ".txt", "" ).replace( file.getParentFile().getName(), "" );
				positions.addAll( Arrays.asList( name.split( "_" ) ) );
			}
			return FileVisitResult.CONTINUE;
		}
		
		public void results() {
			System.out.println( "All Positions: " + positions.toString() );
		}
		
	}
	
	class Order implements Comparator<String> {
		
		public int compare( String o1, String o2 ) {
			if ( o1.equals( o2 ) ) return 0;
			if ( o1.equals( "Intro" ) ) return -1;
			if ( o2.equals( "Intro" ) ) return 1;
			if ( o1.equals( "Orgasm" ) ) return 1;
			if ( o2.equals( "Orgasm" ) ) return -1;
			return o1.compareTo( o2 );
		}
		
	}
	
	class OrderL implements Comparator<JLabel> {
		
		public int compare( JLabel o1, JLabel o2 ) {
			if ( o1.getText().equals( o2.getText() ) ) return 0;
			if ( o1.getText().equals( "Intro" ) ) return -1;
			if ( o2.getText().equals( "Intro" ) ) return 1;
			if ( o1.getText().equals( "Orgasm" ) ) return 1;
			if ( o2.getText().equals( "Orgasm" ) ) return -1;
			return o1.getText().compareTo( o2.getText() );
		}
		
	}
	
	class Display extends JFrame {
		
		JPanel panel;
		JMenuBar menu;
		JMenu file;
		GridBagLayout gbl;
		TreeMap<JLabel, TreeMap<JLabel, ArrayList<EditableJLabel>>> data = new TreeMap<JLabel, TreeMap<JLabel, ArrayList<EditableJLabel>>>(
				new OrderL() );
		
		public Display() {
			setTitle( name );
			
			DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
			int w = (int) ( dm.getWidth() * 0.8f );
			int h = (int) ( dm.getHeight() * 0.8f );
			setSize( w, h );
			// setLocationRelativeTo( null ); // Centres the component
			setMinimumSize( new Dimension( 500, 500 ) );
			
			setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
			
			menu = new JMenuBar();
			// this.setJMenuBar( menu );
			file = new JMenu( "File" );
			menu.add( file );
			JMenuItem write = new JMenuItem( "Write" );
			file.add( write );
			
			panel = new JPanel();
			
			gbl = new GridBagLayout() {
				public GridBagConstraints getConstraints( Component comp ) {
					return lookupConstraints( comp );
				}
			};
			panel.setLayout( gbl );
			JScrollPane scroll = new JScrollPane( panel );
			scroll.getVerticalScrollBar().setUnitIncrement( 16 );
			add( scroll );
			
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0d;
			
			JLabel jl = null;
			EditableJLabel ejl = null;
			for ( String key : files.keySet() ) {
				c.insets = new Insets( 25, 10, 5, 10 );
				c.gridy++ ;
				c.gridx = 0;
				c.gridwidth = 1;
				c.gridheight = 1;
				jl = new JLabel( key );
				panel.add( jl, c );
				TreeMap<JLabel, ArrayList<EditableJLabel>> scene = new TreeMap<JLabel, ArrayList<EditableJLabel>>( new OrderL() );
				data.put( jl, scene );
				System.out.println( key );
				try ( Reader reader = new InputStreamReader( new FileInputStream( files.get( key ) ) ) ) {
					Stage s = g.fromJson( reader, Stage.class ).build();
					if ( s != null ) for ( String per : s.strings.keySet() ) {
						String[] strings = s.strings.get( per );
						c.insets = new Insets( 0, 40, 0, 5 );
						c.gridy++ ;
						jl = new JLabel( per );
						panel.add( jl, c );
						c.insets = new Insets( 0, 70, 0, 5 );
						ArrayList<EditableJLabel> perspec = new ArrayList<EditableJLabel>();
						scene.put( jl, perspec );
						for ( String str : strings ) {
							c.gridy++ ;
							ejl = new EditableJLabel( str );
							panel.add( ejl, c );
							perspec.add( ejl );
						}
						if ( strings.length > 1 | !strings[0].equals( "" ) ) {
							c.gridy++ ;
							panel.add( new EditableJLabel( "" ), c );
						}
					}
				}
				catch ( JsonSyntaxException e ) {
					System.err.println( "Error parsing " + files.get( key ).getAbsolutePath().replace( db, "\\db\\" ) );
					System.err.println( e.getMessage() );
				}
				catch ( IOException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			setVisible( true );
		}
		
		public void insert( JComponent add, JComponent above ) {
			int sceneY = -1, persY = -1;
			Component sceneMarker = null, persMarker = null;
			GridBagConstraints point = gbl.getConstraints( above );
			for ( Component comp : panel.getComponents() ) {
				GridBagConstraints c = gbl.getConstraints( comp );
				if ( c.gridy > point.gridy ) {
					c.gridy++ ;
					comp.invalidate();
				}
				else {
					/*
					 * System.out.println(
					 * "In: " + c.insets.left + ", Y: " + c.gridy + ", : " + ( ( comp instanceof EditableJLabel )
					 * ? ( (EditableJLabel) comp ).getText() : ( (JLabel) comp ).getText() ) );
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
			panel.add( add, a );
			JLabel scene = ( (JLabel) sceneMarker );
			JLabel pers = ( (JLabel) persMarker );
			EditableJLabel old = (EditableJLabel) above;
			System.out.println( "Adding \"" + old.getText() + "\" to " + scene.getText() + ": " + pers.getText() );
			data.get( scene ).get( pers ).add( old );
			
			for ( JLabel k1 : data.keySet() ) {
				System.out.println( k1.getText() + ": " + data.get( k1 ).size() );
				for ( JLabel k2 : data.get( k1 ).keySet() ) {
					System.out.println( "\t" + k2.getText() + ": " + data.get( k1 ).get( k2 ).size() );
					for ( EditableJLabel s : data.get( k1 ).get( k2 ) )
						System.out.println( "\t\t" + s.getText() );
				}
			}
		}
		
		class EditableJLabel extends JPanel {
			
			private JLabel label;
			private JTextField textField;
			private LinkedList<ValueChangedListener> listeners = new LinkedList<ValueChangedListener>();
			
			public EditableJLabel( String startText ) {
				super();
				
				// Create the listener and the layout
				CardLayout layout = new CardLayout( 0, 0 );
				this.setLayout( layout );
				EditableListener hl = new EditableListener();
				
				// Create the JPanel for the "normal" state
				JPanel labelPanel = new JPanel( new GridLayout( 1, 1 ) );
				label = new JLabel( startText.equals( "" ) ? "<add new>" : startText );
				labelPanel.add( label );
				
				// Create the JPanel for the "hover state"
				JPanel inputPanel = new JPanel( new GridLayout( 1, 1 ) );
				textField = new JTextField( startText );
				textField.addMouseListener( hl );
				textField.addKeyListener( hl );
				textField.addFocusListener( hl );
				inputPanel.add( textField );
				
				this.addMouseListener( hl );
				
				// Set the states
				this.add( labelPanel, "NORMAL" );
				this.add( inputPanel, "HOVER" );
				
				// Show the correct panel to begin with
				layout.show( this, "NORMAL" );
			}
			
			public void setText( String text ) {
				System.out.println( text );
				if ( getText().equals( "" ) ) {
					if ( !text.equals( "" ) ) {
						this.label.setText( text );
						EditableJLabel add = new EditableJLabel( "" );
						add.setHoverState( true );
						insert( add, this );
						add.textField.grabFocus();
					}
				}
				else if ( text.equals( "" ) )
					this.label.setText( "<add new>" );
				else
					this.label.setText( text );
				this.textField.setText( text );
			}
			
			public String getText() {
				String text = this.label.getText();
				return text.equals( "<add new>" ) ? "" : text;
			}
			
			public JTextField getTextField() {
				return textField;
			}
			
			public JLabel getLabel() {
				return label;
			}
			
			public void setHoverState( boolean hover ) {
				CardLayout cl = (CardLayout) ( this.getLayout() );
				
				if ( hover )
					cl.show( this, "HOVER" );
				else
					cl.show( this, "NORMAL" );
			}
			
			public void addValueChangedListener( ValueChangedListener l ) {
				this.listeners.add( l );
			}
			
			public int positionFromPoint( int x, String s ) {
				int w = label.getGraphics().getFontMetrics().stringWidth( s );
				float pos = ( (float) x / (float) w ) * (float) s.length();
				return Math.min( (int) pos, s.length() );
			}
			
			public class EditableListener implements MouseListener, KeyListener, FocusListener {
				boolean locked = false;
				String oldValue;
				
				public void focusGained( FocusEvent arg0 ) {
					oldValue = textField.getText();
				}
				public void mouseClicked( MouseEvent e ) {
					if ( e.getClickCount() == 2 ) {
						setHoverState( true );
						textField.grabFocus();
						textField.setCaretPosition( positionFromPoint( e.getX(), textField.getText() ) );
					}
				}
				public void release() {
					this.locked = false;
				}
				public void focusLost( FocusEvent e ) {
					if ( !locked ) setText( oldValue );
					setHoverState( false );
					release();
					mouseExited( null );
				}
				public void keyTyped( KeyEvent e ) {
					if ( e.getKeyChar() == KeyEvent.VK_ENTER ) {
						setText( textField.getText() );
						for ( ValueChangedListener v : listeners ) {
							v.valueChanged( textField.getText(), EditableJLabel.this );
						}
						setHoverState( false );
						locked = true;
						mouseExited( null );
					}
					else if ( e.getKeyChar() == KeyEvent.VK_ESCAPE ) {
						setHoverState( false );
						release();
						setText( oldValue );
					}
				}
				public void mousePressed( MouseEvent e ) {}
				public void mouseReleased( MouseEvent e ) {}
				public void keyPressed( KeyEvent e ) {}
				public void keyReleased( KeyEvent e ) {}
				public void mouseEntered( MouseEvent e ) {}
				public void mouseExited( MouseEvent e ) {}
			}
			
		}
		
	}
	
}

/**
 * A listener for the EditableJLabel. Called when the value of the JLabel is
 * updated.
 * 
 * @author James McMinn
 * 
 */
/*
 * interface ValueChangedListener {
 * public void valueChanged( String value, JComponent source );
 * }
 */
