package com.loverslab.apropos.edit;

import java.awt.FontMetrics;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.SwingWorker;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;

/**
 * Model class to handle all database interaction
 *
 */
public class Model {
	
	String db;
	View view;
	AproposLabel root;
	TreeMap<String, Boolean> uniques = null;
	TreeMap<String, ArrayList<String>> synonyms;
	SynonymsLengthMap synonymsLengths;
	//@formatter:off
	static HashMap<BytePair, String[][]> shiftTable = new HashMap<BytePair, String[][]>() { private static final long serialVersionUID = -7986832642422472332L; {
		put( new BytePair( 1, 2 ), new String[][] { 
			new String[] { "^ I'm( |\\p{Punct})", " You're$1" }, 
			new String[] { "([\\.?!]) I'm( |\\p{Punct})", "$1 You're$2" }, 
			new String[] { " I'm( |\\p{Punct})", " you're$1" }, 
			new String[] { "^ I was", " You were" }, 
			new String[] { "([\\.?!]) I was", "$1 You were" }, 
			new String[] { " I was", " you were" }, 
			new String[] { "^ I((?:'[\\w]+)?)( |\\p{Punct})", " You$1$2" }, 
			new String[] { "([\\.?!]) I((?:'[\\w]+)?)( |\\p{Punct})", "$1 You$2$3" }, 
			new String[] { " I((?:'[\\w]+)?)( |\\p{Punct})", " you$1$2" }, 
			new String[] { " my( |\\p{Punct})", " your$1" }, 
			new String[] { " My( |\\p{Punct})", " Your$1" }, 
			new String[] { " me( |\\p{Punct})", " you$1" },
			new String[] { " Me( |\\p{Punct})", " You$1" },
			new String[] { " mine( |\\p{Punct})", " yours$1" }, 
			new String[] { " Mine( |\\p{Punct})", " Yours$1" },
			new String[] { " myself( |\\p{Punct})", " yourself$1" },
			new String[] { " Myself( |\\p{Punct})", " Yourself$1" },
			new String[] { " am( |\\p{Punct})", " are$1" },
			new String[] { " Am( |\\p{Punct})", " Are$1" },
		});
		put( new BytePair( 2, 1 ), new String[][] { 
			new String[] { " (?:y|Y)ou're( |\\p{Punct})", " I'm$1" }, 
			new String[] { " (?:y|Y)ou were", " I was" }, 
			new String[] { " (?:y|Y)ou((?:'[\\w]+)?)( |\\p{Punct})", " I$1$2" }, 
			new String[] { " your( |\\p{Punct})", " my$1" }, 
			new String[] { " Your( |\\p{Punct})", " My$1" },
			new String[] { " yours( |\\p{Punct})", " mine$1" }, 
			new String[] { " Yours( |\\p{Punct})", " Mine$1" },
			new String[] { " yourself( |\\p{Punct})", " myself$1" },
			new String[] { " Yourself( |\\p{Punct})", " Myself$1" },
		});
	}};
	//@formatter:on
	public Model( View view ) {
		super();
		this.view = view;
	}
	
	/**
	 * Sets the Filepath of the chosen database's root folder.
	 * 
	 * @param path Filepath. Should not end in \
	 */
	public void setDataBase( String path ) {
		db = path + "\\";
		root = new AproposLabel( db, null );
		uniques = null;
		new UniquesFetcher().execute();
		new SynonymsFetcher().execute();
	}
	
	/**
	 * Composes the three variables to match the filename structure of the database
	 * 
	 * @param folder The folder the file would be stored in, like FemaleActor_Male or FemaleActor_DarkInvestigationsBlowjob
	 * @param pos The position within this folder
	 * @param rape If the context is non-consensual
	 * @return Composed String
	 */
	public static String getAnimString( String folder, Position pos, boolean rape ) {
		return folder + ( pos != Position.Unique ? "_" + pos.name() : "" ) + ( rape ? "_Rape" : "" );
	}
	
	/**
	 * Provided the files for the animation denoted by the parent label exist, parses them and adds them to the returned StageMap
	 * 
	 * @param parent Text for this label should be an animation, Text for it's parent should be a folder
	 */
	public StageMap getStages( AproposLabel parent ) {
		StageMap data = new StageMap();
		String path = db + parent.getParentLabel().getText() + "\\" + parent.getText();
		System.out.println( path );
		File file = new File( path + ".txt" );
		if ( file.exists() ) {
			AproposLabel stage = new AproposLabel( "Intro", parent );
			data.put( stage, getPerspectives( stage, file ) );
		}
		file = new File( path + "_Stage1.txt" ); // Official database skips stage 1 to use the Intro file instead, so a missing stage one
												 // does not imply a missing stage 2+
		if ( file.exists() ) {
			AproposLabel stage = new AproposLabel( "Stage 1", parent );
			data.put( stage, getPerspectives( stage, file ) );
		}
		int i = 2;
		while ( ( file = new File( path + "_Stage" + i + ".txt" ) ).exists() ) {
			AproposLabel stage = new AproposLabel( "Stage " + i, parent );
			data.put( stage, getPerspectives( stage, file ) );
			i++ ;
		}
		file = new File( path + "_Orgasm.txt" );
		if ( file.exists() ) {
			AproposLabel stage = new AproposLabel( "Orgasm", parent );
			data.put( stage, getPerspectives( stage, file ) );
		}
		if ( data.size() == 0 ) view.handleException( new Exception( "No files found" ) );
		return data;
	}
	
