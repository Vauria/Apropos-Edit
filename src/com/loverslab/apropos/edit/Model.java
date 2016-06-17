package com.loverslab.apropos.edit;

import java.io.File;
import java.io.FileInputStream;
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

import javax.swing.SwingWorker;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;

public class Model {
	
	String db;
	AproposLabel root = new AproposLabel( db, null );
	
	public Model() {
		
	}
	
	public void setDataBase( String actionCommand ) {
		db = actionCommand + "\\";
	}
	
	public static String getAnimString( String folder, Position pos, boolean rape ) {
		return folder + ( pos != Position.Unique ? "_" + pos.name() : "" ) + ( rape ? "_Rape" : "" );
	}
	
	public StageMap getStages( AproposLabel parent ) {
		StageMap data = new StageMap();
		String path = db + parent.getParentLabel().toString() + "\\" + parent.toString();
		System.out.println( path );
		File file = new File( path + ".txt" );
		if ( file.exists() ) {
			AproposLabel stage = new AproposLabel( "Intro", parent );
			data.put( stage, getPerspectives( stage, file ) );
		}
		file = new File( path + "_Stage1.txt" );
		if(file.exists() ) {
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
		System.out.println( data.size() + " stages found" );
		return data;
	}
	
	public PerspectiveMap getPerspectives( AproposLabel parent, File file ) {
		PerspectiveMap data = new PerspectiveMap();
		try ( JsonReader reader = new JsonReader( new InputStreamReader( new FileInputStream( file ) ) ) ) {
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
		}
		catch ( IllegalStateException | MalformedJsonException e ) {
			System.err.println( "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) );
			System.err.println( e.getMessage() );
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		return data;
	}
	
	public void writeStages( StageMap stageMap ) {
		File file;
		AproposLabel first = stageMap.keySet().iterator().next();
		String path = db + first.getParentLabel().getParentLabel().getText() + "\\" + first.getParentLabel().getText();
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
			e.printStackTrace();
		}
	}
	
	public abstract class FolderListFetcher extends SwingWorker<List<String>, String> {
		
		public List<String> doInBackground() {
			File root = new File( db );
			File[] files = root.listFiles();
			for ( File file : files ) {
				if ( file.isDirectory() ) publish( file.getAbsolutePath().replace( db, "" ) );
			}
			return null;
		}
		
		protected abstract void done();
		
		public abstract void process( List<String> strings );
		
	}
	
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
	
	public class JSonRebuilder extends SimpleFileVisitor<Path> {
		private String[] skip = new String[] { "AnimationPatchups.txt", "Arousal_Descriptors.txt", "Themes.txt",
				"UniqueAnimations.txt", "WearAndTear_Damage.txt", "WearAndTear_Descriptors.txt",
				"WearAndTear_Effects.txt" };
		
		public FileVisitResult visitFile( Path path, BasicFileAttributes attr ) {
			File file = path.toFile();
			if ( file.getName().endsWith( ".txt" ) ) {
				for ( String str : skip )
					if ( str.equals( file.getName() ) ) return FileVisitResult.CONTINUE;
				writePerspectives( getPerspectives( null, file ), file );
			}
			return FileVisitResult.CONTINUE;
		}
	}
	
	public abstract class DatabaseRebuilder extends SwingWorker<Object, Object> {
		
		public DatabaseRebuilder() {
			super();
		}
		
		public Object doInBackground() {
			JSonRebuilder rebuilder = new JSonRebuilder();
			try {
				Files.walkFileTree( Paths.get( db ), rebuilder );
			}
			catch ( IOException e ) {
				e.printStackTrace();
			}
			return null;
		}
		
		public abstract void done();
		
		public void process( Object o ) {};
		
	}
	
	// This is the alternative to parameterising everything with
	// TreeMap<AproposLabel,TreeMap<AproposLabel,TreeMap<AproposLabel,TreeMap<AproposLabel,ArrayList<AproposLabel>>>>>
	public class LabelList extends ArrayList<AproposLabel> {
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
	
	public class PerspectiveMap extends TreeMap<AproposLabel, LabelList> {
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
	
	public class StageMap extends TreeMap<AproposLabel, PerspectiveMap> {
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
	
	public class PositionMap extends TreeMap<AproposLabel, StageMap> {
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
	
	public class FolderMap extends TreeMap<AproposLabel, PositionMap> {
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
	
}
