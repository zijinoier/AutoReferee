/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.math.circle;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;

import com.google.common.collect.Streams;
import com.sleepycat.persist.model.Persistent;

import edu.tigers.sumatra.math.SumatraMath;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.math.vector.Vector2f;
import edu.tigers.sumatra.stream.StreamUtil;


/**
 * Geometrical representation of a circle.
 * 
 * @author Malte
 */
@Persistent(version = 1)
public class Circle extends ACircle
{
	private final Vector2f center;
	private final double radius;
	
	
	protected Circle()
	{
		this(Vector2f.ZERO_VECTOR, 1);
	}
	
	
	/**
	 * @param circle a circle
	 */
	protected Circle(final ICircle circle)
	{
		this(circle.center(), circle.radius());
	}
	
	
	/**
	 * Defines a circle by a radius and a center.
	 * Radius must not be negative or zero!
	 * 
	 * @param center
	 * @param radius
	 * @throws IllegalArgumentException if the radius is not real positive
	 */
	protected Circle(final IVector2 center, final double radius)
	{
		if (radius <= 0)
		{
			throw new IllegalArgumentException("Radius of a circle must be larger than zero!");
		}
		this.center = Vector2f.copy(center);
		this.radius = radius;
	}
	
	
	/**
	 * Create the smallest circle from 2 points on the arc (no center given).
	 * Center is in the middle between the two points.
	 *
	 * @param p1 First point
	 * @param p2 Second point
	 * @return The unique circle going through all given points.
	 */
	public static ICircle from2Points(final IVector2 p1, final IVector2 p2)
	{
		IVector2 center = p1.addNew(p2).multiply(0.5);
		double radius = center.distanceTo(p1);
		
		return createCircle(center, radius);
	}
	
	
	/**
	 * Create a circle from 3 points on the arc (no center given).
	 *
	 * @param p1 First point
	 * @param p2 Second point
	 * @param p3 Third point
	 * @return The unique circle going through all given points, if this is possible
	 */
	public static Optional<ICircle> from3Points(final IVector2 p1, final IVector2 p2, final IVector2 p3)
	{
		return fromNPoints(Arrays.asList(p1, p2, p3));
	}
	
	
	/**
	 * Create a circle from N points on the arc (no center given).
	 *
	 * @param points
	 * @return The best matching circle for all given points, if this is possible
	 */
	public static Optional<ICircle> fromNPoints(final List<IVector2> points)
	{
		if (points.size() < 2)
		{
			throw new IllegalArgumentException("At least 2 points required");
		}
		if (points.size() == 2)
		{
			// the implementation does not work for 2 points, so use #from2Points
			return Optional.of(from2Points(points.get(0), points.get(1)));
		}
		RealMatrix qrA = new Array2DRowRealMatrix(points.size(), 3);
		RealVector qrB = new ArrayRealVector(points.size());
		for (int i = 0; i < points.size(); i++)
		{
			IVector2 p = points.get(i);
			qrA.setEntry(i, 0, 1);
			qrA.setEntry(i, 1, p.x());
			qrA.setEntry(i, 2, p.y());
			qrB.setEntry(i, (p.x() * p.x()) + (p.y() * p.y()));
		}
		
		DecompositionSolver solver = new QRDecomposition(qrA).getSolver();
		
		RealVector solution;
		try
		{
			solution = solver.solve(qrB);
		} catch (@SuppressWarnings("squid:S1166") SingularMatrixException err)
		{
			return Optional.empty();
		}
		
		RealVector center = solution.getSubVector(1, 2).mapMultiplyToSelf(0.5);
		
		double sq = center.ebeMultiply(center).getL1Norm() + solution.getEntry(0);
		double radius = SumatraMath.sqrt(sq);
		
		return Optional.of(createCircle(Vector2.fromReal(center), radius));
	}
	
	
	/**
	 * Create a circle that encloses all points in the list.
	 * This method is not the most effective one and should not be used in match mode.
	 *
	 * @param points
	 * @return Smallest circle enclosing all points.
	 */
	public static Optional<ICircle> hullCircle(final List<IVector2> points)
	{
		return Streams
				.concat(StreamUtil.nonRepeatingPermutation2Fold(points), StreamUtil.nonRepeatingPermutation3Fold(points))
				.map(Circle::fromNPoints)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.filter(c -> points.stream().allMatch(p -> c.isPointInShape(p, 1e-10)))
				.min(Comparator.comparing(ICircle::radius));
	}
	
	
	/**
	 * @param center
	 * @param radius
	 * @return a new circle
	 */
	public static ICircle createCircle(final IVector2 center, final double radius)
	{
		return new Circle(center, radius);
	}
	
	
	@Override
	public ICircle withMargin(final double margin)
	{
		return new Circle(center(), radius() + margin);
	}
	
	
	@Override
	public ICircle mirror()
	{
		return new Circle(center.multiplyNew(-1), radius);
	}
	
	
	@Override
	public double radius()
	{
		return radius;
	}
	
	
	@Override
	public IVector2 center()
	{
		return center;
	}
	
	
	@Override
	public final boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof ICircle))
		{
			return false;
		}
		
		final ICircle circle = (ICircle) o;
		
		return center.equals(circle.center())
				&& SumatraMath.isEqual(radius, circle.radius());
	}
	
	
	@Override
	public final int hashCode()
	{
		int result;
		long temp;
		result = center.hashCode();
		temp = Double.doubleToLongBits(radius);
		result = (31 * result) + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	
	@Override
	public String toString()
	{
		return "Circle{" +
				"center=" + center +
				", radius=" + radius +
				'}';
	}
}
