package com.loverslab.apropos.edit;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.LinkedList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/**
 * Original Author James McMinn
 * github.com/JamesMcMinn/EditableJLabel
 */
@SuppressWarnings("serial")
public class AproposLabel extends JPanel implements Comparable<AproposLabel> {
	
	private String string;
	private AproposLabel parent;
	private GridBagConstraints cons;
	private JLabel label;
	private JTextField textField;
	private MouseWheelTransferListener mwtl;
	private LinkedList<ValueChangedListener> listeners = new LinkedList<ValueChangedListener>();
	
	public AproposLabel( String startText, AproposLabel parent ) {
		super();
		string = startText;
		this.parent = parent;
		EditingListener hl = new EditingListener();
		this.addMouseListener( hl );
	}
	
	public AproposLabel display( GridBagConstraints c, JScrollPane scroll ) {
		cons = (GridBagConstraints) c.clone();
		
		// Create the listener and the layout
		CardLayout layout = new CardLayout( 0, 0 );
		this.setLayout( layout );
		EditingListener hl = new EditingListener();
		mwtl = new MouseWheelTransferListener( scroll );
		
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
		this.add( label, "NORMAL" );
		this.add( textField, "HOVER" );
		
		// Show the correct panel to begin with
		layout.show( this, "NORMAL" );
		revalidate();
		
		return this;
	}
	
	public void setText( String text ) {
		if ( getText().equals( "" ) ) {
			if ( !text.equals( "" ) ) {
				this.label.setText( text );
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
	
	public AproposLabel getParent() {
		return parent;
	}
	
	public GridBagConstraints getGridBagCons() {
		return cons;
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
	
	public class EditingListener implements MouseListener, KeyListener, FocusListener {
		boolean locked = false;
		String oldValue;
		
		public void focusGained( FocusEvent arg0 ) {
			System.out.println( arg0.getSource() + ": Focus Gained" );
			oldValue = textField.getText();
		}
		public void mouseClicked( MouseEvent e ) {
			System.out.println( e.getSource() + ": Clicked" );
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
					v.valueChanged( textField.getText(), AproposLabel.this );
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
		public void mouseEntered( MouseEvent e ) {
			System.out.println( e );
		}
		public void mouseExited( MouseEvent e ) {
			System.out.println( e );
		}
	}
	
	// http://stackoverflow.com/a/11403697
	public class MouseWheelTransferListener implements MouseWheelListener {
		
		private JScrollPane parentScrollPane;
		
		public MouseWheelTransferListener( JScrollPane parentScrollPane ) {
			this.parentScrollPane = parentScrollPane;
		}
		
		/** {@inheritDoc} */
		@Override
		public void mouseWheelMoved( MouseWheelEvent event ) {
			System.out.println( "Caught Event" );
			parentScrollPane.dispatchEvent( cloneEvent( event ) );
		}
		
		/**
		 * Copies the given MouseWheelEvent.
		 */
		private MouseWheelEvent cloneEvent( MouseWheelEvent event ) {
			return new MouseWheelEvent( parentScrollPane, event.getID(), event.getWhen(), event.getModifiers(), 1, 1,
					event.getClickCount(), false, event.getScrollType(), event.getScrollAmount(),
					event.getWheelRotation() );
		}
		
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
