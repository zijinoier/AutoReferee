/*
 * *********************************************************
 * Copyright (c) 2009 - 2016, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: Feb 17, 2016
 * Author(s): Lukas Magel
 * *********************************************************
 */
package edu.tigers.autoreferee.engine.rules.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.g3force.configurable.Configurable;
import com.google.common.collect.Sets;

import edu.tigers.autoreferee.engine.IRuleEngineFrame;
import edu.tigers.autoreferee.engine.rules.RuleResult;
import edu.tigers.autoreferee.engine.violations.IRuleViolation.ERuleViolation;
import edu.tigers.autoreferee.engine.violations.RuleViolation;
import edu.tigers.sumatra.Referee.SSL_Referee.Command;
import edu.tigers.sumatra.ids.BotID;
import edu.tigers.sumatra.ids.ETeamColor;
import edu.tigers.sumatra.ids.IBotIDMap;
import edu.tigers.sumatra.math.IVector2;
import edu.tigers.sumatra.shapes.circle.Circle;
import edu.tigers.sumatra.wp.data.EGameStateNeutral;
import edu.tigers.sumatra.wp.data.Geometry;
import edu.tigers.sumatra.wp.data.ITrackedBot;


/**
 * This rule monitors the bot to ball distance of the defending team during a freekick situation and restarts the play
 * if necessary.
 * 
 * @author Lukas Magel
 */
public class DefenderToKickPointDistanceRule extends APreparingGameRule
{
	private static final int	priority								= 1;
	
	@Configurable(comment = "Activates/deactivates this rule")
	private static boolean		active								= true;
	
	@Configurable(comment = "If disabled only bots that are on a collision course with the ball will be considered violators")
	private static boolean		STRICT_MODE							= true;
	
	@Configurable(comment = "The amount of time in ms a bot can be located inside the outer circle (500mm>x>250mm from the kick pos) without logging a violation")
	private long					MAX_OUTER_CIRCLE_LINGER_TIME	= 3_000;
	
	private IVector2				ballPos								= null;
	private Set<BotID>			lastViolators						= new HashSet<>();
	private Map<BotID, Long>	outerCircleBots					= new HashMap<>();
	
	static
	{
		AGameRule.registerClass(DefenderToKickPointDistanceRule.class);
	}
	
	
	/**
	 * 
	 */
	public DefenderToKickPointDistanceRule()
	{
		super(Arrays.asList(
				EGameStateNeutral.DIRECT_KICK_BLUE, EGameStateNeutral.DIRECT_KICK_YELLOW,
				EGameStateNeutral.INDIRECT_KICK_BLUE, EGameStateNeutral.INDIRECT_KICK_YELLOW,
				EGameStateNeutral.KICKOFF_BLUE, EGameStateNeutral.KICKOFF_YELLOW));
	}
	
	
	@Override
	public int getPriority()
	{
		return priority;
	}
	
	
	@Override
	protected void prepare(final IRuleEngineFrame frame)
	{
		ballPos = frame.getWorldFrame().getBall().getPos();
	}
	
	
	@Override
	protected Optional<RuleResult> doUpdate(final IRuleEngineFrame frame)
	{
		if (active == false)
		{
			doReset();
			return Optional.empty();
		}
		
		Set<BotID> curViolators = getViolators(frame);
		Set<BotID> newViolators = Sets.difference(curViolators, lastViolators);
		
		/*
		 * Remove all old violators which are still in the set
		 */
		lastViolators.removeAll(Sets.difference(lastViolators, curViolators));
		
		Optional<BotID> optViolator = newViolators.stream().findFirst();
		
		if (optViolator.isPresent())
		{
			BotID violator = optViolator.get();
			lastViolators.add(violator);
			RuleViolation violation = new RuleViolation(ERuleViolation.DEFENDER_TO_KICK_POINT_DISTANCE,
					frame.getTimestamp(), violator);
			return Optional.of(new RuleResult(Command.STOP, frame.getFollowUp().orElse(null), violation));
		}
		
		return Optional.empty();
	}
	
	
	private Set<BotID> getViolators(final IRuleEngineFrame frame)
	{
		EGameStateNeutral state = frame.getGameState();
		ETeamColor attackingColor = state.getTeamColor();
		
		IBotIDMap<ITrackedBot> bots = frame.getWorldFrame().getBots();
		List<ITrackedBot> defendingBots = bots.values().stream()
				.filter(bot -> bot.getBotId().getTeamColor() == attackingColor.opposite())
				.collect(Collectors.toList());
		
		
		Set<BotID> violators = new HashSet<>();
		long curTimestamp = frame.getTimestamp();
		/*
		 * Only consider bots which have fully entered the circle
		 */
		Circle outerCircle = new Circle(ballPos, Geometry.getBotToBallDistanceStop() - Geometry.getBotRadius());
		Circle innerCircle = new Circle(ballPos, Geometry.getBotToBallDistanceStop() / 2);
		
		if (STRICT_MODE == true)
		{
			violators.addAll(botsInCircle(defendingBots, outerCircle));
		} else
		{
			Set<BotID> innerCircleViolators = botsInCircle(defendingBots, innerCircle);
			violators.addAll(innerCircleViolators);
			
			Set<BotID> outerCircleViolators = Sets.difference(botsInCircle(defendingBots, outerCircle),
					innerCircleViolators);
			Set<BotID> newViolators = Sets.difference(outerCircleViolators, outerCircleBots.keySet());
			Set<BotID> oldViolators = Sets.difference(outerCircleBots.keySet(), outerCircleViolators);
			
			newViolators.forEach(id -> outerCircleBots.put(id, curTimestamp));
			oldViolators.forEach(id -> outerCircleBots.remove(id));
			
			outerCircleBots.forEach((id, entryTimestamp) -> {
				if ((curTimestamp - entryTimestamp) > TimeUnit.MILLISECONDS.toNanos(MAX_OUTER_CIRCLE_LINGER_TIME))
				{
					violators.add(id);
				}
			});
		}
		
		return violators;
	}
	
	
	private Set<BotID> botsInCircle(final List<ITrackedBot> bots, final Circle circle)
	{
		return bots.stream()
				.filter(bot -> circle.isPointInShape(bot.getPos()))
				.map(bot -> bot.getBotId())
				.collect(Collectors.toSet());
	}
	
	
	@Override
	protected void doReset()
	{
		lastViolators.clear();
		outerCircleBots.clear();
	}
}