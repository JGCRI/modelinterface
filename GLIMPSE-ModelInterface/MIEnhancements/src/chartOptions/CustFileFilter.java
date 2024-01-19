package chartOptions;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * The class to handle fileter for a file selection
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class CustFileFilter extends FileFilter {
	String ext;
	String fileName;

	public CustFileFilter(String ext, String fileName) {
		this.ext = ext;
		this.fileName = fileName;
	}

	public boolean accept(File file) {
		boolean b = false;
		String name = file.getName();
		if (ext != null) {
			String[] s = ext.split(",");
			for (int i = 0; i < s.length; i++) {
				boolean me = matchExt(s[0].trim(), name);
				if (me && fileName != null) {
					if (matchFileName(name)) {
						b = true;
						break;
					}
				} else if (me) {
					b = true;
					break;
				}
			}
		}
		return b;
	}

	boolean matchExt(String e, String name) {
		return name.endsWith(e.trim());
	}

	boolean matchFileName(String name) {
		boolean b = false;
		String[] s = fileName.split(",");
		for (int i = 0; i < s.length; i++) {
			if (name.contains(s[i].trim())) {
				b = true;
				break;
			}
		}
		return b;
	}

	public String getDescription() {
		return null;
	}
}
