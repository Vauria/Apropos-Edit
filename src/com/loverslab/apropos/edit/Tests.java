package com.loverslab.apropos.edit;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;

public class Tests {
	
	public static void main( String[] args ) throws Exception {
		isJson();
	}
	
	public static void shifting() {
		LabelList list = new LabelList();
		//@formatter:off
		list.add( new AproposLabel( "{ACTIVE} keeps a steady rhythm as it is obviously enjoying my {MOUTH}.",null));
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
		list.add( new AproposLabel( "A rat is {FUCKING} my {MOUTH} as deep and fast as it can and I'm just waiting for my stomach to be filled with {VILE} {CUM}.!",null));
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
	
	public static void labelFromPath() {
		String s = "E:\\User Files\\Dumps\\Workspace\\Apropos Diffing\\dbOfficial\\FemaleActor_aMSleeping\\FemaleActor_aMSleeping_Rape\\Stage 1\\2nd Person";
		System.out.println( s );
		AproposLabel test = new AproposLabel( s );
		System.out.println( test );
		System.out.println( test.getDepth() );
	}
	
	public static void versionComp() {
		System.out.println( "1.2a2".compareTo( "1.2b" ) );
	}
	
	public static void isJson() throws Exception {
		String json = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents( null )
				.getTransferData( DataFlavor.stringFlavor );
		System.out.println( json );
		System.out.println( json.trim().matches( "^\\{[\\s\\S]*\\}$" ) );
	}
	
}
