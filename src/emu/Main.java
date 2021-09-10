package emu;

import java.awt.BorderLayout;

import chip.Chip;
import chip.ChipData;

public class Main extends Thread{
	
	private Chip chip8;
	private ChipFrame frame;
	private MemFrame frame2;

	public Main() {
		chip8 = new Chip();
		chip8.init();
		chip8.loadProgram("C:\\Users\\Samar Khan\\Desktop\\CHIP 8\\Rush Hour [Hap, 2006].ch8");
		frame = new ChipFrame(chip8);
		frame2 = new MemFrame(chip8);
	}
	
	public void run() {
		//60 hz = 60 fps
		while (true) {
			chip8.setKeyBuffer(frame.getKeyBuffer());
			chip8.run();
			if (chip8.needsRedraw()) {
				frame.repaint();
				chip8.removeDrawFlag();
			}
			if (chip8.needsClear()) {
//				frame.panel.removeAll();;
				chip8.removeDrawFlag();
			}
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				//Not needed
			}
		}
	}
	
	public static void main(String args[]) {
		 
		Main main = new Main();
		main.start();
	}

}
