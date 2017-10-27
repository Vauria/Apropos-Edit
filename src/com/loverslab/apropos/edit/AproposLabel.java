package com.loverslab.apropos.edit;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Collection;
import java.util.Deque;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import com.loverslab.apropos.edit.SynonymsLengthMap.MinMax;

/**
 * A JLabel disguised as JPanel designed to be initialised without fully creating all the sub-components and then allow
 * editing.
 * 
 * Original Author James McMinn
 * github.com/JamesMcMinn/EditableJLabel
 */
@SuppressWarnings("serial")
public class AproposLabel extends JPanel implements Comparable<AproposLabel> {
	
	String string, simulateString;
	AproposLabel parent;
	JLabel label, simuLabel;
	JTextArea textField;
	SynonymsLengthMap synonymsLength;
	private boolean displayed, hoverState, simulateState, highlighted, matched = false;
	
	/**
	 * Creates a non-displayable AproposLabel with the given parent and given text
	 * 
	 * @param startText
	 * @param parent
	 */
	public AproposLabel( String startText, AproposLabel parent ) {
		super();
		string = startText;
		this.parent = parent;
	}
	
	/**
	 * Creates an AproposLabel using a given virtual path, such that
	 * <code>Label.equals(new AproposLabel(Label.toString()))</code>
	 * <br>
	 * (or at least it would if <code>equals(Object o)</code> was implemented)
	 * 
	 * @param path
	 */
	public AproposLabel( String path ) {
		super();
		String s = File.separator;
		path = path.replaceAll( s + s + "$", "" );
		// Check if valid database to terminate recursion
		File f = new File( path + s + "Themes.txt" ); // TODO: Have a more reliable way to determine if this is a DB folder
		if ( f.exists() ) {
			string = path;
			this.parent = null;
			return;
		}
		int ind = 0, i = -1;
		while ( ( i = path.indexOf( s, ind + 1 ) ) != -1 )
			ind = i;
		string = path.substring( ind + 1 );
		String rest = path.substring( 0, ind );
		this.parent = new AproposLabel( rest );
		
	}
	
	AproposLabel( AproposLabel copy ) {
		this( copy.getText(), copy.getParentLabel() );
	}
	
	/**
	 * Creates and arranged the components required for this Label to function.
	 * 
	 * @return This AproposLabel
	 */
	public AproposLabel display( LineChangedListener lcL, PopupMenuListener pmL, SynonymsLengthMap synonymsLength ) {
		if ( displayed ) return this;
		
		this.synonymsLength = synonymsLength;
		
		// Create the listener and the layout
		CardLayout layout = new CardLayout( 0, 0 );
		this.setLayout( layout );
		EditingListener hl = new EditingListener();
		
		// Create the JPanel for the "normal" state
		JPanel labelPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints cl = new GridBagConstraints( 0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets( 1, 0, 2, 0 ), 0, 0 );
		label = new JLabel( string.equals( "" ) ? "<add new>" : toHTML( string ) );
		labelPanel.add( label, cl );
		
		updateBorder();
		
		// Create the JPanel for the "hover state"
		JPanel inputPanel = new JPanel( new GridLayout( 1, 1 ) );
		textField = new JTextArea( string );
		textField.setLineWrap( true );
		textField.setWrapStyleWord( true );
		textField.addMouseListener( hl );
		textField.addKeyListener( hl );
		textField.addFocusListener( hl );
		Document doc = textField.getDocument();
		// Adding a document filter that just eats all newline characters, so pressing enter just saves the line
		( (AbstractDocument) doc ).setDocumentFilter( new DocumentFilter() {
			public void insertString( FilterBypass fb, int offset, String string, AttributeSet attr ) throws BadLocationException {
				fb.insertString( offset, string.replaceAll( "\n", "" ), attr );
			}
			public void replace( FilterBypass fb, int offset, int length, String text, AttributeSet attrs ) throws BadLocationException {
				if ( text == "\n" ) return;
				fb.replace( offset, length, text.replaceAll( "\n", "" ), attrs );
			}
		} );
		doc.addDocumentListener( hl );
		inputPanel.add( textField );
		
		JPanel simulatePanel = new JPanel( new GridBagLayout() );
		simulateString = "<pending simulation>";
		simuLabel = new JLabel( simulateString );
		simulatePanel.add( simuLabel, cl );
		
		this.addMouseListener( hl );
		this.addLineChangedListener( lcL );
		this.addPopupMenuListener( pmL );
		
		// Set the states
		this.add( labelPanel, "NORMAL" );
		this.add( inputPanel, "HOVER" );
		this.add( simulatePanel, "SIMULATE" );
		
		// Show the correct panel to begin with
		layout.show( this, "NORMAL" );
		revalidate();
		displayed = true;
		
		return this;
	}
	
