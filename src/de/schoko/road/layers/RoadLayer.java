package de.schoko.road.layers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;

import de.schoko.rendering.Graph;
import de.schoko.rendering.HUDGraph;
import de.schoko.road.CatmullRomSpline;
import de.schoko.road.Constants;
import de.schoko.road.Vector2D;

public class RoadLayer extends Layer {
	private CatmullRomSpline catmullRomSpline;
	
	public RoadLayer(CatmullRomSpline catmullRomSpline) {
		this.catmullRomSpline = catmullRomSpline;
	}
	
	@Override
	public void update(double deltaTime) {
		
	}

	@Override
	public void draw(Graph g) {
		Stroke prevStroke = g.getAWTGraphics().getStroke();
		if (Constants.DRAW_ROAD) {
			if (Constants.RENDER_QUALITY.hasSmoothEdges()) {
				g.getAWTGraphics().setStroke(new BasicStroke(g.convSLW(0.25f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
			} else {
				g.getAWTGraphics().setStroke(new BasicStroke(g.convSLW(0.25f)));
			}
			
			Vector2D[] points = new Vector2D[18];
			Vector2D point;
			for (double t = 0; t < catmullRomSpline.getMaxT(); t += Constants.RENDER_QUALITY.getRoadDetail()) {
				point = catmullRomSpline.getPoint(t);
				if (!onScreen(g, point, Constants.RENDER_QUALITY.getRoadOutOfBoundsHide())) continue;
				Vector2D derivative = catmullRomSpline.getDerivative(t).normalize().multiply(Constants.ROAD_WIDTH);
				Vector2D offDeriv = derivative.rotate(Math.toRadians(90));
				Vector2D[] newPoints = new Vector2D[points.length];
				for (int j = 0; j < newPoints.length; j++) {
					newPoints[j] = point.add(offDeriv.multiply(-1 + 2 * (((double) j) / newPoints.length)));
				}
				if (points[0] != null) {
					for (int j = 0; j < newPoints.length; j++) {
						drawLine(g, newPoints[j], points[j], Color.GRAY);
					}
				}
				points = newPoints;
			}
			Vector2D lastLeftLine = null;
			Vector2D lastRightLine = null;
			Vector2D lastPoint = null;
			for (double t = 0; t < catmullRomSpline.getMaxT(); t += Constants.RENDER_QUALITY.getRoadDetail()) {
				point = catmullRomSpline.getPoint(t);
				
				if (lastPoint != null) minimapDrawLine(g, lastPoint, point, Graph.getColor(255, 255, 255));
				lastPoint = point;
				
				Vector2D derivative = catmullRomSpline.getDerivative(t).normalize().multiply(Constants.ROAD_WIDTH);
				Vector2D offDeriv = derivative.rotate(Math.toRadians(90));
				Vector2D leftLine = point.add(offDeriv);
				Vector2D rightLine = point.add(offDeriv.multiply(-1));
				
				if (lastLeftLine != null) {
					drawLine(g, leftLine, lastLeftLine, Color.WHITE);
					drawLine(g, rightLine, lastRightLine, Color.WHITE);
				}
				lastLeftLine = leftLine;
				lastRightLine = rightLine;
			}
		} else {
			for (double t = 0; t < catmullRomSpline.getMaxT(); t += 0.1) {
				Vector2D point = catmullRomSpline.getPoint(t);
				drawPoint(g, point);
			}
			if (Constants.RENDER_QUALITY.hasSmoothEdges()) {
				g.getAWTGraphics().setStroke(new BasicStroke(g.convSLW(0.1f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			}
			for (int i = 0; i < catmullRomSpline.getPointAmount() - 3; i++) {
				Vector2D point = catmullRomSpline.getPoints().get(i);
				Vector2D derivative = catmullRomSpline.getDerivative(i).normalize();
				Vector2D offDerivative = derivative.rotate(Math.toRadians(90));
				drawPoint(g, point, Color.GREEN);
				drawLine(g, point, derivative.add(point), Color.RED, 0.1f);
				drawLine(g, point, offDerivative.add(point), Color.GREEN, 0.1f);
			}
		}
		g.getAWTGraphics().setStroke(prevStroke);
	}
	
	public boolean inBounds(Vector2D pos) {
		return getNearestT(pos) != -1;
	}
	
	/**
	 * Returns the nearest t or -1 when the pos is out of bounds
	 */
	public double getNearestT(Vector2D pos) {
		for (int i = 0; i < catmullRomSpline.getMaxT(); i++) {
			//if (pos.subtract(catmullRomSpline.getPoints().get(i)).getLengthSQ() > 100) continue;
			for (double t = 0; t < 1; t += 0.1) {
				Vector2D v = catmullRomSpline.getInternalPoint(t, i);
				if (pos.subtract(v).getLengthSQ() < Constants.ROAD_BOUNDS_WIDTH * Constants.ROAD_BOUNDS_WIDTH) {
					return i + t;
				}
			}
			
		}
		return -1;
	}

	public boolean onScreen(Graph g, Vector2D v, int bounds) {
		return (g.convSX(v.getX()) > -bounds && g.convSY(v.getY()) > -bounds && 
				g.convSX(v.getX()) < g.getHUD().getWidth() + bounds && g.convSY(v.getY()) < g.getHUD().getHeight() + bounds);
	}
	
	public void drawPoint(Graph g, Vector2D v) {
		drawPoint(g, v, Color.YELLOW);
	}
	
	public void drawPoint(Graph g, Vector2D v, Color color) {
		if (!onScreen(g, v, 20)) return;
		g.drawCircle(v.getX(), v.getY(), color, 0.1);
	}

	public void drawLine(Graph g, Vector2D v, Color color) {
		if (!onScreen(g, v, 20)) return;
		g.drawLine(0, 0, v.getX(), v.getY(), color);
	}
	
	/**
	 * Draws a line with a width of 0.25f and the parameters
	 */
	public void drawLine(Graph g, Vector2D v0, Vector2D v1, Color color) {
		drawLine(g, v0, v1, color, 0.25f);
	}

	public void drawLine(Graph g, Vector2D v0, Vector2D v1, Color color, float width) {
		if (Math.abs(v0.getX() - v1.getX()) > 1.5) return;
		if (Math.abs(v0.getY() - v1.getY()) > 1.5) return;
		
		g.drawLine(v0.getX(), v0.getY(), v1.getX(), v1.getY(), color);
	}

	public void minimapDrawPoint(Graph g, Vector2D v, Color color) {
		HUDGraph hud = g.getHUD();
		hud.drawCircle(hud.getWidth() - 80 + v.getX() * 4,
					 hud.getHeight() - 80 - v.getY() * 4,
					 10, color);
	}
	
	public void minimapDrawLine(Graph g, Vector2D v0, Vector2D v1, Color color) {
		HUDGraph hud = g.getHUD();
		if (Constants.RENDER_QUALITY.getRoadOutOfBoundsHide() < 150) {
			if (Math.abs(g.convSX(v0.getX()) - g.convSX(v1.getX())) < 1) return;
			if (Math.abs(g.convSY(v0.getY()) - g.convSY(v1.getY())) < 1) return;
		}
		hud.drawLine(hud.getWidth() - 80 + v0.getX() * 4,
					 hud.getHeight() - 80 - v0.getY() * 4,
					 hud.getWidth() - 80 + v1.getX() * 4,
					 hud.getHeight() - 80 - v1.getY() * 4,
					 color, 10f);
	}

	public CatmullRomSpline getCatmullRomSpline() {
		return catmullRomSpline;
	}
}
