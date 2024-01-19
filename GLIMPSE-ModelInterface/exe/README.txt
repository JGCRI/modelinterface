The GLIMPSE-ModelInterface can be launched with a variety of arguments.  None of these are mandatory for the system to work but
they can be a convient way to consistently launch the tool if the same files are used frequently.

Option       Description
------       -----------
-help       print usage information
-l <String>  log file into which to redirect ModelInterface output
-o <String>  path to XML DB Directory
-q <String>  path to query file
-u <String>  Path to CSV file for unit conversions


An example command line would look like:

java -jar GLIMPSE-ModelInterface.jar -o c:\somePath\To\A\Basex\Directory -q Main_queries_GLIMPSE-5p4.xml -u units_rules.csv

A similar command can be found in the LaunchGLIMPSE-ModelInterfaceExampleArgs.bat batch file provided with this EXE directory. 
Update to appropriate path for your basex directory and double click to run the GLIMPSE-ModelInterface.