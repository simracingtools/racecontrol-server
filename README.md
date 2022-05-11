# iRacing RaceControl

This server supports the organization of team endurance events and enables a live race control support for 
(team) endurance racing on the iRacing platform.

Event organization is supported by

* Team and driver self registration process
* Consistency check of a drivers iRacing id and name utilizing the iRacing Data API
* Automatic creation of Discord roles/channels for registered teams
* Optional check for iRacing league and/or team membership
* Support for limited grid slots/wildcards/waiting-list in multiple classes
* Direct utilization of car/track information from iRacing

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

User security relies on Keycloak - so you need a running instance
to be able to login.

[logz.io](https://app-eu.logz.io/) is supported as logging backend.

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

The server requires Java 11 or higher.

Server component is build as executable jar file by executing

    mvn clean package

## Deployment

Mandatory requirements are

* Java 11 JRE or higher
* MariaDB / MySQL server installed
* Access to running keycloak instance to be able to do login

Optional requirements are

* Nginx web server installed (for the use as frontend proxy)

To help with the deployment on a Linux based system files in

    /src/deb/...

should provide help to 

* prepare database
* add a user dedicated to run the server
* run the server as system service
* configure an nginx proxy 
* initial/update deployment of a server

In these support file it is assumed the server's home will be 

    /opt/race-control
    
## Configuration

To enable user authentication you have to configure a Keycloak 
realm and an OpenId Connect client withing this realm for your server / domain.

Server configuration is done by modifying the deployed application.properties.template
and save it as application.properties.

You can use logz.io for analyzing your server logs by modifying the deployed
logback.xml.template by interting your personal logz.io token and saving it as
logback.xml

You can change your server port by providing the environment variable 
SERVER_PORT=[your desired port number]. 
