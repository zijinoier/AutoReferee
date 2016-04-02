/*
 * *********************************************************
 * Copyright (c) 2009 - 2016, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: Feb 7, 2016
 * Author(s): "Lukas Magel"
 * *********************************************************
 */
package edu.tigers.autoreferee.engine.states.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.g3force.configurable.Configurable;

import edu.tigers.autoreferee.AutoRefConfig;
import edu.tigers.autoreferee.IAutoRefFrame;
import edu.tigers.autoreferee.engine.AutoRefMath;
import edu.tigers.autoreferee.engine.RefCommand;
import edu.tigers.autoreferee.engine.states.IAutoRefStateContext;
import edu.tigers.sumatra.Referee.SSL_Referee.Command;
import edu.tigers.sumatra.drawable.IDrawableShape;
import edu.tigers.sumatra.wp.data.Geometry;
import edu.tigers.sumatra.wp.data.SimpleWorldFrame;
import edu.tigers.sumatra.wp.data.TrackedBall;
import edu.tigers.sumatra.wp.vis.EWpShapesLayer;


/**
 * This rule waits for the teams to settle and take their positions before a kickoff. When all bots are on their sides
 * and the ball/bots are stationary the kickoff command is issued.
 * 
 * @author "Lukas Magel"
 */
public class PrepareKickoffState extends AbstractAutoRefState
{
	@Configurable(comment = "The minimum time to wait before sending the kickoff signal in ms")
	private static long	MIN_WAIT_TIME_MS		= 3_500;
	
	@Configurable(comment = "The minimum time to wait before sending the kickoff signal in ms")
	private static long	READY_WAIT_TIME_MS	= 1_500;
	
	private Long			readyWaitTime			= null;
	static
	{
		AbstractAutoRefState.registerClass(PrepareKickoffState.class);
	}
	
	
	/**
	 *
	 */
	public PrepareKickoffState()
	{
	}
	
	
	@Override
	public void doUpdate(final IAutoRefFrame frame, final IAutoRefStateContext ctx)
	{
		if (!timeElapsedSinceEntry(MIN_WAIT_TIME_MS))
		{
			return;
		}
		setCanProceed(true);
		
		List<IDrawableShape> shapes = frame.getShapes().get(EWpShapesLayer.AUTOREFEREE);
		SimpleWorldFrame wFrame = frame.getWorldFrame();
		TrackedBall ball = wFrame.getBall();
		
		boolean ballIsStationary = AutoRefMath.ballIsStationary(ball);
		boolean ballIsPlaced = checkBallPlaced(ball, Geometry.getCenter(), shapes);
		boolean ballInsideField = Geometry.getField().isPointInShape(ball.getPos());
		boolean maxUnplacedWaitTimeElapsed = timeElapsedSinceEntry(AutoRefConfig.getMaxUnplacedWaitTime());
		
		boolean ballIsCloselyPlaced = AutoRefMath.ballIsCloselyPlaced(ball, Geometry.getCenter());
		boolean closelyPlacedWaitTimeElapsed = timeElapsedSinceEntry(AutoRefConfig.getMaxCloselyPlacedWaitTime());
		
		boolean botsStationary = checkBotsStationary(frame, shapes);
		boolean botPosCorrect = checkBotsOnCorrectSide(frame, shapes);
		
		boolean readyWaitTimeOver = false;
		
		if ((ballIsPlaced && botsStationary && botPosCorrect)
				|| (ballInsideField && ballIsStationary && maxUnplacedWaitTimeElapsed && (AutoRefConfig
						.getMaxUnplacedWaitTime() > 0))
				|| (ballIsCloselyPlaced && ballIsStationary && closelyPlacedWaitTimeElapsed && (AutoRefConfig
						.getMaxCloselyPlacedWaitTime() > 0)))
		{
			if (readyWaitTime == null)
			{
				readyWaitTime = frame.getTimestamp();
			}
			long waitTime_ms = TimeUnit.NANOSECONDS.toMillis(frame.getTimestamp() - readyWaitTime);
			readyWaitTimeOver = waitTime_ms > READY_WAIT_TIME_MS;
			drawReadyCircle((int) ((waitTime_ms * 100) / READY_WAIT_TIME_MS), ball.getPos(), shapes);
		} else
		{
			readyWaitTime = null;
		}
		
		if (readyWaitTimeOver || ctx.doProceed())
		{
			ctx.sendCommand(new RefCommand(Command.NORMAL_START));
		}
	}
	
	
	@Override
	protected void doReset()
	{
		readyWaitTime = null;
	}
}