package graphicLayer;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;

public class GImage extends GElement {
	Point position;
	Image rawImage;
	String sourcePath;
	
	public GImage(Image image) {
		this.position = new Point(0,0);
		this.rawImage = image;
	}
	
	public Point getPosition() {
		return position;
	}
	
	public void setPosition(Point p) {
		position = p;
		repaint();		
	}

	public Image getRawImage() {
		return rawImage;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getSourcePath() {
		return sourcePath;
	}
	
	public void translate(Point gap) {
		Point p = getPosition();
		this.setPosition(new Point(p.x+gap.x, p.y+gap.y));
	}
	
	@Override
	public void draw(Graphics2D g) {
		g.drawImage(rawImage, getPosition().x, getPosition().y, null);
	}

}