	/**
	 * Returns all the perspectives (and in turn their lines) accessible from the given <code>File</code>. Uses lazy exception handling
	 * (prints to sys.err)
	 * 
	 * @param parent Label that will be the parent of every perspective label
	 * @param file object representation of the desired animation file
	 * @return
	 */
	public PerspectiveMap getPerspectives( AproposLabel parent, File file ) {
		PerspectiveMap data = new PerspectiveMap();
		try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
			data = getPerspectives( parent, reader );
		}
		catch ( IllegalStateException | MalformedJsonException e ) {
			String message = "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) + " (" + e.getMessage() + ")";
			view.handleException( new IllegalStateException( message, e ) );
		}
		catch ( IOException e ) {
			view.handleException( e );
			e.printStackTrace();
		}
		return data;
	}
	
	/**
	 * Returns all the perspectives (and in turn their lines) accessible from the given <code>JsonReader</code>
	 * 
	 * @param parent Label that will be the parent of every perspective label
	 * @param reader Reader for the json file. Caller is responsible for closing
	 * @throws IllegalStateException If JSON errors occur
	 * @throws IOException If file errors occur
	 */
	public PerspectiveMap getPerspectives( AproposLabel parent, JsonReader reader ) throws IllegalStateException, IOException {
		PerspectiveMap data = new PerspectiveMap();
		reader.beginObject();
		while ( reader.hasNext() ) {
			AproposLabel key = new AproposLabel( reader.nextName(), parent );
			LabelList list = new LabelList();
			reader.beginArray();
			while ( reader.hasNext() )
				list.add( new AproposLabel( reader.nextString(), key ) );
			reader.endArray();
			if ( list.size() == 0 ) {
				list.add( new AproposLabel( "", key ) );
			}
			else if ( ! ( list.size() == 1 & list.get( 0 ).getText().equals( "" ) ) ) {
				list.add( new AproposLabel( "", key ) );
			}
			data.put( key, list );
		}
		return data;
	}
	
	/**
	 * Writes the lines, perspectives and stages contained within the given StageMap. Write location is determined by the parents of the
	 * contained labels, and any missing directories denoted by these labels will be created before writing
	 * 
	 * @param stageMap
	 */
	public void writeStages( StageMap stageMap ) {
		File file;
		AproposLabel first = stageMap.firstKey();
		String folder = db + first.getParentLabel().getParentLabel().getText();
		new File( folder ).mkdirs();
		String path = folder + "\\" + first.getParentLabel().getText();
		for ( AproposLabel stage : stageMap.keySet() ) {
			PerspectiveMap persMap = stageMap.get( stage );
			String[] split = stage.getText().split( " " );
			String key = split[0];
			switch ( key ) {
				case "Intro":
					file = new File( path + ".txt" );
					break;
				case "Stage":
					file = new File( path + "_Stage" + split[1] + ".txt" );
					break;
				case "Orgasm":
					file = new File( path + "_Orgasm.txt" );
					break;
				default:
					view.handleException( new IllegalStateException( "Unsupported Stage Identifier: " + key ) );
					return;
			}
			writePerspectives( persMap, file );
		}
	}
	
	/**
	 * Writes the given PerspectiveMap to the given file, writing a JSON array for each perspective. Caller must ensure the folder the file
	 * is to be written to exists
	 * 
	 * @param persMap
	 * @param file
	 */
	public void writePerspectives( PerspectiveMap persMap, File file ) {
		try ( JsonWriter writer = new JsonWriter( new FileWriter( file ) ) ) {
			writer.setIndent( "    " );
			writer.beginObject();
			for ( AproposLabel perspec : persMap.keySet() ) {
				LabelList list = persMap.get( perspec );
				writer.name( perspec.getText() );
				writer.beginArray();
				for ( AproposLabel label : list ) {
					String text = label.getText();
					if ( !text.equals( "" ) )
						writer.value( text );
					else if ( list.size() == 1 ) writer.value( text );
				}
				writer.endArray();
			}
			writer.endObject();
		}
		catch ( IOException e ) {
			view.handleException( e );
			e.printStackTrace();
		}
	}
	
	public void deleteStage( AproposLabel stageLabel ) {
		String path = db + stageLabel.getParentLabel().getParentLabel().getText() + "\\" + stageLabel.getParentLabel().getText();
		String[] split = stageLabel.getText().split( " " );
		String key = split[0];
		File file;
		switch ( key ) {
			case "Intro":
				file = new File( path + ".txt" );
				break;
			case "Stage":
				file = new File( path + "_Stage" + split[1] + ".txt" );
				break;
			case "Orgasm":
				file = new File( path + "_Orgasm.txt" );
				break;
			default:
				view.handleException( new IllegalStateException( "Unsupported Stage Identifier: " + key ) );
				return;
		}
		file.delete();
	}
	
	/**
	 * Checks a string against UniqueAnimations.txt
	 * 
	 * @param string
	 * @return True if the given Animation name appears on the UniqueAnimations list.
	 */
	public boolean isUnique( String string ) {
		if ( uniques == null | !string.contains( "_" ) ) return false;
		string = string.substring( string.lastIndexOf( '_' ) + 1 );
		Boolean b = uniques.get( string );
		System.out.println( string + ": " + b );
		return b == null ? false : b;
	}
	
	public void writeUniques() {
		File file = new File( db + "UniqueAnimations.txt" );
		try ( JsonWriter writer = new JsonWriter( new FileWriter( file ) ) ) {
			writer.setIndent( "    " );
			writer.beginObject();
			for ( String key : uniques.keySet() ) {
				writer.name( key );
				writer.value( uniques.get( key ) );
			}
			writer.endObject();
		}
		catch ( IllegalStateException | MalformedJsonException e ) {
			String message = "Error writing " + file.getAbsolutePath().replace( db, "\\db\\" ) + " (" + e.getMessage() + ")";
			view.handleException( new IllegalStateException( message, e ) );
		}
		catch ( IOException e ) {
			view.handleException( e );
			e.printStackTrace();
		}
	}
	
	/**
	 * Extracts the folder part of an animation file name
	 * 
	 * @param animString
	 * @return
	 */
	public String extractFolder( String animString ) {
		animString = animString.replace( ".txt", "" ).replace( "_Rape", "" ).replace( "_Orgasm", "" ).replaceAll( "_Stage[1-9]+", "" );
		for ( Position p : Position.values() )
			animString = animString.replaceAll( "_" + p.name() + "$", "" );
		return animString;
	}
	
	public Position getPosition( String animString ) {
		return getPosition( extractFolder( animString ), animString );
	}
	
	public Position getPosition( String folder, String animString ) {
		String position = animString.replace( ".txt", "" ).replace( "_Rape", "" ).replace( "_Orgasm", "" ).replaceAll( "_Stage[1-9]+", "" )
				.replaceAll( "(?i)" + Pattern.quote( folder ), "" ).replaceAll( "^_", "" );
		Position p = Position.lookup( position );
		if ( p == null ) {
			if ( position.equals( "" ) )
				return Position.Unique;
			else
				System.err.println( "Unknown Position: \"" + position + "\" (" + folder + "\\" + animString + ")" );
			return null;
		}
		return p;
	}
	
	public Position[] getPositions( String folder ) {
		PositionFinder finder = new PositionFinder( folder );
		try {
			Files.walkFileTree( Paths.get( db + folder ), finder );
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		return finder.getPositions();
	}
	
	public int hasRape( String animString ) {
		return hasRape( extractFolder( animString ), animString );
	}
	
	/**
	 * Checks if this animation has the normal files, rape files or both
	 * 
	 * @param folder
	 * @param animString
	 * @return Neither=0, ConsOnly=1, RapeOnly=2, Both=3
	 */
	public int hasRape( String folder, String animString ) {
		animString = animString.replace( "_Rape", "" );
		String path = db + folder + "//" + animString;
		String[] stages = new String[] { ".txt", "_Stage1.txt", "_StageN.txt", "_Orgasm.txt" };
		boolean consen = false, rape = false;
		for ( int i = 0; i < 2; i++ ) {
			String localPath = path + ( i == 1 ? "_Rape" : "" );
			boolean bool = false;
			int j = 0;
			while ( bool == false && j < stages.length ) {
				File file = new File( localPath + stages[j] );
				if ( j == 2 ) {
					int k = 2;
					while ( bool == false && k < 10 ) {
						file = new File( localPath + "_Stage" + k + ".txt" );
						if ( file.exists() ) bool = true;
						k++ ;
					}
				}
				else if ( file.exists() ) bool = true;
				j++ ;
			}
			if ( i == 0 )
				consen = bool;
			else
				rape = bool;
		}
		return ( consen ? 1 << 0 : 0 ) + ( rape ? 1 << 1 : 0 ); // Neither=0, ConsOnly=1, RapeOnly=2, Both=3
	}
	
	/**
	 * Takes a string and replaces any tags that have matches in the Synonyms table with a randomly selected element.
	 * 
	 * @param str String containing {TAGS}
	 * @return String containing porn words
	 */
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
	
	private static float perDiff( int n, int m ) {
		float diff = Math.abs( n - m );
		return diff / (float) Math.max( 1, Math.max( n, m ) );
	}
	
	private static String stripPunctuation( String str ) {
		str = str.toLowerCase( Locale.ENGLISH );
		str = str.replaceAll( "[\\.,!?'\\(\\)]", "" );
		return str;
	}
	
	private static HashMap<String, Integer> getBagOfWords( String str ) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		String[] split = str.split( " " );
		for ( String word : split ) {
			Integer get = map.get( word );
			map.put( word, get != null ? get + 1 : 1 );
		}
		return map;
	}
	
	private static <T> int sum( HashMap<T, Integer> map ) {
		int ret = 0;
		for ( T key : map.keySet() )
			ret += map.get( key );
		return ret;
	}
	
	/**
	 * Compares the two strings on length, number of words and finally number of each word to determine if they are vaguely similar
	 * 
	 * @return 0: Literally Equal. 1: Equal ignoring case and punctuation. 2: Equal ignoring a few missmatched words. 9: No Match
	 */
	public static int fuzzyMatches( String st1, String st2 ) {
		int l1 = st1.length(), l2 = st2.length();
		if ( st1.equals( st2 ) ) return 0; // Run the equal string comparison
		if ( perDiff( l1, l2 ) > 0.25f ) return 9; // If the length of the two strings is different by over 25%, discard
		st1 = stripPunctuation( st1 );
		st2 = stripPunctuation( st2 );
		if ( st1.equals( st2 ) ) return 1; // Run the equal string comparison again
		HashMap<String, Integer> map1 = getBagOfWords( st1 ), map2 = getBagOfWords( st2 );
		int sum1 = sum( map1 ), sum2 = sum( map2 );
		int misses = (int) Math.ceil( (float) Math.max( sum1, sum2 ) / 4f );
		if ( Math.abs( sum1 - sum2 ) > misses ) return 9;
		Set<String> left = new HashSet<String>( map1.keySet() ), right = new HashSet<String>( map2.keySet() );
		left.removeAll( map2.keySet() );
		right.removeAll( map1.keySet() );
		left.addAll( right );
		if ( left.size() > misses ) return 9;
		return 2;
	}
	
	public static int fuzzyMatches( AproposLabel lab1, AproposLabel lab2 ) {
		return fuzzyMatches( lab1.getText(), lab2.getText() );
	}
	
	public static int fuzzyMatches( AproposConflictLabel labCon, AproposLabel label ) {
		String[] strings = labCon.getTexts();
		int ret = 9;
		for ( int i = 0; i < strings.length; i++ ) {
			ret = Math.min( ret, fuzzyMatches( labCon.getText(), label.getText() ) );
			if ( ret == 0 ) break;
		}
		return ret;
	}
	
	private static boolean contains( String string, String regex ) {
		return string.matches( "^.*" + regex + ".*$" );
	}
	
	private static boolean contains( String string, String[][] regexers ) {
		if ( regexers == null ) return false;
		for ( int i = 0; i < regexers.length; i++ )
			if ( contains( string, regexers[i][0] ) ) return true;
		return false;
	}
	
	/**
	 * Attempts to convert the perspective by replacing pronouns based on regex filters
	 * 
	 * @param list List to be shifted
	 * @param current PerspectiveLabel that represents the current perspective
	 * @param target PerspectiveLabel that represents the target perspective
	 * @return new shifted LabelList
	 */
	public static LabelList perspectiveShift( LabelList list, AproposLabel current, AproposLabel target ) {
		BytePair key = new BytePair( current.getText().charAt( 0 ), target.getText().charAt( 0 ) );
		String[][] shifts = shiftTable.get( key );
		if ( shifts == null ) return list; // This PerspectiveShift is not supported
		String[][] shiftsInv = shiftTable.get( key.invert() );
		LabelList shifted = new LabelList();
		for ( AproposLabel label : list ) {
			shifted.add( new AproposLabel( perspectiveShift( label.getText(), shifts, shiftsInv ), label.getParentLabel() ) );
		}
		return shifted;
	}
	
	/**
	 * Attempts to convert the perspective by replacing pronouns based on regex filters
	 * 
	 * @param label LineLabel to shift perspective on
	 * @param current PerspectiveLabel that represents the current perspective
	 * @param target PerspectiveLabel that represents the target perspective
	 * @return new shifted LineLable
	 */
	public static AproposLabel perspectiveShift( AproposLabel label, AproposLabel current, AproposLabel target ) {
		BytePair key = new BytePair( current.getText().charAt( 0 ), target.getText().charAt( 0 ) );
		String[][] shifts = shiftTable.get( key );
		if ( shifts == null ) return label; // This PerspectiveShift is not supported
		String[][] shiftsInv = shiftTable.get( key.invert() );
		return new AproposLabel( perspectiveShift( label.getText(), shifts, shiftsInv ), label.getParentLabel() );
	}
	
	private static String perspectiveShift( String text, String[][] shifts, String[][] shiftsInv ) {
		text = " " + text + " ";
		if ( contains( text, shiftsInv ) )
			text = " ({PRIMARY})" + text; // TODO: Make this dynamic on perspectives
		else
			for ( int i = 0; i < shifts.length; i++ )
				text = text.replaceAll( shifts[i][0], shifts[i][1] );
		text = text.replaceAll( "^ (.*) $", "$1" );
		return text;
	}
	
	/**
	 * SwingWorker that fetches the name of every folder in the model db's directory, and publishes them as they are found.
	 * <br>
	 * <code>public void process( List&lt;String&gt; strings )</code> must be implemented
	 */
	public abstract class FolderListFetcher extends SwingWorker<List<String>, String> {
		
		public List<String> doInBackground() {
			File root = new File( db );
			File[] files = root.listFiles();
			if ( files != null && files.length > 0 )
				for ( File file : files ) {
					if ( file.isDirectory() ) publish( file.getAbsolutePath().replace( db, "" ) );
				}
			else
				view.handleException( new FileNotFoundException( "No folders found in chosen directory" ) );
			return null;
		}
		
		protected void done() {};
		
		public abstract void process( List<String> strings );
		
	}
	
	/**
	 * SwingWorker that loads the database's UniqueAnimations.txt file into the member TreeMap, for future writing or querying with
	 * <code>isUnique(String string)</code>
	 * <br>
	 * Does not Publish.
	 */
	public class UniquesFetcher extends SwingWorker<Object, Object> {
		
		public Object doInBackground() {
			uniques = new TreeMap<String, Boolean>( String.CASE_INSENSITIVE_ORDER );
			File file = new File( db + "UniqueAnimations.txt" );
			if ( file.exists() )
				try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
					reader.beginObject();
					while ( reader.hasNext() )
						uniques.put( reader.nextName(), reader.nextBoolean() );
				}
				catch ( IllegalStateException | MalformedJsonException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) + " (" + e.getMessage() + ")";
					view.handleException( new IllegalStateException( message, e ) );
				}
				catch ( IOException e ) {
					view.handleException( e );
					e.printStackTrace();
				}
			else
				view.handleException( new FileNotFoundException( "No UniqueAnimations.txt file found" ) );
			return null;
		}
		
		public void done() {
			try {
				get();
			}
			catch ( InterruptedException | ExecutionException e ) {
				view.handleException( e );
				e.printStackTrace();
			}
		};
		
		public void process( Object o ) {};
		
	}
	
	class PositionFinder extends SimpleFileVisitor<Path> {
		private String folder;
		private Set<Position> positions;
		
		public PositionFinder( String folder ) {
			super();
			this.folder = folder;
			this.positions = new TreeSet<Position>();
		}
		
		public FileVisitResult visitFile( Path path, BasicFileAttributes attr ) {
			File file = path.toFile();
			Position p = getPosition( folder, file.getName() );
			if ( p == null ) return FileVisitResult.CONTINUE;
			positions.add( p );
			return FileVisitResult.CONTINUE;
		}
		
		public Position[] getPositions() {
			return positions.toArray( new Position[ 0 ] );
		}
		
	}
	
	/**
	 * SwingWorker that loads the database's Synonyms.txt for simulating.
	 * <br>
	 * Does not publish
	 */
	public class SynonymsFetcher extends SwingWorker<Object, Object> {
		
		public Object doInBackground() {
			synonyms = new TreeMap<String, ArrayList<String>>();
			File file = new File( db + "Synonyms.txt" );
			if ( file.exists() ) {
				try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
					reader.beginObject();
					while ( reader.hasNext() ) {
						String key = reader.nextName();
						ArrayList<String> list = new ArrayList<String>();
						reader.beginArray();
						while ( reader.hasNext() )
							list.add( reader.nextString() );
						reader.endArray();
						System.out.println( key + ":" + Prototype.concatenate( list.toArray( new String[ list.size() ] ), ", " ) );
						synonyms.put( key, list );
					}
				}
				catch ( IllegalStateException | MalformedJsonException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) + " (" + e.getMessage() + ")";
					view.handleException( new IllegalStateException( message, e ) );
				}
				catch ( IOException e ) {
					view.handleException( e );
					e.printStackTrace();
				}
			}
			else
				view.handleException( new FileNotFoundException( "No Synonyms.txt file found" ) );
			file = new File( db + "Arousal_Descriptors.txt" );
			if ( file.exists() ) {
				try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
					reader.beginObject();
					while ( reader.hasNext() ) {
						String key = reader.nextName();
						ArrayList<String> list = new ArrayList<String>();
						reader.beginObject();
						while ( reader.hasNext() ) {
							reader.nextName();
							reader.beginArray();
							while ( reader.hasNext() )
								list.add( reader.nextString() );
							reader.endArray();
						}
						reader.endObject();
						System.out.println( key + ":" + Prototype.concatenate( list.toArray( new String[ list.size() ] ), ", " ) );
						synonyms.put( key, list );
					}
				}
				catch ( IllegalStateException | MalformedJsonException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) + " (" + e.getMessage() + ")";
					view.handleException( new IllegalStateException( message, e ) );
				}
				catch ( IOException e ) {
					view.handleException( e );
					e.printStackTrace();
				}
			}
			else
				view.handleException( new FileNotFoundException( "No Arousal_Descriptors.txt file found" ) );
			if ( file.exists() ) {
				file = new File( db + "WearAndTear_Descriptors.txt" );
				try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
					reader.beginObject();
					reader.nextName();
					ArrayList<String> list = new ArrayList<String>();
					reader.beginObject();
					while ( reader.hasNext() ) {
						reader.nextName();
						reader.beginArray();
						while ( reader.hasNext() )
							list.add( reader.nextString() );
						reader.endArray();
					}
					reader.endObject();
					System.out.println( "{WT}" + ":" + Prototype.concatenate( list.toArray( new String[ list.size() ] ), ", " ) );
					synonyms.put( "{WTVAGINAL}", list );
					synonyms.put( "{WTORAL}", list );
					synonyms.put( "{WTANAL}", list );
				}
				catch ( IllegalStateException | MalformedJsonException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) + " (" + e.getMessage() + ")";
					view.handleException( new IllegalStateException( message, e ) );
				}
				catch ( IOException e ) {
					view.handleException( e );
					e.printStackTrace();
				}
			}
			else
				view.handleException( new FileNotFoundException( "No WearAndTear_Descriptors.txt file found" ) );
			
			synonymsLengths = new SynonymsLengthMap( synonyms );
			return null;
		}
		
		public void done() {
			try {
				get();
			}
			catch ( InterruptedException | ExecutionException e ) {
				view.handleException( e );
				e.printStackTrace();
			}
		};
		
		public void process( Object o ) {};
		
	}
	
	/**
	 * SwingWorker that fetches a <code>StageMap</code> to be processed by done() for displaying
	 * <br>
	 * Does not Publish.
	 */
	public abstract class PositionFetcher extends SwingWorker<StageMap, Object> {
		
		private AproposLabel parent;
		
		public PositionFetcher( String folder, String animString ) {
			super();
			this.parent = new AproposLabel( animString, new AproposLabel( folder, root ) );
		}
		
		public StageMap doInBackground() {
			return getStages( parent );
		}
		
		public abstract void done();
		
		public void process( Object o ) {};
		
	}
	
	/**
	 * SwingWorker that writes a given <code>StageMap</code> to disk
	 * <br>
	 * Does not Publish.
	 */
	public abstract class PositionWriter extends SwingWorker<Object, Object> {
		
		private StageMap stageMap;
		
		public PositionWriter( StageMap stageMap ) {
			super();
			this.stageMap = stageMap;
		}
		
		public Object doInBackground() {
			writeStages( stageMap );
			return null;
		}
		
		public abstract void done();
		
		public void process( Object o ) {};
		
	}
	
	/**
	 * SwingWorker that loads the <code>StageMap</code> denoted by the <code>folder</code> and <code>animString</code> constructor
	 * parameters, changes the parent's text to assign a new write location, then completes the write.
	 * <br>
	 * Does not Publish.
	 */
	public class PositionCopier extends SwingWorker<StageMap, Object> {
		String folder, animString, newAnim;
		
		public PositionCopier( String folder, String animString, String newAnim ) {
			super();
			this.folder = folder;
			this.animString = animString;
			this.newAnim = newAnim;
		}
		
		public StageMap doInBackground() {
			AproposLabel parent = new AproposLabel( animString, new AproposLabel( folder, root ) );
			StageMap toCopy = getStages( parent );
			AproposLabel first = toCopy.firstKey();
			AproposLabel position = first.getParentLabel();
			AproposLabel folder = position.getParentLabel();
			
			position.setText( newAnim );
			folder.setText( extractFolder( newAnim ) );
			writeStages( toCopy );
			
			if ( getPosition( newAnim ) == Position.Unique ) {
				System.out.println( newAnim + " is unique!" );
				uniques.put( newAnim.substring( newAnim.indexOf( '_' ) + 1 ), true );
				writeUniques();
			}
			
			return toCopy;
		}
		
		public void done() {};
		
		public void process( Object o ) {};
		
	}
	
	/**
	 * SwingWorker that loads the <code>StageMap</code> denoted by the <code>folder</code> and <code>animString</code> constructor
	 * parameters, changes the parent's text to match those of the passed <code>newFolder</code> and <code>newAnim</code>, merges each
	 * child <code>LabelList</code> then completes the write.
	 * <br>
	 * Does not Publish.
	 */
	public class PositionAppender extends SwingWorker<StageMap, Object> {
		String folder, animString, newFolder, newAnim;
		
		public PositionAppender( String folder, String animString, String newFolder, String newAnim ) {
			super();
			this.folder = folder;
			this.animString = animString;
			this.newFolder = newFolder;
			this.newAnim = newAnim;
		}
		
		public StageMap doInBackground() {
			AproposLabel parent = new AproposLabel( animString, new AproposLabel( folder, root ) );
			StageMap toCopy = getStages( parent );
			
			AproposLabel parentDest = new AproposLabel( newAnim, new AproposLabel( newFolder, root ) );
			StageMap dest = getStages( parentDest );
			
			AproposLabel first = toCopy.firstKey();
			AproposLabel position = first.getParentLabel();
			AproposLabel folder = position.getParentLabel();
			
			position.setText( newAnim );
			folder.setText( extractFolder( newAnim ) );
			
			for ( AproposLabel stage : toCopy.keySet() ) {
				PerspectiveMap persMap = toCopy.get( stage );
				PerspectiveMap persMapDest = dest.getEquivalent( stage );
				if ( persMapDest != null ) for ( AproposLabel pers : persMap.keySet() ) {
					LabelList list = persMap.get( pers );
					LabelList listDest = persMapDest.getEquivalent( pers );
					listDest.remove( listDest.size() - 1 );
					listDest.addAll( list );
				}
			}
			
			writeStages( dest );
			return dest;
		}
		
		public void done() {};
		
		public void process( Object o ) {};
		
	}
	
	/**
	 * <code>FileVisitor</code> that simply loads and rewrites every .txt file it visits.
	 *
	 */
	class JSonRebuilder extends SimpleFileVisitor<Path> {
		private String[] skip = new String[] { "AnimationPatchups.txt", "Arousal_Descriptors.txt", "Themes.txt", "UniqueAnimations.txt",
				"WearAndTear_Damage.txt", "WearAndTear_Descriptors.txt", "WearAndTear_Effects.txt" };
		
		public FileVisitResult visitFile( Path path, BasicFileAttributes attr ) {
			File file = path.toFile();
			if ( file.getName().endsWith( ".txt" ) ) {
				for ( String str : skip )
					if ( str.equals( file.getName() ) ) return FileVisitResult.CONTINUE;
				try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
					writePerspectives( getPerspectives( null, reader ), file );
				}
				catch ( MalformedJsonException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) + " (" + e.getMessage() + ")";
					view.handleException( new MalformedJsonException( message, e ) );
				}
				catch ( IllegalStateException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) + " (" + e.getMessage() + ")";
					view.handleException( new IllegalStateException( message, e ) );
				}
				catch ( IOException e ) {
					String message = e.getClass().getSimpleName() + " parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) + " ("
							+ e.getMessage() + ")";
					view.handleException( new RuntimeException( message, e ) );
					e.printStackTrace();
				}
			}
			return FileVisitResult.CONTINUE;
		}
	}
	
	/**
	 * @see Model.JSonRebuilder
	 */
	public abstract class DatabaseRebuilder extends SwingWorker<Object, Object> {
		
		public Object doInBackground() {
			JSonRebuilder rebuilder = new JSonRebuilder();
			try {
				Files.walkFileTree( Paths.get( db ), rebuilder );
			}
			catch ( IOException e ) {
				view.handleException( e );
				e.printStackTrace();
			}
			
			if ( uniques == null ) new UniquesFetcher().doInBackground();
			writeUniques();
			return null;
		}
		
		public abstract void done();
		
		public void process( Object o ) {};
		
	}
	
	/**
	 * Takes every line in a Map of AproposLabels, replaces the synonyms, toggles the display state and highlights one from each perspective
	 */
	public abstract class LabelSimulator extends SwingWorker<Object, AproposLabel> {
		
		StageMap stageMap;
		String active, primary;
		Random r = new Random();
		
		public LabelSimulator( StageMap stageMap, String active, String primary ) {
			super();
			this.stageMap = stageMap;
			this.active = active;
			this.primary = primary;
		}
		
		public Object doInBackground() {
			// Put the Active and Primary names into the Synonyms Map
			synonyms.put( "{ACTIVE}", new ArrayList<String>( Arrays.asList( new String[] { active } ) ) );
			synonyms.put( "{PRIMARY}", new ArrayList<String>( Arrays.asList( new String[] { primary } ) ) );
			
			// Create a new StageMap with only one label randomly selected from the original map
			StageMap selectedMap = new StageMap();
			for ( AproposLabel stage : stageMap.keySet() ) {
				PerspectiveMap persMap = stageMap.get( stage );
				PerspectiveMap selPersMap = new PerspectiveMap();
				selectedMap.put( stage, selPersMap );
				for ( AproposLabel perspec : persMap.keySet() ) {
					LabelList list = persMap.get( perspec );
					if ( list.size() == 0 ) continue;
					LabelList selList = new LabelList();
					selPersMap.put( perspec, selList );
					// Select one element at random and add it to the selectedMap
					selList.add( list.get( list.size() == 1 ? 0 : r.nextInt( list.size() - 1 ) ) );
				}
			}
			
			// Iterate through every label and insert words
			for ( AproposLabel stage : stageMap.keySet() ) {
				PerspectiveMap persMap = stageMap.get( stage );
				for ( AproposLabel perspec : persMap.keySet() ) {
					LabelList list = persMap.get( perspec );
					LabelList selList = selectedMap.get( stage ).get( perspec );
					AproposLabel selected = selList.size() == 0 ? null : selList.get( 0 );
					for ( AproposLabel label : list ) {
						if ( label.getText().equals( "" ) ) continue;
						if ( label == selected ) label.setHighlighted( true );
						label.setSimulateString( insert( label.getText() ) );
						publish( label );
					}
				}
			}
			
			return null;
		}
		
		public void done() {
			try {
				get();
			}
			catch ( InterruptedException | ExecutionException e ) {
				view.handleException( e );
				e.printStackTrace();
			}
		};
		
		public abstract void process( List<AproposLabel> labels );
	}
	
}

