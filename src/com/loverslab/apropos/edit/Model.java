package com.loverslab.apropos.edit;

import java.awt.FontMetrics;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;
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
	SynonymsMap synonyms;
	SynonymsLengthMap synonymsLengths;
	public static String fs = File.separator;
	private String[] skip = new String[] { "AnimationPatchups.txt", "Arousal_Descriptors.txt", "Themes.txt", "UniqueAnimations.txt",
			"WearAndTear_Damage.txt", "WearAndTear_Descriptors.txt", "WearAndTear_Effects.txt", "WearAndTear_Map.txt",
			"file_layout_scheme.txt" };
	private String[] searchSkip = Stream.concat( Arrays.stream( skip ), Arrays.stream( new String[] { "Synonyms.txt" } ) )
			.toArray( String[]::new );
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
			new String[] { " we( |\\p{Punct})", " you$1" },
			new String[] { " We( |\\p{Punct})", " You$1" },
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
	static HashMap<BytePair, String[][]> shiftTableFixes = new HashMap<BytePair, String[][]>() { private static final long serialVersionUID = -4049369740518911206L; {
		put( new BytePair( 1, 2 ), new String[][] {
			//Unfuck common phrases that only work in 1st person
			new String[] { " Your your( |\\p{Punct})", " My my$1" },
			new String[] { " your your( |\\p{Punct})", " my my$1" },
			new String[] { " Oh your( |\\p{Punct})", " Oh my$1" },
			new String[] { " oh your( |\\p{Punct})", " oh my$1" },
			new String[] { " Your (?:g|G)od( |\\p{Punct})", " My God$1" },
			new String[] { " your (?:g|G)od( |\\p{Punct})", " my God$1" },
			new String[] { " Your goodness( |\\p{Punct})", " My goodness$1" },
			new String[] { " your goodness( |\\p{Punct})", " my goodness$1" },
		});
		put( new BytePair( 2, 1 ), new String[][]{
			
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
		db = path + fs;
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
		String path = db + parent.getParentLabel().getText() + fs + parent.getText();
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
		catch ( IllegalStateException | MalformedJsonException | EOFException e ) {
			String message = "Error parsing " + file.getAbsolutePath().replace( db, fs + "db" + fs ) + " (" + e.getMessage() + ")";
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
		String folder = db + first.getParentLabel().getParentLabel().getText(); // TODO: Get path from parent chain
		new File( folder ).mkdirs();
		String path = folder + fs + first.getParentLabel().getText();
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
		String path = db + stageLabel.getParentLabel().getParentLabel().getText() + fs + stageLabel.getParentLabel().getText();
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
			String message = "Error writing " + file.getAbsolutePath().replace( db, fs + "db" + fs ) + " (" + e.getMessage() + ")";
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
	public static String extractFolder( String animString ) {
		animString = animString.replace( ".txt", "" ).replace( "_Rape", "" ).replace( "_Orgasm", "" ).replaceAll( "_Stage[1-9][0-9]*", "" );
		for ( Position p : Position.values() )
			animString = animString.replaceAll( "_" + p.name() + "$", "" );
		return animString;
	}
	
	public static String extractStage( String animString ) {
		animString = animString.replace( ".txt", "" ).replace( "_Rape", "" ).replace( extractFolder( animString ), "" );
		for ( Position p : Position.values() )
			animString = animString.replaceAll( "_" + p.name(), "" );
		return animString.replaceFirst( "^_", "" );
	}
	
	public static String befriendStage( String stage ) {
		if ( stage == null ) return null;
		if ( stage.equals( "" ) ) return "Intro";
		if ( stage.equalsIgnoreCase( "Orgasm" ) ) return "Orgasm";
		if ( stage.startsWith( "Stage" ) ) return "Stage " + stage.charAt( stage.length() - 1 );
		throw new IllegalArgumentException( "Attempted to load unknown stage: " + stage );
	}
	
	public static AproposLabel stageLabelFromFile( File f ) {
		String s = f.getName();
		String stage = extractStage( s );
		return new AproposLabel( f.getParentFile().getAbsolutePath() + File.separator
				+ s.replace( ( stage.equals( "" ) ? "" : ( "_" + stage ) ) + ".txt", "" ) + File.separator + befriendStage( stage ) );
	}
	
	public static String shorten( String s ) {
		if ( s == null ) return null;
		return s.replaceFirst( "FemaleActor_", "FA_" ).replaceFirst( "MaleActor_", "MA_" );
	}
	
	public static String expand( String s ) {
		if ( s == null ) return null;
		return s.replaceFirst( "^FA_", "FemaleActor_" ).replaceFirst( "^MA_", "MaleActor_" );
	}
	
	public Position getPosition( String animString ) {
		return getPosition( extractFolder( animString ), animString );
	}
	
	public Position getPosition( String folder, String animString ) {
		String position = animString.replace( ".txt", "" ).replace( "_Rape", "" ).replace( "_Orgasm", "" )
				.replaceAll( "_Stage[1-9][0-9]*", "" ).replaceAll( "(?i)" + Pattern.quote( folder ), "" ).replaceAll( "^_", "" );
		Position p = Position.lookup( position );
		if ( p == null ) {
			if ( position.equals( "" ) )
				return Position.Unique;
			else
				System.err.println( "Unknown Position: \"" + position + "\" (" + folder + fs + animString + ")" );
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
		return synonyms.insert( str );
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
	 * @return 0: Literally Equal.<br>
	 *         1: Equal ignoring case and punctuation.<br>
	 *         2: Equal ignoring a few missmatched words.<br>
	 *         7: Not enough Words in Common.<br>
	 *         8: #Words too different.<br>
	 *         9: Length Difference too great
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
		if ( Math.abs( sum1 - sum2 ) > misses ) return 8;
		Set<String> left = new HashSet<String>( map1.keySet() ), right = new HashSet<String>( map2.keySet() ),
				both = new HashSet<String>( map1.keySet() );
		both.retainAll( map2.keySet() );
		left.removeAll( map2.keySet() );
		right.removeAll( map1.keySet() );
		left.addAll( right );
		if ( left.size() > both.size() ) return 7;
		return 2;
	}
	
	/**
	 * Compares the strings of two AproposLabels on length, number of words and finally number of each word to determine if they are vaguely
	 * similar
	 * 
	 * @return 0: Literally Equal.<br>
	 *         1: Equal ignoring case and punctuation.<br>
	 *         2: Equal ignoring a few missmatched words.<br>
	 *         7: Not enough Words in Common.<br>
	 *         8: #Words too different.<br>
	 *         9: Length Difference too great
	 */
	public static int fuzzyMatches( AproposLabel lab1, AproposLabel lab2 ) {
		return fuzzyMatches( lab1.getText(), lab2.getText() );
	}
	
	/**
	 * Compares the string of one AproposLabel with every string in a Conflict Label on length, number of words and finally number of each
	 * word to determine if they are vaguely similar
	 * 
	 * @return 0: Literally Equal.<br>
	 *         1: Equal ignoring case and punctuation.<br>
	 *         2: Equal ignoring a few missmatched words.<br>
	 *         7: Not enough Words in Common.<br>
	 *         8: #Words too different.<br>
	 *         9: Length Difference too great
	 */
	public static int fuzzyMatches( AproposConflictLabel labCon, AproposLabel label ) {
		String[] strings = labCon.getTexts();
		int ret = 9;
		for ( int i = 0; i < strings.length; i++ ) {
			ret = Math.min( ret, fuzzyMatches( labCon.getText(), label.getText() ) );
			if ( ret == 0 ) break;
		}
		return ret;
	}
	
	/**
	 * Checks if a substring defined by regex can be found within the passed string
	 * 
	 * @return true if a match is found
	 */
	public static boolean contains( String string, String regex ) {
		return string.matches( "^.*" + regex + ".*$" );
	}
	
	/**
	 * Utility method for the <code>shiftTable</code>, checks if the string contains any of the regexs in one of the tables
	 * 
	 * @param string Search String
	 * @param regexers Array of Regex
	 * @return true if any of the regexes matched
	 */
	private static boolean contains( String string, String[][] regexers ) {
		if ( regexers == null ) return false;
		for ( int i = 0; i < regexers.length; i++ )
			if ( contains( string, regexers[i][0] ) ) return true;
		return false;
	}
	
	/**
	 * Isolates groups from within a string
	 * 
	 * @param str String to Search
	 * @param regex Regex with at least one capturing group
	 * @return Array of the captured strings from the first match, or null if no match found
	 */
	public static String[] matchFirstGroups( String str, String regex ) {
		Pattern p = Pattern.compile( regex );
		return matchFirstGroups( str, p );
	}
	
	/**
	 * Isolates groups from within a string
	 * 
	 * @param str String to Search
	 * @param p Pattern with at least one capturing group
	 * @return Array of the captured strings from the first match, or null if no match found
	 */
	public static String matchFirstGroups( String str, Pattern p )[] { // Why can I do this
		Matcher m = p.matcher( str );
		if ( m.find() ) {
			int c = m.groupCount();
			String ret[] = new String[ c ]; // this looks so dumb
			for ( int i = 0; i < c; i++ ) {
				ret[i] = m.group( i + 1 );
			}
			return ret;
		}
		return null;
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
		String[][] shifts = shiftTable.get( key ), shiftsInv = shiftTable.get( key.invert() ), fixes = shiftTableFixes.get( key ),
				fixesInv = shiftTableFixes.get( key.invert() );
		if ( shifts == null ) return list; // This PerspectiveShift is not supported
		LabelList shifted = new LabelList();
		for ( AproposLabel label : list ) {
			shifted.add(
					new AproposLabel( perspectiveShift( label.getText(), shifts, shiftsInv, fixes, fixesInv ), label.getParentLabel() ) );
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
		String[][] shifts = shiftTable.get( key ), shiftsInv = shiftTable.get( key.invert() ), fixes = shiftTableFixes.get( key ),
				fixesInv = shiftTableFixes.get( key.invert() );
		if ( shifts == null ) return label; // This PerspectiveShift is not supported
		return new AproposLabel( perspectiveShift( label.getText(), shifts, shiftsInv, fixes, fixesInv ), label.getParentLabel() );
	}
	
	private static String perspectiveShift( String text, String[][] shifts, String[][] shiftsInv, String[][] fixes, String[][] fixesInv ) {
		text = " " + text + " ";
		String speakingCheck = text; // Remove all the fixes text, as these are idioms that use the language that would trigger speaking
		if ( fixesInv != null ) {
			for ( int i = 0; i < fixesInv.length; i++ )
				speakingCheck = speakingCheck.replaceAll( fixesInv[i][1].replaceAll( "\\$1", "( |\\\\p{Punct})" ), "$1" );
		}
		if ( contains( text, Pattern.quote( "({ACTIVE})" ) ) | contains( text, Pattern.quote( "({PRIMARY})" ) ) )
			;
		else if ( contains( speakingCheck, shiftsInv ) )
			text = " ({PRIMARY})" + text; // TODO: Make this dynamic on perspectives
		else
			for ( int i = 0; i < shifts.length; i++ )
				text = text.replaceAll( shifts[i][0], shifts[i][1] );
		if ( fixes != null ) {
			for ( int i = 0; i < fixes.length; i++ )
				text = text.replaceAll( fixes[i][0], fixes[i][1] );
		}
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
					String message = "Error parsing " + file.getAbsolutePath().replace( db, fs + "db" + fs ) + " (" + e.getMessage() + ")";
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
			synonyms = new SynonymsMap();
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
						synonyms.put( key, list );
					}
				}
				catch ( IllegalStateException | MalformedJsonException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, fs + "db" + fs ) + " (" + e.getMessage() + ")";
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
						reader.beginObject();
						while ( reader.hasNext() ) {
							String level = reader.nextName();
							ArrayList<String> list = new ArrayList<String>();
							reader.beginArray();
							while ( reader.hasNext() )
								list.add( reader.nextString() );
							reader.endArray();
							synonyms.putArousalLevel( key, level, list );
						}
						reader.endObject();
					}
				}
				catch ( IllegalStateException | MalformedJsonException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, fs + "db" + fs ) + " (" + e.getMessage() + ")";
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
					reader.beginObject();
					while ( reader.hasNext() ) {
						String level = reader.nextName();
						ArrayList<String> list = new ArrayList<String>();
						reader.beginArray();
						while ( reader.hasNext() )
							list.add( reader.nextString() );
						reader.endArray();
						synonyms.putWNTLevel( level, list );
					}
					reader.endObject();
				}
				catch ( IllegalStateException | MalformedJsonException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, fs + "db" + fs ) + " (" + e.getMessage() + ")";
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
				uniques.put( newAnim.substring( newAnim.indexOf( '_' ) + 1 ).replace( "_Rape", "" ), true );
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
			return getStages( parentDest );
		}
		
		public void done() {};
		
		public void process( Object o ) {};
		
	}
	
	class FileCounter extends SimpleFileVisitor<Path> {
		public int count = 0;
		public int est = 3200; // Number of files in the official DB + 200 or so
		
		private Runnable postProgress = new Runnable() {
			public void run() {
				int p = ( count * 100 ) / est;
				view.updateProgress( p );
			}
		};
		
		public FileVisitResult visitFile( Path path, BasicFileAttributes attrs ) throws IOException {
			File file = path.toFile();
			if ( file.getName().endsWith( ".txt" ) ) {
				for ( String str : skip )
					if ( str.equals( file.getName() ) ) return FileVisitResult.CONTINUE;
				count++ ;
			}
			if ( count % 100 == 0 ) {
				SwingUtilities.invokeLater( postProgress );
			}
			return FileVisitResult.CONTINUE;
		}
	}
	
	/**
	 * <code>FileVisitor</code> that simply loads and rewrites every .txt file it visits.
	 *
	 */
	class JSonRebuilder extends SimpleFileVisitor<Path> {
		private int max;
		private int count;
		private boolean sort;
		
		private Runnable postProgress = new Runnable() {
			public void run() {
				int p = ( count * 100 ) / max;
				view.updateProgress( p );
			}
		};
		
		public JSonRebuilder( int count, boolean sort ) {
			super();
			max = count;
			this.sort = sort;
		}
		
		public FileVisitResult visitFile( Path path, BasicFileAttributes attr ) {
			File file = path.toFile();
			if ( file.getName().endsWith( ".txt" ) ) {
				for ( String str : skip )
					if ( str.equals( file.getName() ) ) return FileVisitResult.CONTINUE;
				try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
					if ( sort ) {
						PerspectiveMap map = getPerspectives( null, reader );
						for ( AproposLabel pers : map.keySet() ) {
							LabelList list = map.get( pers );
							list.sort( null );
						}
						writePerspectives( map, file );
					}
					else
						writePerspectives( getPerspectives( null, reader ), file );
					count++ ;
					if ( count % 10 == 0 ) {
						SwingUtilities.invokeLater( postProgress );
					}
				}
				catch ( MalformedJsonException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, fs + "db" + fs ) + " (" + e.getMessage() + ")";
					view.handleException( new MalformedJsonException( message, e ) );
				}
				catch ( IllegalStateException e ) {
					String message = "Error parsing " + file.getAbsolutePath().replace( db, fs + "db" + fs ) + " (" + e.getMessage() + ")";
					view.handleException( new IllegalStateException( message, e ) );
				}
				catch ( IOException e ) {
					String message = e.getClass().getSimpleName() + " parsing " + file.getAbsolutePath().replace( db, fs + "db" + fs )
							+ " (" + e.getMessage() + ")";
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
		private boolean sort;
		
		public DatabaseRebuilder( boolean sort ) {
			super();
			this.sort = sort;
		}
		
		public Object doInBackground() {
			FileCounter counter = new FileCounter();
			
			// Count files first, updating progress semi-nondeterministically
			view.setProgress( "Counting files...", "File Count Complete", 0 );
			try {
				Files.walkFileTree( Paths.get( db ), counter );
			}
			catch ( IOException e ) {
				view.handleException( e );
				e.printStackTrace();
			}
			
			view.setProgress( "Rebuilding Database...", "Database Rebuilt", 0 );
			JSonRebuilder rebuilder = new JSonRebuilder( counter.count, sort );
			try {
				Files.walkFileTree( Paths.get( db ), rebuilder );
			}
			catch ( IOException e ) {
				view.handleException( e );
				e.printStackTrace();
			}
			
			if ( uniques == null ) new UniquesFetcher().doInBackground();
			writeUniques();
			view.updateProgress( 100 );
			return null;
		}
		
		public abstract void done();
		
		public void process( Object o ) {};
		
	}
	
	class DatabaseSearch extends SimpleFileVisitor<Path> {
		
		private Thread t;
		private SearchTerms terms;
		private SearchView searchview;
		private int page = 1;
		private int hitAnimations = 0, hitLines = 0;
		private AproposLabel positionlabel;
		private StageMap currentmap;
		
		private TreeMap<Path, BasicFileAttributes> directory;
		private final Comparator<Path> fileSorter = new Comparator<Path>() {
			public int compare( Path o1, Path o2 ) {
				String s1 = o1.getFileName().toString(), s2 = o2.getFileName().toString();
				boolean r1 = s1.replaceAll( "\\.txt|_Stage\\d{1,2}|_Orgasm", "" ).endsWith( "_Rape" );
				boolean r2 = s2.replaceAll( "\\.txt|_Stage\\d{1,2}|_Orgasm", "" ).endsWith( "_Rape" );
				if ( r1 != r2 ) {
					if ( r1 ) return 1;
					if ( r2 ) return -1;
				}
				return o1.compareTo( o2 );
			}
		};
		
		public DatabaseSearch( SearchTerms terms, SearchView searchview ) {
			this.terms = terms;
			this.searchview = searchview;
		}
		
		public void execute() {
			view.setProgress( "Searching: " + terms.name, "Search Page " + page + " Complete", 0 );
			t = new Thread( new Start(), "edit.search.thread" );
			t.start();
		}
		
		public void stop() {
			t.interrupt();
		}
		
		public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
			String path = dir.toString().replace( db, "" );
			if ( ! ( dir + fs ).equals( db ) && !terms.matchesDirectory( path ) ) return FileVisitResult.SKIP_SUBTREE;
			directory = new TreeMap<Path, BasicFileAttributes>( fileSorter );
			return FileVisitResult.CONTINUE;
		}
		
		public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
			for ( Path key : directory.keySet() ) {
				FileVisitResult ret = visitOrderedFile( key, directory.get( key ) );
				if ( ret != FileVisitResult.CONTINUE ) return ret;
			}
			directory.clear();
			return FileVisitResult.CONTINUE;
		}
		
		public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
			directory.put( file, attrs );
			return FileVisitResult.CONTINUE;
		}
		
		public FileVisitResult visitOrderedFile( Path path, BasicFileAttributes attrs ) throws IOException {
			if ( t.isInterrupted() ) return FileVisitResult.TERMINATE;
			File file = path.toFile();
			if ( !file.getName().endsWith( ".txt" ) ) return FileVisitResult.CONTINUE;
			for ( String str : searchSkip )
				if ( str.equals( file.getName() ) ) return FileVisitResult.CONTINUE;
			AproposLabel stagelabel = stageLabelFromFile( file );
			if ( positionlabel != null ) {
				// Check if we're still in the same animation
				if ( positionlabel.toString().equals( stagelabel.getParentLabel().toString() ) ) {}
				else {
					// New animation! Time to publish the last one!
					FileVisitResult ret = publishStageMap();
					if ( ret != FileVisitResult.CONTINUE ) return ret;
				}
			}
			if ( terms.matchesStage( stagelabel ) ) {
				PerspectiveMap map = getPerspectives( stagelabel, file );
				boolean hit = false;
				for ( AproposLabel perslabel : map.keySet() )
					if ( terms.matchesPerspective( perslabel ) ) {
						LabelList labelList = map.get( perslabel );
						for ( int i = 0; i < labelList.size(); i++ ) {
							AproposLabel line = labelList.get( i );
							if ( terms.matches( line.getText() ) ) {
								line.setMatch( true );
								hit = true;
								hitLines++ ;
								AproposConflictLabel con = terms.matchReplacement( line );
								if ( con != null ) {
									labelList.set( i, con );
									labelList.setConflicted( true );
								}
							}
						}
					}
				if ( hit ) {
					if ( positionlabel == null ) {
						positionlabel = stagelabel.getParentLabel();
						currentmap = new StageMap();
					}
					currentmap.put( stagelabel, map );
				}
			}
			return FileVisitResult.CONTINUE;
		}
		
		public FileVisitResult publishStageMap() {
			if ( positionlabel != null ) {
				if ( terms.prePublishCheck( currentmap ) ) {
					hitAnimations++ ;
					int pAnims = ( hitAnimations * 100 ) / ( terms.maxAnimations * page );
					int pLines = ( hitLines * 100 ) / ( terms.maxLines * page );
					int progress = Math.max( pAnims, pLines );
					SwingUtilities.invokeLater( new UpdateProgress( Math.min( progress, 100 ) ) );
					SwingUtilities.invokeLater( new PublishStageMap( currentmap ) );
					positionlabel = null;
					currentmap = null;
					if ( progress >= 100 ) {
						SwingUtilities.invokeLater( new PublishPage( false ) );
						try {
							synchronized ( this ) {
								this.wait();
							}
							page++ ;
						}
						catch ( InterruptedException e ) {
							return FileVisitResult.TERMINATE;
						}
					}
				}
				else {
					hitLines -= currentmap.size();
					positionlabel = null;
					currentmap = null;
				}
			}
			return FileVisitResult.CONTINUE;
		}
		
		private class Start implements Runnable {
			public void run() {
				try {
					Files.walkFileTree( Paths.get( db ), DatabaseSearch.this );
					if ( !t.isInterrupted() ) {
						publishStageMap();
						SwingUtilities.invokeLater( new PublishPage( true ) );
					}
					else
						SwingUtilities.invokeLater( new UpdateAbort() );
				}
				catch ( IOException e ) {
					view.handleException( e );
					e.printStackTrace();
				}
			}
		}
		
		private class UpdateProgress implements Runnable {
			private int p;
			
			public UpdateProgress( int p ) {
				this.p = p;
			}
			public void run() {
				view.updateProgress( p );
			}
		}
		
		private class UpdateAbort implements Runnable {
			public void run() {
				view.setProgress( "", "Search cancelled", 100 );
			}
		}
		
		private class PublishPage implements Runnable {
			private boolean searchComplete;
			
			public PublishPage( boolean searchComplete ) {
				this.searchComplete = searchComplete;
			}
			public void run() {
				searchview.pageComplete( searchComplete );
			}
		}
		
		private class PublishStageMap implements Runnable {
			private StageMap map;
			
			public PublishStageMap( StageMap map ) {
				this.map = map;
			}
			public void run() {
				searchview.addStageMap( map );
			}
		}
		
		@SuppressWarnings("unused")
		private class PublishStageMapDebug implements Runnable {
			private StageMap map;
			
			public PublishStageMapDebug( StageMap map ) {
				this.map = map;
			}
			public void run() {
				System.out.println( "===== New Publish =====" );
				System.out.println( map.firstKey().getParentLabel() );
				for ( AproposLabel stage : map.keySet() ) {
					System.out.println( "\t" + stage.getText() );
					for ( AproposLabel perspec : map.get( stage ).keySet() ) {
						System.out.println( "\t\t" + perspec.getText() );
						for ( AproposLabel line : map.get( stage ).get( perspec ) ) {
							if ( line.isMatch() ) {
								System.out.println( "\t\t\t" + line.getText() );
							}
						}
					}
				}
			}
		}
		
	}
	
	public static abstract class SearchTerms {
		
		String name;
		int maxAnimations = 50, maxLines = 500;
		
		/**
		 * Protected Constructor for Deserialisation
		 */
		protected SearchTerms() {
			super();
		}
		
		public SearchTerms( String name ) {
			super();
			this.name = name;
		}
		
		public void setLimits( int maxAnimations, int maxLines ) {
			this.maxAnimations = maxAnimations;
			this.maxLines = maxLines;
		}
		
		/**
		 * Checks this perspective label against the three configurable bools, will only open their labellist if this returns true.
		 * 
		 * @param perslabel
		 * @return
		 */
		public abstract boolean matchesPerspective( AproposLabel perslabel );
		
		/**
		 * Gets given the name of a directory, which will only be opened if this returns true. Can be used to limit themes or FA/MA
		 * 
		 * @param dirname
		 * @return
		 */
		public abstract boolean matchesDirectory( String dirname );
		
		/**
		 * Gets given the label for a stage, which will only be opened if this returns true. Can be used to discard certain stages,
		 * positions, rape files, etc
		 * 
		 * @param stagelabel
		 * @return
		 */
		public abstract boolean matchesStage( AproposLabel stagelabel );
		
		/**
		 * The function that each line will be checked against
		 * 
		 * @param text
		 * @return true if this line matched the SearchTerms, and should be displayed.
		 */
		public abstract boolean matches( String text );
		
		/**
		 * Can be used to suggest a replacement for an matched AproposLabel
		 * 
		 * @param label
		 * @return
		 */
		public abstract AproposConflictLabel matchReplacement( AproposLabel label );
		
		/**
		 * Optional method that allows manipulation and verification of the stagemap of matches before it is published
		 * 
		 * @param map
		 * @return
		 */
		public boolean prePublishCheck( StageMap map ) {
			return true;
		}
		
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + maxAnimations;
			result = prime * result + maxLines;
			return result;
		}
		public boolean equals( Object obj ) {
			if ( this == obj ) return true;
			if ( obj == null ) return false;
			if ( getClass() != obj.getClass() ) return false;
			SearchTerms other = (SearchTerms) obj;
			if ( maxAnimations != other.maxAnimations ) return false;
			if ( maxLines != other.maxLines ) return false;
			return true;
		}
		
	}
	
	public static abstract class FileFilterSearchTerms extends SearchTerms {
		
		String pathSub = "";
		
		public FileFilterSearchTerms() {
			super();
		}
		public FileFilterSearchTerms( String name ) {
			super( name );
		}
		public void setPathSub( String pathSub ) {
			this.pathSub = pathSub;
		}
		public boolean matchesPerspective( AproposLabel perslabel ) {
			return true;
		}
		public boolean matchesDirectory( String dirname ) {
			return pathSub == null || dirname.contains( pathSub );
		}
		public boolean matchesStage( AproposLabel stagelabel ) {
			return true;
		}
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ( ( pathSub == null ) ? 0 : pathSub.hashCode() );
			return result;
		}
		public boolean equals( Object obj ) {
			if ( this == obj ) return true;
			if ( !super.equals( obj ) ) return false;
			if ( getClass() != obj.getClass() ) return false;
			FileFilterSearchTerms other = (FileFilterSearchTerms) obj;
			if ( pathSub == null ) {
				if ( other.pathSub != null ) return false;
			}
			else if ( !pathSub.equals( other.pathSub ) ) return false;
			return true;
		}
		
	}
	
	public static abstract class UserSearchTerms extends FileFilterSearchTerms implements Serializable {
		
		private static final long serialVersionUID = -4581630134768413310L;
		transient AproposLabel lowerBound, upperBound;
		boolean first = true, second = true, third = true;
		int searchMode = 0, rapeMode = 2, lowerBoundInt = 0, upperBoundInt = 6;
		String search;
		boolean caseSens;
		private static final String[] searchModes = new String[] { "Simple", "Whole Word", "Regex" };
		private static final String[] rapeModes = new String[] { "No Rape", "Rape Only", "" };
		private static final String[] perModes = new String[] { "", "1st Only", "2nd Only", "No 3rd", "3rd Only", "No 2nd", "No 1st", "" };
		public static final String[] stageValues = new String[] { "Intro", "Stage 1", "Stage 2", "Stage 3", "Stage 4", "Stage 5+",
				"Orgasm" };
		
		/**
		 * An extension of SearchTerms to provide more methods for constructing and deconstructing SearchTerm objects
		 * 
		 * @param name
		 */
		public UserSearchTerms() {
			super();
		}
		
		public void setStages( int lower, int upper ) {
			AproposLabel parent = new AproposLabel( "Position", new AproposLabel( "Folder", new AproposLabel( "Database", null ) ) );
			lowerBoundInt = lower;
			upperBoundInt = upper;
			String lowerBound = stageValues[lower];
			String upperBound = stageValues[upper];
			if ( lowerBound.equals( "Stage 5+" ) ) lowerBound = "Stage 5";
			if ( upperBound.equals( "Stage 5+" ) ) upperBound = "Stage 999";
			this.lowerBound = new AproposLabel( lowerBound, parent );
			this.upperBound = new AproposLabel( upperBound, parent );
		}
		
		/**
		 * Designed to take the value from three JCheckBoxes (at least one should be true)
		 * <br>
		 * Decides how {@link #matchesPerspective(AproposLabel)} will return given a <code>PerspectiveLabel</code>
		 * 
		 * @param first
		 * @param second
		 * @param third
		 */
		public void setPerspectives( boolean first, boolean second, boolean third ) {
			this.first = first;
			this.second = second;
			this.third = third;
		}
		
		/**
		 * Designed to take the value from three JRadioButtons (i.e. only one should be true)
		 * <br>
		 * Decides how {@link #matchesRape(boolean)} will return in {@link #matchesStage(AproposLabel)}.
		 * 
		 * @param noRape
		 * @param rapeOnly
		 * @param both
		 */
		public void setRapes( boolean noRape, boolean rapeOnly, boolean both ) {
			if ( noRape )
				rapeMode = 0;
			else if ( rapeOnly ) rapeMode = 1;
		}
		
		/**
		 * Sets the search string entered by the user. used in {@link SearchTerms#matches(String)}
		 * 
		 * @param str
		 */
		public void setSearchString( String str ) {
			search = str;
		}
		
		public String generateName() {
			int perMode = ( first ? 1 : 0 ) + ( second ? 1 : 0 ) * 2 + ( third ? 1 : 0 ) * 4;
			String perStr = perModes[perMode];
			String rapeStr = rapeModes[rapeMode];
			name = search + "-" + searchModes[searchMode] + ( perStr.length() > 1 ? ", " + perStr : "" )
					+ ( rapeStr.length() > 1 ? ", " + rapeStr : "" );
			return name;
		}
		
		public String toString() {
			return name != null ? name : generateName();
		}
		
		public boolean matchesPerspective( AproposLabel perslabel ) {
			if ( perslabel.getText().equals( "1st Person" ) ) return first;
			if ( perslabel.getText().equals( "2nd Person" ) ) return second;
			if ( perslabel.getText().equals( "3rd Person" ) ) return third;
			return false;
		}
		
		/**
		 * @param isRape If the current StageLabel denotes rape
		 * @return true if the user wished to include this label in their results
		 */
		public boolean matchesRape( boolean isRape ) {
			return rapeMode == 2 | ( isRape & rapeMode == 1 ) | ( !isRape & rapeMode == 0 );
		}
		
		public boolean matchesDirectory( String dirname ) {
			// TODO: Add Support for Themes/Uniques/Stuff
			return super.matchesDirectory( dirname );
		}
		
		public boolean matchesStage( AproposLabel stagelabel ) {
			boolean matchesStage = stagelabel.compareEquals( lowerBound ) >= 0 && stagelabel.compareEquals( upperBound ) <= 0;
			boolean matchesRape = matchesRape( stagelabel.getParentLabel().getText().contains( "_Rape" ) );
			return matchesStage && matchesRape;
		}
		
		public AproposConflictLabel matchReplacement( AproposLabel label ) {
			return null;
		}
		
		public String serialise() {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream( baos );
				oos.writeObject( this );
				oos.close();
				return Base64.getEncoder().encodeToString( baos.toByteArray() );
			}
			catch ( IOException e ) {
				Thread.getDefaultUncaughtExceptionHandler().uncaughtException( Thread.currentThread(), e );
			}
			return null;
		}
		
		public static UserSearchTerms deserialise( String obj ) {
			try {
				ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( Base64.getDecoder().decode( obj ) ) );
				UserSearchTerms terms = (UserSearchTerms) ois.readObject();
				ois.close();
				return terms;
			}
			catch ( IOException | ClassNotFoundException e ) {
				Thread.getDefaultUncaughtExceptionHandler().uncaughtException( Thread.currentThread(), e );
			}
			return null;
		}

		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ( caseSens ? 1231 : 1237 );
			result = prime * result + ( first ? 1231 : 1237 );
			result = prime * result + lowerBoundInt;
			result = prime * result + rapeMode;
			result = prime * result + ( ( search == null ) ? 0 : search.hashCode() );
			result = prime * result + searchMode;
			result = prime * result + ( second ? 1231 : 1237 );
			result = prime * result + ( third ? 1231 : 1237 );
			result = prime * result + upperBoundInt;
			return result;
		}
		
		public boolean equals( Object obj ) {
			if ( this == obj ) return true;
			if ( !super.equals( obj ) ) return false;
			if ( getClass() != obj.getClass() ) return false;
			UserSearchTerms other = (UserSearchTerms) obj;
			if ( caseSens != other.caseSens ) return false;
			if ( first != other.first ) return false;
			if ( lowerBoundInt != other.lowerBoundInt ) return false;
			if ( rapeMode != other.rapeMode ) return false;
			if ( search == null ) {
				if ( other.search != null ) return false;
			}
			else if ( !search.equals( other.search ) ) return false;
			if ( searchMode != other.searchMode ) return false;
			if ( second != other.second ) return false;
			if ( third != other.third ) return false;
			if ( upperBoundInt != other.upperBoundInt ) return false;
			return true;
		}
		
	}
	
	public static class SimpleUserSearchTerms extends UserSearchTerms {
		private static final long serialVersionUID = 7921686259569421410L;
		private String realSearch;
		
		public SimpleUserSearchTerms() {
			super();
			searchMode = 0;
		}
		public void setSearchString( String str ) {
			super.setSearchString( str );
			if ( !caseSens )
				realSearch = search.toLowerCase();
			else
				realSearch = search;
		}
		public boolean matches( String text ) {
			if ( !caseSens ) text = text.toLowerCase();
			return text.matches( "^.*" + Pattern.quote( realSearch ) + ".*$" );
		}
	}
	
	public static class WWordUserSearchTerms extends UserSearchTerms {
		private static final long serialVersionUID = -6481315721704786126L;
		private String[] words;
		private String bound = "( |\\p{Punct})";
		
		public WWordUserSearchTerms() {
			super();
			searchMode = 1;
		}
		public void setSearchString( String str ) {
			if ( !caseSens ) str = str.toLowerCase();
			words = str.split( "\\|" );
			for ( int i = 0; i < words.length; i++ ) {
				words[i] = Pattern.quote( words[i] ).replaceAll( "\\*", "\\\\E.\\\\Q" );
			}
			search = str;
		}
		public boolean matches( String text ) {
			if ( !caseSens ) text = text.toLowerCase();
			text = " " + text + " ";
			for ( int i = 0; i < words.length; i++ ) {
				if ( text.matches( "^.*" + bound + words[i] + bound + ".*$" ) )
					return true;
			}
			return false;
		}
	}
	
	public static class RegexUserSearchTerms extends UserSearchTerms {
		private static final long serialVersionUID = -5000239467274727949L;
		
		public RegexUserSearchTerms() {
			super();
			searchMode = 2;
		}
		public boolean matches( String text ) {
			Pattern p = Pattern.compile( search, caseSens ? 0 : Pattern.CASE_INSENSITIVE );
			return p.matcher( text ).find();
		}
	}
	
	public static class BrokenSynonymsFinder extends FileFilterSearchTerms {
		
		SynonymsMap synonyms;
		
		Pattern p = Pattern.compile( "\\{|\\}|([A-Z_0-9]{3,})" );
		
		String line;
		String replacement;
		int consumed;
		int openTag;
		String potentialTag;
		int potentialStart;
		boolean confident;
		
		public BrokenSynonymsFinder( SynonymsMap synonyms ) {
			super( "Broken Synonyms" );
			this.synonyms = synonyms;
			setLimits( maxAnimations, maxLines / 2 );
		}
		
		public String fixSynonyms( String lineP ) {
			line = lineP;
			
			// Reset Fields
			replacement = "";
			consumed = -1;
			openTag = -1;
			potentialTag = "";
			potentialStart = -1;
			confident = true;
			
			Matcher m = p.matcher( line );
			
			while ( m.find() ) {
				char c = m.group().charAt( 0 );
				switch ( c ) {
					case '{':
						if ( openTag != -1 ) {
							fixUnclosedOpen();
							potentialTag = "";
						}
						openTag = m.start();
						break;
					case '}':
						int closeTag = m.start();
						if ( openTag != -1 & !potentialTag.equals( "" ) ) {
							// We have a complete tag, lets check it
							potentialTag = '{' + potentialTag + '}';
							String tag = line.substring( openTag, closeTag + 1 );
							// Collect all the characters between our last match and this one.
							if ( consumed + 1 <= openTag ) replacement = replacement + line.substring( consumed + 1, openTag );
							if ( synonyms.containsKey( tag ) ) {
								// All good!
								replacement = replacement + tag;
							}
							else if ( synonyms.containsKey( potentialTag ) ) {
								// Stray spaces most likely, lets just ignore them
								replacement = replacement + potentialTag;
							}
							else {
								// Attempt to correct typos... TODO: that.
								confident = false;
								replacement = replacement + "{UNKNOWN_TAG}";
							}
							consumed = closeTag;
						}
						else if ( openTag == -1 ) {
							// We're missing an open tag
							potentialTag = '{' + potentialTag + '}';
							if ( synonyms.containsKey( potentialTag ) ) {
								if ( consumed + 1 <= potentialStart )
									replacement = replacement + line.substring( consumed + 1, potentialStart );
								replacement = replacement + potentialTag;
								consumed = closeTag;
							}
						}
						else {
							// We're missing text between the tags
						}
						openTag = -1;
						potentialTag = "";
						break;
					default:
						// Found a string of AllCaps/Snakecase
						if ( !potentialTag.equals( "" ) ) {
							if ( openTag == -1 | potentialStart - openTag != 1 ) { // In case the open tag is unrelated
								// We have a possible tag missing brackets
								fixUnTagged();
							}
							else {
								// We had an unclosed tag and have just run into an unopened tag
								// Edge cases for days.
								fixUnclosedOpen();
								openTag = -1;
							}
						}
						potentialTag = m.group();
						potentialStart = m.start();
						break;
				}
			}
			if ( potentialTag.equals( "" ) & openTag != -1 ) {
				fixUnclosedOpen();
			}
			else if ( openTag == -1 ) {
				fixUnTagged();
			}
			
			replacement = replacement + line.substring( consumed + 1 );
			
			return replacement;
		}
		
		private void fixUnTagged() {
			potentialTag = '{' + potentialTag + '}';
			if ( synonyms.containsKey( potentialTag ) ) {
				// We do have a tag missing brackets
				if ( consumed + 1 <= potentialStart ) replacement = replacement + line.substring( consumed + 1, potentialStart );
				replacement = replacement + potentialTag;
				consumed = potentialStart + potentialTag.length() - 3; // The three brackets we added. Look, don't ask
																		 // questions, off by one errors suck.
			}
			else {
				// Probably just all caps, lets move on
			}
		}
		
		private void fixUnclosedOpen() {
			potentialTag = '{' + potentialTag + '}';
			if ( synonyms.containsKey( potentialTag ) ) {
				if ( consumed + 1 <= openTag - 1 ) replacement = replacement + line.substring( consumed + 1, openTag );
				replacement = replacement + potentialTag;
				consumed = openTag + potentialTag.length() - 2; // The two brackets we added
			}
		}
		
		public boolean matches( String text ) {
			String replacement = fixSynonyms( text );
			if ( replacement.equals( text ) ) return false;
			return true;
		}
		
		public AproposConflictLabel matchReplacement( AproposLabel label ) {
			AproposConflictLabel ret = new AproposConflictLabel( label );
			ret.markAll( !confident );
			AproposLabel rep = new AproposLabel( replacement, label.getParentLabel() );
			rep.setMatch( true );
			ret.addConflict( rep, true );
			return ret;
		}
		
	}
	
	public static class SynonymsSuggester extends FileFilterSearchTerms {
		
		SynonymsMap synonyms;
		String[] keys;
		String replacement;
		
		public SynonymsSuggester( SynonymsMap synonyms, String[] keys ) {
			super( "Synonyms Suggestions" );
			setLimits( maxAnimations, maxLines / 2 );
			this.synonyms = synonyms;
			this.keys = keys;
		}
		
		public boolean matches( String text ) {
			replacement = " " + text + " ";
			for ( String key : keys ) {
				for ( String synonym : synonyms.get( key ) ) {
					replacement = replacement.replaceAll( "( |\\p{Punct})" + Pattern.quote( synonym ) + "( |\\p{Punct})",
							"$1" + key + "$2" );
				}
			}
			replacement = replacement.replaceAll( "^ (.*) $", "$1" );
			if ( text.equals( replacement ) ) return false;
			return true;
		}
		
		public AproposConflictLabel matchReplacement( AproposLabel label ) {
			AproposConflictLabel ret = new AproposConflictLabel( label );
			AproposLabel rep = new AproposLabel( replacement, label.getParentLabel() );
			rep.setMatch( true );
			ret.addConflict( rep, true );
			return ret;
		}
		
	}
	
	public static class DupeFinder extends FileFilterSearchTerms {
		
		public DupeFinder() {
			super( "Duplicate Lines" );
			setLimits( maxAnimations / 2, maxLines * 16 );
		}
		
		public boolean matches( String text ) {
			return true;
		}
		
		public AproposConflictLabel matchReplacement( AproposLabel label ) {
			return null;
		}
		
		public boolean prePublishCheck( StageMap map ) {
			for ( AproposLabel line : map ) {
				line.setMatch( false );
			}
			map.resetMatches();
			boolean dupes = map.checkDuplicates();
			if ( dupes ) {
				LinkedList<AproposLabel> remove = new LinkedList<AproposLabel>();
				for ( AproposLabel stage : map.keySet() ) {
					PerspectiveMap perspectiveMap = map.get( stage );
					if ( perspectiveMap.isConflicted() )
						for ( AproposLabel perspec : perspectiveMap.keySet() ) {
							LabelList labelList = perspectiveMap.get( perspec );
							if ( labelList.isConflicted() ) for ( AproposLabel line : labelList ) {
								AproposConflictLabel conLab = (AproposConflictLabel) line;
								if ( conLab.matches() > 1 ) {
									conLab.setMatch( true );
								}
							}
						}
					else {
						remove.add( stage );
					}
				}
				for ( AproposLabel key : remove )
					map.remove( key );
				map.resetMatches();
			}
			return dupes;
		}
		
	}
	
	public static class LongLineFinder extends FileFilterSearchTerms {
		
		float cutoff;
		private SynonymsLengthMap synonymsLength;
		
		public LongLineFinder( SynonymsLengthMap synonymsLength, float per ) {
			super( "Long Lines" );
			this.synonymsLength = synonymsLength;
			this.cutoff = per;
		}
		
		public boolean matches( String text ) {
			float per = AproposLabel.getWarningPer( synonymsLength, text );
			return per >= cutoff;
		}
		
		public AproposConflictLabel matchReplacement( AproposLabel label ) {
			return null;
		}
		
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
			view.setProgress( "Simulating...", "Simulation Complete", 0 );
			// Put the Active and Primary names into the Synonyms Map
			synonyms.setActors( primary, active );
			synonyms.setLevels( -1, -1 );
			
			// Don't bother picking out 'played' lines if the map is being used in a seach
			StageMap selectedMap = null;
			if ( !stageMap.hasMatches() ) {
				// Create a new StageMap with only one label randomly selected from the original map
				selectedMap = new StageMap();
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
			}
			
			// Iterate through every label and insert words
			for ( AproposLabel stage : stageMap.keySet() ) {
				if ( r.nextFloat() > 0.2f & synonyms.levelArousal != -1 ) {
					synonyms.levelArousal++ ;
				}
				if ( r.nextFloat() > 0.5f & synonyms.levelWearNTear != -1 ) {
					synonyms.levelWearNTear++ ;
				}
				PerspectiveMap persMap = stageMap.get( stage );
				for ( AproposLabel perspec : persMap.keySet() ) {
					LabelList list = persMap.get( perspec );
					if ( stageMap.hasMatches() & !list.hasMatches() ) continue;
					AproposLabel selected;
					if ( selectedMap != null ) {
						LabelList selList = selectedMap.get( stage ).get( perspec );
						selected = selList.size() == 0 ? null : selList.get( 0 );
					}
					else
						selected = null;
					for ( AproposLabel label : list ) {
						if ( stageMap.hasMatches() & !label.isMatch() ) continue;
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
			view.updateProgress( 100 );
		};
		
		public abstract void process( List<AproposLabel> labels );
	}
	
}

/**
 * Wrapper for an Arraylist of the AproposLabels representing individual lines
 */
class LabelList extends ArrayList<AproposLabel> implements AproposMap {
	private static final long serialVersionUID = -3091716550688577792L;
	private boolean hasConflicts = false;
	private Boolean hasMatches = null;
	
	public LabelList() {}
	
	public LabelList( int initialCapacity ) {
		super( initialCapacity );
	}
	
	public LabelList( Collection<? extends AproposLabel> list ) {
		super( list );
	}
	
	public AproposLabel firstLine() {
		return get( 0 );
	}
	
	public boolean checkDuplicates() {
		if ( hasConflicts ) return true;
		int match = 0;
		LabelList conflictList = new LabelList( size() ); // ConflictLabels will be stored in a temporary list to preserve the main list
		TreeSet<Integer> skip = new TreeSet<Integer>();
		for ( int i = 0; i < size(); i++ ) {
			if ( skip.contains( i ) ) continue;
			AproposLabel oldLabel = get( i );
			AproposConflictLabel label = new AproposConflictLabel( oldLabel );
			conflictList.add( label );
			for ( int j = i + 1; j < size(); j++ ) { // Triangular Matrix iteration
				if ( skip.contains( j ) ) continue;
				AproposLabel checking = get( j );
				match = Model.fuzzyMatches( label, checking );
				if ( match < 5 ) {
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
	
	public boolean checkDuplicatesInPlace() {
		if ( hasConflicts ) return true;
		if ( !hasMatches() ) return false;
		TreeSet<Integer> skip = new TreeSet<Integer>();
		for ( int i = 0; i < size(); i++ ) {
			AproposLabel current = get( i );
			if ( !current.isMatch() ) continue;
			if ( skip.contains( i ) ) continue;
			AproposConflictLabel label = null;
			for ( int j = i + 1; j < size(); j++ ) {
				AproposLabel checking = get( j );
				if ( !checking.isMatch() ) continue;
				if ( skip.contains( j ) ) continue;
				if ( label != null ) {
					int match = Model.fuzzyMatches( label, checking );
					if ( match < 5 ) {
						label.addConflict( checking, match );
						skip.add( j );
					}
				}
				else {
					int match = Model.fuzzyMatches( current, checking );
					if ( match < 5 ) {
						label = new AproposConflictLabel( current );
						label.addConflict( checking, match );
						skip.add( j );
					}
				}
			}
			if ( label != null ) {
				set( i, label );
				hasConflicts = true;
			}
		}
		Integer i;
		while ( ( i = skip.pollLast() ) != null ) {
			remove( i.intValue() );
		}
		return hasConflicts;
	}
	
	public void resolveConflicts() {
		if ( !hasConflicts ) return;
		LabelList newList = new LabelList( size() * 3 ); // Assume up to three duplicates per line, to avoid needing to expand the array
		for ( AproposLabel apLabel : this ) {
			if ( ! ( apLabel instanceof AproposConflictLabel ) ) {
				newList.add( apLabel ); // Thanks to searches, not every entry has to be a conflict
				continue;
			}
			AproposConflictLabel label = (AproposConflictLabel) apLabel;
			newList.addAll( label.resolveConficts() );
		}
		clear();
		addAll( newList );
		hasConflicts = false;
	}
	
	public boolean isConflicted() {
		return hasConflicts;
	}
	
	public void setConflicted( boolean hasConflicts ) {
		this.hasConflicts = hasConflicts;
	}
	
	public void resetMatches() {
		hasMatches = null;
	}
	
	public boolean hasMatches() {
		if ( hasMatches != null ) return hasMatches;
		boolean bool = false;
		for ( AproposLabel l : this ) {
			if ( l.isMatch() ) {
				bool = true;
				break;
			}
		}
		hasMatches = bool;
		return bool;
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
	
	public String toJSON() throws IOException {
		StringWriter json = new StringWriter();
		JsonWriter writer = new JsonWriter( json );
		writer.setIndent( "    " );
		writer.beginObject();
		toJSON( writer );
		writer.endObject();
		writer.close();
		String string = json.toString();
		string = "    " + string.replaceFirst( "^\\{([\\s\\S]*)\\}$", "$1" ).trim();
		return string;
	}
	
	public JsonWriter toJSON( JsonWriter writer ) throws IOException {
		writer.name( get( 0 ).getParentLabel().getText() );
		writer.beginArray();
		for ( AproposLabel label : this ) {
			String text = label.getText();
			if ( !text.equals( "" ) )
				writer.value( text );
			else if ( size() == 1 ) writer.value( text );
		}
		writer.endArray();
		return writer;
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
	
	public JsonWriter toJSON( JsonWriter writer ) throws IOException {
		writer.beginObject();
		for ( AproposLabel per : keySet() )
			get( per ).toJSON( writer );
		writer.endObject();
		return writer;
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
	private Boolean hasMatches = null;
	
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
	
	public AproposLabel firstLine() {
		return firstEntry().getValue().firstLine();
	}
	
	public Iterator<AproposLabel> iterator() {
		return new Linerator();
	}
	
	private class Linerator implements Iterator<AproposLabel> {
		
		Iterator<AproposLabel> keys;
		Iterator<AproposLabel> next;
		
		public boolean hasNext() {
			if ( keys == null ) {
				keys = keySet().iterator();
				if ( hasNext() ) {
					next = get( keys.next() ).iterator();
					return true;
				}
				else
					return false;
			}
			return keys.hasNext() || next.hasNext();
		}
		
		public AproposLabel next() {
			boolean hasNext = next.hasNext();
			if ( !hasNext ) {
				next = get( keys.next() ).iterator();
				return next();
			}
			AproposLabel next2 = next.next();
			return next2;
		}
		
	}
	
	public boolean checkDuplicates() {
		if ( hasMatches() ) return checkDuplicatesInPlace();
		boolean bool = false;
		for ( AproposLabel key : keySet() ) {
			bool = get( key ).checkDuplicates() | bool;
		}
		return bool;
	}
	
	public boolean checkDuplicatesInPlace() {
		boolean bool = false;
		for ( AproposLabel key : keySet() ) {
			bool = get( key ).checkDuplicatesInPlace() | bool;
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
	
	public void resetMatches() {
		hasMatches = null;
		for ( AproposLabel key : keySet() )
			get( key ).resetMatches();
	}
	
	public boolean hasMatches() {
		if ( hasMatches != null ) return hasMatches;
		boolean bool = false;
		for ( AproposLabel key : keySet() ) {
			bool = get( key ).hasMatches();
			if ( bool ) break;
		}
		hasMatches = bool;
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
	
	public String toJSON() throws IOException {
		StringWriter string = new StringWriter();
		JsonWriter writer = new JsonWriter( string );
		writer.setIndent( "    " );
		toJSON( writer );
		writer.close();
		return string.toString();
	}
	
	public JsonWriter toJSON( JsonWriter writer ) throws IOException {
		writer.beginObject();
		for ( AproposLabel key : keySet() ) {
			writer.name( key.getText() );
			get( key ).toJSON( writer );
		}
		writer.endObject();
		return writer;
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

interface AproposMap extends Iterable<AproposLabel> {
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
	 * @return The first line label in this map;
	 */
	public AproposLabel firstLine();
	/**
	 * Checks the entire map for duplicate lines within the individual perspectives, and transforms Labels into ConflictLabels if conflicts
	 * are found
	 * 
	 * @return true if the map has been modified
	 */
	public boolean checkDuplicates();
	/**
	 * More memory efficient version of {@link #checkDuplicates()} for use in searches
	 * 
	 * @return true if there are conflicts among the matched lines
	 */
	public boolean checkDuplicatesInPlace();
	/**
	 * Rebuilds the map by deleting any lines marked un-needed and re-merging suspected duplicates back in
	 */
	public void resolveConflicts();
	/**
	 * @return true if any of the submaps have conflicts
	 */
	public boolean isConflicted();
	/**
	 * @return true if any of the submaps have matches
	 */
	public boolean hasMatches();
	/**
	 * To be used if lines have manually had their match state adjusted, allows the entire match to be rechecked
	 */
	public void resetMatches();
	/**
	 * @return This map represented in JSON
	 * @throws IOException
	 */
	public String toJSON() throws IOException;
	/**
	 * Appends this AproposMap to an existing JsonWriter
	 * 
	 * @param writer
	 * @return writer
	 * @throws IOException
	 */
	public JsonWriter toJSON( JsonWriter writer ) throws IOException;
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

class SynonymsMap {
	
	// If static typing has a downside, this is it.
	private TreeMap<String, ArrayList<String>> mapSynonyms = new TreeMap<String, ArrayList<String>>();
	private TreeMap<String, TreeMap<String, ArrayList<String>>> mapArousal = new TreeMap<String, TreeMap<String, ArrayList<String>>>();
	private TreeMap<String, TreeMap<String, ArrayList<String>>> mapWearNTear = new TreeMap<String, TreeMap<String, ArrayList<String>>>();
	private StageMap stagesSynonyms, stagesArousal, stagesWearNTear;
	private Set<String> keySet;
	String primary, active;
	int levelArousal = -1, levelWearNTear = -1;
	private Random r = new Random();
	
	public SynonymsMap() {
		super();
		TreeMap<String, ArrayList<String>> map = new TreeMap<String, ArrayList<String>>();
		mapWearNTear.put( "{WTVAGINAL}", map );
		mapWearNTear.put( "{WTORAL}", map );
		mapWearNTear.put( "{WTANAL}", map );
	}
	
	public String insert( String str ) {
		if ( isEmpty() ) return str;
		for ( String key : mapSynonyms.keySet() ) {
			List<String> list = get( key );
			if ( list.size() == 0 ) continue;
			String synonym = list.get( list.size() == 1 ? 0 : r.nextInt( list.size() ) );
			str = str.replace( key, synonym );
		}
		if ( levelArousal == -1 ) levelArousal = r.nextInt( mapArousal.firstEntry().getValue().size() );
		for ( String key : mapArousal.keySet() ) {
			List<String> list = get( key, levelArousal );
			if ( list.size() == 0 ) continue;
			String synonym = list.get( list.size() == 1 ? 0 : r.nextInt( list.size() ) );
			str = str.replace( key, synonym );
		}
		if ( levelWearNTear == -1 ) levelWearNTear = r.nextInt( mapArousal.firstEntry().getValue().size() );
		for ( String key : mapWearNTear.keySet() ) {
			List<String> list = get( key, levelWearNTear );
			if ( list.size() == 0 ) continue;
			String synonym = list.get( list.size() == 1 ? 0 : r.nextInt( list.size() ) );
			str = str.replace( key, synonym );
		}
		str = str.replace( "{ACTIVE}", active );
		str = str.replace( "{PRIMARY}", primary );
		return str;
	}
	
	public void put( String key, ArrayList<String> list ) {
		mapSynonyms.put( key, list );
	}
	
	public void putArousalLevel( String key, String level, ArrayList<String> list ) {
		TreeMap<String, ArrayList<String>> arousalmap = mapArousal.get( key );
		if ( arousalmap == null ) {
			arousalmap = new TreeMap<String, ArrayList<String>>();
			mapArousal.put( key, arousalmap );
		}
		arousalmap.put( level, list );
	}
	
	public void putWNTLevel( String level, ArrayList<String> list ) {
		TreeMap<String, ArrayList<String>> wntmap = mapWearNTear.get( mapWearNTear.firstKey() );
		wntmap.put( level, list );
	}
	
	public void setActors( String primary, String active ) {
		this.primary = primary;
		this.active = active;
	}
	
	public void setLevels( int levelArousal, int levelWearNTear ) {
		this.levelArousal = levelArousal;
		this.levelWearNTear = levelWearNTear;
	}
	
	public ArrayList<String> get( String key ) {
		if ( mapSynonyms.containsKey( key ) ) return mapSynonyms.get( key );
		if ( mapArousal.containsKey( key ) ) {
			ArrayList<String> ret = new ArrayList<String>();
			TreeMap<String, ArrayList<String>> map = mapArousal.get( key );
			for ( String level : map.keySet() )
				ret.addAll( map.get( level ) );
			return ret;
		}
		if ( mapWearNTear.containsKey( key ) ) {
			ArrayList<String> ret = new ArrayList<String>();
			TreeMap<String, ArrayList<String>> map = mapWearNTear.get( key );
			for ( String level : map.keySet() )
				ret.addAll( map.get( level ) );
			return ret;
		}
		return null;
	}
	
	public ArrayList<String> get( String key, int level ) {
		if ( mapSynonyms.containsKey( key ) ) return mapSynonyms.get( key );
		if ( mapArousal.containsKey( key ) ) {
			TreeMap<String, ArrayList<String>> map = mapArousal.get( key );
			level = Math.min( Math.max( 0, level ), map.size() - 1 );
			String keyLevel = "level" + level;
			return map.get( keyLevel );
		}
		if ( mapWearNTear.containsKey( key ) ) {
			TreeMap<String, ArrayList<String>> map = mapWearNTear.get( key );
			level = Math.min( Math.max( 0, level ), map.size() - 1 );
			String keyLevel = "level" + level;
			return map.get( keyLevel );
		}
		return null;
	}
	
	public boolean containsKey( String key ) {
		return mapSynonyms.containsKey( key ) | mapArousal.containsKey( key ) | mapWearNTear.containsKey( key ) | key.equals( "{ACTIVE}" )
				| key.equals( "{PRIMARY}" );
	}
	
	public Set<String> keySet() {
		if ( keySet == null ) {
			keySet = new HashSet<String>();
			keySet.addAll( mapSynonyms.keySet() );
			keySet.addAll( mapArousal.keySet() );
			keySet.addAll( mapWearNTear.keySet() );
		}
		return keySet;
	}
	
	public StageMap getSynonymsMap() {
		if ( stagesSynonyms == null ) {
			stagesSynonyms = new StageMap();
			AproposLabel parent = new AproposLabel( "Synonyms", new AproposLabel( "", new AproposLabel( "", null ) ) );
			for ( String key : mapSynonyms.keySet() ) {
				PerspectiveMap map = new PerspectiveMap();
				AproposLabel keyword = new AproposLabel( key, parent );
				ArrayList<String> source = mapSynonyms.get( key );
				LabelList list = new LabelList( source.size() );
				AproposLabel mandatorylabel = new AproposLabel( " ", keyword );
				for ( String word : mapSynonyms.get( key ) )
					list.add( new AproposLabel( word, mandatorylabel ) );
				map.put( mandatorylabel, list );
				stagesSynonyms.put( keyword, map );
			}
		}
		return stagesSynonyms;
	}
	
	public StageMap getArousalMap() {
		if ( stagesArousal == null ) {
			stagesArousal = new StageMap();
			AproposLabel parent = new AproposLabel( "Arousal_Descriptors", new AproposLabel( "", new AproposLabel( "", null ) ) );
			for ( String key : mapArousal.keySet() ) {
				PerspectiveMap map = new PerspectiveMap();
				AproposLabel keyword = new AproposLabel( key, parent );
				for ( String level : mapArousal.get( key ).keySet() ) {
					ArrayList<String> source = mapArousal.get( key ).get( level );
					LabelList list = new LabelList( source.size() );
					AproposLabel levellabel = new AproposLabel( level, keyword );
					for ( String word : source )
						list.add( new AproposLabel( word, levellabel ) );
					map.put( levellabel, list );
				}
				stagesArousal.put( keyword, map );
			}
		}
		return stagesArousal;
	}
	
	public StageMap getWearNTear() {
		if ( stagesWearNTear == null ) {
			stagesWearNTear = new StageMap();
			AproposLabel parent = new AproposLabel( "WearAndTear_Descriptors", new AproposLabel( "", new AproposLabel( "", null ) ) );
			PerspectiveMap map = new PerspectiveMap();
			AproposLabel keyword = new AproposLabel( "descriptors", parent );
			for ( String level : mapWearNTear.get( "{WTANAL}" ).keySet() ) {
				ArrayList<String> source = mapWearNTear.get( "{WTANAL}" ).get( level );
				LabelList list = new LabelList( source.size() );
				AproposLabel levellabel = new AproposLabel( level, keyword );
				for ( String word : source )
					list.add( new AproposLabel( word, levellabel ) );
				map.put( levellabel, list );
				stagesWearNTear.put( keyword, map );
			}
		}
		return stagesWearNTear;
	}
	
	public boolean isEmpty() {
		return mapSynonyms.isEmpty();
	}
	
}

class SynonymsLengthMap implements Serializable {
	// I apologise for every part of this class's naming scheme.
	
	private static final long serialVersionUID = 2103287506847983320L;
	private final HashMap<String, MinMax> map = new HashMap<String, MinMax>();
	public final int max;
	
	public SynonymsLengthMap( SynonymsMap synonyms ) {
		FontMetrics metrics = AproposLabel.metrics;
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
	
	public class MinMax implements Serializable {
		private static final long serialVersionUID = 3999420674018453005L;
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