	/**
	 * Creates a more lightweight AproposLabel that is not editable
	 * 
	 * @param gbl
	 * @param editable Ignored
	 * @return This AproposLabel
	 */
	public AproposLabel display( LineChangedListener lcL, PopupMenuListener pmL, boolean editable ) {
		if ( displayed ) return this;
		
		setLayout( new GridLayout( 1, 1 ) );
		AproposListener al = new AproposListener();
		
		label = new JLabel( string.equals( "" ) ? "<add new>" : toHTML( string ) );
		add( label );
		
		this.addMouseListener( al );
		this.addLineChangedListener( lcL );
		this.addPopupMenuListener( pmL );
		
		revalidate();
		displayed = true;
		
		return this;
	}
	
	public static String toHTML( String text ) {
		String html = "<html>" + text;
		return html;
	}
	
	public static String fromHTML( String html ) {
		String text = html.substring( 6 );
		return text;
	}
	
	/**
	 * Sets the String this label holds and displays.
	 * 
	 * @param text
	 */
	public void setText( String text ) {
		if ( this.label == null & this.textField == null ) {
			this.string = text;
			return;
		}
		if ( this.textField == null ) {
			this.string = text;
			this.label.setText( toHTML( text ) );
			return;
		}
		if ( getText().equals( "" ) ) {
			if ( !text.equals( "" ) ) {
				this.label.setText( toHTML( text ) );
				this.string = text;
				fireLineInserted( this );
			}
		}
		else if ( text.equals( "" ) )
			this.label.setText( "<add new>" );
		else
			this.label.setText( toHTML( text ) );
		this.textField.setText( text );
		this.string = text;
	}
	
	public String getText() {
		return string;
	}
	
	public String toString() {
		return ( parent != null ? parent.toString() + ( parent.toString().endsWith( Model.fs ) ? "" : Model.fs ) : "" ) + getText();
	}
	
	/**
	 * Returns a new label with equal reference to this label's parent and copied text
	 */
	public AproposLabel clone() {
		AproposLabel clone = new AproposLabel( getText(), getParentLabel() );
		clone.setMatch( isMatch() );
		return clone;
	}
	
	public AproposLabel getParentLabel() {
		return parent;
	}
	
	public AproposLabel getParentLabel( int depth ) {
		int d = getDepth();
		if ( depth > d ) return null;
		AproposLabel label = getParentLabel();
		d = label.getDepth();
		while ( label.getDepth() > depth )
			label = label.getParentLabel();
		d = label.getDepth();
		return label;
	}
	
	public JTextArea getTextField() {
		return textField;
	}
	
	public JLabel getLabel() {
		return label;
	}
	
	/**
	 * @param hover Shows textField on true, JLabel on false
	 */
	public void setHoverState( boolean hover ) {
		CardLayout cl = (CardLayout) ( this.getLayout() );
		
		hoverState = hover;
		if ( hover )
			cl.show( this, "HOVER" );
		else if ( simulateState )
			cl.show( this, "SIMULATE" );
		else
			cl.show( this, "NORMAL" );
		
		fireStateChanged( hover );
	}
	
	public boolean getHoverState() {
		return hoverState;
	}
	
	/**
	 * @param simulate Shows simulated text on true, normal Label on false
	 */
	public void setSimulateState( boolean simulate ) {
		if ( !displayed ) return;
		
		CardLayout cl = (CardLayout) ( this.getLayout() );
		
		simulateState = simulate;
		if ( simulate )
			cl.show( this, "SIMULATE" );
		else
			cl.show( this, "NORMAL" );
	}
	
	public boolean getSimulateState() {
		return simulateState;
	}
	
	public boolean isHighlighted() {
		return highlighted;
	}
	
	public void setHighlighted( boolean highlighted ) {
		this.highlighted = highlighted;
	}
	
