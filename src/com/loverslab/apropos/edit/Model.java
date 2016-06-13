package com.loverslab.apropos.edit;

import java.io.File;
import java.util.List;

import javax.swing.SwingWorker;

public class Model {
	
	String db;
	
	public Model() {
		
	}
	
	public void setDataBase( String actionCommand ) {
		db = actionCommand;
	}
	
	public abstract class AnimationFetcher extends SwingWorker<List<String>, String> {
		
		public List<String> doInBackground() {
			File root = new File( db );
			File[] files = root.listFiles();
			for ( File file : files )
				publish( file.getAbsolutePath().replace( db + "\\", "" ).replace( ".txt", "" ) );
			return null;
		}
		
		protected abstract void done();
		
		public abstract void process( List<String> strings );
		
	}
	
}
