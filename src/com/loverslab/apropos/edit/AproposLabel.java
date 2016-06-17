package com.loverslab.apropos.edit;

import java.awt.CardLayout;
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

/**
 * Original Author James McMinn
 * github.com/JamesMcMinn/EditableJLabel
 */
@SuppressWarnings("serial")
public class AproposLabel extends JPanel implements Comparable<AproposLabel> {
	
	private String string;
	private AproposLabel parent;
	private GridBagLayout layout;
	private GridBagConstraints cons;
	private JLabel label;
	private JTextField textField;
	private boolean hoverState;
	
	public AproposLabel( String startText, AproposLabel parent ) {
		super();
		string = startText;
		this.parent = parent;
	}
	
	public AproposLabel display( GridBagLayout gbl ) {
		layout = gbl;
		
		// Create the listener and the layout
		CardLayout layout = new CardLayout( 0, 0 );
		this.setLayout( layout );
		EditingListener hl = new EditingListener();
		
		// Create the JPanel for the "normal" state
		JPanel labelPanel = new JPanel( new GridLayout( 1, 1 ) );
		label = new JLabel( string.equals( "" ) ? "<add new>" : string );
		// label.addMouseListener( hl );
		labelPanel.add( label );
		// labelPanel.addMouseListener( hl );
		
		// Create the JPanel for the "hover state"
		JPanel inputPanel = new JPanel( new GridLayout( 1, 1 ) );
		textField = new JTextField( string );
		textField.addMouseListener( hl );
		textField.addKeyListener( hl );
		textField.addFocusListener( hl );
		inputPanel.add( textField );
		
		// label.addMouseWheelListener( mwtl );
		// labelPanel.addMouseWheelListener( mwtl );
		// textField.addMouseWheelListener( mwtl );
		// inputPanel.addMouseWheelListener( mwtl );
		
		this.addMouseListener( hl );
		
		// Set the states
		this.add( labelPanel, "NORMAL" );
		this.add( inputPanel, "HOVER" );
		
		// Show the correct panel to begin with
		layout.show( this, "NORMAL" );
		revalidate();
		
		return this;
	}
	
	public AproposLabel display( GridBagLayout gbl, boolean editable ) {
		layout = gbl;
		
		setLayout( new GridLayout( 1, 1 ) );
		AproposListener al = new AproposListener();
		
		label = new JLabel( string.equals( "" ) ? "<add new>" : string );
		add( label );
		
		this.addMouseListener( al );
		
		revalidate();
		
		return this;
	}
	
	public void setText( String text ) {
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
	
	public AproposLabel clone() {
		return new AproposLabel(getText(),getParentLabel());
	}
	
	public AproposLabel getParentLabel() {
		return parent;
	}
	
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
	
	public void bump() {
		getGridBagCons().gridy++ ;
		invalidate();
	}
	
	public void boop() {
		getGridBagCons().gridy-- ;
		invalidate();
	}
	
	public void setHoverState( boolean hover ) {
		CardLayout cl = (CardLayout) ( this.getLayout() );
		
		hoverState = hover;
		if ( hover )
			cl.show( this, "HOVER" );
		else
			cl.show( this, "NORMAL" );
	}
	
	public boolean getHoverState() {
		return hoverState;
	}
	
	public void setEnabled( boolean enabled ) {
		super.setEnabled( enabled );
		textField.setEnabled( enabled );
		label.setEnabled( enabled );
	}
	
	public void addValueChangedListener( ValueChangedListener listener ) {
		listenerList.add( ValueChangedListener.class, listener );
	}
	
	public void removeValueChangedListener( ValueChangedListener listener ) {
		listenerList.remove( ValueChangedListener.class, listener );
	}
	
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
	
	public void addLineChangedListener( LineChangedListener listener ) {
		listenerList.add( LineChangedListener.class, listener );
	}
	
	public void removeLineChangedListener( LineChangedListener listener ) {
		listenerList.remove( LineChangedListener.class, listener );
	}
	
	protected void fireLineInserted( AproposLabel above ) {
		fireLineInserted(above, new AproposLabel( "", above.getParentLabel() ) );
	}
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
	
	public void addPopupMenuListener( PopupMenuListener listener ) {
		listenerList.add( PopupMenuListener.class, listener );
	}
	
	public void removePopupMenuListener( PopupMenuListener listener ) {
		listenerList.remove( PopupMenuListener.class, listener );
	}
	
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
	
	public int positionFromPoint( int x, String s ) {
		int w = label.getGraphics().getFontMetrics().stringWidth( s );
		float pos = ( (float) x / (float) w ) * (float) s.length();
		return Math.min( (int) pos, s.length() );
	}
	
	public class AproposListener implements MouseListener {
		
		public void mousePressed( MouseEvent e ) {
			if ( e.isPopupTrigger() ) {
				firepopupMenuTriggered( AproposLabel.this, e );
			}
		}
		public void mouseReleased( MouseEvent e ) {
			if ( e.isPopupTrigger() ) {
				firepopupMenuTriggered( AproposLabel.this, e );
			}
		}
		
		public void mouseEntered( MouseEvent e ) {}
		public void mouseExited( MouseEvent e ) {}
		public void mouseClicked( MouseEvent e ) {}
	}
	
	public class EditingListener extends AproposListener implements KeyListener, FocusListener {
		boolean locked = false;
		String oldValue;
		
		public void focusGained( FocusEvent arg0 ) {
			oldValue = textField.getText();
		}
		public void mouseClicked( MouseEvent e ) {
			if ( e.getClickCount() == 2 & !e.isPopupTrigger() & !getHoverState()) {
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
				fireValueChanged( textField.getText() );
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
				default: // The lines themselves? We're fucked
					return 0;
			}
		}
		else
			return (int) Math.signum( odepth - ldepth );
	}
	
}

interface ValueChangedListener extends EventListener {
	public void valueChanged( String value, JComponent source );
}

interface LineChangedListener extends EventListener {
	public void lineInserted( AproposLabel above, AproposLabel toAdd );
	public void lineRemoved( AproposLabel removed );
}

interface PopupMenuListener extends EventListener {
	public void popupMenuTriggered( AproposLabel label, MouseEvent e );
}
