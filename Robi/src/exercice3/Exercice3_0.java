package exercice3;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import graphicLayer.GRect;
import graphicLayer.GSpace;
import stree.parser.SNode;
import stree.parser.SParser;
import tools.Tools;

public class Exercice3_0 {
	GSpace space = new GSpace("Exercice 3", new Dimension(200, 100));
	GRect robi = new GRect();
	String script = "" +
	"   (space setColor black) " +
	"   (robi setColor yellow)" +
	"   (space sleep 1000)" +
	"   (space setColor white)\n" + 
	"   (space sleep 1000)" +
	"	(robi setColor red) \n" + 
	"   (space sleep 1000)" +
	"	(robi translate 100 0)\n" + 
	"	(space sleep 1000)\n" + 
	"	(robi translate 0 50)\n" + 
	"	(space sleep 1000)\n" + 
	"	(robi translate -100 0)\n" + 
	"	(space sleep 1000)\n" + 
	"	(robi translate 0 -40)";

	public Exercice3_0() {
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
		Command cmd = getCommandFromExpr(expr);
		if (cmd == null)
			throw new Error("unable to get command for: " + expr);
		cmd.run();
	}

	Command getCommandFromExpr(SNode expr) {
		checkHasMinArity(expr, 2);
		String target = getAtom(expr, 0);
		String commandName = getAtom(expr, 1);

		if (target.equals("space")) {
			if (commandName.equals("setColor") || commandName.equals("color")) {
				checkArity(expr, 3);
				return new SpaceChangeColor(getColorArg(expr, 2));
			}
			if (commandName.equals("sleep")) {
				checkArity(expr, 3);
				return new SpaceSleep(getIntArg(expr, 2));
			}
			throw new Error("Invalid space command: " + expr);
		}

		if (target.equals("robi")) {
			if (commandName.equals("setColor") || commandName.equals("color")) {
				checkArity(expr, 3);
				return new RobiChangeColor(getColorArg(expr, 2));
			}
			if (commandName.equals("translate")) {
				checkArity(expr, 4);
				return new RobiTranslate(getIntArg(expr, 2), getIntArg(expr, 3));
			}
			throw new Error("Invalid robi command: " + expr);
		}

		throw new Error("Unknown target in command: " + expr);
	}

	public static void main(String[] args) {
		new Exercice3_0();
	}

	public interface Command {
		abstract public void run();
	}

	private void checkHasMinArity(SNode expr, int minSize) {
		if (expr == null || !expr.isNode() || expr.size() < minSize) {
			throw new Error("Invalid command syntax: " + expr);
		}
	}

	private void checkArity(SNode expr, int expectedSize) {
		if (expr == null || !expr.isNode() || expr.size() != expectedSize) {
			throw new Error("Invalid command syntax: " + expr);
		}
	}

	private String getAtom(SNode expr, int index) {
		SNode atom = expr.get(index);
		if (atom == null || !atom.isLeaf() || atom.contents() == null) {
			throw new Error("Expected atom at position " + index + " in " + expr);
		}
		return atom.contents();
	}

	private int getIntArg(SNode expr, int index) {
		try {
			return Integer.parseInt(getAtom(expr, index));
		} catch (NumberFormatException e) {
			throw new Error("Expected integer at position " + index + " in " + expr);
		}
	}

	private Color getColorArg(SNode expr, int index) {
		Color color = Tools.getColorByName(getAtom(expr, index));
		if (color == null) {
			throw new Error("Unknown color in command: " + expr);
		}
		return color;
	}

	public class SpaceChangeColor implements Command {
		Color newColor;

		public SpaceChangeColor(Color newColor) {
			this.newColor = newColor;
		}

		@Override
		public void run() {
			space.setColor(newColor);
		}
	}

	public class SpaceSleep implements Command {
		int millis;

		public SpaceSleep(int millis) {
			this.millis = millis;
		}

		@Override
		public void run() {
			Tools.sleep(millis);
		}
	}

	public class RobiChangeColor implements Command {
		Color newColor;

		public RobiChangeColor(Color newColor) {
			this.newColor = newColor;
		}

		@Override
		public void run() {
			robi.setColor(newColor);
		}
	}

	public class RobiTranslate implements Command {
		Point gap;

		public RobiTranslate(int deltaX, int deltaY) {
			this.gap = new Point(deltaX, deltaY);
		}

		@Override
		public void run() {
			robi.translate(gap);
		}
	}
}