	public void highlight( boolean b ) {
		simuLabel.setForeground( b ? Color.RED : Color.BLACK );
	}
	
	public void mark( boolean b ) {
		label.setForeground( b ? Color.GREEN.darker().darker() : Color.RED.darker() );
	}
	
	public void deMark() {
		label.setForeground( Color.BLACK );
	}
	
	public void setMatch( boolean b ) {
		matched = b;
	}
	
	public boolean isMatch() {
		return matched;
	}
	
	public void setSimulateString( String string ) {
		simulateString = toHTML( string );
	}
	
	public void simulate() {
		simuLabel.setText( simulateString );
		if ( highlighted ) highlight( true );
		setSimulateState( true );
	}
	
	public void deSimulate() {
		if ( highlighted ) {
			highlight( false );
			highlighted = false;
		}
		setSimulateState( false );
	}
	
	/**
	 * Allows enabling and disabling this panel, the textfield and the label.
	 */
	public void setEnabled( boolean enabled ) {
		super.setEnabled( enabled );
		textField.setEnabled( enabled );
		label.setEnabled( enabled );
		simuLabel.setEnabled( enabled );
	}
	
	/**
	 * Register a listener to be notified whenever the text value of this Label changes
	 */
	public void addValueChangedListener( ValueChangedListener listener ) {
		listenerList.add( ValueChangedListener.class, listener );
	}
	
	public void removeValueChangedListener( ValueChangedListener listener ) {
		listenerList.remove( ValueChangedListener.class, listener );
	}
	
