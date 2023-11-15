#!/bin/sh

# -- This shell script compiles the program with gson and javac
# -- After compilation, the program runs with the following command using the argument "Everett". Alternatively, you can use "Sydney" as an argument for a different result

# You can run it with any city as follows:
# java -cp gson-2.10.1.jar: MyCity "<your city>"

# If your city is more than 1 word, please surround your city with quotes. Ex: "new york city"

# This shell is for Linux
# For windows, instead of a colon after the java run command, change to ";."

javac -cp gson-2.10.1.jar MyCity.java

# To run the program use the command: java -cp gson-2.10.1.jar: MyCity Everett