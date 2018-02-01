package knowledgeBase;

import java.io.File;
import java.io.FileFilter;

/**
 * This class filters all files that do not end with ".ttl" 
 * according to the FileFilter interface.
 *
 */
public class TTLFilter implements FileFilter {

	/**
	 * Tests if the filename must be filtered.
	 * @param pathname - the path to the file to test
	 * @return whether this file has to be accepted or filtered
	 */
	public boolean accept(File pathname) {
		
		if(pathname.getName().contains(".ttl"))
			return true;
		return false;
	}
}