class LabelList extends ArrayList<AproposLabel> implements AproposMap {
	private static final long serialVersionUID = -3091716550688577792L;
	private boolean hasConflicts = false;
	
	public LabelList() {}
	
	public LabelList( int initialCapacity ) {
		super( initialCapacity );
	}
	
	public LabelList( Collection<? extends AproposLabel> list ) {
		super( list );
	}
	
	public boolean checkDuplicates() {
		if ( hasConflicts ) return true;
		int match = 0;
		LabelList conflictList = new LabelList( size() ); // ConflictLabels will be stored in a temporary list to preserve the main list
		ArrayList<Integer> skip = new ArrayList<Integer>( size() );
		for ( int i = 0; i < size(); i++ ) {
			if ( skip.contains( i ) ) continue;
			AproposConflictLabel label = new AproposConflictLabel( get( i ) );
			conflictList.add( label );
			for ( int j = i + 1; j < size(); j++ ) { // Triangular Matrix iteration
				if ( skip.contains( i ) ) continue;
				AproposLabel checking = get( j );
				match = Model.fuzzyMatches( label, checking );
				if ( match < 9 ) {
					label.addConflict( checking, match );
					skip.add( j );
					hasConflicts = true;
				}
			}
		}
		if ( hasConflicts ) {
			clear();
			addAll( conflictList );
		}
		return hasConflicts;
	}
	
