package com.citi.uiapps;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.citi.crcalc.Beetle;
import com.citi.crcalc.CumRetCalculator;
import com.moandjiezana.toml.Toml;

public class Canary {
	static private Logger log = LoggerFactory.getLogger(Canary.class);
	public static void main(String[] args) {
		log.info("Initializing Canary.....");
		// -------------------------------------------------------
		// Some local variables
		// -------------------------------------------------------
		// String defConfig = "runConfigTest.config";
		Beetle be = new Beetle();
		FileSystemManager fse = be.getFSM();
		log.info("Parsing Commandline Options: .....");
		CommandLine cliOpts = parseCLIOptions(args);
		
		// Help
		if ( cliOpts.hasOption("help") ) {
			HelpFormatter fmtr = new HelpFormatter();
			fmtr.printHelp("Canary - Cumulative Return Calculator: ", getOptions());
		}
		
		if ( ! cliOpts.hasOption("runConfig") ) {
			log.error("[ErrCD: xC00009.0] Error occured while processing Canary, --runConfig <Filename.toml> was not passed!");
			System.exit(255);
		}
		String runConfig = cliOpts.getOptionValue("runConfig").trim();
		log.info("runConfig File: " + runConfig);
		FileObject fo = getFileObject(fse, runConfig);
		
		Map<String, Object> tConfig = getTOML2HM(fo);
		
		if (! tConfig.containsKey("datasets") ) {
			log.error("[ErrCD: xC00009.2] Error occured while processing Canary, TOML config file does not have datasets variable!");
			System.exit(255);
		}
		
		// There are safe ways to handle this, but i am running out of time!
		@SuppressWarnings("unchecked")
		HashMap<String, String> datasets = (HashMap<String, String>) tConfig.get("datasets");

		if (! datasets.containsKey("infile") ) {
			log.error("[ErrCD: xC00009.3] Error occured while processing Canary, TOML config file does not have infile variable!");
			System.exit(255);
		}
		
		if (! datasets.containsKey("queryfile") ) {
			log.error("[ErrCD: xC00009.4] Error occured while processing Canary, TOML config file does not have queryfile variable!");
			System.exit(255);
		}
		
		
		
		// Get data into an Map<Date, Double>
		/*
		 * ------------------------------------------------ jUnit test Case [TBD] -------------------------------------------
		 * try {
		 *		testMap.put((Date) tdt.parse("2015-01-10"),  0.10);
		 *		testMap.put((Date) tdt.parse("2015-02-10"),  0.05);
		 *		testMap.put((Date) tdt.parse("2015-04-10"),  0.15);
		 *		testMap.put((Date) tdt.parse("2015-04-15"), -0.10);
		 *		testMap.put((Date) tdt.parse("2015-06-10"), -0.12);
		 *	} catch (java.text.ParseException pe) {
		 *		log.error("[ErrCD: xC00009.3] Error DateFormat java.text.ParseException occurred while parsing DateString! ErrMsg: " 
		 *				+ org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(pe) 
		 *				+ " - Cause: " 
		 *				+ org.apache.commons.lang3.exception.ExceptionUtils.getMessage(pe));
		 *		log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(pe));
		 *		System.exit(255);
		 *	} catch (Exception e) {
		 *		log.error("[ErrCD: xC00009.4] Error Unknown Exception while DateFormat occurred while parsing DateString! ErrMsg: " 
		 *				+ org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e) 
		 *				+ " - Cause: " 
		 *				+ org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e));
		 *		log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
		 *		System.exit(255);
		 *	}
		 * ------------------------------------------------ jUnit test Case [TBD] -------------------------------------------
		*/
		Map<Date, Double> testMap = new HashMap<Date, Double>();
		DateFormat tdt = new SimpleDateFormat("yyyy-MM-dd");
		// Read from CSV File
		String var1 =null;
		String var2 =null;
		try {
			FileObject in = fse.resolveFile(datasets.get("infile"));
			CSVFormat csvFmt = CSVFormat.newFormat(',');
			InputStream bis = new BufferedInputStream(in.getContent().getInputStream());
            final Reader reader = new InputStreamReader(bis);
            CSVParser cPar = new CSVParser(reader, csvFmt);
            int headCnt = 0;
            for( final CSVRecord record: cPar) {
            	if ( headCnt == 0 ) {
            		headCnt++;
            		continue;
            	}
            	var1 = record.get(0);
            	var2 = record.get(1);
            	Date asof = (Date) tdt.parse(var1);
            	Double retVal = Double.parseDouble(var2);
            	testMap.put(asof, retVal);
            }
            cPar.close();
		} catch (FileSystemException e1) {
			log.error("[ErrCD: xC00009.5] Error Unknown Exception while Creating file object for Infile: " + datasets.get("infile") + "! ErrMsg: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e1) 
					+ " - Cause: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e1));
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e1));
			System.exit(255);
		} catch (IOException e) {
			log.error("[ErrCD: xC00009.6] Error Unknown Exception while While parsing CSV File for Infile: " + datasets.get("infile") + "! ErrMsg: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e) 
					+ " - Cause: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e));
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			System.exit(255);
		} catch (java.text.ParseException e) {
			log.error("[ErrCD: xC00009.7] Error Unknown Exception while While parsing CSV [Column #1] Expected date in YYYY-MM-DD format (Found: " + var1 + ")in Infile: " + datasets.get("infile") + "! ErrMsg: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e) 
					+ " - Cause: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e));
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			System.exit(255);
		}
		
		// Create Object CumRetCalculator
		CumRetCalculator crc = new CumRetCalculator(testMap);
		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.DOWN);
		log.debug("MapData: \n" + crc.showData());
		
		// Parse the second file!
		/*
		 * -------------------------------------------------- jUnit Test Case [TBD] -----------------------------------------------------------
		 * try {
		 *		log.info("Cumulative Return for AsOf: 20150131 \\w base 20150201: " + df.format(crc.findCumReturn((Date) tdt.parse("2015-01-31"), (Date) tdt.parse("2015-02-01"))));
		 *		log.info("Cumulative Return for AsOf: 20150228 \\w base 20150201: " + df.format(crc.findCumReturn(tdt.parse("2015-02-28"), tdt.parse("2015-02-01"))));
		 *		log.info("Cumulative Return for AsOf: 20150313 \\w base 20150201: " + df.format(crc.findCumReturn(tdt.parse("2015-03-13"), tdt.parse("2015-02-01"))));
		 *		log.info("Cumulative Return for AsOf: 20150430 \\w base 20150201: " + df.format(crc.findCumReturn(tdt.parse("2015-04-30"), tdt.parse("2015-02-01"))));
		 *		log.info("Cumulative Return for AsOf: 20150508 \\w base 20150201: " + df.format(crc.findCumReturn(tdt.parse("2015-05-08"), tdt.parse("2015-02-01"))));
		 *		log.info("Cumulative Return for AsOf: 20150630 \\w base 20150201: " + df.format(crc.findCumReturn(tdt.parse("2015-06-30"), tdt.parse("2015-02-01"))));
		 *		
		 *	} catch (java.text.ParseException pe) {
		 *		log.error("[ErrCD: xC00009.5] Error DateFormat java.text.ParseException occurred while parsing DateString! ErrMsg: " 
		 *				+ org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(pe) 
		 *				+ " - Cause: " 
		 *				+ org.apache.commons.lang3.exception.ExceptionUtils.getMessage(pe));
		 *		log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(pe));
		 *		System.exit(255);
		 *	} catch (Exception e) {
		 *		log.error("[ErrCD: xC00009.6] Error Unknown Exception while DateFormat occurred while parsing DateString! ErrMsg: " 
		 *				+ org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e) 
		 *				+ " - Cause: " 
		 *				+ org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e));
		 *		log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
		 *		System.exit(255);
		 *	}
		 * -------------------------------------------------- jUnit Test Case [TBD] -----------------------------------------------------------
		 */
		var1 = null;
		var2 = null;
		try {
			FileObject in = fse.resolveFile(datasets.get("queryfile"));
			CSVFormat csvFmt = CSVFormat.newFormat(',');
			InputStream bis = new BufferedInputStream(in.getContent().getInputStream());
            final Reader reader = new InputStreamReader(bis);
            CSVParser cPar = new CSVParser(reader, csvFmt);
            int headCnt = 0;
            for( final CSVRecord record: cPar) {
            	if ( headCnt == 0 ) {
            		headCnt++;
            		continue;
            	}
            	var1 = record.get(0);
            	var2 = record.get(1);
            	Date asof = (Date) tdt.parse(var1);
            	Date base = (Date) tdt.parse(var2);
            	log.info("Cumulative Return for AsOf: " + var1 + " \\w base : " + base + " => " + df.format(crc.findCumReturn(asof, base)));
            }
            cPar.close();
		} catch (FileSystemException e1) {
			log.error("[ErrCD: xC00009.8] Error Unknown Exception while Creating file object for Infile: " + datasets.get("infile") + "! ErrMsg: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e1) 
					+ " - Cause: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e1));
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e1));
			System.exit(255);
		} catch (IOException e) {
			log.error("[ErrCD: xC00009.9] Error Unknown Exception while While parsing CSV File for Infile: " + datasets.get("infile") + "! ErrMsg: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e) 
					+ " - Cause: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e));
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			System.exit(255);
		} catch (java.text.ParseException e) {
			log.error("[ErrCD: xC00009.10] Error Unknown Exception while While parsing CSV [Column #1] Expected date in YYYY-MM-DD format (Found: " + var1 + ")in Infile: " + datasets.get("infile") + "! ErrMsg: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e) 
					+ " - Cause: " 
					+ org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e));
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			System.exit(255);
		}
				
