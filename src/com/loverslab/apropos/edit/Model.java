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
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

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
	AproposLabel root = new AproposLabel( db, null );
	TreeMap<String, Boolean> uniques = null;
	
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
		uniques = null;
		new UniquesFetcher().execute();
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
		String path = db + parent.getParentLabel().toString() + "\\" + parent.toString();
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
			else if ( ! ( list.size() == 1 & list.get( 0 ).toString().equals( "" ) ) ) {
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
		AproposLabel first = stageMap.keySet().iterator().next();
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
					writePerspectives( persMap, file );
					break;
				case "Stage":
					file = new File( path + "_Stage" + split[1] + ".txt" );
					writePerspectives( persMap, file );
					break;
				case "Orgasm":
					file = new File( path + "_Orgasm.txt" );
					writePerspectives( persMap, file );
					break;
				default:
					break;
			}
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
	
	/**
	 * Extracts the folder part of an animation file name
	 * 
	 * @param animString
	 * @return
	 */
	public String extractFolder( String animString ) {
		animString = animString.replace( ".txt", "" ).replace( "_Rape", "" ).replace( "_Orgasm", "" ).replaceAll( "_Stage[1-9]", "" );
		for ( Position p : Position.values() )
			animString = animString.replace( "_" + p.name(), "" );
		return animString;
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
			if ( files != null && files.length > 0 ) for ( File file : files ) {
				if ( file.isDirectory() ) publish( file.getAbsolutePath().replace( db, "" ) );
			}
			else
				view.handleException( new FileNotFoundException("No folders found in chosen directory") );
			return null;
		}
		
		protected void done() {};
		
		public abstract void process( List<String> strings );
		
	}
	
	/**
	 * SwingWorker that loads the database's UniqueAnimations.txt file into the member TreeMap, for future writing or querying with
	 * <code>isUnique(String string)</code>
	 *
	 * Does not Publish.
	 */
	public class UniquesFetcher extends SwingWorker<Object, Object> {
		
		public Object doInBackground() {
			uniques = new TreeMap<String, Boolean>();
			File file = new File( db + "UniqueAnimations.txt" );
			if ( file.exists() )
				try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
					reader.beginObject();
					while ( reader.hasNext() ) {
						while ( reader.hasNext() )
							uniques.put( reader.nextName(), reader.nextBoolean() );
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
	
	/**
	 * SwingWorker that fetches a <code>StageMap</code> to be processed by done() for displaying
	 *
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
	 * 
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
	 *
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
			AproposLabel first = toCopy.keySet().iterator().next();
			AproposLabel position = first.getParentLabel();
			AproposLabel folder = position.getParentLabel();
			
			position.setText( newAnim );
			folder.setText( extractFolder( newAnim ) );
			writeStages( toCopy );
			return toCopy;
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
			return null;
		}
		
		public abstract void done();
		
		public void process( Object o ) {};
		
	}
	
}

// This is the alternative to parameterising everything with
// TreeMap<AproposLabel,TreeMap<AproposLabel,TreeMap<AproposLabel,TreeMap<AproposLabel,ArrayList<AproposLabel>>>>>

class LabelList extends ArrayList<AproposLabel> {
	private static final long serialVersionUID = -3091716550688577792L;
	
	public int totalSize() {
		return size();
	}
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for ( int i = 0; i < size(); i++ ) {
			if ( i > 0 ) builder.append( "\n\t\t\t\t" );
			builder.append( get( i ) );
		}
		return builder.toString();
	}
}

class PerspectiveMap extends TreeMap<AproposLabel, LabelList> {
	private static final long serialVersionUID = 1659741172660975737L;
	
	public int totalSize() {
		int i = 0;
		for ( AproposLabel label : keySet() ) {
			i += get( label ).totalSize();
		}
		return i;
	}
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Set<AproposLabel> keySet = keySet();
		AproposLabel[] keys = keySet.toArray( new AproposLabel[ keySet.size() ] );
		for ( int i = 0; i < size(); i++ ) {
			if ( i > 0 ) builder.append( "\n\t\t\t" );
			builder.append( keys[i].toString() + "\n\t\t\t\t" + get( keys[i] ) );
		}
		return builder.toString();
	}
}

class StageMap extends TreeMap<AproposLabel, PerspectiveMap> {
	private static final long serialVersionUID = -4569924813567288184L;
	
	public int totalSize() {
		int i = 0;
		for ( AproposLabel label : keySet() ) {
			i += get( label ).totalSize();
		}
		return i;
	}
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Set<AproposLabel> keySet = keySet();
		AproposLabel[] keys = keySet.toArray( new AproposLabel[ keySet.size() ] );
		for ( int i = 0; i < size(); i++ ) {
			if ( i > 0 ) builder.append( "\n\t\t" );
			builder.append( keys[i].toString() + "\n\t\t\t" + get( keys[i] ) );
		}
		return builder.toString();
	}
}

class PositionMap extends TreeMap<AproposLabel, StageMap> {
	private static final long serialVersionUID = 8253283878828610516L;
	
	public int totalSize() {
		int i = 0;
		for ( AproposLabel label : keySet() ) {
			i += get( label ).totalSize();
		}
		return i;
	}
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Set<AproposLabel> keySet = keySet();
		AproposLabel[] keys = keySet.toArray( new AproposLabel[ keySet.size() ] );
		for ( int i = 0; i < size(); i++ ) {
			if ( i > 0 ) builder.append( "\n\t" );
			builder.append( keys[i].toString() + "\n\t\t" + get( keys[i] ) );
		}
		return builder.toString();
	}
}

class FolderMap extends TreeMap<AproposLabel, PositionMap> {
	private static final long serialVersionUID = 3997804667766094854L;
	
	public int totalSize() {
		int i = 0;
		for ( AproposLabel label : keySet() ) {
			i += get( label ).totalSize();
		}
		return i;
	}
	public String toString() {
		StringBuilder builder = new StringBuilder();
		Set<AproposLabel> keySet = keySet();
		AproposLabel[] keys = keySet.toArray( new AproposLabel[ keySet.size() ] );
		for ( int i = 0; i < size(); i++ ) {
			if ( i > 0 ) builder.append( "\n" );
			builder.append( keys[i].toString() + "\n\t" + get( keys[i] ) );
		}
		return builder.toString();
	}
}
