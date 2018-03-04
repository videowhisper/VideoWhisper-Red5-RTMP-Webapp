# VideoWhisper-Red5-RTMP-Webapp

[VideoWhisper RTMP Applications Homepage](https://videowhisper.com/?p=RTMP+Applications)

The VideoWhisper RTMP applications are deployed on RTMP servers as part of the installation of [VideoWhisper Webcam Applications](https://videowhisper.com).
This application is for Red5 server.

## Installation Instructions for Red5 VideoWhisper RTMP Applications
 1. Make sure you have Red5 1.0.5 or higher installed, functional and accessible.
 2. Copy contents to a videowhisper folder in your "webapps" folder in your Red5 installation folder. Verify that you have webapps/videowhisper/WEB-INF/ .
 2. Restart Red5 server. This is usually done with "service red5 restart" but sometimes you need to kill existing process.

## VideoWhisper Red5 Settings
 1. Edit WEB-INF/red5-web.properties from application folder:
 Variable and Default Value	Description
 * acceptPlayers=true	accept external flash players to connect by rtmp and play a stream without providing username
 * recordEverything=false	record all streams as flv files for archiving purposes (can use a lot of space); see options below for setting record path
 * recordPath=videowhisperStreams/	path to recordings, all recorded streams can be found here
 * playbackPath=videowhisperStreams/	path to video files for playback, use same to be able to play recordings
 * absolutePath=false	provide paths as relative or absolute; set this as true if you want to provide an absolute path like /home/mysite/public_html/archive or c:/www/archive
 * allowedDomains=	set comma separated domains that can host swf to use this rtmp address (videowhisper.com, videochat-software.com); leave blank to allow connections from all domains
 * withLogging=false	enable debug logs
 * logFilename=videowhisper	log file prefix
 Make sure you do not leave ending spaces.

To configure a streams storage folder in a public web location configure something like:
 * playbackPath=/home/account/public_html/streams/
 * recordPath=/home/account/public_html/streams/
 * absolutePath=true
 
 2. Restart Red5 server or just the VideoWhisper application from the Red5 Admin.


## Troubleshooting Red5
 * ps aux | grep red5 = find out if red5 process is running;
 * * there should be only one java red5 process and the grep process ;
 * * if multiple processes are running kill all with kill -9 {process id} then start red5 again;
 * * if multiple are running first one is bound to ports and not aware of new applications added (kill it)
 * netstat -anp | grep 1935 = find out if red5 is listening on rtmp port 1935;
 * also you can check web port 5080 if you need to use http://yourserver:5080/ to test/access red5 web server