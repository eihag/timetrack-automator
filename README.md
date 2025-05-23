```
___________.__             ___________                     __    
\__    ___/|__| _____   ___\__    ___/___________    ____ |  | __
  |    |   |  |/     \_/ __ \|    |  \_  __ \__  \ _/ ___\|  |/ /
  |    |   |  |  Y Y  \  ___/|    |   |  | \// __ \\  \___|    < 
  |____|   |__|__|_|  /\___  >____|   |__|  (____  /\___  >__|_ \
                    \/     \/                    \/     \/     \/
   _____          __                         __                  
  /  _  \  __ ___/  |_  ____   _____ _____ _/  |_  ___________   
 /  /_\  \|  |  \   __\/  _ \ /     \\__  \\   __\/  _ \_  __ \  
/    |    \  |  /|  | (  <_> )  Y Y  \/ __ \|  | (  <_> )  | \/  
\____|__  /____/ |__|  \____/|__|_|  (____  /__|  \____/|__|     
        \/                         \/     \/                     
```
# Timetrack automator for JIRA
Automate the manual chore of daily time registration in JIRA. 

This utility automatically registers JIRA work log items on your active JIRA tasks.

## Logic / Flow
1. Verify how many hours already logged / how many hours missing
2. Log remaining time on active JIRA tasks

## Build + Configuration
It is assumed you already have configured GraalVM / maven on your system for compiling. The build will create a native executable.

Steps:
1. Configure - create a `application.properties` in `src/main/resources` directory. See `application-SAMPLE.properties`, rename, and fill in the blanks (JIRA username etc.).
2. Compile to native executable `mvn clean package -Pnative`
3. Run - example: `./timetrack-automator dry-run today`

If you prefer, you can still build a docker image: `mvn spring-boot:build-image` (works without GraalVM)

## GraalVM
To install GraalVM with MacOS/homebrew: `brew install graalvm-jdk`

## Usage
```
timetrack-automator dry-run {<date> | today}
timetrack-automator log-work {<date> | today}
timetrack-automator report {month | year}
```

Examples:
```
timetrack-automator log-work today
timetrack-automator log-work 2024-02-01

timetrack-automator report month 
timetrack-automator report year
```
You can still also run the jar file directly:
```
java -jar timetrack-automator-1.0.0.jar dry-run today 
```


## Run automatically
You can set up the time tracker to run automatically in a daily cron job.

From a command line run: 
```
crontab -e
```
and enter the schedule. 

To run daily at 5pm:
```
0 17 * * * /usr/local/bin/timetrack-automator dry-run log-work today>/tmp/stdout.log 2>/tmp/stderr.log
```

## Limitations
Code does not handle pagination - but hopefully you have less than 50 active tasks.

# Goodhart's Law
When a measure becomes a target, it ceases to be a good measure.
