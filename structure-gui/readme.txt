TODO in order for this to compile:

Install JmolApplet.jar from the Jmol web site using:

http://jmol.sourceforge.net/download/

mvn install:install-file \
 -Dfile=/Users/andreas/workspace_mvn/Jmol/build/JmolApplet.jar \
 -DgroupId=jmolapplet \
 -DartifactId=JmolApplet \
 -Dversion=11.8.17 \
 -Dpackaging=jar \
 -DgeneratePom=true 
 
 
 Install Javaws.jar
 (probably somewhere in your Java install)