rem A simple script to launch ORDModelInterface with no args.  Depending on the particular machine setup, double clicking JAR may have
REM same effect.

set JAVA_HOME=.\jre8-corretto-402-x64

set PATH=.;%JAVA_HOME%;%JAVA_HOME%\bin;%PATH%

java -jar GLIMPSE-ModelInterface.jar

REM pause