
#2014#

* Xponents 2.5.1 - July 2014 
 - Java 7+ required now;  Java 6 source syntax supported, but release will be Java 7 binary
 - Javadoc cleanup
 - XText refactor, given added archive file support; concept of caching and crawling is optional and moved out of main conversion logic. 

* Xponents 2.4.3 - June 2014
 - Extraction: MGRS filters for well known dates/months, lower case (default is to filter out lowercase), and Line endings in Latband/GZD
 - XText bug fixes; check style review:  v1.5.4
 - POM cleanup and indentation; review unspecified compile time dependencies

*  Xponents 2.3  - May 2014
 - minor tweeks in APIs
 - added set_match_id(match, counter)  to FlexPat matchers

*  Xponents XText 1.5 - May 2014
 - numerous fixes in XText proper, and many path normalization fixes in ConvertedDocument
 - added Mail crawler and MessageConverter for handling email
 - many improvements to JPEG/EXIF conversion
