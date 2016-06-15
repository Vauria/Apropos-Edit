package com.loverslab.apropos.edit;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.loverslab.apropos.edit.Prototype.Stage;

@SuppressWarnings("serial")
class Display extends JPanel {
		
		JPanel panel;
		JMenuBar menu;
		JMenu file;
		GridBagLayout gbl;
		TreeMap<JLabel, TreeMap<JLabel, ArrayList<EditableJLabel>>> data = new TreeMap<JLabel, TreeMap<JLabel, ArrayList<EditableJLabel>>>(
				new Prototype().new OrderL() );
		
		public Display() {
			gbl = new GridBagLayout() {
				public GridBagConstraints getConstraints( Component comp ) {
					return lookupConstraints( comp );
				}
			};
			setLayout( gbl );
			
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.FIRST_LINE_START;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0d;
			
			JLabel jl = null;
			EditableJLabel ejl = null;
			
			TreeMap<String,File> files = new TreeMap<String,File>();
			files.put( "Intro", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation.txt") );
			files.put( "Stage 1", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation_Stage1.txt") );
			files.put( "Stage 2", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation_Stage2.txt") );
			files.put( "Stage 3", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation_Stage3.txt") );
			files.put( "Stage 4", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation_Stage4.txt") );
			files.put( "Orgasm", new File("C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\FemaleActor\\FemaleActor_Masturbation_Orgasm.txt") );
			
			for ( String key : files.keySet() ) {
				c.insets = new Insets( 25, 10, 5, 10 );
				c.gridy++ ;
				c.gridx = 0;
				c.gridwidth = 1;
				c.gridheight = 1;
				jl = new JLabel( key );
				add( jl, c );
				TreeMap<JLabel, ArrayList<EditableJLabel>> scene = new TreeMap<JLabel, ArrayList<EditableJLabel>>(
						new Prototype().new OrderL() );
				data.put( jl, scene );
				System.out.println( key );
				try ( Reader reader = new InputStreamReader( new FileInputStream( files.get( key ) ) ) ) {
					Gson g = new Gson();
					Stage s = g.fromJson( reader, Stage.class ).build();
					if ( s != null ) for ( String per : s.strings.keySet() ) {
						String[] strings = s.strings.get( per );
						c.insets = new Insets( 0, 40, 0, 5 );
						c.gridy++ ;
						jl = new JLabel( per );
						add( jl, c );
						c.insets = new Insets( 0, 70, 0, 5 );
						ArrayList<EditableJLabel> perspec = new ArrayList<EditableJLabel>();
						scene.put( jl, perspec );
						for ( String str : strings ) {
							c.gridy++ ;
							ejl = new EditableJLabel( str );
							add( ejl, c );
							perspec.add( ejl );
						}
						if ( strings.length > 1 | !strings[0].equals( "" ) ) {
							c.gridy++ ;
							add( new EditableJLabel( "" ), c );
						}
					}
				}
				catch ( JsonSyntaxException e ) {
					System.err.println( "Error parsing " + files.get( key ).getAbsolutePath().replace( "C:\\Program Files (x86)\\Steam\\SteamApps\\common\\Skyrim\\Mod Organizer\\mods\\Apropos Beta 2015 04 24 01\\Apropos\\dbOfficial\\", "\\db\\" ) );
					System.err.println( e.getMessage() );
				}
				catch ( IOException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			revalidate();
			
		}
		
		public void insert( JComponent add, JComponent above ) {
			int sceneY = -1, persY = -1;
			Component sceneMarker = null, persMarker = null;
			GridBagConstraints point = gbl.getConstraints( above );
			for ( Component comp : getComponents() ) {
				GridBagConstraints c = gbl.getConstraints( comp );
				if ( c.gridy > point.gridy ) {
					c.gridy++ ;
					comp.invalidate();
				}
				else {
					/*
					 * System.out.println(
					 * "In: " + c.insets.left + ", Y: " + c.gridy + ", : " + ( ( comp instanceof EditableJLabel )
					 * ? ( (EditableJLabel) comp ).getText() : ( (JLabel) comp ).getText() ) );
					 */
					if ( c.insets.left == 10 & c.gridy > sceneY ) {
						sceneY = c.gridy;
						sceneMarker = comp;
					}
					if ( c.insets.left == 40 & c.gridy > persY ) {
						persY = c.gridy;
						persMarker = comp;
					}
				}
			}
			GridBagConstraints a = (GridBagConstraints) point.clone();
			a.gridy++ ;
			add( add, a );
			JLabel scene = ( (JLabel) sceneMarker );
			JLabel pers = ( (JLabel) persMarker );
			EditableJLabel old = (EditableJLabel) above;
			System.out.println( "Adding \"" + old.getText() + "\" to " + scene.getText() + ": " + pers.getText() );
			data.get( scene ).get( pers ).add( old );
			
			for ( JLabel k1 : data.keySet() ) {
				System.out.println( k1.getText() + ": " + data.get( k1 ).size() );
				for ( JLabel k2 : data.get( k1 ).keySet() ) {
					System.out.println( "\t" + k2.getText() + ": " + data.get( k1 ).get( k2 ).size() );
					for ( EditableJLabel s : data.get( k1 ).get( k2 ) )
						System.out.println( "\t\t" + s.getText() );
				}
			}
		}
		
		class EditableJLabel extends JPanel {
			
			private JLabel label;
			private JTextField textField;
			private LinkedList<ValueChangedListener> listeners = new LinkedList<ValueChangedListener>();
			
			public EditableJLabel( String startText ) {
				super();
				
				// Create the listener and the layout
				CardLayout layout = new CardLayout( 0, 0 );
				this.setLayout( layout );
				EditableListener hl = new EditableListener();
				
				// Create the JPanel for the "normal" state
				JPanel labelPanel = new JPanel( new GridLayout( 1, 1 ) );
				label = new JLabel( startText.equals( "" ) ? "<add new>" : startText );
				labelPanel.add( label );
				
				// Create the JPanel for the "hover state"
				JPanel inputPanel = new JPanel( new GridLayout( 1, 1 ) );
				textField = new JTextField( startText );
				textField.addMouseListener( hl );
				textField.addKeyListener( hl );
				textField.addFocusListener( hl );
				inputPanel.add( textField );
				
				this.addMouseListener( hl );
				
				// Set the states
				this.add( labelPanel, "NORMAL" );
				this.add( inputPanel, "HOVER" );
				
				// Show the correct panel to begin with
				layout.show( this, "NORMAL" );
			}
			
			public void setText( String text ) {
				System.out.println( text );
				if ( getText().equals( "" ) ) {
					if ( !text.equals( "" ) ) {
						this.label.setText( text );
						EditableJLabel add = new EditableJLabel( "" );
						add.setHoverState( true );
						insert( add, this );
						add.textField.grabFocus();
					}
				}
				else if ( text.equals( "" ) )
					this.label.setText( "<add new>" );
				else
					this.label.setText( text );
				this.textField.setText( text );
			}
			
			public String getText() {
				String text = this.label.getText();
				return text.equals( "<add new>" ) ? "" : text;
			}
			
			public JTextField getTextField() {
				return textField;
			}
			
			public JLabel getLabel() {
				return label;
			}
			
			public void setHoverState( boolean hover ) {
				CardLayout cl = (CardLayout) ( this.getLayout() );
				
				if ( hover )
					cl.show( this, "HOVER" );
				else
					cl.show( this, "NORMAL" );
			}
			
			public void addValueChangedListener( ValueChangedListener l ) {
				this.listeners.add( l );
			}
			
			public int positionFromPoint( int x, String s ) {
				int w = label.getGraphics().getFontMetrics().stringWidth( s );
				float pos = ( (float) x / (float) w ) * (float) s.length();
				return Math.min( (int) pos, s.length() );
			}
			
			public class EditableListener implements MouseListener, KeyListener, FocusListener {
				boolean locked = false;
				String oldValue;
				
				public void focusGained( FocusEvent arg0 ) {
					oldValue = textField.getText();
				}
				public void mouseClicked( MouseEvent e ) {
					if ( e.getClickCount() == 2 ) {
						setHoverState( true );
						textField.grabFocus();
						textField.setCaretPosition( positionFromPoint( e.getX(), textField.getText() ) );
					}
				}
				public void release() {
					this.locked = false;
				}
				public void focusLost( FocusEvent e ) {
					if ( !locked ) setText( oldValue );
					setHoverState( false );
					release();
					mouseExited( null );
				}
				public void keyTyped( KeyEvent e ) {
					if ( e.getKeyChar() == KeyEvent.VK_ENTER ) {
						setText( textField.getText() );
						for ( ValueChangedListener v : listeners ) {
							v.valueChanged( textField.getText(), EditableJLabel.this );
						}
						setHoverState( false );
						locked = true;
						mouseExited( null );
					}
					else if ( e.getKeyChar() == KeyEvent.VK_ESCAPE ) {
						setHoverState( false );
						release();
						setText( oldValue );
					}
				}
				public void mousePressed( MouseEvent e ) {}
				public void mouseReleased( MouseEvent e ) {}
				public void keyPressed( KeyEvent e ) {}
				public void keyReleased( KeyEvent e ) {}
				public void mouseEntered( MouseEvent e ) {}
				public void mouseExited( MouseEvent e ) {}
			}
			
		}
		
	}