package com.loverslab.apropos.edit;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import com.loverslab.apropos.edit.Model.BrokenSynonymsFinder;
import com.loverslab.apropos.edit.Model.DatabaseSearch;
import com.loverslab.apropos.edit.Model.SearchTerms;
import com.loverslab.apropos.edit.Model.UserSearchTerms;

public class Tests {
	
	public static void main( String[] args ) throws Exception {
		synonymDetecting();
	}
	
	public static void wordWrap() {
		final JFrame frame = new JFrame( "Word Wrap Test" );
		frame.setLocationRelativeTo( null );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		
		JPanel panel = new ScrollPanel( new GridBagLayout() );
		SynonymsLengthMap map = null;
		try ( ObjectInputStream ois = new ObjectInputStream( new FileInputStream( new File( "synLen.obj" ) ) ) ) {
			map = (SynonymsLengthMap) ois.readObject();
		}
		catch ( IOException | ClassNotFoundException e ) {
			e.printStackTrace();
		}
		
		GridBagConstraints c = new GridBagConstraints( 0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
				new Insets( 0, 0, 0, 0 ), 0, 0 );
		c.insets = new Insets( 25, 10, 5, 10 );
		c.weightx = 1;
		c.weighty = 1;
		
		AproposLabel label = new AproposLabel(
				"Oh my! He's certainly enthusiastic, even with his packmate's dick so close. I moan around the slowly thrusting {COCK} in my mouth...",
				null );
		// JLabel jlabel = new JLabel("<html>Oh my! He's certainly enthusiastic, even with his packmate's dick so close. I moan around the
		// slowly thrusting {COCK} in my mouth...");
		// jlabel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));
		
		panel.add( label.display( null, null, map ), c );
		// panel.add(jlabel,c);
		
		JScrollPane scrollpane = new JScrollPane( panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		
		frame.setContentPane( scrollpane );
		
		frame.setSize( 800, 100 );
		frame.setVisible( true );
	}
	
	static class ScrollPanel extends JPanel implements Scrollable {
		private static final long serialVersionUID = -5835799549417498766L;
		
		public ScrollPanel() {
			super();
		}
		public ScrollPanel( LayoutManager layout ) {
			super( layout );
		}
		
		public Dimension getPreferredScrollableViewportSize() {
			return this.getPreferredSize();
		}
		public int getScrollableUnitIncrement( Rectangle visibleRect, int orientation, int direction ) {
			return 16;
		}
		public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ) {
			return 64;
		}
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
		
	}
	
	public static void shifting() {
		LabelList list = new LabelList();
		//@formatter:off
		/*list.add( new AproposLabel( "{ACTIVE} keeps a steady rhythm as it is obviously enjoying my {MOUTH}.",null));
		list.add( new AproposLabel( "I concentrate on keeping my breathing steady and let {ACTIVE} have his fun with my {MOUTH}.",null));
		list.add( new AproposLabel( "As its {BEAST} {COCK} pushes deeper into my {MOUTH}, I can't ignore the intense taste of it's precum.",null));
		list.add( new AproposLabel( "{SALTY} precum coats my tongue and throat... I try not to focus on it and work my tongue to help get him off faster.",null));
		list.add( new AproposLabel( "The Skeever's thrusts keep getting stronger and deeper, I can't imagine how good my {MOUTH} must feel to him.",null));
		list.add( new AproposLabel( "It's {COCK} seems to swell further with every extra inch inserted, I think he has been keeping a large load in him.",null));
		list.add( new AproposLabel( "With the way {ACTIVE} is {FUCKING} my mouth, I think it won't be letting me go for a while.",null));
		list.add( new AproposLabel( "{ACTIVE} finally gives me more of its {BEAST} {COCK}, now I can properly suck it!",null));
		list.add( new AproposLabel( "Is it already {CUMMING}? It's so {SALTY}... this can't just be precum...",null));
		list.add( new AproposLabel( "I'd love to {FUCK} this Skeever, but if my mouth is what he want's, I'll be sure to give it to him!",null));
		list.add( new AproposLabel( "I can't believe this is happening! I can't believe it. I'll fuck them all? I could... I will!",null));
		list.add( new AproposLabel( "Before I knew it, {ACTIVE} was on top of me, prodding to find a way into my {MOUTH} with its {BEAST} {COCK}.",null));
		list.add( new AproposLabel( "Come on you stinking rat, coat my throat in your {VILE} {CUM}!",null));
		list.add( new AproposLabel( "A rat is {FUCKING} my {MOUTH} as deep and fast as it can and I'm just waiting for my stomach to be filled with {VILE} {CUM}.!",null));*/
		list.add( new AproposLabel( "My my, two horny skeevers... my {PUSSY} is too {GENWT} to let them fuck it, but maybe... ooooh, this might just work...",null));
		list.add( new AproposLabel( "Oh my! He's certainly enthusiastic, even with his packmate's dick so close. I moan around the slowly thrusting {COCK} in my mouth...",null));
		list.add( new AproposLabel( "Oh my Gods! Dogs are so good at this...",null));
		//@formatter:on
		AproposLabel current = new AproposLabel( "1st Person", null );
		AproposLabel target = new AproposLabel( "2st Person", null );
		
		System.out.println( " ---- BEFORE ---- " );
		System.out.println( "\t\t\t\t\t" + list );
		System.out.println( " ---- AFTER ---- " );
		LabelList shift = Model.perspectiveShift( list, current, target );
		System.out.println( "\t\t\t\t\t" + shift );
		System.out.println( " ---- AGAIN ---- " );
		System.out.println( "\t\t\t\t\t" + Model.perspectiveShift( shift, target, current ) );
	}
	
