/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.autoreferee;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import edu.tigers.autoreferee.engine.calc.PossibleGoalCalc.PossibleGoal;
import edu.tigers.autoreferee.generic.BotPosition;
import edu.tigers.autoreferee.generic.TeamData;
import edu.tigers.autoreferee.generic.TimedPosition;
import edu.tigers.sumatra.drawable.ShapeMap;
import edu.tigers.sumatra.math.vector.IVector2;
import edu.tigers.sumatra.referee.data.GameState;
import edu.tigers.sumatra.referee.data.RefereeMsg;
import edu.tigers.sumatra.wp.data.SimpleWorldFrame;


/**
 * The autoRef frame contains information that are required for further processing
 */
public interface IAutoRefFrame
{
	IAutoRefFrame getPreviousFrame();
	
	
	SimpleWorldFrame getWorldFrame();
	
	
	GameState getGameState();
	
	
	List<BotPosition> getBotsLastTouchedBall();
	
	
	List<BotPosition> getBotsTouchingBall();
	
	
	Optional<TimedPosition> getBallLeftFieldPos();
	
	
	boolean isBallInsideField();
	
	
	IVector2 getLastStopBallPosition();
	
	
	RefereeMsg getRefereeMsg();
	
	
	Set<TeamData> getTeamInfo();
	
	
	/**
	 * Returns a list of a specified number of previous game states as well as the current one
	 * 
	 * @return the list, not empty, unmodifiable, the current state has the index 0
	 */
	List<GameState> getStateHistory();
	
	
	/**
	 * @return timestamp in ns
	 */
	long getTimestamp();
	
	
	/**
	 * Clean up reference to previous frame
	 */
	void cleanUp();
	
	
	ShapeMap getShapes();
	
	
	Optional<PossibleGoal> getPossibleGoal();
}
