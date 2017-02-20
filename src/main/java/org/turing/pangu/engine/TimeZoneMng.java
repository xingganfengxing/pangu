package org.turing.pangu.engine;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.turing.pangu.utils.DateUtils;

/*
 * 每天的时间区间管理
 * */
public class TimeZoneMng {
	public static int SPAN_TIME = 10;// 10S 
	public static int INCREMENT_MONEY_TIMEOUT = 6*60*1000;
	public static int INCREMENT_WATERAMY_TIMEOUT = 3*60*1000;
	public static int STOCK_MONEY_TIMEOUT = 6*60*1000;
	public static int STOCK_WATERAMY_TIMEOUT = 3*60*1000;
	public static float OPEN_MAX_VPN_PHONE_WEIGHT = 1;
	private static TimeZoneMng mInstance = new TimeZoneMng();
	public static TimeZoneMng getInstance()
	{
		if(null == mInstance)
			mInstance = new TimeZoneMng();
		return mInstance;
	}
	public float getTimeZoneWeight(){
		float weigth = 0.0f;
		Calendar cal=Calendar.getInstance(TimeZone.getTimeZone( "GMT+8")); 
		int hour =cal.get(Calendar.HOUR_OF_DAY);
		if(hour >= 18 && hour <= 24 ){
			weigth = 1;
		}else if( hour >= 0 && hour <= 6 ){
			weigth = 0.5f;
		}else{
			weigth = 0.8f;
		}
		return weigth;
	}
}