	public void resolveConflicts() {
		if ( !hasConflicts ) return;
		LabelList newList = new LabelList( size() * 3 ); // Assume up to three duplicates per line, to avoid needing to expand the array
		for ( int i = 0; i < size(); i++ ) {
			AproposConflictLabel label = (AproposConflictLabel) get( i );
			newList.addAll( label.resolveConficts() );
		}
		clear();
		addAll( newList );
		hasConflicts = false;
	}
	
	public boolean isConflicted() {
		return hasConflicts;
	}
	
	public int totalSize() {
		return size();
	}
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for ( int i = 0; i < size(); i++ ) {
			if ( i > 0 ) builder.append( "\n\t\t\t\t\t" );
			builder.append( get( i ) );
		}
		return builder.toString();
	}
	public Result query( AproposLabel key ) {
		return null;
	}
}

class PerspectiveMap extends LabelMap<LabelList> {
	private static final long serialVersionUID = 1659741172660975737L;
	{
		indent = "\n\t\t\t\t";
		keyDepth = 4;
	}
}

class StageMap extends LabelMap<PerspectiveMap> {
	private static final long serialVersionUID = -4569924813567288184L;
	{
		indent = "\n\t\t\t";
		keyDepth = 3;
	}
}

class PositionMap extends LabelMap<StageMap> {
	private static final long serialVersionUID = 8253283878828610516L;
	{
		indent = "\n\t\t";
		keyDepth = 2;
	}
}