	/**
	 * Notifies all listeners of the text this label holds being changed, passing the new text.
	 */
	void fireValueChanged( String value ) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == ValueChangedListener.class ) {
				( (ValueChangedListener) listeners[i + 1] ).valueChanged( value, this );
			}
		}
	}
	
	/**
	 * Register a listener to be notified whenever this label spawns a new line or needs to be removed.
	 */
	public void addLineChangedListener( LineChangedListener listener ) {
		listenerList.add( LineChangedListener.class, listener );
	}
	
	public void removeLineChangedListener( LineChangedListener listener ) {
		listenerList.remove( LineChangedListener.class, listener );
	}
	
	/**
	 * Notifies all listeners that a new, blank label is to be displayed below this one
	 */
	void fireLineInserted( AproposLabel above ) {
		fireLineInserted( above, new AproposLabel( "", above.getParentLabel() ) );
	}
	/**
	 * Notifies all listeners that a new label is to be displayed below this one
	 */
	void fireLineInserted( AproposLabel above, AproposLabel toAdd ) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == LineChangedListener.class ) {
				( (LineChangedListener) listeners[i + 1] ).lineInserted( above, toAdd );
			}
		}
	}
	
	/**
	 * Notifies all listeners that this Label is to be removed.
	 */
	void fireLineRemoved( AproposLabel removed ) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == LineChangedListener.class ) {
				( (LineChangedListener) listeners[i + 1] ).lineRemoved( removed );
			}
		}
	}
	
	/**
	 * Notifies all listeners that this Label needs to have it's simulate string redone.
	 */
	void fireLineUpdated( AproposLabel label ) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == LineChangedListener.class ) {
				( (LineChangedListener) listeners[i + 1] ).lineUpdated( label );
			}
		}
	}
	
	/**
	 * Register a listener to be notified whenever this label needs to display a PopupMenu
	 */
	public void addPopupMenuListener( PopupMenuListener listener ) {
		listenerList.add( PopupMenuListener.class, listener );
	}
	
	public void removePopupMenuListener( PopupMenuListener listener ) {
		listenerList.remove( PopupMenuListener.class, listener );
	}
	
	/**
	 * Notifies all listeners that this Label needs to display a PopupMenu
	 */
	void firepopupMenuTriggered( AproposLabel label, MouseEvent e ) {
		if ( label.getText().equals( "" ) ) return;
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == PopupMenuListener.class ) {
				( (PopupMenuListener) listeners[i + 1] ).popupMenuTriggered( label, e );
			}
		}
	}
	
	public void addInteractionListener( InteractionListener listener ) {
		listenerList.add( InteractionListener.class, listener );
	}
	
	public void removeInteractionListener( InteractionListener listener ) {
		listenerList.remove( InteractionListener.class, listener );
	}
	
	void fireStateChanged( boolean editing ) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == InteractionListener.class ) {
				( (InteractionListener) listeners[i + 1] ).stateChanged( editing, this );
			}
		}
	}
	
	void fireClicked() {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == InteractionListener.class ) {
				( (InteractionListener) listeners[i + 1] ).clicked( this );
			}
		}
	}
	
	public Color getWarningColor( String text ) {
		int min = 0, max = 0, ub = synonymsLength.max;
		for ( String key : synonymsLength.keySet() ) {
			int l = text.length();
			text = text.replace( key, "" );
			int diff = ( l - text.length() ) / key.length();
			if ( diff > 0 ) {
				MinMax mm = synonymsLength.get( key );
				min += mm.min * diff;
				max += mm.max * diff;
			}
		}
		int remaining = label.getFontMetrics( label.getFont() ).stringWidth( text );
		min += remaining;
		max += remaining;
		if ( max < ub ) return null;
		if ( min > ub ) return Color.RED;
		float per = ( 1f + (float) ( ( max - ub ) - ( ub - min ) ) / (float) ( max - min ) ) / 2f;
		Color c = new Color( 1f, 1 - per, 0f );
		return c;
	}
	
	public void updateBorder( String str ) {
		Color c = getWarningColor( str );
		if ( c == null )
			setBorder( BorderFactory.createEmptyBorder() );
		else
			setBorder( BorderFactory.createMatteBorder( 0, 1, 1, 5, c ) );
		repaint();
	}
	
	public void updateBorder() {
		updateBorder( getText() );
	}
	
	/**
	 * Gets the number of characters into the given string is based on a relative point co-ordinate
	 * 
	 * @deprecated this is why we google the problem first.
	 */
	public int positionFromPoint( Point p, String s, boolean deprecated ) {
		int x = p.x, y = p.y;
		FontMetrics metrics = label.getGraphics().getFontMetrics();
		int w = metrics.stringWidth( s );
		int h = metrics.getMaxAscent() + metrics.getMaxDescent();
		int size = label.getSize().width;
		float line = (float) y / (float) h;
		int xline = ( (int) Math.floor( line ) ) * size + x;
		float pos = ( (float) xline / (float) w ) * (float) s.length();
		return Math.min( Math.round( pos ), s.length() );
	}
	
	/**
	 * Gets the number of characters into the given string a given point is
	 */
	public int positionFromPoint( Point p, String s ) {
		return s.equals( "" ) ? 0 : Math.max( label.getAccessibleContext().getAccessibleText().getIndexAtPoint( p ) - 1, 0 );
	}
	
	/**
	 * A listener for non-editable AproposLabels, only provides PopupMenu actions
	 */
	public class AproposListener implements MouseListener {
		
		public void mousePressed( MouseEvent e ) {
			if ( e.isPopupTrigger() & !getHoverState() ) firepopupMenuTriggered( AproposLabel.this, e );
		}
		public void mouseReleased( MouseEvent e ) {
			if ( e.isPopupTrigger() & !getHoverState() ) firepopupMenuTriggered( AproposLabel.this, e );
		}
		public void mouseClicked( MouseEvent e ) {
			if ( !e.isPopupTrigger() & !getHoverState() ) fireClicked();
			
		}
		
		public void mouseEntered( MouseEvent e ) {}
		public void mouseExited( MouseEvent e ) {}
	}
	
	/**
	 * Listener for full editing support
	 */
	public class EditingListener extends AproposListener implements KeyListener, FocusListener, DocumentListener {
		boolean locked = false;
		String oldValue;
		
		public void focusGained( FocusEvent arg0 ) {
			oldValue = textField.getText();
		}
		public void mouseClicked( MouseEvent e ) {
			if ( !e.isPopupTrigger() & !getHoverState() ) if ( e.getClickCount() == 2 ) {
				setHoverState( true );
				textField.grabFocus();
				textField.setCaretPosition( positionFromPoint( e.getPoint(), textField.getText() ) );
			}
			else
				fireClicked();
		}
		public void release() {
			this.locked = false;
		}
		public void focusLost( FocusEvent e ) {
			if ( e.getOppositeComponent() != null ) {
				Component opp = e.getOppositeComponent();
				if ( SwingUtilities.getRoot( opp ) == SwingUtilities.getRoot( AproposLabel.this ) ) {
					if ( !locked ) setText( oldValue );
					setHoverState( false );
					release();
				}
			}
		}
		public void keyTyped( KeyEvent e ) {
			if ( e.getKeyChar() == KeyEvent.VK_ENTER ) {
				String text = textField.getText();
				setText( text );
				fireValueChanged( text );
				if ( getSimulateState() ) fireLineUpdated( AproposLabel.this );
				setHoverState( false );
				locked = true;
			}
			else if ( e.getKeyChar() == KeyEvent.VK_ESCAPE ) {
				setHoverState( false );
				release();
				setText( oldValue );
			}
		}
		
		public void changed( DocumentEvent e ) {
			updateBorder( textField.getText() );
		}
		
		public void insertUpdate( DocumentEvent e ) {
			changed( e );
		}
		public void removeUpdate( DocumentEvent e ) {
			changed( e );
		}
		public void changedUpdate( DocumentEvent e ) {}
		public void keyPressed( KeyEvent e ) {}
		public void keyReleased( KeyEvent e ) {}
	}
	
	/**
	 * 0: Database<br>
	 * 1: Folder<br>
	 * 2: Position<br>
	 * 3: Stage<br>
	 * 4: Perspective<br>
	 * 5: Line<br>
	 * 
	 * @return The Number of Parents
	 */
	public int getDepth() {
		return parent == null ? 0 : 1 + parent.getDepth();
	}
	
	public int compareTo( AproposLabel o ) {
		if ( o == null ) return 1;
		int ldepth = getDepth(), odepth = o.getDepth();
		String lstr = getText(), ostr = o.getText();
		if ( ldepth == odepth ) {
			switch ( ldepth ) {
				case 0: // The Database
					if ( lstr.equals( ostr ) ) return (int) Math.signum( hashCode() - o.hashCode() );
					return lstr.compareTo( ostr );
				case 1: // The Folder
					if ( lstr.equals( ostr ) ) return (int) Math.signum( hashCode() - o.hashCode() );
					return lstr.compareTo( ostr );
				case 2: // The Position
					if ( lstr.equals( ostr ) ) return (int) Math.signum( hashCode() - o.hashCode() );
					return lstr.compareTo( ostr );
				case 3: // The Animation Stage
					if ( lstr.equals( ostr ) ) return (int) Math.signum( hashCode() - o.hashCode() );
					if ( lstr.equals( "Intro" ) ) return -1;
					if ( ostr.equals( "Intro" ) ) return 1;
					if ( lstr.equals( "Orgasm" ) ) return 1;
					if ( ostr.equals( "Orgasm" ) ) return -1;
					if ( lstr.startsWith( "Stage " ) & ostr.startsWith( "Stage " ) )
						return Integer.valueOf( lstr.replace( "Stage ", "" ) ).compareTo( Integer.valueOf( ostr.replace( "Stage ", "" ) ) );
					else
						return lstr.compareTo( ostr );
				case 4: // The Animation Perspective
					if ( lstr.equals( ostr ) ) return (int) Math.signum( hashCode() - o.hashCode() );
					return lstr.compareTo( ostr );
				default: // The lines themselves? We fucked up
					return 0;
			}
		}
		else
			return (int) Math.signum( odepth - ldepth );
	}
	
}

