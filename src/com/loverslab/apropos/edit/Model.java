package com.loverslab.apropos.edit;

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
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

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
	
	protected class PositionFinder extends SimpleFileVisitor<Path> {
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
	 * parameters, changes the parent's text to match those of the passed <code>newFolder</code> and <code>newAnim</code>, merges the each
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
	protected class JSonRebuilder extends SimpleFileVisitor<Path> {
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
					view.handleException( e );
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
	
	public LabelList() {}
	
	public LabelList( Collection<? extends AproposLabel> list ) {
		super( list );
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
	
	public T getEquivalent( AproposLabel equivKey ) {
		for ( AproposLabel key : keySet() ) {
			if ( key.getText().equals( equivKey.getText() ) ) return get( key );
		}
		return null;
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
	public LabelList labelList;
	public PerspectiveMap perspecMap;
	public StageMap stageMap;
	public PositionMap posMap;
	
	public <T extends AproposMap> Result( T map ) {
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
	public int totalSize();
	public Result query( AproposLabel key );
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