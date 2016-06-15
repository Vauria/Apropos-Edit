package com.loverslab.apropos.edit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.SwingWorker;

import com.google.gson.stream.JsonReader;

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
		String path = db + parent.getParent().toString() + "\\" + parent.toString();
		System.out.println( path );
		File file = new File( path + ".txt" );
		if ( file.exists() ) {
			AproposLabel stage = new AproposLabel( "Intro", parent );
			data.put( stage, getPerspectives( stage, file ) );
		}
		int i = 1;
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
					list.add( new AproposLabel( reader.nextString(), parent ) );
				reader.endArray();
				data.put( key, list );
			}
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		catch ( IllegalStateException e ) {
			System.err.println( "Error parsing " + file.getAbsolutePath().replace( db, "\\db\\" ) );
			System.err.println( e.getMessage() );
		}
		return data;
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
			System.out.println( "Position Fetcher Running" );
			return getStages( parent );
		}
		
		public abstract void done();
		
		public void process( Object o ) {};
		
	}
	
	// This is the alternative to parameterising everything with
	// TreeMap<AproposLabel,TreeMap<AproposLabel,TreeMap<AproposLabel,TreeMap<AproposLabel,ArrayList<AproposLabel>>>>>
	public class LabelList extends ArrayList<AproposLabel> {
		private static final long serialVersionUID = -3091716550688577792L;
		
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
		
		public String toString() {
			StringBuilder builder = new StringBuilder();
			Set<AproposLabel> keySet = keySet();
			AproposLabel[] keys = keySet.toArray( new AproposLabel[ keySet.size() ] );
			for ( int i = 0; i < size(); i++ ) {
				if ( i > 0 ) builder.append( "\n\t\t\t" );
				builder.append( keys[i].toString()+ "\n\t\t\t\t" + get( keys[i] ) );
			}
			return builder.toString();
		}
	}
	
	public class StageMap extends TreeMap<AproposLabel, PerspectiveMap> {
		private static final long serialVersionUID = -4569924813567288184L;
		
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
		
		public String toString() {
			StringBuilder builder = new StringBuilder();
			Set<AproposLabel> keySet = keySet();
			AproposLabel[] keys = keySet.toArray( new AproposLabel[ keySet.size() ] );
			for ( int i = 0; i < size(); i++ ) {
				if ( i > 0 ) builder.append( "\n\t" );
				builder.append( keys[i].toString() + "\n\t\t"  + get( keys[i] ) );
			}
			return builder.toString();
		}
	}
	
	public class FolderMap extends TreeMap<AproposLabel, PositionMap> {
		private static final long serialVersionUID = 3997804667766094854L;
		
		public String toString() {
			StringBuilder builder = new StringBuilder();
			Set<AproposLabel> keySet = keySet();
			AproposLabel[] keys = keySet.toArray( new AproposLabel[ keySet.size() ] );
			for ( int i = 0; i < size(); i++ ) {
				if ( i > 0 ) builder.append( "\n" );
				builder.append( keys[i].toString() + "\n\t"  + get( keys[i] ) );
			}
			return builder.toString();
		}
	}
	
}
