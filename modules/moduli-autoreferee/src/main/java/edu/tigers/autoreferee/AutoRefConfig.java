/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.autoreferee;

import java.util.ArrayList;
import java.util.List;

import com.github.g3force.configurable.ConfigRegistration;
import com.github.g3force.configurable.Configurable;

import edu.tigers.sumatra.ids.ETeamColor;


/**
 * Static class which contains configuration parameters that can be changed through the Configurable interface
 * 
 * @author "Lukas Magel"
 */
public class AutoRefConfig
{
	@Configurable(comment = "Enable ball placement calls for the blue teams", defValue = "true")
	private static boolean ballPlacementBlueEnabled = true;
	
	@Configurable(comment = "Enable ball placement calls for the yellow teams", defValue = "true")
	private static boolean ballPlacementYellowEnabled = true;
	
	@Configurable(comment = "[mm] The accuracy with which the ball needs to be placed by the human referee", defValue = "200.0")
	private static double ballPlacementAccuracy = 200;
	
	@Configurable(comment = "[mm] The accuracy with which the ball needs to be placed by the robots", defValue = "100.0")
	private static double robotBallPlacementAccuracy = 100;
	
	@Configurable(comment = "[m/s] The maximum bot velocity during game stoppage", defValue = "1.5")
	private static double maxBotStopSpeed = 1.5;
	
	@Configurable(comment = "[m/s] The velocity below which a bot is considered to be stationary", defValue = "0.3")
	private static double botStationarySpeedThreshold = 0.3;
	
	@Configurable(comment = "[m/s] The velocity below which the ball is considered to be stationary", defValue = "0.2")
	private static double ballStationarySpeedThreshold = 0.2;
	
	@Configurable(comment = "[ms] The time each team has to place the ball", defValue = "30000")
	private static int ballPlacementWindow = 30_000;
	
	@Configurable(comment = "The hostname/ip address of the refbox", defValue = "localhost")
	private static String refboxHostname = "localhost";
	
	@Configurable(comment = "The port which will be used to connect to the refbox", defValue = "10007")
	private static int refboxPort = 10007;
	
	@Configurable(defValue = "200.0", comment = "[mm] offset to placement positions for automatic ball placement")
	private static double placementOffset = 200.0;
	
	static
	{
		ConfigRegistration.registerClass("autoreferee", AutoRefConfig.class);
	}
	
	
	private AutoRefConfig()
	{
		// hide public constructor
	}
	
	
	/**
	 * Init class
	 */
	public static void touch()
	{
		// only for static initialization
	}
	
	
	/**
	 * @return The teams which are capable of performing a ball placement
	 */
	public static List<ETeamColor> getBallPlacementTeams()
	{
		List<ETeamColor> teams = new ArrayList<>();
		if (ballPlacementYellowEnabled)
		{
			teams.add(ETeamColor.YELLOW);
		}
		if (ballPlacementBlueEnabled)
		{
			teams.add(ETeamColor.BLUE);
		}
		
		return teams;
	}
	
	
	/**
	 * @return
	 */
	public static double getBallPlacementAccuracy()
	{
		return ballPlacementAccuracy;
	}
	
	
	/**
	 * @return
	 */
	public static double getRobotBallPlacementAccuracy()
	{
		return robotBallPlacementAccuracy;
	}
	
	
	/**
	 * @return The maximum allowed bot speed during game stoppage in m/s
	 */
	public static double getMaxBotStopSpeed()
	{
		return maxBotStopSpeed;
	}
	
	
	/**
	 * @return The velocity below which a bot is considered to be stationary in m/s
	 */
	public static double getBotStationarySpeedThreshold()
	{
		return botStationarySpeedThreshold;
	}
	
	
	/**
	 * @return The velocity below which the ball is considered to be stationary in m/s
	 */
	public static double getBallStationarySpeedThreshold()
	{
		return ballStationarySpeedThreshold;
	}
	
	
	/**
	 * @return
	 */
	public static long getBallPlacementWindow()
	{
		return ballPlacementWindow;
	}
	
	
	/**
	 * The hostname of the refbox
	 * 
	 * @return a string that represents the hostname/ ip address of the refbox
	 */
	public static String getRefboxHostname()
	{
		return refboxHostname;
	}
	
	
	/**
	 * The port on which the refbox accepts remote control connections
	 * 
	 * @return port number
	 */
	public static int getRefboxPort()
	{
		return refboxPort;
	}
	
	
	public static double getPlacementOffset()
	{
		return placementOffset;
	}
}
