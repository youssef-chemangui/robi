package exercice2;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Color;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import graphicLayer.GRect;
import graphicLayer.GSpace;
import stree.parser.SNode;
import stree.parser.SParser;


public class Exercice2_2_0 {
	GSpace space = new GSpace("Exercice 2_2", new Dimension(200, 100));
	GRect robi = new GRect();
	private static final int STEP = 2;
    private static final int DELAY = 15;
	String script = "(space color white) (robi color red) (robi translate 10 0) (space sleep 100) (robi translate 0 10) (space sleep 100) (robi translate -10 0) (space sleep 100) (robi translate 0 -10)";

	public Exercice2_2_0() {
		space.addElement(robi);
		space.open();
		this.runScript();
	}
	
	 private void sleep() {
	        try {
	        	Thread.sleep(DELAY); 
	        	} catch (InterruptedException e) {}
	    }

	private void runScript() {
		SParser<SNode> parser = new SParser<>();
		List<SNode> rootNodes = null;
		try {
			rootNodes = parser.parse(script);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Iterator<SNode> itor = rootNodes.iterator();
		while (itor.hasNext()) {
			this.run(itor.next());
		}
	}
	
	private void run(SNode expr) {

	    String object = expr.get(0).contents();
	    String command = expr.get(1).contents();

	    if (object.equals("space")) {

	        if (command.equals("color")) {
	            space.setColor(stringToColor(expr.get(2).contents()));
	        }

	        if (command.equals("sleep")) {
	            try {
	                Thread.sleep(Integer.parseInt(expr.get(2).contents()));
	            } catch (InterruptedException e) {}
	        }
	    }

	    if (object.equals("robi")) {

	        if (command.equals("color")) {
	            robi.setColor(stringToColor(expr.get(2).contents()));
	        }

	        if (command.equals("translate")) {
	            int dx = Integer.parseInt(expr.get(2).contents());
	            int dy = Integer.parseInt(expr.get(3).contents());

	            int movedX = 0;
	            int movedY = 0;

	            while (movedX != dx || movedY != dy) {

	                int stepX = 0;
	                int stepY = 0;

	                if (movedX < dx) stepX = Math.min(STEP, dx - movedX);
	                if (movedX > dx) stepX = -Math.min(STEP, movedX - dx);

	                if (movedY < dy) stepY = Math.min(STEP, dy - movedY);
	                if (movedY > dy) stepY = -Math.min(STEP, movedY - dy);

	                robi.translate(new Point(stepX, stepY));

	                movedX += stepX;
	                movedY += stepY;

	                sleep();
	            }
	        }
	    }
	}
	    
	    private Color stringToColor(String name) {
		    switch(name) {
		        case "black": return Color.BLACK;
		        case "white": return Color.WHITE;
		        case "red": return Color.RED;
		        case "yellow": return Color.YELLOW;
		        case "blue": return Color.BLUE;
		        case "green": return Color.GREEN;
		        default: return Color.GRAY;
		    }
		}

	
	

	public static void main(String[] args) {
		new Exercice2_2_0();
	}

}