/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
* 
* Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 3.0 of the License, or (at your option) any later
* version.
* 
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
*
*/
package org.bigbluebutton.deskshare.server.sessions


import org.bigbluebutton.deskshare.server.svc1.{BlockManager, Dimension}
import org.bigbluebutton.deskshare.server.stream.{StreamManager, Stream, UpdateStream, StartStream, StopStream, UpdateStreamMouseLocation}

import scala.actors.Actor
import scala.actors.Actor._
import net.lag.logging.Logger
import java.awt.Point

case object StartSession
case class UpdateSessionBlock(position: Int, blockData: Array[Byte], keyframe: Boolean, seqNum: Int)
case class UpdateSessionMouseLocation(loc: Point, seqNum: Int)
                                      
case object StopSession
case object GenerateKeyFrame

class SessionSVC(sessionManager:SessionManagerSVC, room: String, screenDim: Dimension, 
                 blockDim: Dimension, streamManager: StreamManager, keyFrameInterval: Int, interframeInterval: Int, waitForAllBlocks: Boolean, useSVC2: Boolean) extends Actor {
	private val log = Logger.get
 
	private var blockManager: BlockManager = new BlockManager(room, screenDim, blockDim, waitForAllBlocks, useSVC2)
	private var stream:Stream = null
	private var lastUpdate:Long = System.currentTimeMillis()
	private var stop = true
	private var mouseLoc:Point = new Point(100,100)
	private var pendingGenKeyFrameRequest = false
	private var timestamp = 0L;
	private var lastUserKeyFrameRequest = 0L
	private var sentInitialKeyFrame = false;
	
	/*
	 * Schedule to generate a key frame after 30seconds of a request.
	 * This prevents us from generating unnecessary key frames when
	 * users join within seconds of each other.
	 */
	def scheduleGenerateKeyFrame(waitSec:Int) {
		if (!pendingGenKeyFrameRequest) {
			pendingGenKeyFrameRequest = true
			val mainActor = self
			actor {
				Thread.sleep(waitSec)
				mainActor ! "GenerateAKeyFrame"
			}
		}
	}
	
	def scheduleGenerateFrame() {
		val mainActor = self
		actor {
			Thread.sleep(interframeInterval)
			mainActor ! "GenerateFrame"
		}
	}
	
	def act() = {
      loop {
        react {
          case StartSession => initialize()
          case StopSession => stopSession()    
          case ml: UpdateSessionMouseLocation => mouseLoc = ml.loc 
          case "GenerateFrame" => {
	            generateFrame(false)
	            if (!stop) {
	              scheduleGenerateFrame()
	            } else {
	              exit()
	            }
            }
          case GenerateKeyFrame => {
            val now = System.currentTimeMillis()
            // Wait 30sec between keyframe request from the users. This prevents
            // creating many keyframes when users join the session close to one
            // another.
            if (now - lastUserKeyFrameRequest > 30000) {
              lastUserKeyFrameRequest = now
              scheduleGenerateKeyFrame(keyFrameInterval)
            }
        	  
          }
          case "GenerateAKeyFrame" => {
        	  pendingGenKeyFrameRequest = false
         	  log.debug("Session: Generating Key Frame for room %s", room)
        	  generateFrame(true)       	  
          }
          case b: UpdateSessionBlock => updateBlock(b.position, b.blockData, b.keyframe, b.seqNum)
          case m: Any => log.warning("Session: Unknown message [%s]", m)
        }
      }
    }

	def initMe():Boolean = {	   
		streamManager.createStream(room, screenDim.width, screenDim.height) match {
		  case None => log.error("Session: Failed to create stream for room %s", room); return false
		  case Some(s) => stream = s; return true
		}
	}
 
	private def initialize() {
		log.debug("Session: Starting session %s", room)
		blockManager.initialize()	
		stop = false
		stream ! StartStream
		generateFrame(true)
		scheduleGenerateFrame()
	}
 
	private def stopSession() {
		log.debug("Session: Stopping session %s", room)
		stream ! StopStream
		stop = true
		streamManager.destroyStream(room)
		blockManager = null
	}
	
	private def updateBlock(position: Int, videoData: Array[Byte], keyFrame: Boolean, seqNum: Int): Unit = {
		lastUpdate = System.currentTimeMillis()
		blockManager.updateBlock(position, videoData, keyFrame, seqNum)
		
		if (!sentInitialKeyFrame) {
		  // We have received all the blocks from the applet. Force sending a key frame
		  // to all clients so they won't see the trickle effect.
		  if (blockManager.hasReceivedAllBlocks) {
		    log.debug("Session: Received all blocks. Generating key frame for session %s", room)
		    scheduleGenerateKeyFrame(1)
		    sentInitialKeyFrame = true;
		  }
		}		
	}
	
	private def generateFrame(keyframe:Boolean) {				  
		if (System.currentTimeMillis() - lastUpdate > 60000) {
			log.warning("Session: Did not received updates for more than 1 minute. Removing session %s", room)
			sessionManager ! new RemoveSession(room)
		} else {
		  if (blockManager != null) {
			  timestamp += 50;
			  stream ! new UpdateStream(room, blockManager.generateFrame(keyframe), timestamp)
			  stream ! new UpdateStreamMouseLocation(room, mouseLoc)
		  }
		}
	}
 
	override def  exit() : Nothing = {
	  log.warning("Session: **** Exiting  Actor for room %s", room)
	  super.exit()
	}
 
	override def exit(reason : AnyRef) : Nothing = {
	  log.warning("Session: **** Exiting Actor %s for room %s", reason, room)
	  super.exit(reason)
	}
}
