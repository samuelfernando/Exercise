/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.shef.zeno.robot;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mechio.api.animation.Animation;
import org.mechio.api.animation.messaging.RemoteAnimationPlayerClient;

import org.mechio.api.motion.Robot;
import org.mechio.api.motion.messaging.RemoteRobot;
import org.mechio.api.speech.SpeechJob;
import org.mechio.api.speech.messaging.RemoteSpeechServiceClient;
import org.mechio.api.speech.utils.DefaultSpeechJob;
import org.mechio.client.basic.MechIO;
import org.mechio.client.basic.UserSettings;

/**
 *
 * @author samf
 */
public class RobotController {

    public RemoteRobot myRobot;
    Robot.RobotPositionMap myGoalPositions;
    Robot.JointId neck_yaw;
    Robot.JointId neck_pitch;
    RemoteSpeechServiceClient mySpeaker;
    public RemoteAnimationPlayerClient myPlayer;
    DefaultSpeechJob currentSpeechJob = null;
    Animation waveAnim;
    PrintStream robotOut;
    SimpleDateFormat dateFormat;
    public RobotController(String robotIP, PrintStream robotOut) {
        this.robotOut = robotOut;
        String robotID = "myRobot";
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss.SSS");
       
        // set respective addresses


        UserSettings.setRobotId(robotID);
        UserSettings.setRobotAddress(robotIP);
        UserSettings.setSpeechAddress(robotIP);
        UserSettings.setAnimationAddress(robotIP);

        waveAnim = MechIO.loadAnimation("wave-anim.xml");
        mySpeaker = MechIO.connectSpeechService();
        myPlayer = MechIO.connectAnimationPlayer();
        myRobot = MechIO.connectRobot();
        myGoalPositions = new org.mechio.api.motion.Robot.RobotPositionHashMap();
        myGoalPositions = myRobot.getDefaultPositions();
        myRobot.move(myGoalPositions, 1000);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(RobotController.class.getName()).log(Level.SEVERE, null, ex);
        }
        myGoalPositions.clear();
    }


    void moveGoalPositions() {
        if (myGoalPositions.isEmpty()) {
            return;
        }
       
        System.out.println(myGoalPositions);
        myRobot.move(myGoalPositions, 200);
        myGoalPositions.clear();
    }
    
    public void speak(String text) {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        String timeNow = dateFormat.format(date);
        robotOut.println(timeNow+"\tspeak\t"+text);
       
        currentSpeechJob = (DefaultSpeechJob)mySpeaker.speak(text);
        
   }
    
    public void stopSpeaking() {
        if (currentSpeechJob!=null) {
          if (currentSpeechJob.getStatus()==DefaultSpeechJob.RUNNING) {
             // currentSpeechJob.cancel();
             // mySpeaker.stop();
               currentSpeechJob = null;
          }
        }
    }
    public void setDefaultPositions() {
        myGoalPositions = myRobot.getDefaultPositions();
        myRobot.move(myGoalPositions, 1000);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(RobotController.class.getName()).log(Level.SEVERE, null, ex);
        }
        myGoalPositions.clear();
    
    }

    public void speakWav(String text) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void playAnim(String animName) {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        String timeNow = dateFormat.format(date);
        robotOut.println(timeNow+"\tplayAnim\t"+animName);
       
        Animation anim = MechIO.loadAnimation("animations/"+animName+".xml");
        myPlayer.playAnimation(anim);
    }

    public DefaultSpeechJob getCurrentSpeechJob() {
        return currentSpeechJob;
    }

    public void setCurrentSpeechJob(DefaultSpeechJob job) {
        currentSpeechJob = job;
    }
    
}
