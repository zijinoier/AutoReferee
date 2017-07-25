/*
 * Copyright (c) 2009 - 2017, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.drawable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sleepycat.persist.model.Persistent;

import edu.tigers.sumatra.math.AngleMath;
import edu.tigers.sumatra.math.line.v2.ILineSegment;
import edu.tigers.sumatra.math.line.v2.Lines;
import edu.tigers.sumatra.math.tube.Tube;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;


/**
 * A Tube with a color
 *
 * @author Ulrike Leipscher <ulrike.leipscher@dlr.de>.
 */
@Persistent(version = 1)
public class DrawableTube extends ADrawableWithStroke
{
	
	private Tube tube;
	private boolean fill;
	
	
	/**
	 * for DB only
	 */
	@SuppressWarnings("unused")
	private DrawableTube()
	{
		tube = Tube.create(Vector2.zero(), Vector2.zero(), 1);
	}
	
	/**
	 * @param tube
	 */
	public DrawableTube(final Tube tube)
	{
		this.tube = tube;
	}
	
	
	/**
	 * @param tube
	 * @param color
	 */
	public DrawableTube(final Tube tube, final Color color)
	{
		this.tube = tube;
		setColor(color);
	}
	
	
	@Override
	public void paintShape(final Graphics2D g, final IDrawableTool tool, final boolean invert)
	{
		super.paintShape(g, tool, invert);
		double radius = tool.scaleXLength(tube.radius());
		final IVector2 startCenter = tool.transformToGuiCoordinates(tube.startCenter(), invert);
		final IVector2 endCenter = tool.transformToGuiCoordinates(tube.endCenter(), invert);
		ILineSegment segment = Lines.segmentFromPoints(startCenter, endCenter);
		List<IVector2> corners = new ArrayList<>();
		corners.add(startCenter.addNew(segment.directionVector().getNormalVector().scaleTo(radius)));
		corners.add(endCenter.addNew(segment.directionVector().getNormalVector().scaleTo(radius)));
		corners.add(startCenter.addNew(segment.directionVector().getNormalVector().scaleTo(-radius)));
		corners.add(endCenter.addNew(segment.directionVector().getNormalVector().scaleTo(-radius)));
		int[] xVals = new int[4];
		int[] yVals = new int[4];
		int i = 0;
		for (IVector2 point : corners)
		{
			xVals[i] = (int) point.x();
			yVals[i] = (int) point.y();
			i++;
		}
		
		if (fill)
		{
			g.fillArc((int) (startCenter.x() - radius), (int) (startCenter.y() - radius),
					(int) radius * 2, (int) radius * 2, 0, 360);
			g.fillArc((int) (endCenter.x() - radius), (int) (endCenter.y() - radius),
					(int) radius * 2, (int) radius * 2, 0, 360);
			g.fillPolygon(xVals, yVals, 4);
			return;
		}
		int startAngle = 0;
		int arcAngle = 360;
		Optional<Double> angle = segment.directionVector().getNormalVector().angleTo(Vector2.fromX(1));
		if (angle.isPresent())
		{
			startAngle = (int) AngleMath.rad2deg(angle.get());
			arcAngle = 180;
		}
		
		g.drawArc((int) (startCenter.x() - radius), (int) (startCenter.y() - radius),
				(int) radius * 2, (int) radius * 2, startAngle, arcAngle);
		g.drawArc((int) (endCenter.x() - radius), (int) (endCenter.y() - radius), (int) radius * 2,
				(int) radius * 2, -180 + startAngle, arcAngle);
		g.drawLine((int) corners.get(0).x(), (int) corners.get(0).y(), (int) corners.get(1).x(),
				(int) corners.get(1).y());
		g.drawLine((int) corners.get(2).x(), (int) corners.get(2).y(), (int) corners.get(3).x(),
				(int) corners.get(3).y());
	}
	
	
	/**
	 * @param fill
	 */
	public void setFill(final boolean fill)
	{
		this.fill = fill;
	}
	
}