	public static void matching() {
		String[] results = new String[] { "Perfect", "Punctuation", "Close", "", "", "", "", "Word Content", "Word Count", "Char Length" };
		String[] one = new String[ 10 ];
		String[] two = new String[ 10 ];
		one[0] = "Oh, my Gods! Oh, he's {CUMMING} in my {ASS}! {SWEARING}, this is so intense...";
		two[0] = "Oh, my Gods! Oh, he's squirting in my {ASS}! Oh shit, this is so intense...";
		one[1] = "My toes curl painfully and my whole body convulses as I pleasure myself to orgasm.";
		two[1] = "My toes curl painfully and my whole body convulses as I pleasure myself to orgasm.";
		one[2] = "My fingers piston deeply into my {FAROUSAL} {PUSSY}, as I finger fuck myself to climax.";
		two[2] = "I draw in a sharp breath and hold it. My pelvis spasms and thrusts against my fingers.";
		one[3] = "To anyone paying attention, it's obvious that the naked girl writhing on the floor is cumming, hard.";
		two[3] = "My body shakes violently, driven to one thunderous climax after another.";
		one[4] = "I feel my breath catch in my chest, and my heart pounds so hard I think I'm going to die.";
		two[4] = "My vision fails me, and everything burns white as my exhausted body shakes in climax.";
		one[5] = "My orgasm erupts violently, my clit painfully sensitive as my fingers continue their assault.";
		two[5] = "My eyes roll back into my head, my toes curl and my free hand clenches painfully as I cum.";
		one[6] = "The whole world goes silent, just before I hear my own cries rip out of my throat.";
		two[6] = "({ACTIVE}) Shit {BITCH}, you sure are wet! I guess you want this!";
		one[7] = "They eye my nude form hungrily. I'm shamefully wet by this point, and my knees are ready to buckle.";
		two[7] = "They eye your nude form hungrily. You're shamefully wet by this point, and your knees are ready to buckle.";
		one[8] = "I... I'm having sex with a rat... And-Ahhh... Enjoying it...";
		two[8] = "I... I'm letting a rat fuck my ass... And-Ahhh... Enjoying it...";
		one[9] = "The {ACTIVE} picks up the pace, and I can already feel his knot pounding against my {ASS} with each thrust";
		two[9] = "The {ACTIVE} picks up the pace, and I can already feel his knot slapping into my lips with each thrust";
		for ( int i = 0; i < one.length; i++ ) {
			System.out.println(
					"===============================================================================================================================" );
			System.out.println( "Pair " + i + ":" );
			System.out.printf( "\t%s\n\t%s\n", one[i], two[i] );
			System.out.println( "Result: " + results[Model.fuzzyMatches( one[i], two[i] )] );
		}
	}
	
	public static void labelFromFile() {
		String s = "/Users/Kealan/Dumps/Workspace/Apropos Diffing/dbOfficial/FemaleActor_Wolf/FemaleActor_Wolf_Vaginal_Rape_Orgasm.txt";
		System.out.println( s );
		AproposLabel test = Model.stageLabelFromFile( new File( s ) );
		System.out.println( test );
		System.out.println( test.getDepth() );
		System.out.println( test.getParentLabel() );
	}
	
	public static void labelFromPath() {
		String s = "E:\\User Files\\Dumps\\Workspace\\Apropos Diffing\\dbOfficial\\FemaleActor_aMSleeping\\FemaleActor_aMSleeping_Rape\\Stage 1\\2nd Person";
		System.out.println( s );
		AproposLabel test = new AproposLabel( s );
		System.out.println( test );
		System.out.println( test.getDepth() );
	}
	
