package chip;

import java.awt.Frame;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

public class Chip {
	
	private char[] memory;
	private char[] V;
	private char I;
	private char pc;
	
	private char[] stack;
	private int stackPointer;
	
	private int delay_timer;
	private int sound_timer;
	
	private byte[] keys;
	
	private byte[] display;
	
	private boolean needRedraw;
	private boolean needClear;
	
	public void init() {
		memory = new char[4096];
		V = new char[16];
		I = 0x0;
		pc = 0x200;
		
		stack = new char[16];
		stackPointer = 0;
		
		delay_timer = 0;
		sound_timer = 0;
		
		keys = new byte[16];
		
		display = new byte[64*32];
		
		needRedraw = false;
		needClear = false;
		
		loadFontset();
	}
	
	public void run() {
		//fetch
		char opcode = (char) ((memory[pc] << 8) | memory[pc+1]);
		System.out.print(Integer.toHexString(pc).toUpperCase() + "|");
		System.out.print(Integer.toHexString(opcode).toUpperCase()+ " : ");
		//decode
		switch (opcode & 0xF000) {
		case 0x0000:
			// Multi Case, can be 00NNN, 00E0, 00EE
			switch (opcode & 0x00FF) {
			case (0x00E0):
				//00E0: Clear Screen
				for (int i = 0; i < display.length; i++) {
					display[i]=0;
				}
				needRedraw = true;
				System.out.println("Clearing Screen");
				pc += 2;
				break;
			case (0x00EE):
				//00EE: Return from Subroutine
				//stack return
				stackPointer--;
				pc = (char) (stack[stackPointer] + 2);
				System.out.println("Returning to " + Integer.toHexString(pc).toUpperCase());
				break;
			default:
				//0NNN: Calls RCA 1802 at NNN
				break;
			}
			break;
		case 0x1000:
			//1NNN: Jumps to NNN
			int nnn = opcode & 0x0FFF;
			pc = (char) nnn;
			System.out.println("Jumping to Address: " + Integer.toHexString(nnn).toUpperCase());
			break;
		case 0x2000: 
			//2NNN: Calls subroutine at N
			stack[stackPointer] = pc;
			stackPointer++;
			pc = (char) (opcode & 0x0FFF);
			System.out.println("Calling " + Integer.toHexString(pc).toUpperCase());
			break;
		case 0x3000: {
			//3XNN: Skip the next instruction if VX == NN
			int x = (opcode & 0x0F00) >> 8;
			int nn = opcode & 0x00FF;
			if (V[x] == nn) {
				pc += 4;
				System.out.println("Skipping next Instruction (V[" + Integer.toHexString(x).toUpperCase() +"] == " + Integer.toHexString(nn).toUpperCase() + ")" );
			} else {
				System.out.println("Not skipping next Instruction (V[" + Integer.toHexString(x).toUpperCase() +"] =/= " + Integer.toHexString(nn).toUpperCase() + ")" );
				pc += 2;
			}
			break;
		}
		case 0x4000: {
			//4XNN: Skip the next instruction if VX == NN
			int x = (opcode & 0x0F00) >> 8;
			int nn = opcode & 0x00FF;
			if (V[x] != nn) {
				pc += 4;
				System.out.println("Skipping next Instruction (V[" + Integer.toHexString(x).toUpperCase() +"] =/= " + Integer.toHexString(nn).toUpperCase() + ")" );
			} else {
				System.out.println("Not skipping next Instruction (V[" + Integer.toHexString(x).toUpperCase() +"] == " + Integer.toHexString(nn).toUpperCase() + ")" );
				pc += 2;
			}
			break;
		}
		case 0x5000: {
			//5XY0: Skip the next instruction if VX == VY
			int x = (opcode & 0x0F00) >> 8;
			int y = (opcode & 0x00F0) >> 4;
			if (V[x] == V[y]) {
				pc += 4;
				System.out.println("Skipping next Instruction (V[" + Integer.toHexString(x).toUpperCase() +"] == V[" + Integer.toHexString(y).toUpperCase() +"])" );
			} else {
				System.out.println("Not skipping next Instruction (V[" + Integer.toHexString(x).toUpperCase() +"] =/= V[" + Integer.toHexString(y).toUpperCase() +"])" );
				pc += 2;
			}
			break;
		}
		case 0x6000: {
			//6XNN Set VX to NN
			int x = (opcode & 0x0F00) >> 8;
			V[x] = (char) (opcode & 0x00FF);
			pc += 0x2;
			System.out.println("Setting V[" + Integer.toHexString(x).toUpperCase() + "] to: " + Integer.toHexString((opcode & 0x00FF)).toUpperCase());
			break;
		}
		case 0x7000: {
			//7XNN: Adds NN to VX
			int x = (opcode & 0x0F00) >> 8;
			int nn = opcode & 0x00FF;
			V[x] = (char) ((V[x] + nn) & 0xFF);
			pc += 0x2;
			System.out.println("Adding "+ Integer.toHexString(nn).toUpperCase() + " to: V[" + Integer.toHexString(x).toUpperCase() +"]");
			break;
		}
		case 0x8000:
			//Contains more data in last nibble
			switch (opcode & 0x000F) {
			case 0x000:{
				//8XY0: Set V[x] equal to V[y]
				int x = (opcode & 0xF00) >> 8;
				int y = (opcode & 0x0F0) >> 4;
				V[x]=V[y];
				pc += 2;
				System.out.println("Setting V["+Integer.toHexString(x).toUpperCase()+"] = V["+Integer.toHexString(y).toUpperCase()+"] ("+Integer.toHexString(V[x])+")");
				break;
			}
			case 0x001:{
//				8XY1: Sets VX to VX or VY. (Bitwise OR operation)
				int x = (opcode & 0xF00) >> 8;
				int y = (opcode & 0x0F0) >> 4;
				V[x] = (char) (V[x]|V[y]); 
				pc += 2;
				System.out.println("Setting V["+Integer.toHexString(x).toUpperCase()+"] = "+Integer.toHexString(V[x]).toUpperCase()+ " (OR)");
				break;
			}
			case 0x002:{
				//8XY2: Sets VX to VX and VY. (Bitwise AND operation)
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				V[x]= (char) (V[x] & V[y]);
				pc += 2;
				System.out.println("Setting V["+Integer.toHexString(x).toUpperCase()+"] = "+Integer.toHexString(V[x]).toUpperCase()+ " (AND)");
				break;
			}
			case 0x003: {
				//8XY3: Sets VX to VX xor VY.
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				V[x]= (char) (V[x] ^ V[y]);
				pc += 2;
				System.out.println("Setting V["+Integer.toHexString(x).toUpperCase()+"] = "+Integer.toHexString(V[x]).toUpperCase()+ " (XOR)");
				break;
			}
			case 0x004:{
				//8XY4: Adds VY to VX. VF is set to 1 when there's a carry, and to 0 when there is not.
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				System.out.print("Adding V["+Integer.toHexString(x).toUpperCase()+"] to V["+Integer.toHexString(y).toUpperCase()+"], ");
				if (V[y] > (0xFF- V[x])) {
					V[0xF] = 1;
					System.out.print(" Carry Exists, ");
				} else {
					V[0xF] = 0;
					System.out.print(" No Carry Exists, ");
				}
				V[x]= (char) (V[x] + V[y] & 0xFF);
				System.out.println("V["+Integer.toHexString(x).toUpperCase()+"] = " + Integer.toHexString(V[x]).toUpperCase());
				pc +=2;
				break;
			}
			case 0x005:{
				//8XY5: VY is subtracted from VX. VF is set to 0 when there's a borrow, and 1 when there is not.
				int x = (opcode & 0xF00) >> 8;
				int y = (opcode & 0x0F0) >> 4;
				if (V[x] > V[y]) {
					V[0xF] = 1;
					V[x]= (char) (V[x] - V[y] & 0xFF);
				} else {
					V[0xF] = 0;
					V[x]= (char) (V[x] - V[y] & 0xFF);
				}
				pc +=2;
				System.out.println("Subtracting V["+Integer.toHexString(y).toUpperCase()+"] from V["+Integer.toHexString(x).toUpperCase()+"], Carrying applied if needed, V[x] = " + Integer.toHexString(V[x]).toUpperCase());
				break;
			}
			case 0x006:{
				//8XY6: Stores the least significant bit of VX in VF and then shifts VX to the right by 1.
				int x = (opcode & 0x0F00) >> 8;
				V[0xF] = (char) (V[x] & 0x01);
				V[x] = (char) (V[x] >>= 1);
				if (V[x] > 0xFF ) {
					V[x] = (char) (V[x] - 0x100);
				}
				System.out.println("LSB of V[x] = "+Integer.toHexString(V[0xF]).toUpperCase()+", Bitwise RHS'd V["+Integer.toHexString(V[x]).toUpperCase()+"]= "+ Integer.toHexString(V[x]));
				pc += 2;
				break;
			}
			case 0x007:{
				//8XY7: Sets VX to VY minus VX. VF is set to 0 when there's a borrow, and 1 when there is not.
				int x = (opcode & 0xF00) >> 8;
				int y = (opcode & 0x0F0) >> 4;
				if (V[y] > V[x]) {
					V[0xF] = 1;
					V[x]= (char) (V[y] - V[x] & 0xFF);
				} else {
					V[0xF] = 0;
					V[x]= (char) (V[y] - V[x] & 0xFF);
				}
				pc +=2;
				System.out.println("Subtracting V["+Integer.toHexString(x).toUpperCase()+"] from V["+Integer.toHexString(y).toUpperCase()+"], Carrying applied if needed, V[x] = " + Integer.toHexString(V[x]).toUpperCase());
				break;
			}
			case 0x00E:{
				//8XYE: Stores the most significant bit of VX in VF and then shifts VX to the left by 1.
				int x = (opcode & 0x0F00) >> 8;
				V[0xF] = (char) (V[x] & 0x80);
				V[x] = (char) (V[x] <<= 1);
				if (V[x] > 0xFF ) {
					V[x] = (char) (V[x] - 0x100);
				}
				System.out.println("MSB of V[x] = "+Integer.toHexString(V[0xF]).toUpperCase()+", Bitwise LHS'd V["+Integer.toHexString(V[x]).toUpperCase()+"]= "+ Integer.toHexString(V[x]));
				pc += 2;
				break;
			}
			default:
				System.err.println("Unsupported Opcode");
				System.exit(0);
				break;
			}
			break;
		case 0x9000: {
			//9XY0: Skip the next instruction if VX =/= VY
			int x = (opcode & 0x0F00) >> 8;
			int y = (opcode & 0x00F0) >> 4;
			if (V[x] != V[y]) {
				pc += 4;
				System.out.println("Skipping next Instruction (V[" + x +"] =/= V[" + y +"])" );
			} else {
				System.out.println("Not skipping next Instruction (V[" + x +"] == V[" + y +"])" );
				pc += 2;
			}
			break;
		}
		case 0xA000: {
			//ANNN: sets I to NNN
			I = (char) (opcode & 0x0FFF);
			pc += 0x2;
			System.out.println("Setting I to: " + Integer.toHexString((opcode & 0x0FFF)).toUpperCase());
			break;
		}
		case 0xB000: {
			//BNNN: sets PC to V[0] + NNN
			nnn= (char) (opcode & 0x0FFF);
			pc = (char) (V[0]+ nnn);
			System.out.println("Setting PC to: V[0] + "+Integer.toHexString(nnn).toUpperCase()+ " = "+Integer.toHexString(pc).toUpperCase());
			break;
		}
		case 0xC000:{
			//CXNN: Sets VX to the result of a bitwise and operation on a random number (Typically: 0 to 255) and NN.
			int x = (opcode & 0x0F00) >> 8;
			int nn = opcode & 0x00FF;
			int RandomNumber = new Random().nextInt(255) & nn;
			System.out.println("V["+x+"] has been set to RNG value: " + Integer.toHexString(RandomNumber).toUpperCase());
			V[x] = (char) RandomNumber;
			pc += 0x2;
			break;
		}
		case 0xD000: {
			//DXYN: Draws a sprite at coordinate (VX, VY) that has a width of 8 pixels and a height of N+1 pixels. Sprite image is taken from I
			int x = V[(opcode & 0x0F00) >> 8];
			int y = V[(opcode & 0x00F0) >> 4];
			int height = opcode & 0x000F;
			
			V[0xF] = 0;
			
			for (int _y = 0; _y < height; _y++) {
				int line = memory[I+_y];
				for (int _x = 0; _x < 8; _x++) {
					int pixel = line & (0x80 >> _x);
					if (pixel != 0) {
						int totalX = x + _x;
						int totalY = y + _y;
						
						totalX = totalX % 64;
						totalY = totalY % 32;
						int index = totalY * 64 + totalX;
						
						if (display[index] == 1) 
							V[0xF] = 1;
						
						
						display[index] ^= 1;
					}
				}
			}
			pc += 0x2;
			needRedraw = true;
			System.out.println("Drawing Sprite at (" +Integer.toHexString(x)+ "," +Integer.toHexString(y)+ ")" + "with Height: " + Integer.toHexString(height));
			break;
		}
		case 0xE000: {
			switch (opcode & 0x00FF) {
			case 0x009E:{
				//EX9E: Skips next instruction if KeyID stored in V[x] is pressed.
				int key = (opcode & 0x0F00) >> 8;
				int val = V[key];
				if (keys[val]==1) {
					pc += 4;
				} else {
					pc += 2;
				}
				System.out.println("Skipping next Instruction if V["+V[key]+"] is pressed");
				break;
			}
			case 0x00A1:{
				//EXA1: Skips next instruction if KeyID stored in V[x] is NOT pressed.
				int key = (opcode & 0x0F00) >> 8;
				int val = V[key];
				if (keys[val]==0) {
					pc += 4;
				} else {
					pc += 2;
				}
				System.out.println("Skipping next Instruction if V["+V[key]+"] is NOT pressed");
				break;
			}
			default:
				System.err.println("Unsupported Opcode");
				System.exit(0);
				break;
			}
			break;
		}
		case 0xF000: {
			switch (opcode & 0x00FF) {
			case 0x0007:{
				//FX07: Set V[x] to the value of delay_timer
				int x = (opcode & 0x0F00) >> 8;
				V[x] = (char) delay_timer;
				System.out.println("Set V["+x+"] to delay time = " + delay_timer);
				pc += 2;
				break;
			}
			case 0x000A:{
				//FX0A: A key press is awaited, and then stored in VX. (Blocking Operation. All instruction halted until next key event)
				System.err.println("Unsupported Opcode");
				System.exit(0);
				break;
			}
			case 0x0015:{
				//FX15: Set Delay timer to V[x]
				int x = (opcode & 0x0F00) >> 8;
				delay_timer=V[x];
				pc += 2;
				System.out.println("Set Delay Timer to V["+x+"] = " + Integer.toHexString(V[x]).toUpperCase());
				break;
			}	
			case 0x0018:{
				//FX18: Set Sound timer to V[x]
				int x = (opcode & 0x0F00) >> 8;
				sound_timer=V[x];
				pc += 2;
				System.out.println("Set Sound Timer to V["+x+"] = " + Integer.toHexString(V[x]).toUpperCase());
				break;
			}
			case 0x001E:{
				//FX1E: Adds VX to I. VF is not affected.
				int x = (opcode & 0x0F00) >> 8;
				I += V[x];
				pc += 2;
				System.out.println("Adding V["+x+"] to I = " + Integer.toHexString(I).toUpperCase());
				break;
			}
			case 0x0029:{
				//FX29: Sets I to the location of the sprite for the character in VX. Characters 0-F (in hexadecimal) are represented by a 4x5 font.
				int x = (opcode & 0x0F00) >> 8;
				int character = V[x];
				I = (char) (0x050 + (character * 5));
				System.out.println("Setting I to Character V["+x+"] = "+Integer.toHexString(V[x]).toUpperCase()+" Offset to 0x" + Integer.toHexString(I).toUpperCase());
				pc += 2;
				break;
			}
			case 0x0033:{
				//FX33: Store a Binary Coded Decimal Value VX in I, I + 1 and I + 2
				int x = (opcode & 0x0F00) >> 8;
				int value = V[x];
				
				int hundreds = (value - (value% 100))/100;
				value -= hundreds*100;
				int tens = (value - (value % 10))/10;;
				value -= tens * 10;
				int ones = value;
				memory[I] = (char) hundreds;
				memory[I+1] = (char) tens;
				memory[I+2] = (char) ones;
				System.out.println("Storing Binary Coded Decimals found at V[" + Integer.toHexString(x).toUpperCase() +"] as " + "{"+hundreds+","+tens+","+ones+",}");
				pc += 2;
				break;
			}
			case 0x0055:{
				//FX55: Stores V0 to VX (including VX) in memory starting at address I. The offset from I is increased by 1 for each value written, but I itself is left unmodified	
				int x = (opcode & 0x0F00) >> 8;
				for (int i = 0; i < x; i++) {
					memory[I+1] = V[i];
				}
				System.out.println("Filling Memory from mem[I] to mem[I+x+1 with V[0] to V["+Integer.toHexString(x).toUpperCase()+"]");
				I = (char) (I + x + 1);
				pc += 2;
				break;
			}
			case 0x0065:{
				//FX65: Fills V0 to VX with values from I
				int x = (opcode & 0x0F00) >> 8;
				for (int i = 0; i < x; i++) {
					V[i] = memory[I+i]; 
				}
				System.out.println("Filling V[0] to V["+x+"] starting from the values of memory[0x"+Integer.toHexString(I & 0xFFFF).toUpperCase()+"]");
				I = (char) (I + x + 1);
				pc += 2;
				break;
			}
			

			default:
				System.err.println("Unsupported Opcode");
				System.exit(0);
				break;
			}
			
			break;
		}
		default:
			System.err.println("Unsupported Opcode");
			System.exit(0);
			break;
		}
		
		if (sound_timer > 0) {
			sound_timer --;
		}
		if (delay_timer > 0) {
			delay_timer --;
		}
			//execute
		//store
	}
	
	public boolean needsClear() {
		return needClear;
	} 
	public boolean needsRedraw() {
		return needRedraw;
	}
	
	public byte[] getDisplay() {
		return this.display;
	}

	public void removeDrawFlag() {
		needRedraw = false;
	}

	public void loadProgram(String file) {
		DataInputStream input = null;
		try {
			input = new DataInputStream(new FileInputStream(new File(file)));
			int offset = 0;
			
			while (input.available()>0) {
				memory[0x200+offset] = (char) (input.readByte() & 0xFF);
				offset++;
			}	
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public void loadFontset() {
		for (int i = 0; i < ChipData.fontset.length; i++) {
			memory[0x50 + i ]= (char) ChipData.fontset[i];
			
		}
	}
	
	public void setKeyBuffer(int[] keyBuffer) {
		for (int i = 0; i < keys.length; i++) {
			keys[i] = (byte) keyBuffer[i];
		}
	}
}


