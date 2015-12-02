package edu.villanova.chitluri.Wii;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JFrame;

public class Shape extends JFrame {

	int width;
	int height;
	
	public Shape(int width, int height){
		this.width = width;
		this.height = height;
		setTitle("Shape");
		setSize(960, 960);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	 public void paint(Graphics g){
		 g.setColor(Color.green);
		 g.fillOval(240, 240, width, height);
	}
	 
	public static void main(String args[]){
		Shape s = new Shape(200, 100);
		s.paint(null);
	}
}
