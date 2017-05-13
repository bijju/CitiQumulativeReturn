package com.citi.crcalc;

// import java.text.DateFormat;
// import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.citi.uiapps.Canary;

public class CumRetCalculator {
	// This will be/used for debug... which i removed!
	private Logger log = LoggerFactory.getLogger(Canary.class);
	public Logger getLogger() { return this.log; }
	public void setLogger(Logger elog) { this.log = elog; }
	private Map<Date, Double> dReturns = new HashMap<Date, Double>();
	private List<Date> cacheSet = new ArrayList<Date>();
	private int maxIndx = 0;
	
	public Map<Date, Double> showData() { return this.dReturns; }
	
	public CumRetCalculator(Map<Date, Double> dailyReturns) {
		this.dReturns = dailyReturns;
		this.cacheSet = new ArrayList<Date>(this.dReturns.keySet());
		this.maxIndx = (this.cacheSet.size()-1);
	}
	
	// Recurssive Method!
	public double findCumReturn(Date asof, Date base) {
		double cumRet = 1;
		
		int iIndx = (this.cacheSet.size() - 1);
		if ( iIndx >= 0 ) {
			Date tmp = this.cacheSet.get(iIndx);
			if ((( tmp.after(base) && tmp.before(asof) )|| tmp.equals(base)|| tmp.equals(asof))) {
				this.cacheSet.remove(iIndx);
				cumRet = (1+this.dReturns.get(tmp)) * findCumReturn(asof, base);
			} else {
				this.cacheSet.remove(iIndx);
				cumRet = findCumReturn(asof, base);
			}
		}
		// log.debug("[" + iIndx + "]cumRet: " + cumRet);
		if ( iIndx == this.maxIndx ) {
			this.cacheSet = new ArrayList<Date>(this.dReturns.keySet());
			this.maxIndx = (this.cacheSet.size()-1);
			cumRet -= 1;
		}
		return cumRet;
	}
}
