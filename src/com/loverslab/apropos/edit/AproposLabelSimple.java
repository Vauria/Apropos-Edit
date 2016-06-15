package com.loverslab.apropos.edit;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
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
import javax.swing.event.MouseInputAdapter;

/**
 * Original Author James McMinn
 * github.com/JamesMcMinn/EditableJLabel
 */
@SuppressWarnings("serial")
public class AproposLabelSimple extends JPanel {
	
	private String stringstring;
	private AproposLabel parentLabel;
	private GridBagConstraints cons;
	private JLabel labellabel;
	private JTextField textFieldFIELD;
	private LinkedList<ValueChangedListener> listeners = new LinkedList<ValueChangedListener>();
	
	public AproposLabelSimple( AproposLabel source ) {
		super();
		stringstring = source.getText();
		this.parentLabel = source;
		
		// Create the listener and the layout
		EditingListener hl = new EditingListener();
		
		// Create the JPanel for the "normal" state
		labellabel = new JLabel( stringstring.equals( "" ) ? "<add new>" : stringstring );
		// label.addMouseListener( hl );
		
		// Create the JPanel for the "hover state"
		textFieldFIELD = new JTextField( stringstring );
		
		// label.addMouseWheelListener( mwtl );
		// labelPanel.addMouseWheelListener( mwtl );
		// textField.addMouseWheelListener( mwtl );
		// inputPanel.addMouseWheelListener( mwtl );
		
		this.add( labellabel );
		
		this.addMouseListener( new MouseInputAdapter(){
			public void mouseEntered( MouseEvent e ) {
				System.out.println( e );
			}
		});
		
		// Set the states
		
		// Show the correct panel to begin with
	}
	
	public void setText( String text ) {
		if ( getText().equals( "" ) ) {
			if ( !text.equals( "" ) ) {
				this.labellabel.setText( text );
			}
		}
		else if ( text.equals( "" ) )
			this.labellabel.setText( "<add new>" );
		else
			this.labellabel.setText( text );
		this.textFieldFIELD.setText( text );
		this.stringstring = text;
	}
	
	public String getText() {
		return stringstring;
	}
	
	public String toString() {
		return stringstring;
	}
	
	public AproposLabel getParent() {
		return parentLabel;
	}
	
	public GridBagConstraints getGridBagCons() {
		return cons;
	}
	
	public JTextField getTextField() {
		return textFieldFIELD;
	}
	
	public JLabel getLabel() {
		return labellabel;
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
		int w = labellabel.getGraphics().getFontMetrics().stringWidth( s );
		float pos = ( (float) x / (float) w ) * (float) s.length();
		return Math.min( (int) pos, s.length() );
	}
	
	public class EditingListener implements MouseListener, KeyListener, FocusListener {
		boolean locked = false;
		String oldValue;
		
		public void focusGained( FocusEvent arg0 ) {
			System.out.println( arg0.getSource() + ": Focus Gained" );
			oldValue = textFieldFIELD.getText();
		}
		public void mouseClicked( MouseEvent e ) {
			System.out.println( e.getSource() + ": Clicked" );
			if ( e.getClickCount() == 2 ) {
				setHoverState( true );
				textFieldFIELD.grabFocus();
				textFieldFIELD.setCaretPosition( positionFromPoint( e.getX(), textFieldFIELD.getText() ) );
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
				setText( textFieldFIELD.getText() );
				for ( ValueChangedListener v : listeners ) {
					v.valueChanged( textFieldFIELD.getText(), AproposLabelSimple.this );
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
	
}
