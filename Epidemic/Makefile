# Makefile for the epidemic simulator
# Author Allison Ryan
# Version 2020-11-20
#
# The following commands are supported
#	make        -- makes Epidemic.class and all other necessary classes
#	make html   -- makes website of all internal documentation
#	make clean  -- deletes files created by other make commands
#	make test   -- runs a demo epidemic simulation test

##########
# All the source files, broken up by categories

UtilityJava = Error.java MyScanner.java MyRandom.java Simulator.java

PlaceSubclassJava = HomePlace.java WorkPlace.java
PersonSubclassJava = Employee.java
ModelJava = Person.java Place.java $(PlaceSubclassJava) $(PersonSubclassJava)

MainJava = Epidemic.java

AllJava = $(UtilityJava) $(ModelJava) $(MainJava)

##########
# Primary make target

UtilityClasses = Error.class MyScanner.class MyRandom.class Simulator.class
ModelClasses = Place.class Person.class
Epidemic.class: Epidemic.java $(UtilityClasses) $(ModelClasses)
	javac Epidemic.java

##########
# Secondary make target -- simulation models

PersonDepends = Place.class HomePlace.class Employee.class
Person.class: Person.class $(PersonDepends) $(UtilityClasses)
	javac Person.java

Employee.class: Employee.java Person.class WorkPlace.class  $(UtilityClasses)
	javac Employee.java

PlaceDepends = Person.class HomePlace.class WorkPlace.class
Place.class: Place.java $(PlaceDepends) $(UtilityClasses)
	javac Place.java

HomePlace.class: HomePlace.java Place.class $(UtilityClasses)
	javac HomePlace.java

WorkPlace.class: WorkPlace.java Employee.class Place.class $(UtilityClasses)
	javac WorkPlace.java

##########
# Secondary make target -- utility classes

Error.class: Error.java
	javac Error.java

MyScanner.class: MyScanner.java Error.class
	javac MyScanner.java

MyRandom.class: MyRandom.java
	javac MyRandom.java

Simulator.class: Simulator.java
	javac Simulator.java

##########
# Utility make targets

clean:
	rm -f *.class *.html package-list script.js stylesheet.css

html:
	javadoc @EpidemicSource

test: Epidemic.class
	java Epidemic testepi
