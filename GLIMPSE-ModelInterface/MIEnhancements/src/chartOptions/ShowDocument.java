package chartOptions;

import java.lang.reflect.Method;

import javax.swing.JOptionPane;

/**
 * The class to handle displaying a document.
 * 
 *    Author			Action						Date		Flag
 *  ======================================================================= 			
 *	TWU				created 						1/2/2016	
 */

public class ShowDocument {
	private static final String errMsg = "Error attempting to launch web browser";
	private static boolean debug = false;

	public ShowDocument() {
	}

	public static void openURL(String url) {
		String osName = System.getProperty("os.name");
		if (debug)
			System.out.println("ShowDocument:url: " + url);
		try {
			if (osName.startsWith("Mac OS")) {
				Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
				Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
				openURL.invoke(null, new Object[] { url });
			} else if (osName.startsWith("Windows")) {
				Runtime.getRuntime()
						.exec((new StringBuilder("rundll32 url.dll,FileProtocolHandler ")).append(url).toString());
			} else {
				String browsers[] = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
				String browser = null;
				for (int count = 0; count < browsers.length && browser == null; count++)
					if (Runtime.getRuntime().exec(new String[] { "which", browsers[count] }).waitFor() == 0)
						browser = browsers[count];

				if (browser == null)
					throw new Exception("Could not find web browser");
				Runtime.getRuntime().exec(new String[] { browser, url });
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, errMsg);
		}
	}

}