		// (Output to file) Simply Generate a Output File with generated output! by call findCumReturn
		// for this option Provide another file or data set of asof Date and Base Date combo
		
		log.info("Exiting Canary!");
	}

	
	// Read Commandline variable for File name to access
	static public Options getOptions()
	{
		Option helpOption = Option.builder("h")
                .longOpt("help")
                .required(false)
                .desc("Show this description and Usage message here <TBD>")
                .build();
		Option configFileOption = Option.builder()
                .longOpt("runConfig")
                .numberOfArgs(1)
                .required(false)
                .type(File.class)
                .desc("Input Daily Return File required file!") // Absolute File Path required!
                .build();
		
		final Options gOptions = new Options();
			gOptions.addOption(helpOption);
			gOptions.addOption(configFileOption);
		return gOptions;
    }
	
	// VFS2 Object or A DB Object!
	static CommandLine parseCLIOptions(String[] args) {
		Throwable rootCause = null;
		String errMsg = null;
		CommandLineParser prs = new DefaultParser();
		CommandLine cliopts = null;
	    try {
	    	cliopts = prs.parse(getOptions(), args);
		} catch (ParseException pe) {
			rootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(pe);
			errMsg = org.apache.commons.lang3.exception.ExceptionUtils.getMessage(pe);
			log.error("[ErrCD: xC00001.0] Error ParseException occurred while parsing commmandline options! ErrMsg: " + errMsg 
					+ " - Cause: " + rootCause );
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(pe));
			System.exit(255);
		} catch ( Exception e ) {
			rootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e);
			errMsg = org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e);
			log.error("[ErrCD: xC00001.1] Error Unknown Exception occurred while parsing commmandline options! ErrMsg: " + errMsg 
					+ " - Cause: " + rootCause );
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			System.exit(255);
		}
	    return cliopts;
	}
	
	static FileObject getFileObject(FileSystemManager fse, String runConfig) {
		Throwable rootCause = null;
		String errMsg = null;
		FileObject fo = null;
		log.info("Attempting to Retrieve the FileObject for runConfig: " + runConfig); 
		try {
			String tmpFile = "file://" + runConfig.replaceAll("\\\\", "/");
			log.info("Temp: " + tmpFile);
			fo  = fse.resolveFile(tmpFile);
		} catch (FileSystemException e) {
			rootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e);
			errMsg = org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e);
			log.error("[ErrCD: xC00003.0] Error FileSystemException occurred while retriving FileObject via VFS2! runConfig: " + runConfig  + " - ErrMsg: " + errMsg 
					+ " - Cause: " + rootCause );
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			System.exit(255);
		} catch ( Exception e ) {
			rootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e);
			errMsg = org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e);
			log.error("[ErrCD: xC00003.1] Error Unknown Exception occurred while retriving FileObject via VFS2! runConfig: " + runConfig  + " - ErrMsg: " + errMsg 
					+ " - Cause: " + rootCause );
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			System.exit(255);
		}
		return fo;
	}
	
	static Map<String, Object> getTOML2HM (FileObject fo) {
		Throwable rootCause = null;
		String errMsg = null;
		InputStream tomlStream = null;
		String fileURL = null;
		Map<String, Object> retMap = new HashMap<String, Object>();
		try {
			fileURL = fo.getURL().toString();
			tomlStream = fo.getContent().getInputStream();
		} catch (FileSystemException e) {
			rootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e);
			errMsg = org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e);
			log.error("[ErrCD: xC00004.0] Error FileSystemException occurred while parsing TOML file! Filename: " + fileURL  + " - ErrMsg: " + errMsg 
					+ " - Cause: " + rootCause );
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			System.exit(255);
		} catch ( Exception e ) {
			rootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e);
			errMsg = org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e);
			log.error("[ErrCD: xC00004.1] Error Unknown Exception occurred while parsing TOML file! Filename: " + fileURL  + " - ErrMsg: " + errMsg 
					+ " - Cause: " + rootCause );
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			System.exit(255);
		}
		try {
			Toml toml = new Toml().read(tomlStream);
			retMap = toml.toMap();
		} catch (Exception e) {
			rootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e);
			errMsg = org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e);
			log.error("[ErrCD: xC00004.3] Error Unknown Exception occurred while parsing TOML file! Filename: " + fileURL  + " - ErrMsg: " + errMsg 
					+ " - Cause: " + rootCause );
			log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
			System.exit(255);
		}
		return retMap;
	}
}
