/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.autoreferee.engine.events.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import edu.tigers.autoreferee.AutoRefUtil.ColorFilter;
import edu.tigers.autoreferee.IAutoRefFrame;
import edu.tigers.autoreferee.engine.AutoRefMath;
import edu.tigers.autoreferee.engine.FollowUpAction;
import edu.tigers.autoreferee.engine.FollowUpAction.EActionType;
import edu.tigers.autoreferee.engine.NGeometry;
import edu.tigers.autoreferee.engine.events.DistanceViolation;
import edu.tigers.autoreferee.engine.events.EGameEvent;
import edu.tigers.autoreferee.engine.events.EGameEventDetectorType;
import edu.tigers.autoreferee.engine.events.IGameEvent;
import edu.tigers.sumatra.geometry.Geometry;
import edu.tigers.sumatra.geometry.IPenaltyArea;
import edu.tigers.sumatra.geometry.RuleConstraints;
import edu.tigers.sumatra.ids.ETeamColor;
import edu.tigers.sumatra.math.line.v2.Lines;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.referee.data.EGameState;
import edu.tigers.sumatra.referee.data.GameState;
import edu.tigers.sumatra.wp.data.ITrackedBall;
import edu.tigers.sumatra.wp.data.ITrackedBot;


/**
 * Monitors the distance between attackers and the defense area of the defending team when a freekick is performed.
 * 
 * @author Lukas Magel
 */
public class AttackerToDefenseAreaDistanceDetector extends APreparingGameEventDetector
{
	private static final int PRIORITY = 1;
	
	private static final double INACCURACY_TOLERANCE = 15;
	private static final Set<EGameState> ALLOWED_PREVIOUS_STATES;
	
	private boolean active = false;
	
	static
	{
		Set<EGameState> states = EnumSet.of(
				EGameState.INDIRECT_FREE, EGameState.DIRECT_FREE, EGameState.KICKOFF);
		ALLOWED_PREVIOUS_STATES = Collections.unmodifiableSet(states);
	}
	
	
	/**
	 * Default constructor
	 */
	public AttackerToDefenseAreaDistanceDetector()
	{
		super(EGameEventDetectorType.ATTACKER_TO_DEFENSE_DISTANCE, EGameState.RUNNING);
	}
	
	
	@Override
	public int getPriority()
	{
		return PRIORITY;
	}
	
	
	@Override
	protected void prepare(final IAutoRefFrame frame)
	{
		List<GameState> stateHistory = frame.getStateHistory();
		
		active = (stateHistory.size() > 1) && ALLOWED_PREVIOUS_STATES.contains(stateHistory.get(1).getState());
	}
	
	
	@Override
	public Optional<IGameEvent> doUpdate(final IAutoRefFrame frame)
	{
		if (active)
		{
			active = false;
			
			GameState lastGameState = frame.getStateHistory().get(1);
			ETeamColor attackerColor = lastGameState.getForTeam();
			
			return new Evaluator(frame, attackerColor).evaluate();
		}
		
		return Optional.empty();
	}
	
	
	@Override
	public void doReset()
	{
		// Nothing to reset
	}
	
	/**
	 * The actual evaluation of this detector is encapsulated in this class and executed only once when the gamestate
	 * changes to running. Since this class is created when the rule is to be evaluated all required values can be stored
	 * as private final attributes. This makes the code cleaner because these values do not need to be passed around
	 * between different functions.
	 * 
	 * @author "Lukas Magel"
	 */
	private static class Evaluator
	{
		private final double requiredMargin = RuleConstraints.getBotToPenaltyAreaMarginStandard()
				+ Geometry.getBotRadius()
				- INACCURACY_TOLERANCE;
		
		private final IAutoRefFrame frame;
		private final ETeamColor attackerColor;
		private final IPenaltyArea defenderPenArea;
		
		private final ITrackedBall ball;
		private final Collection<ITrackedBot> bots;
		
		
		public Evaluator(final IAutoRefFrame frame, final ETeamColor attackerColor)
		{
			this.frame = frame;
			this.attackerColor = attackerColor;
			defenderPenArea = NGeometry.getPenaltyArea(attackerColor.opposite());
			ball = frame.getWorldFrame().getBall();
			bots = frame.getWorldFrame().getBots().values();
		}
		
		
		private Optional<IGameEvent> evaluate()
		{
			Optional<ITrackedBot> optOffender = bots.stream()
					.filter(ColorFilter.get(attackerColor))
					.filter(bot -> defenderPenArea.isPointInShape(bot.getPos(), requiredMargin))
					.filter(this::notBeingPushed)
					.findFirst();
			
			return optOffender.map(this::buildViolation);
		}
		
		
		private boolean notBeingPushed(ITrackedBot bot)
		{
			ETeamColor defenderColor = attackerColor.opposite();
			return !bots.stream()
					// bots from defending team
					.filter(ColorFilter.get(defenderColor))
					// that touch the attacker
					.filter(b -> bot.getPos().distanceTo(b.getPos()) <= Geometry.getBotRadius() * 2)
					// push in direction of penalty area
					.map(b -> Lines.halfLineFromPoints(b.getPos(), bot.getPos()))
					// find intersection that show that defenders pushs towards penArea
					.map(defenderPenArea::lineIntersections)
					.flatMap(List::stream)
					.findAny()
					// if any intersection is present, some defender pushes the attacker
					.isPresent();
		}
		
		
		private DistanceViolation buildViolation(final ITrackedBot offender)
		{
			double distance = AutoRefMath
					.distanceToNearestPointOutside(defenderPenArea, requiredMargin, offender.getPos());
			
			IVector2 kickPos = AutoRefMath.getClosestFreeKickPos(ball.getPos(), offender.getTeamColor().opposite());
			FollowUpAction followUp = new FollowUpAction(EActionType.INDIRECT_FREE, attackerColor.opposite(), kickPos);
			return new DistanceViolation(EGameEvent.ATTACKER_TO_DEFENCE_AREA, frame.getTimestamp(),
					offender.getBotId(), followUp, distance);
		}
	}
	
}