@SuppressWarnings("serial")
class AproposConflictLabel extends AproposLabel {
	
	private Deque<AproposLabel> matches = new LinkedList<AproposLabel>();
	private Map<AproposLabel, Boolean> keepMap = new HashMap<AproposLabel, Boolean>();
	private boolean displayed = false;
	
	public AproposConflictLabel( String startText, AproposLabel parent ) {
		super( startText, parent );
		matches.add( new AproposLabel( startText, parent ) );
		keepMap.put( matches.getLast(), true );
	}
	
	public AproposConflictLabel( AproposLabel label ) {
		super( label );
		matches.add( label.clone() );
		keepMap.put( matches.getLast(), true );
	}
	
	public void addConflict( AproposLabel match, boolean keep ) {
		matches.add( match.clone() );
		keepMap.put( matches.getLast(), keep );
	}
	
	public void addConflict( AproposLabel match, int weak ) {
		if ( weak < 5 ) {
			addConflict( match, weak > 0 );
		}
	}
	
	public void addConflict( AproposLabel match ) {
		addConflict( match, true );
	}
	
	public void markAll( boolean keep ) {
		for ( AproposLabel key : keepMap.keySet() )
			keepMap.put( key, keep );
	}
	
	public String[] getTexts() {
		String[] ret = new String[ matches.size() ];
		Iterator<AproposLabel> it = matches.iterator();
		int i = 0;
		while ( it.hasNext() ) {
			ret[i] = it.next().getText();
			i++ ;
		}
		return ret;
	}
	
