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
import java.util.LinkedList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Original Author James McMinn
 * github.com/JamesMcMinn/EditableJLabel
 */
@SuppressWarnings("serial")
public class AproposLabelSimple extends JPanel {
	
	private String string;
	private AproposLabel parentLabel;
	private GridBagConstraints cons;
	private JLabel label;
	private JTextField textField;
	private LinkedList<ValueChangedListener> listeners = new LinkedList<ValueChangedListener>();
	
	public AproposLabelSimple( AproposLabel source ) {
		this( source.getText() );
	}
	public AproposLabelSimple( String source ) {
		super();
		string = source;
		this.parentLabel = new AproposLabel( string, null );
		
		// Create the listener and the layout
		CardLayout layout = new CardLayout( 0, 0 );
		this.setLayout( layout );
		EditableListener hl = new EditableListener();
		
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
	
	public AproposLabel getParentLabel() {
		return parentLabel;
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
					v.valueChanged( textField.getText(), AproposLabelSimple.this );
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
