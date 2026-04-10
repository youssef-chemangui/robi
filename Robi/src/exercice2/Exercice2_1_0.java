

package exercice2;

import java.awt.Dimension;
import java.awt.Color;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import graphicLayer.GRect;
import graphicLayer.GSpace;
import stree.parser.SNode;
import stree.parser.SParser;


public class Exercice2_1_0 {
	GSpace space = new GSpace("Exercice 2_1", new Dimension(200, 100));
	GRect robi = new GRect();
	String script = "(space setColor black) (robi setColor yellow)";

	public Exercice2_1_0() {
		space.addElement(robi);
		space.open();
		this.runScript();
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

	    // ----- SPACE -----
	    if (object.equals("space")) {

	        if (command.equals("setColor")) {
	            String colorName = expr.get(2).contents();
	            space.setColor(stringToColor(colorName));
	        }
	    }

	    // ----- ROBI -----
	    if (object.equals("robi")) {

	        if (command.equals("setColor")) {
	            String colorName = expr.get(2).contents();
	            robi.setColor(stringToColor(colorName));
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
		new Exercice2_1_0();
	}

}