	public static void versionComp() {
		String current = "1.2.5", release = "1.3.5a4";
		Pattern p = Pattern.compile( "([0-9.]+)([ab][0-9]*)?" );
		String[] cparts = matchFirstGroups( current, p ), rparts = matchFirstGroups( release, p );
		int ret;
		int c1 = cparts[0].compareTo( rparts[0] );
		if ( c1 == 0 ) {
			if ( cparts[1] == null & rparts[1] == null )
				ret = 0;
			else if ( rparts[1] == null )
				ret = -1;
			else if ( cparts[1] == null )
				ret = 1;
			else
				ret = cparts[1].compareTo( rparts[1] );
		}
		else
			ret = c1;
		System.out.println( ret );
	}
	
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
	
	public static void isJson() throws Exception {
		String json = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents( null )
				.getTransferData( DataFlavor.stringFlavor );
		json = json.replaceFirst( "^\\{([\\s\\S]*)\\}$", "$1" ).trim();
		System.out.println( json );
		System.out.println( json.trim().matches( "^\\{[\\s\\S]*\\}$" ) );
	}
	
	public static SynonymsMap synonymsMap() throws Exception {
		Model m = new Model( new ViewStub() );
		Globals globals = new Globals( new File( "apropos-edit.config" ) );
		globals.read();
		String db = globals.getProperty( "locations" ).split( globals.delimiter )[0];
		m.setDataBase( db );
		Thread.sleep( 1000 );
		return m.synonyms;
	}
	
	public static void searchTest() throws Exception {
		Model m = new Model( new ViewStub() );
		Globals globals = new Globals( new File( "apropos-edit.config" ) );
		globals.read();
		String db = globals.getProperty( "locations" ).split( globals.delimiter )[0];
		m.setDataBase( db );
		Thread.sleep( 2000 );
		
		SearchTerms terms = new UserSearchTerms() {
			private static final long serialVersionUID = -1326037863059987072L;
			
			public boolean matchesStage( AproposLabel stagelabel ) {
				return true;
			}
			
			public boolean matchesDirectory( String dirname ) {
				return true;
			}
			
			public boolean matches( String text ) {
				return text.contains( "knot" );
			}
		};
		
		DatabaseSearch search = m.new DatabaseSearch( terms, null );
		search.execute();
	}
	
	public static void fileSorting() throws Exception {
		String[] files = new String[] { "FemaleActor_Dragon_Vaginal_Rape_Orgasm.txt", "FemaleActor_Dragon_Vaginal_Rape.txt",
				"FemaleActor_Dragon_Vaginal_Rape_Stage2.txt" };
		for ( String file : files ) {
			file = file.replaceAll( "\\.txt|_Stage\\d{1,2}|_Orgasm", "" );
			System.out.println( file );
		}
	}
	
	private static void synonymDetecting() throws Exception {
		SynonymsMap synonyms = synonymsMap();
		String[] lines = new String[ 7 ];
		lines[0] = "{PRIMARY} cries out in shock as {ACTIVE} jams its {BEAST} {COCK} inside her {WTVAGINAL} {PUSSY}.";
		lines[1] = "{PRIMARY cries out in shock as {ACTIVE2} jams its {BEAST } COCK inside her {WTHAND} {PUSSY}";
		lines[2] = "{ACTIVE}'s {THICK} {CUM} runs from {PRIMARY}'s {ASS} down and into her {WTVAGINAL} {PUSSY}...";
		lines[3] = "ACTIVE's THICK CUM runs from PRIMARY's ASS down and into her WTVAGINAL PUSSY...";
		lines[4] = "{PRIMARY} and {ACTIVE} both look so beautiful, their {BOOBS} bouncing as {ACTIVE}'s {SO_ADJ} {STRAPON} reams {PRIMARY}'s {ASS}.";
		lines[5] = "{PRIMARY and ACTIVE} both look so BEAUTIFUL, their {BOOB} bouncing as {ACTIVE's} {SO_ADJ} {StRAPON} reams {PRIMARY}'s {ASS}.";
		lines[6] = "({PRIMARY) Ooohhh yes! I want your {CUM}, I want it inside me... Hurry and cum inside me!!!";

		for ( String line : lines ) {
			BrokenSynonymsFinder bsf = new Model.BrokenSynonymsFinder( synonyms );
			String replacement = bsf.fixSynonyms( line );
			System.out.println( "Original lineeeee: " + line );
			System.out.println( "Final Replacement: " + replacement );
		}
		
	}
	
}

class ViewStub extends View {
	private static final long serialVersionUID = 4108044065693221460L;
	
	public ViewStub() {}
	
	public void handleException( Throwable e ) {
		e.printStackTrace();
	}
	
	public void setProgress( String working, String complete, int percent ) {
		System.err.println( "Starting task " + working );
	}
	
	public void updateProgress( int percent ) {
		System.err.println( percent + "% Complete" );
	}
	
}
