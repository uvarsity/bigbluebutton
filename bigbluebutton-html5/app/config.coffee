# TODO: should be split on server and client side
# # Global configurations file

config = {}

# Default global variables
config.appName = 'BigBlueButton HTML5 Client'
config.bbbServerVersion = '0.9.0'
config.copyrightYear = '2015'
config.dateOfBuild = 'Feb 06, 2015' #TODO
config.defaultWelcomeMessage = 'Welcome to %%CONFNAME%%!\r\rFor help on using BigBlueButton see these (short) <a href="event:http://www.bigbluebutton.org/content/videos"><u>tutorial videos</u></a>.\r\rTo join the audio bridge click the headset icon (upper-left hand corner).  Use a headset to avoid causing background noise for others.\r\r\r'
config.defaultWelcomeMessageFooter = "This server is running a build of <a href='https://code.google.com/p/bigbluebutton/wiki/090Overview' target='_blank'><u>BigBlueButton #{config.bbbServerVersion}</u></a>."

config.maxUsernameLength = 30
config.maxChatLength = 140

## Application configurations
config.app = {}

#default font sizes for mobile / desktop
config.app.mobileFont = 24
config.app.desktopFont = 12

# Will offer the user to join the audio when entering the meeting
config.app.autoJoinAudio = false
# The amount of time the client will wait before making another call to successfully hangup the WebRTC conference call
config.app.WebRTCHangupRetryInterval = 2000

# Configs for redis
config.redis = {}
config.redis.host = "127.0.0.1"
config.redis.post = "6379"
config.redis.timeout = 5000
config.redis.channels = {}
config.redis.channels.fromBBBApps = "bigbluebutton:from-bbb-apps:*"
config.redis.channels.toBBBApps = {}
config.redis.channels.toBBBApps.pattern = "bigbluebutton:to-bbb-apps:*"
config.redis.channels.toBBBApps.chat = "bigbluebutton:to-bbb-apps:chat"
config.redis.channels.toBBBApps.meeting = "bigbluebutton:to-bbb-apps:meeting"
config.redis.channels.toBBBApps.users = "bigbluebutton:to-bbb-apps:users"
config.redis.channels.toBBBApps.voice = "bigbluebutton:to-bbb-apps:voice"
config.redis.channels.toBBBApps.whiteboard = "bigbluebutton:to-bbb-apps:whiteboard"

# Logging
config.log = {}

if Meteor.isServer
  config.log.path = if process?.env?.NODE_ENV is "production"
    "/var/log/bigbluebutton/bbbnode.log"
  else
    # logs in the directory immediatly before the meteor app
     process.env.PWD + '/../log/development.log'

  # Setting up a logger in Meteor.log
  winston = Winston #Meteor.require 'winston'
  file = config.log.path
  transports = [ new winston.transports.Console(), new winston.transports.File { filename: file } ]

  Meteor.log = new winston.Logger
    transports: transports

Meteor.config = config
