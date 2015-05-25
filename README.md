================================
RepositoryToolsRestrictionFilter
================================

(which should actually be called '...AuthorisationFilter' but I haven't refactored all the class names, etc. yet)

This is an attempt at working around unrestricted access to bitstreams within Symplectic Elements.

pom.xml refers to DSpace 3.2, but any version >= 3 should work.

TODO
==
1. Better context handling - I thought I was saving time/resource creating the Context at init(), but I think this holds DB connections open to long and is generally a bad idea.
2. Think about some way to detect Elements user with each request
   AND/OR think about some way to allow depositors access to their own workflow items

Installation: (just quick/testing for now, not a permanent deployment)
==

1. (optional) create project over these sources in your favourite IDE
2. Build the jar with 'mvn package' (see pom.xml for dependencies)
3. Deploy the jar into the WEB-INF/lib/ directory of your rt4ds webapp directory
4. Edit the WEB-INF/web.xml file in your rt4ds webapp directory and insert the following lines:


    <!-- put in filters section -->
    <filter>
        <filter-name>authorisation-filter</filter-name>
        <filter-class>nz.ac.auckland.researchoutputs.rtc.RepositoryToolsRestrictionFilter</filter-class>
        <init-param>
            <param-name>enable</param-name>
            <param-value>true</param-value>
        </init-param>
        <init-param>
            <param-name>testuser</param-name>
            <param-value>digital.development@auckland.ac.nz</param-value>
        </init-param>
        <init-param>
            <param-name>segment</param-name>
            <param-value>3</param-value>
        </init-param>
    </filter>



    <!-- put in filter mappings section -->
    <filter-mapping>
        <filter-name>authorisation-filter</filter-name>
        <url-pattern>/file/*</url-pattern>
    </filter-mapping>


5. Restart Tomcat (or whatever webapp container you use), test.
