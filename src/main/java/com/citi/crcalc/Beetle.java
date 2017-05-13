package com.citi.crcalc;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.citi.uiapps.Canary;

public class Beetle {
	private Logger log = LoggerFactory.getLogger(Canary.class);
	private FileSystemManager fsMgr = null;
	private Throwable beRootCause = null;
	private String beErrMsg = null;
	
	public Logger getLogger() { return this.log; }
	public void setLogger(Logger elog) { this.log = elog; }
	
	/*
	 * Make FileSystemManager Object
	 * -------------------------------
	 * local or network file!
	 */
	public FileSystemManager getFSM() {
		// Initialize VFS Object \w some variables
		if ( this.fsMgr == null ) {
			try {
				this.fsMgr = VFS.getManager();
			} catch (FileSystemException fse) {
				beRootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(fse);
				beErrMsg = org.apache.commons.lang3.exception.ExceptionUtils.getMessage(fse);
				log.error("[ErrCD: xB00001.0] Error occurred while creating a VFS2 Object! ErrMsg: " + beErrMsg 
						+ " - Cause: " + beRootCause );
				log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(fse));
				System.exit(255);
			} catch ( Exception e ) {
				beRootCause = org.apache.commons.lang3.exception.ExceptionUtils.getRootCause(e);
				beErrMsg = org.apache.commons.lang3.exception.ExceptionUtils.getMessage(e);
				log.error("[ErrCD: xB00002.0] Error occurred while creating a VFS2 Object! ErrMsg: " + beErrMsg 
						+ " - Cause: " + beRootCause );
				log.error("Stack Trace: \n" + org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(e));
				System.exit(255);
			}
		}
		return this.fsMgr;
	}
}
