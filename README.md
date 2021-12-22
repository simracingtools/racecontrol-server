# iRacing RaceControl

This server enables a live race control support for (team) endurance 
racing on the iRacing platform.

RaceControl is supported by the server providing information about

* track time (the time each driver spends on track between 
  pit exit and pit entry)
* track events (off-/ontrack, approaching pits, pit-exit/-entry,
  pit stalls, driver change)
* Average team iRating
* Rule violation on max driving time, min rest time for a driver and
  time based fair share

In addition with a [replay remote control](https://github.com/simracingtools/ir-replay-rc)
race control staff may click on a track events timestamp to set the
replay position to time and driver causing the event. This helps in
investigating incidents faster.

User security relies on Google oAuth provider - so you need a Google
account to be able to login.

## Components

### Server

You should run the server on a machine which is accessible by your
whole race control staff.

### Racecontrol data feed

ONE person of your race control staff has to run the 
[racecontrol](https://github.com/simracingtools/racecontrol) 
python script. It feeds the session and event data to the 
server component.

### Replay remote control

Each member of you race control staff may use the
[replay remote control](https://github.com/simracingtools/ir-replay-rc)
to enable fast event based replay time and driver selection.

## Build

The server requires Java 8 or higher. Be aware using Java 11 or 
higher may cause problems while some internal implementation classes
have been removed. There are no problems expected but as the project
utilizes the Spring Boot framework side effects ***may*** occur.

Server component is build as executable jar file by executing

    mvn clean package

## Deployment

Mandatory requirements are

* Java 8 JRE or higher
* MariaDB / MySQL server installed
* Google account to be able to do login

Optional requirements are

* Nginx web server installed (for the use as frontend proxy)

To help with the deployment on a Linux based system files in

    /src/deb/...

should provide help to 

* prepare database
* add a user dedicated to run the server
* run the server as system service
* configure an nginx proxy 

In these support file it is assumed the server's home will be 

    /opt/race-control
    
## Configuration

To enable user authentication you have to configure 
[Google OAuth 2.0](https://developers.google.com/identity/protocols/oauth2/openid-connect)
for your server / domain.

Configuration is done by providing an external application.properties file:

    # You must provide values here depending on your personal 
    # Google OAuth 2.0 configuration
    spring.security.oauth2.client.registration.google.client-id=
    spring.security.oauth2.client.registration.google.client-secret=
    
    # URL on which your server is publically reachable 
    racecontrol.serverBaseUrl=http://race-control.bausdorf-engineering.de
    
    #
    # Rule enforcement settings
    #
    
    # Max track time allowed between driver change
    racecontrol.maxDrivingTimeMinutes=180
    
    # Max track time after which a rest period is required for a driver
    racecontrol.maxDrivingTimeRequiresRestMinutes=120
    
    # Min rest time for a driver
    racecontrol.minRestTimeMinutes=120
    
    # Time based fair share track time factor
    racecontrol.fairShareFactor=0.5
    
    # iRating value limit for a driver/team to be considered as PRO
    racecontrol.proAmDiscriminator = 2500
