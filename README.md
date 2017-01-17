# scalyr-logback

The Logback Scalyr appender enables Logback to post messages to [Scalyr](https://www.scalyr.com), a cloud logging service.


## Usage


### Pattern-formatted Messages

Simple `logback.xml` configuration:

```xml
<configuration>
    <appender name="SCALYR" class="io.grhodes.scalyr.logback.ScalyrAppender">
        <apiKey>API_TOKEN</apiKey>
        <logfile>application</logfile>
        <parser>slf4j</parser>
        <serverAttributes>application_id=${APPID}</serverAttributes>
        <serverAttributes>application_version=${APPLICATION_VERSION}</serverAttributes>
        <serverAttributes>stack=${STACK}</serverAttributes>
        <serverAttributes>source=${SOURCE}</serverAttributes>
        <serverAttributes>image=${IMAGE}</serverAttributes>
    </appender>
    ...
</configuration>
```

By default the Scalyr Appender uses a PatternLayout to control log event formatting.  The default pattern normalizes timestamps to UTC time, which is usually best for clustered environments that process requests from around the world.  The default pattern is:

    %d{"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",UTC} %-5level [%thread] %logger: %m%n

If this is not suitable, you can configure the pattern directly on the appender:

```xml
<configuration>
    <appender name="SCALYR" class="io.grhodes.scalyr.logback.ScalyrAppender">
        <apiKey>API_TOKEN</apiKey>
        <logfile>application</logfile>
        <parser>slf4j</parser>
        <pattern>%date %-5level [%thread] %logger{36} %m%n</pattern>
    </appender>
    ...
</configuration>
```

