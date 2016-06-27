package com.loverslab.apropos.edit;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * A JLabel disguised as JPanel designed to be initialised without fully creating all the sub-components and then allow editing.
 * 
 * Original Author James McMinn
 * github.com/JamesMcMinn/EditableJLabel
 */
@SuppressWarnings("serial")
public class AproposLabel extends JPanel implements Comparable<AproposLabel> {
	
	private String string, simulateString;
	private AproposLabel parent;
	private GridBagLayout layout;
	private GridBagConstraints cons;
	private JLabel label, simuLabel;
	private JTextField textField;
	private boolean displayed, hoverState, simulateState, highlighted;
	
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
	 * Creates and arranged the components required for this Label to function.
	 * 
	 * @param gbl The GridBagLayout used in the container this Label is initialised in.
	 * @return This AproposLabel
	 */
	public AproposLabel display( GridBagLayout gbl, LineChangedListener lcL, PopupMenuListener pmL ) {
		layout = gbl;
		if ( displayed ) {
			cons = null;
			return this;
		}
		
		// Create the listener and the layout
		CardLayout layout = new CardLayout( 0, 0 );
		this.setLayout( layout );
		EditingListener hl = new EditingListener();
		
		// Create the JPanel for the "normal" state
		JPanel labelPanel = new JPanel( new GridLayout( 1, 1 ) );
		label = new JLabel( string.equals( "" ) ? "<add new>" : string );
		labelPanel.add( label );
		
		// Create the JPanel for the "hover state"
		JPanel inputPanel = new JPanel( new GridLayout( 1, 1 ) );
		textField = new JTextField( string );
		textField.addMouseListener( hl );
		textField.addKeyListener( hl );
		textField.addFocusListener( hl );
		inputPanel.add( textField );
		
		JPanel simulatePanel = new JPanel( new GridLayout( 1, 1 ) );
		simulateString = "<pending simulation>";
		simuLabel = new JLabel( simulateString );
		simulatePanel.add( simuLabel );
		
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
	public AproposLabel display( GridBagLayout gbl, LineChangedListener lcL, PopupMenuListener pmL, boolean editable ) {
		layout = gbl;
		if ( displayed ) {
			cons = null;
			return this;
		}
		
		setLayout( new GridLayout( 1, 1 ) );
		AproposListener al = new AproposListener();
		
		label = new JLabel( string.equals( "" ) ? "<add new>" : string );
		add( label );
		
		this.addMouseListener( al );
		this.addLineChangedListener( lcL );
		this.addPopupMenuListener( pmL );
		
		revalidate();
		displayed = true;
		
		return this;
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
			this.label.setText( text );
			return;
		}
		if ( getText().equals( "" ) ) {
			if ( !text.equals( "" ) ) {
				this.label.setText( text );
				this.string = text;
				fireLineInserted( this );
			}
		}
		else if ( text.equals( "" ) )
			this.label.setText( "<add new>" );
		else
			this.label.setText( text );
		this.textField.setText( text );
		this.string = text;
	}
	
	public String getText() {
		return string;
	}
	
	public String toString() {
		return string;
	}
	
