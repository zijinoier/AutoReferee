/*
 * *********************************************************
 * Copyright (c) 2009 - 2016, DHBW Mannheim - Tigers Mannheim
 * Project: TIGERS - Sumatra
 * Date: Apr 4, 2016
 * Author(s): "Lukas Magel"
 * *********************************************************
 */
package edu.tigers.autoreferee.remote;


/**
 * @author "Lukas Magel"
 */
public interface ICommandResult
{
	/**
	 * @return
	 */
	boolean isCompleted();
	
	
	/**
	 * @return
	 */
	boolean isSuccessful();
	
	
	/**
	 * Returns true if command delivery has been completed but the command was not successful
	 * 
	 * @return
	 */
	boolean hasFailed();
}