class FolderMap extends LabelMap<PositionMap> {
	private static final long serialVersionUID = 3997804667766094854L;
	{
		indent = "\n\t";
		keyDepth = 1;
	}
}

class DatabaseMap extends LabelMap<FolderMap> {
	private static final long serialVersionUID = 6214629552869731592L;
	{
		indent = "\n";
		keyDepth = 0;
	}
}

abstract class LabelMap<T extends AproposMap> extends TreeMap<AproposLabel, T> implements AproposMap {
	private static final long serialVersionUID = 7745720831421872902L;
	String indent;
	int keyDepth;
	
	public Result query( AproposLabel key ) {
		if ( key.getDepth() == keyDepth ) {
			T map = get( key );
			Result res = new Result( map );
			return res;
		}
		AproposLabel levelKey = key.getParentLabel( keyDepth );
		T map = get( levelKey );
		return map.query( key );
	}
	
	/**
	 * Returns the key in this map that has the same labeltext as the label passed
	 * 
	 * @param equivKey Key whom's text you want to match
	 * @return The key in this Map that matches
	 */
	public T getEquivalent( AproposLabel equivKey ) {
		for ( AproposLabel key : keySet() ) {
			if ( key.getText().equals( equivKey.getText() ) ) return get( key );
		}
		return null;
	}
	
