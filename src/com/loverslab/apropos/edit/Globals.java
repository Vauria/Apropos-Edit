package com.loverslab.apropos.edit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Globals extends Properties {
	
	private static final long serialVersionUID = -7138614321046877052L;
	private File file;
	public String delimiter = "<";
	Properties defaultss = new Properties() {
		private static final long serialVersionUID = -4651248334485299896L;
		{
			setProperty( "locations", "" );
			setProperty( "size", "auto" );
			setProperty( "position", "auto" );
			setProperty( "active", "Sven" );
			setProperty( "primary", "Dovahkiin" );
			setProperty( "oc", "0" );
			setProperty( "lastsearch", "" );
		}
	};
	
	public Globals( File file ) {
		super();
		this.defaults = defaultss;
		this.file = file;
	}
	
	/**
	 * Read properties data from file
	 * 
	 * @return False if no file was found and defaults where used
	 */
	public boolean read() {
		boolean exists = file.exists();
		if ( exists ) try ( FileInputStream fis = new FileInputStream( file ) ) {
			load( fis );
		}
		catch ( FileNotFoundException e ) {
			// Should be immpossible
			e.printStackTrace();
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		return exists;
	}
	
	/**
	 * Write properties data to file
	 */
	public void write() {
		try ( FileOutputStream fos = new FileOutputStream( file ) ) {
			store( fos, "Apropos Edit Config" );
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
	}
	
}
