### Website scrapping 

A simple program that scraps HTML website and creates ServiceNow Knowledge records. 
It was developed to convert existing legacy database to ServiceNow.

#### Program features

* Creates the kb_knowledge records from from folder with HTML files - one kb_knowledge per file, 
* Supports attachments and links between the files - re-writes img and href references to point directly to attachment records in ServiceNow instance
* Supports knowledgebase hierarchy by trying to match info in the file with existing knowledgebase and categories by name.

#### Reference Documentation

Pre-requisites:

* HTTP Basic Authentication account in ServiceNow instance.
* A folder with HTML files in the filesystem
* Existing ServiceNow Knowledgabses and Categories to link the files

Creates kb_knowledge records with 'draft' worklow state.

To run the program:

`mvn package && java -jar target/demo-0.0.1-SNAPSHOT.jar`