	public boolean checkDuplicates() {
		boolean bool = false;
		for ( AproposLabel key : keySet() ) {
			bool = get( key ).checkDuplicates() | bool;
		}
		return bool;
	}
	
	public void resolveConflicts() {
		for ( AproposLabel key : keySet() ) {
			get( key ).resolveConflicts();
		}
	}
	
	public boolean isConflicted() {
		boolean bool = false;
		for ( AproposLabel key : keySet() ) {
			bool = bool | get( key ).isConflicted();
			if ( bool ) break;
		}
		return bool;
	}
	
	public int totalSize() {
		int i = 0;
		for ( AproposLabel label : keySet() ) {
			i += get( label ).totalSize();
		}
		return i;
	}
	
	public void modifyKey( AproposLabel key, String newText ) {
		T map = get( key );
		remove( key );
		key.setText( newText );
		put( key, map );
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		AproposLabel[] keys = keySet().toArray( new AproposLabel[ 0 ] );
		for ( int i = 0; i < size(); i++ ) {
			if ( i > 0 ) builder.append( indent );
			builder.append( keys[i].getText() + indent + "\t" + get( keys[i] ) );
		}
		return builder.toString();
	}
}

class Result {
	public boolean found = false;
	public AproposMap map;
	public LabelList labelList;
	public PerspectiveMap perspecMap;
	public StageMap stageMap;
	public PositionMap posMap;
	
