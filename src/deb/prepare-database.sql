create database `racecontrol`;
CREATE USER 'racecontrol' IDENTIFIED BY 'rcadmin';
GRANT ALL privileges ON `racecontrol`.* TO 'racecontrol'@'%';
