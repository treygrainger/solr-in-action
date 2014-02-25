================================================================================
                       Solr in Action Example Code
================================================================================

Thank you for purchasing Solr in Action! Here are some basic instructions on
running the example code provided in the book.

1. Directory layout

  $SOLR_IN_ACTION/ - this is the location where you pulled or extracted the code
  |
  |__src/
  |   |__main/
  |      |__java/ - contains all the Java source files from examples in the book
  |
  |__example-docs/ - contains example config and content files for each chapter
  |
  |__scripts/ - contains scripts for separately executing each chapter's examples
  |
  |__pom.xml - maven build file
  |
  |__README.txt - you're looking at it ;-)
  |
  |__chapter-examples.sh - script to conveniently execute all examples in the book
  |
  |__solr-in-action.jar - compiled source code (follow step 2 to build this file)


2. Building the source code

You'll need Maven to build the source code. If you need some help on getting
Maven setup and running, please see: Maven in Five Minutes 
  
  http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html

Once you have Maven setup, cd into the directory where you extracted the
example source code zip file and do:

  mvn clean package

This will compile the source code and build an executable JAR file in the
main directory ($SOLR_IN_ACTION) named: solr-in-action.jar


3. Several chapters include executable code examples. To make these easy to run, 
we built a simple driver application that allows you to just pass 
in the name of the example you want to run and it will figure it out. 

To run a specific example from the book, use the java -jar command to
launch the executable JAR you built in step 2 above.

For example, to run the ExampleSolrJClient application from Chapter 5, do:

  java -jar solr-in-action.jar ch5.ExampleSolrJClient

In most cases, you can just pass the example class name without the package
information and the driver will figure it out, i.e.
 
  java -jar solr-in-action.jar examplesolrjclient

The driver will figure out that you're trying to run example class:
sia.ch5.ExampleSolrJClient

To see a list of all available examples, simply do:

  java -jar solr-in-action.jar

To see a list of all examples for a specific chapter, pass the chapter number,
e.g. the following command will show all examples for chapter 5:

  java -jar solr-in-action.jar 5

4. The book is filled with code listings, many of which demonstrate the HTTP request
syntax for executing specific types of Solr queries. You can type these URLs into
you favorite web browser, or you can alternatively just pass the listing number 
into the included http utility to execute the request.
e.g. the following command will execute the request demonstrated in listing 2.1:

  java -jar solr-in-action.jar listing 2.1

Enjoy!
================================================================================