/*
 * Copyright (c) 2009 - 2018, DHBW Mannheim - TIGERs Mannheim
 */
package edu.tigers.sumatra.presenter.log;

import edu.tigers.sumatra.views.ASumatraView;
import edu.tigers.sumatra.views.ESumatraViewType;
import edu.tigers.sumatra.views.ISumatraViewPresenter;


/**
 * @author Nicolai Ommer <nicolai.ommer@gmail.com>
 */
public class LogView extends ASumatraView
{
	private final boolean	addAppender;
	
	
	/**
	 * @param addAppender
	 */
	public LogView(final boolean addAppender)
	{
		super(ESumatraViewType.LOG);
		this.addAppender = addAppender;
	}
	
	
	@Override
	public ISumatraViewPresenter createPresenter()
	{
		return new LogPresenter(addAppender);
	}
}
