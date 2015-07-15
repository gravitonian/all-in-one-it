# All-In-One (AIO) Alfresco SDK 2.1 Project with Integration Testing

This AIO project contains an extra module called integration-test-runner that demonstrates
how Web Scripts can be integration tested against a running embedded tomcat.

The AIO project is configured to be used with the Alfresco 5.0.1 version.

This project also fixes some stuff so the WARs produced from it can be directly deployed into
an Alfresco installation, see: https://issues.alfresco.com/jira/browse/DEVPLAT-118

Build WARs: $ mvn clean install -Penterprise

Run: $ mvn clean install -Penterprise,run

Integration Test: $ mvn clean install -Penterprise,enable-it