	public <T extends AproposMap> Result( T map ) {
		this.map = map;
		if ( map instanceof LabelList )
			labelList = (LabelList) map;
		else if ( map instanceof PerspectiveMap )
			perspecMap = (PerspectiveMap) map;
		else if ( map instanceof StageMap )
			stageMap = (StageMap) map;
		else if ( map instanceof PositionMap ) posMap = (PositionMap) map;
		this.found = true;
	}
	
	public String toString() {
		return Result.class + "@" + hashCode() + " [found=" + found + ",LabelList=" + labelList + ",PerspectiveMap=" + perspecMap
				+ ",StageMap=" + stageMap + ",PositionMap=" + posMap + "]";
	}
}

interface AproposMap {
	/**
	 * @return Total Number of lines in the Map.
	 */
	public int totalSize();
	/**
	 * Searches this map and all it's children by a given key
	 * 
	 * @param key
	 * @return The Map/List associated with that key wrapped in a <code>Result</code> object
	 */
	public Result query( AproposLabel key );
	/**
	 * Checks the entire map for duplicate lines within the individual perspectives, and transforms Labels into ConflictLabels if conflicts
	 * are found
	 * 
	 * @return true if the map has been modified
	 */
	public boolean checkDuplicates();
	/**
	 * Rebuilds the map by deleting any lines marked un-needed and re-merging suspected duplicates back in
	 */
	public void resolveConflicts();
	/**
	 * @return true if any of the submaps have conflicts
	 */
	public boolean isConflicted();
}

