rem A simple script to launch ORDModelInterface with args.  Check out README.TXT for a list of available args.

set JAVA_HOME=.\jre8-corretto-402-x64

set PATH=.;%JAVA_HOME%;%JAVA_HOME%\bin;%PATH%

java -jar GLIMPSE-ModelInterface.jar -o PATHtoBASExDBdirectory -q Main_queries_GLIMPSE-5p4.xml -u units_rules.csv

REM pause