	/**
	 * Returns a new label with equal reference to this label's parent and copied text
	 */
	public AproposLabel clone() {
		return new AproposLabel( getText(), getParentLabel() );
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
	
	/**
	 * @return the GridBagConstraints that directly represent how this object is positioned.
	 */
	public GridBagConstraints getGridBagCons() {
		if ( cons == null ) {
			cons = layout.getConstraints( this );
		}
		return cons;
	}
	
	public JTextField getTextField() {
		return textField;
	}
	
	public JLabel getLabel() {
		return label;
	}
	
	/**
	 * Nudge this label up or down based on this passed value. Negative is Up, Positive is Down.
	 * 
	 * @param amount
	 */
	public void poke( int amount ) {
		getGridBagCons().gridy += amount;
		invalidate();
	}
	
	/**
	 * Nudge label down one grid spot in it's container
	 */
	public void bump() {
		poke( 1 );
	}
	
	/**
	 * Nudge label up one grid spot in it's container
	 */
	public void boop() {
		poke( -1 );
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
	}
	
	public boolean getHoverState() {
		return hoverState;
	}
	
	/**
	 * @param simulate Shows simulated text on true, normal Label on false
	 */
	public void setSimulateState( boolean simulate ) {
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
	
	public void setSimulateString( String string ) {
		simulateString = string;
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
	protected void fireValueChanged( String value ) {
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
	protected void fireLineInserted( AproposLabel above ) {
		fireLineInserted( above, new AproposLabel( "", above.getParentLabel() ) );
	}
	/**
	 * Notifies all listeners that a new label is to be displayed below this one
	 */
	protected void fireLineInserted( AproposLabel above, AproposLabel toAdd ) {
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
	protected void fireLineRemoved( AproposLabel removed ) {
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
	protected void firepopupMenuTriggered( AproposLabel label, MouseEvent e ) {
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
	
	/**
	 * Gets the number of characters into the given string is based on a relative point co-ordinate
	 */
	public int positionFromPoint( int x, String s ) {
		int w = label.getGraphics().getFontMetrics().stringWidth( s );
		float pos = ( (float) x / (float) w ) * (float) s.length();
		return Math.min( (int) pos, s.length() );
	}
	
	/**
	 * A listener for non-editable AproposLabels, only provides PopupMenu actions
	 */
	public class AproposListener implements MouseListener {
		
		public void mousePressed( MouseEvent e ) {
			if ( e.isPopupTrigger() & !getHoverState() ) {
				firepopupMenuTriggered( AproposLabel.this, e );
			}
		}
		public void mouseReleased( MouseEvent e ) {
			if ( e.isPopupTrigger() & !getHoverState() ) {
				firepopupMenuTriggered( AproposLabel.this, e );
			}
		}
		
		public void mouseEntered( MouseEvent e ) {}
		public void mouseExited( MouseEvent e ) {}
		public void mouseClicked( MouseEvent e ) {}
	}
	
	/**
	 * Listener for full editing support
	 */
	public class EditingListener extends AproposListener implements KeyListener, FocusListener {
		boolean locked = false;
		String oldValue;
		
		public void focusGained( FocusEvent arg0 ) {
			oldValue = textField.getText();
		}
		public void mouseClicked( MouseEvent e ) {
			if ( e.getClickCount() == 2 & !e.isPopupTrigger() & !getHoverState() ) {
				setHoverState( true );
				textField.grabFocus();
				textField.setCaretPosition( positionFromPoint( e.getX(), textField.getText() ) );
			}
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
				setText( textField.getText() );
				fireValueChanged( textField.getText() );
				setHoverState( false );
				locked = true;
			}
			else if ( e.getKeyChar() == KeyEvent.VK_ESCAPE ) {
				setHoverState( false );
				release();
				setText( oldValue );
			}
		}
		
		public void keyPressed( KeyEvent e ) {}
		public void keyReleased( KeyEvent e ) {}
	}
	
	/**
	 * @return The Number of Parents
	 */
	public int getDepth() {
		return parent == null ? 0 : 1 + parent.getDepth();
	}
	
	public int compareTo( AproposLabel o ) {
		if(o == null) return 1;
		int ldepth = getDepth(), odepth = o.getDepth();
		String lstr = toString(), ostr = o.toString();
		if ( ldepth == odepth ) {
			switch ( ldepth ) {
				case 0: // The Database
					return lstr.compareTo( ostr );
				case 1: // The Folder
					return lstr.compareTo( ostr );
				case 2: // The Position
					return lstr.compareTo( ostr );
				case 3: // The Animation Stage
					if ( lstr.equals( ostr ) ) return 0;
					if ( lstr.equals( "Intro" ) ) return -1;
					if ( ostr.equals( "Intro" ) ) return 1;
					if ( lstr.equals( "Orgasm" ) ) return 1;
					if ( ostr.equals( "Orgasm" ) ) return -1;
					return lstr.compareTo( ostr );
				case 4: // The Animation Perspective
					return lstr.compareTo( ostr );
				default: // The lines themselves? We fucked up
					return 0;
			}
		}
		else
			return (int) Math.signum( odepth - ldepth );
	}
	
}

/**
 * A listener that will be notified whenever the text of an attached JLabel is modified
 */
interface ValueChangedListener extends EventListener {
	public void valueChanged( String value, JComponent source );
}

/**
 * A listener that will be notified whenever the attached JLabel needs to display a new Label below it or be removed
 */
interface LineChangedListener extends EventListener {
	public void lineInserted( AproposLabel above, AproposLabel toAdd );
	public void lineRemoved( AproposLabel removed );
}

/**
 * A listener that will be notified whenever the attached JLabel needs to display a PopupMenu
 */
interface PopupMenuListener extends EventListener {
	public void popupMenuTriggered( AproposLabel label, MouseEvent e );
}
