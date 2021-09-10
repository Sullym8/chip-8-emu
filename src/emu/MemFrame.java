package emu;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

import chip.Chip;

public class MemFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	public MemPanel panel;
	
	public MemFrame(Chip c) {
		setPreferredSize(new Dimension(640,720));
		pack();
		setPreferredSize(new Dimension(640+getInsets().left+getInsets().left,720 + getInsets().top+getInsets().bottom));
		panel = new MemPanel(c);
		setLayout(new BorderLayout());
		add(panel,BorderLayout.CENTER);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("CHIP 8 EMU");
		pack();
		setVisible(true);
		
		

		
	}
}
