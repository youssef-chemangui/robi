package exercice1;

import java.awt.Color;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Random;

import graphicLayer.GRect;
import graphicLayer.GSpace;


public class Exercice1_0 {
	GSpace space = new GSpace("Exercice 1", new Dimension(200, 150));
	GRect robi = new GRect();
    private static final int STEP = 3;
    private static final int DELAY = 15;
    private static final Random random = new Random();
	
    public Exercice1_0() {
        space.addElement(robi);
        space.open();
        animate();
    }
    
    private void sleep() {
        try {
        	Thread.sleep(DELAY); 
        	} catch (InterruptedException e) {}
    }

    private void animate() {
        while (true) {
            moveToRight();
            moveToBottom();
            moveToLeft();
            moveToTop();
            robi.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
        }
    }
    private void moveToRight() {
        while (robi.getBounds().getMaxX() < space.getWidth()) {
            robi.translate(new Point(STEP, 0));
            sleep();
        }
    }

    private void moveToBottom() {
        while (robi.getBounds().getMaxY() < space.getHeight()) {
            robi.translate(new Point(0, STEP));
            sleep();
        }
    }

    private void moveToLeft() {
        while (robi.getBounds().getMinX() > 0) {
            robi.translate(new Point(-STEP, 0));
            sleep();
        }
    }

    private void moveToTop() {
        while (robi.getBounds().getMinY() > 0) {
            robi.translate(new Point(0, -STEP));
            sleep();
        }
    }
    
    

	public static void main(String[] args) {
		new Exercice1_0();
	}

}