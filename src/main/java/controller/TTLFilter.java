package controller;

import java.io.File;
import java.io.FileFilter;

/**
 * This class filters all files that do not end with ".ttl" 
 * according to the FileFilter interface.
 * @author lars
 *
 */
public class TTLFilter implements FileFilter {

	public boolean accept(File pathname) {
		
		if(pathname.getName().contains(".ttl"))
			return true;
		return false;
	}

}