class BytePair {
	public byte[] p;
	
	public BytePair( byte a, byte b ) {
		p = new byte[] { a, b };
	}
	
	public BytePair( int a, int b ) {
		this( (byte) a, (byte) b );
	}
	
	public BytePair invert() {
		return new BytePair( p[1], p[0] );
	}
	
	public BytePair( char a, char b ) {
		// Subtract the value of the 0 character to get the integer of the digit
		this( a - '0', b - '0' );
	}
	
	public boolean equals( Object o ) {
		if ( this == o ) return true;
		if ( ( o == null ) || ( o.getClass() != this.getClass() ) ) return false;
		byte[] q = ( (BytePair) o ).p;
		return p[0] == q[0] & p[1] == q[1];
	}
	
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + p[0];
		hash = 31 * hash + p[1];
		return hash;
	}
	
	public String toString() {
		return "[" + p[0] + "," + p[1] + "]";
	}
}

class SynonymsLengthMap {
	// I apologise for every part of this class's naming scheme.
	
	private final HashMap<String, MinMax> map = new HashMap<String, MinMax>();
	public final int max;
	
	public SynonymsLengthMap( TreeMap<String, ArrayList<String>> synonyms ) {
		JLabel swingFont = new JLabel();
		FontMetrics metrics = swingFont.getFontMetrics( swingFont.getFont() );
		max = metrics.stringWidth( "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM" ); // The Holy String of
																														 // 78 M's
		for ( String key : synonyms.keySet() ) {
			ArrayList<String> list = synonyms.get( key );
			MinMax minmax = new MinMax();
			for ( String word : list ) {
				int length = metrics.stringWidth( word );
				minmax.add( length );
			}
			if ( minmax.isValid() ) map.put( key, minmax );
		}
		MinMax names = new MinMax( metrics.stringWidth( "Zaz" ), metrics.stringWidth( "Balgruuf the Greater" ) ); // He's a pretty good
																													 // worst case
		map.put( "{ACTIVE}", names );
		map.put( "{PRIMARY}", names );
	}
	
	public MinMax get( String key ) {
		return map.get( key );
	}
	
	public class MinMax {
		public int min, max;
		
		MinMax( int min, int max ) {
			this.min = min;
			this.max = max;
		}
		MinMax() {
			min = Integer.MAX_VALUE;
			max = Integer.MIN_VALUE;
		}
		void min( int i ) {
			if ( i < min ) min = i;
		}
		void max( int i ) {
			if ( i > max ) max = i;
		}
		void add( int i ) {
			min( i );
			max( i );
		}
		boolean isValid() {
			return min != Integer.MAX_VALUE & max != Integer.MIN_VALUE;
		}
		public String toString() {
			return min + "<-->" + max;
		}
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		String[] keys = map.keySet().toArray( new String[ 0 ] );
		for ( int i = 0; i < keys.length; i++ ) {
			if ( i > 0 ) builder.append( "\n" );
			builder.append( keys[i] + ": " + map.get( keys[i] ) );
		}
		return builder.toString();
	}
	
	public Set<String> keySet() {
		return map.keySet();
	}
	
}

/**
 * I CBA to make a second message reporter for stuff that isn't exceptions, so I'm making an exception with a friendlier class name.
 */
class Information extends RuntimeException {
	public Information( String string ) {
		super( string );
	}
	
	private static final long serialVersionUID = -1543550760928302667L;
}