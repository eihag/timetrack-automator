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
It is assumed you already have configured Java 21 / maven on your system.

Steps:
1. Configure - create a `application.properties` in `src/main/resources` directory. See `application-SAMPLE.properties`, rename, and fill in the blanks (JIRA username etc.).
2. Compile `mvn clean install`
3. Run - example: `java -jar timetrack-automator-1.0.0.jar dry-run 2024-02-14`


## Usage
```
java -jar timetrack-automator-1.0.0.jar dry-run {<date> | today}
java -jar timetrack-automator-1.0.0.jar log-work {<date> | today}
java -jar timetrack-automator-1.0.0.jar report {month | year}
```

Examples:
```
java -jar timetrack-automator-1.0.0.jar log-work today
java -jar timetrack-automator-1.0.0.jar log-work 2024-02-01

java -jar timetrack-automator-1.0.0.jar report month 
java -jar timetrack-automator-1.0.0.jar report year
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
0 17 * * * java -jar /my/path/timetrack-automator-1.0.0.jar log-work today>/tmp/stdout.log 2>/tmp/stderr.log
```

## Limitations
Code does not handle pagination - but hopefully you have less than 50 active tasks.

# Goodhart's Law
When a measure becomes a target, it ceases to be a good measure.
