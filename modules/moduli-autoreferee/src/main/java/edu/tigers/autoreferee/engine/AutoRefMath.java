/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.autoreferee.engine;

import java.util.Collection;
import java.util.stream.Stream;

import edu.tigers.autoreferee.AutoRefConfig;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.geometry.IPenaltyArea;
import edu.tigers.sumatra.geometry.RuleConstraints;
import edu.tigers.sumatra.ids.ETeamColor;
import edu.tigers.sumatra.math.rectangle.IRectangle;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.math.vector.Vector2;
import edu.tigers.sumatra.math.vector.VectorMath;
import edu.tigers.sumatra.wp.data.ITrackedBall;
import edu.tigers.sumatra.wp.data.ITrackedBot;


/**
 * Encapsulates static math operations for the autoref rules
 * 
 * @author "Lukas Magel"
 */
public class AutoRefMath
{
	private AutoRefMath()
	{
		// private default constructor
	}
	
	
	/**
	 * Determines the corner kick position that is located closest to the specified position
	 * 
	 * @param pos
	 * @return
	 */
	public static IVector2 getClosestCornerKickPos(final IVector2 pos)
	{
		IVector2 corner = NGeometry.getClosestCorner(pos);
		int ySide = corner.y() > 0 ? -1 : 1;
		int xSide = corner.x() > 0 ? -1 : 1;
		return corner.addNew(Vector2.fromXY(xSide * getThrowInDistanceWithOffset(),
				ySide * getThrowInDistanceWithOffset()));
	}
	
	
	/**
	 * Determines the corner kick position that is located closest to the specified position
	 * 
	 * @param pos
	 * @return
	 */
	public static IVector2 getClosestGoalKickPos(final IVector2 pos)
	{
		IVector2 corner = NGeometry.getClosestCorner(pos);
		int xSide = corner.x() > 0 ? -1 : 1;
		int ySide = corner.y() > 0 ? -1 : 1;
		return corner
				.addNew(Vector2.fromXY(xSide * getGoalKickDistanceWithOffset(), ySide * getThrowInDistanceWithOffset()));
	}
	
	
	/**
	 * This method checks if the specified position is located closer than
	 * {@link AutoRefMath#getOffenseFreeKickDistanceWithOffset()} to the defense area.
	 * If the freekick is to be executed by the attacking team then the ball is positioned at the
	 * closest point that is located {@link AutoRefMath#getOffenseFreeKickDistanceWithOffset()} from the defense area.
	 * If the freekick is to be executed by the defending team then the ball is positioned at one of the two corner
	 * points of the field which are located 600 mm from the goal line and 100 mm from the side line.
	 * 
	 * @param pos
	 * @param kickerColor
	 * @return
	 */
	public static IVector2 getClosestFreeKickPos(final IVector2 pos, final ETeamColor kickerColor)
	{
		IRectangle field = NGeometry.getField();
		ETeamColor goalColor = NGeometry.getTeamOfClosestGoalLine(pos);
		IVector2 newKickPos;
		if (goalColor == kickerColor)
		{
			newKickPos = getDefenseKickPos(pos);
		} else
		{
			newKickPos = getOffenseKickPos(pos);
		}
		/*
		 * Check if the ball is located too close to the touch lines or goal lines
		 */
		int xSide = newKickPos.x() > 0 ? 1 : -1;
		int ySide = newKickPos.y() > 0 ? 1 : -1;
		
		if (Math.abs(newKickPos.x()) > ((field.xExtent() / 2) - getThrowInDistanceWithOffset()))
		{
			double newXPos = ((field.xExtent() / 2) - getThrowInDistanceWithOffset()) * xSide;
			newKickPos = Vector2.fromXY(newXPos, newKickPos.y());
		}
		
		if (Math.abs(newKickPos.y()) > ((field.yExtent() / 2) - getThrowInDistanceWithOffset()))
		{
			double newYPos = ((field.yExtent() / 2) - getThrowInDistanceWithOffset()) * ySide;
			newKickPos = Vector2.fromXY(newKickPos.x(), newYPos);
		}
		
		return newKickPos;
	}
	
	
	public static IVector2 getOffenseKickPos(final IVector2 pos)
	{
		IPenaltyArea penArea = NGeometry.getPenaltyArea(NGeometry.getTeamOfClosestGoalLine(pos))
				.withMargin(getOffenseFreeKickDistanceWithOffset());
		return penArea.nearestPointOutside(pos);
	}
	
	
	private static IVector2 getDefenseKickPos(final IVector2 pos)
	{
		int xSide = pos.x() > 0 ? -1 : 1;
		IPenaltyArea penArea = NGeometry.getPenaltyArea(NGeometry.getTeamOfClosestGoalLine(pos));
		if (penArea.isPointInShape(pos, getOffenseFreeKickDistanceWithOffset()))
		{
			return getClosestGoalKickPos(pos)
					.addNew(Vector2.fromXY((getDistanceToGoaLineWithOffset() - getGoalKickDistanceWithOffset()) * xSide, 0));
		}
		return pos;
	}
	
	
	/**
	 * Checks if all bots that are located entirely inside the field area are on their own side of the field
	 * 
	 * @param bots
	 * @return true if all bots are in their half of the field
	 */
	public static boolean botsAreOnCorrectSide(final Collection<ITrackedBot> bots)
	{
		IRectangle field = Geometry.getField();
		Stream<ITrackedBot> onFieldBots = bots.stream()
				.filter(bot -> field.isPointInShape(bot.getPos(), Geometry.getBotRadius()));
		
		return onFieldBots.allMatch(bot -> {
			IRectangle side = NGeometry.getFieldSide(bot.getTeamColor());
			return side.isPointInShape(bot.getPos());
		});
	}
	
	
	/**
	 * Returns true if all bots have stopped moving
	 * 
	 * @param bots
	 * @return
	 */
	public static boolean botsAreStationary(final Collection<ITrackedBot> bots)
	{
		return bots.stream().allMatch(
				bot -> bot.getVelByTime(0).getLength() < AutoRefConfig.getBotStationarySpeedThreshold());
	}
	
	
	/**
	 * Returns true if the ball has been placed at {@code destPos} with a precision of
	 * {@link AutoRefConfig#getBallPlacementAccuracy()} and is stationary.
	 * 
	 * @param ball
	 * @param destPos
	 * @return
	 */
	public static boolean ballIsPlaced(final ITrackedBall ball, final IVector2 destPos)
	{
		return ballIsPlaced(ball, destPos, AutoRefConfig.getBallPlacementAccuracy());
	}
	
	
	/**
	 * @param ball
	 * @param destPos
	 * @param accuracy
	 * @return
	 */
	public static boolean ballIsPlaced(final ITrackedBall ball, final IVector2 destPos, final double accuracy)
	{
		double dist = VectorMath.distancePP(ball.getPos(), destPos);
		return (dist < accuracy) &&
				ballIsStationary(ball) &&
				NGeometry.getField().isPointInShape(ball.getPos());
	}
	
	
	/**
	 * Returns {@code true} if the velocity is below the {@link AutoRefConfig#getBallStationarySpeedThreshold()}
	 * threshold.
	 * 
	 * @param ball
	 * @return
	 */
	public static boolean ballIsStationary(final ITrackedBall ball)
	{
		return ball.getVel().getLength() < AutoRefConfig.getBallStationarySpeedThreshold();
	}
	
	
	/**
	 * Returns true if all bots are located further than {@link RuleConstraints#getStopRadius()} from the ball
	 * 
	 * @param bots
	 * @param ballPos
	 * @return
	 */
	public static boolean botStopDistanceIsCorrect(final Collection<ITrackedBot> bots, IVector2 ballPos)
	{
		return bots.stream().allMatch(
				bot -> VectorMath.distancePP(bot.getPos(), ballPos) >= RuleConstraints.getStopRadius() - 10);
	}
	
	
	/**
	 * Calculates the distance between the supplied {@code pos} and the edge of the penalty area.
	 * If the point does not lie inside the penalty area a distance of 0 is returned.
	 * 
	 * @param penArea
	 * @param pos
	 * @return
	 */
	public static double distanceToNearestPointOutside(final IPenaltyArea penArea, final IVector2 pos)
	{
		return distanceToNearestPointOutside(penArea, 0, pos);
	}
	
	
	/**
	 * Calculates the distance between the supplied {@code pos} and the edge of the penalty area plus the specified
	 * {@code margin}. If the point does not lie inside the penalty area plus margin a distance of 0 is returned.
	 * 
	 * @param penArea
	 * @param margin
	 * @param pos
	 * @return
	 */
	public static double distanceToNearestPointOutside(final IPenaltyArea penArea, final double margin,
			final IVector2 pos)
	{
		IVector2 nearestPointOutsideMargin = penArea.withMargin(margin).nearestPointOutside(pos);
		return VectorMath.distancePP(pos, nearestPointOutsideMargin);
	}
	
	
	/**
	 * Determines if the position is located inside the penalty kick area of the goal that belongs to the defending team,
	 * i.e. the team not executing the penalty kick.
	 * 
	 * @param execTeam The team performing the penalty kick, i.e. offensive team
	 * @param pos The position to be checked
	 * @param margin margin around the kick area, i.e. a positive margin (>0) equals a larger kick area
	 * @return true if the position is located inside the restricted area
	 */
	public static boolean positionInPenaltyKickArea(final ETeamColor execTeam, final IVector2 pos, final double margin)
	{
		double kickAreaX = Geometry.getPenaltyMarkTheir().x()
				- RuleConstraints.getDistancePenaltyMarkToPenaltyLine();
		int side = execTeam == Geometry.getNegativeHalfTeam() ? 1 : -1;
		
		return (pos.x() * side) >= (kickAreaX - margin);
	}
	
	
	public static double getThrowInDistanceWithOffset()
	{
		return RuleConstraints.getThrowInDistance() + AutoRefConfig.getPlacementOffset();
	}
	
	
	public static double getDistanceToGoaLineWithOffset()
	{
		return RuleConstraints.getDistanceToGoalLine() + +AutoRefConfig.getPlacementOffset();
	}
	
	
	public static double getOffenseFreeKickDistanceWithOffset()
	{
		return RuleConstraints.getOffenseFreeKickDistance() + +AutoRefConfig.getPlacementOffset();
	}
	
	
	public static double getGoalKickDistanceWithOffset()
	{
		return RuleConstraints.getGoalKickDistance() + AutoRefConfig.getPlacementOffset();
	}
}