	public AproposLabel display( LineChangedListener lcL, PopupMenuListener pmL, SynonymsLengthMap synonymsLength ) {
		if ( displayed ) return this;
		
		if ( matches.size() == 1 ) {
			setLayout( new GridLayout( 1, 1 ) );
			add( matches.getFirst().display( lcL, pmL, synonymsLength ) );
		}
		else {
			setLayout( new GridBagLayout() );
			setBorder( BorderFactory.createLoweredSoftBevelBorder() );
			
			GridBagConstraints c = new GridBagConstraints() {
				{
					anchor = LINE_START;
					gridy = -1;
					fill = HORIZONTAL;
					insets = new Insets( 0, 5, 0, 5 );
				}
			};
			
			Iterator<AproposLabel> it = matches.iterator();
			while ( it.hasNext() ) {
				AproposLabel l = it.next();
				JCheckBox keepBox = new JCheckBox();
				keepBox.setSelected( keepMap.get( l ) );
				keepBox.addItemListener( new ConflictListener( l ) );
				
				c.gridy++ ;
				c.gridx = 0;
				c.weightx = 0d;
				add( keepBox, c );
				c.gridx++ ;
				c.weightx++ ;
				add( l.display( lcL, pmL, synonymsLength ), c );
				l.mark( keepMap.get( l ) );
			}
		}
		
		revalidate();
		displayed = true;
		return this;
	}
	
	public AproposLabel display( LineChangedListener lcL, PopupMenuListener pmL, boolean editable ) {
		return display( lcL, pmL, editable );
	}
	
	public void setSimulateState( boolean simulate ) {
		return;
	}
	
	public void simulate() {
		return;
	}
	
	public void deSimulate() {
		return;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder( getParentLabel().toString() + " [" );
		Iterator<AproposLabel> it = matches.iterator();
		while ( it.hasNext() ) {
			AproposLabel l = it.next();
			builder.append( "\n\t\t\t\t\t\t" + keepMap.get( l ).toString() + ": " + l.getText() );
		}
		builder.append( "\n\t\t\t\t\t]" );
		return builder.toString();
	}
	
	public Collection<AproposLabel> resolveConficts() {
		LinkedList<AproposLabel> ret = new LinkedList<AproposLabel>();
		Iterator<AproposLabel> it = matches.iterator();
		while ( it.hasNext() ) {
			AproposLabel l = it.next();
			if ( keepMap.get( l ) ) ret.add( l.clone() );
		}
		return ret;
	}
	
	class ConflictListener implements ItemListener {
		private AproposLabel label;
		
		public ConflictListener( AproposLabel label ) {
			this.label = label;
		}
		public void itemStateChanged( ItemEvent e ) {
			boolean selected = e.getStateChange() == ItemEvent.SELECTED;
			keepMap.put( label, selected );
			label.mark( selected );
		}
	}
	
}

/**
 * A listener that will be notified whenever the text of an attached JLabel is modified
 */
interface ValueChangedListener extends EventListener {
	public void valueChanged( String value, AproposLabel source );
}

/**
 * A listener that will be notified whenever the attached JLabel needs to display a new Label below it or be removed
 */
interface LineChangedListener extends EventListener {
	public void lineInserted( AproposLabel above, AproposLabel toAdd );
	public void lineRemoved( AproposLabel removed );
	public void lineUpdated( AproposLabel label );
}

/**
 * A listener that will be notified whenever the attached JLabel needs to display a PopupMenu
 */
interface PopupMenuListener extends EventListener {
	public void popupMenuTriggered( AproposLabel label, MouseEvent e );
}

interface InteractionListener extends EventListener {
	public void stateChanged( boolean editing, JComponent source );
	public void clicked( JComponent source );
}