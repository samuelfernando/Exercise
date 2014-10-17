/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.shef.zeno.simon;

import uk.ac.shef.zeno.utils.KinectVideoRecorder;
import com.primesense.nite.Skeleton;
import com.primesense.nite.SkeletonState;
import com.primesense.nite.UserData;
import com.primesense.nite.UserTracker;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author samf
 */
public class SimonApp {

    VisitorState state = VisitorState.NOTHINGNESS;
    PlayState playState = PlayState.PLAY_START;
    Action currentAction;
    Random rand;
    SimonUtils mu;
    int score = 0;
    boolean simonSays;
    long lastRequest;
    boolean announcedTracking;
    UserData activeUser;
    boolean greeted;
    long endOfRequest;
    boolean requestMade;
    KinectVideoRecorder kinectVideoRecorder;
    int userCount;
    boolean kinectRecording;
    long pendingGoodbye;
    private long startedWaiting;
    private boolean pendingGoodbyeSpoken;
    int roundCount;
    PlayPatterns patterns;
    PlayPattern currentPattern;
    PrintStream saveState;
    public SimonApp(UserTracker tracker, PositionPanel panel, KinectVideoRecorder recorder) {
        
        pendingGoodbyeSpoken = false;
        rand = new Random();
        mu = new SimonUtils(tracker, panel);
        requestMade = false;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("save/saved-state.txt"));
            String line;
            int lastPattern =0;
            while ((line=reader.readLine())!=null) {
                lastPattern = Integer.parseInt(line);
            }
            patterns = new PlayPatterns(lastPattern+1);
            saveState = new PrintStream("save/saved-state.txt");
        } catch (Exception ex) {
            Logger.getLogger(SimonApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (recorder==null) {
            kinectRecording = false;
        }
        else {
            kinectRecording = true;
            kinectVideoRecorder = recorder;
        }
        userCount = 0;
    }

    Action chooseAction() {
        
        return currentPattern.getNextAction();
        /*
        Class c = Action.class;
        int x = rand.nextInt(c.getEnumConstants().length);
        return (Action) c.getEnumConstants()[x];*/
        //return Action.HANDS_UP;
    }

    boolean doesSimonSay() {
        
        return currentPattern.getNextSimon();
        /*double r = rand.nextDouble();
        if (r < 0.5) {
            return true;
        }
        return false;*/
    }

    boolean isAnyoneTracking(List<UserData> users) {
        boolean isAnyoneTracking = false;
        for (UserData user : users) {
            if (user.getSkeleton().getState() == SkeletonState.TRACKED) {
                isAnyoneTracking = true;
            }
        }
        return isAnyoneTracking;
    }

    public void update(List<UserData> users) {
       
        if (state == VisitorState.NOTHINGNESS) {
            if (!users.isEmpty()) {
                state = VisitorState.BODIES;
                for (UserData user : users) {
                    mu.mTracker.startSkeletonTracking(user.getId());
                }
             //   mu.stopSpeaking();
            } else if (mu.speechFinished()) {
                //mu.speak("I am all alone");
            }
        }

        if (state == VisitorState.BODIES) {
            if (users.isEmpty()) {
                state = VisitorState.NOTHINGNESS;
               // mu.stopSpeaking();
            } else {
                if (isAnyoneTracking(users)) {
                    state = VisitorState.TRACKING;
                 //   mu.stopSpeaking();
                } else {
                    for (UserData user : users) {
                        //if (user.isNew()) {
                        if (user.getSkeleton().getState() == SkeletonState.NONE) {
                            mu.mTracker.startSkeletonTracking(user.getId());
                        }
                        //}
                    }

                    if (mu.speechFinished()) {
                        //mu.speak("I can see somebody, but not clearly yet." + " You will need to stand still and wave your arms at me.");
                    }
                }
            }
        }

        if (state == VisitorState.TRACKING) {
            // System.out.println("Tracking state");
            if (!isAnyoneTracking(users)) {
                state = VisitorState.BODIES;
               // mu.stopSpeaking();
            } else if (anyoneInZone(users)) {
                state = VisitorState.INZONE_START_GAME;
               // mu.stopSpeaking();
            } else if (mu.speechFinished()) {
                for (UserData user : users) {
                    //if (user.isNew()) {
                    mu.mTracker.startSkeletonTracking(user.getId());
                    //}
                }

                //mu.speak("I can see you. But you have to get into the zone if you want to play.");
            }
        }

        if (state == VisitorState.INZONE_START_GAME) {
            //System.out.println("In zone start game");

            if (!anyoneInZone(users)) {
                state = VisitorState.TRACKING;
                //mu.stopSpeaking();
            }
            if (!greeted && mu.speechFinished()) {
                // System.out.println("Greeting");

               // mu.speak("Hello! Are you ready to play with me? Let's play Simon Says!"
                 //       + " If I say Simon Says you must do the action. Otherwise do not.");
                mu.speak("Hello! Are you ready to play with me? Let's play Simon Says! "
             +"If I say Simon Says, you must do the action. If I doo not say Simon Says, you must keep still.");
                currentPattern = patterns.getNextPattern();
                saveState.println(currentPattern.id);
                ++userCount;
                activeUser = getActiveUser(users);
                if (kinectRecording) {
                    kinectVideoRecorder.start("User"+activeUser.getId());
                }
                for (UserData user : users) {
                    if (user.getId() != activeUser.getId()) {
                        mu.mTracker.stopSkeletonTracking(user.getId());
                    }
                }
                //while (!mu.speechFinished()) {}
                greeted = true;

            }

            if (greeted && mu.speechFinished()) {
                state = VisitorState.PLAYING_GAME;
                roundCount = 0;
            }
            activeUser = getActiveUser(users);

            if (activeUser!=null) {
                mu.makeLog(activeUser, state, playState, currentPattern);
            }
            else {
                state = VisitorState.PENDING_GOODBYE;
            }

        }



        if (state == VisitorState.PLAYING_GAME) {
            activeUser = getActiveUser(users);
            if (activeUser == null) {
               // mu.stopSpeaking();
                //pendingGoodbye = System.currentTimeMillis();
                state = VisitorState.PENDING_GOODBYE;
            } else {
                if (playState == PlayState.PLAY_START) {
                    // System.out.println("Play start");
                    //mu.resetExpression();
                    if (mu.speechFinished()) {
                        currentAction = chooseAction();
                        simonSays = doesSimonSay();
                        makeRequest();
                        playState = PlayState.ACTION_GIVEN;

                    }
                }
                if (playState == PlayState.ACTION_GIVEN) {
                    //                                    System.out.println("action given");

                    long now = System.currentTimeMillis();
                    if (mu.speechFinished()) {
                        mu.addSkeleton(activeUser.getSkeleton());
                        if (!requestMade) {
                            endOfRequest = now;
                            requestMade = true;
                        }
                    }
                    if (requestMade && now - endOfRequest > 3000) {
                        playState = PlayState.EVALUATION;
                        requestMade = false;
                    }
                }
                if (playState == PlayState.EVALUATION) {
                    //   System.out.println("evaluation");
                    if (mu.speechFinished()) {
                        checkRequest();
                        ++roundCount;
                        if (roundCount>=10) {
                            state = VisitorState.GOODBYE_LIMIT;
                        }
                        mu.resetExpression();
                        playState = PlayState.PLAY_START;
                    }
                }
                mu.makeLog(activeUser, state, playState, currentPattern);
                
            }


        }

        if (state == VisitorState.PENDING_GOODBYE) {
            activeUser = getActiveUser(users);
            long now = System.currentTimeMillis();
            
            if (activeUser == null) { 
                if (!pendingGoodbyeSpoken) {
                    if (mu.speechFinished()) {
                        pendingGoodbye = now;
                        mu.speak("Are you going? You can play up to 10 rounds. Stay on the mat to keep playing.");
                        pendingGoodbyeSpoken = true;
                    }
                }
                else {
                    if (now-pendingGoodbye>5000) {
                        pendingGoodbyeSpoken = false;
                        state = VisitorState.GOODBYE_LEFT;
                    } 
                }
            }
            else {
                state=VisitorState.PLAYING_GAME;
                //playState = PlayState.PLAY_START;
                pendingGoodbye=now;
                pendingGoodbyeSpoken = false;
                //requestMade = false;
            }
            
        }
        
        if (state == VisitorState.GOODBYE_LIMIT) {
            if (mu.speechFinished()) {
                mu.speak("All right! We had 10 goes, and your final score was "+score+". I had fun playing with you, but it's time for me to play with someone else now. Bye bye! ");
 
                if (kinectRecording) {
                    kinectVideoRecorder.stop();
                }
                playState = PlayState.PLAY_START;
                state = VisitorState.WAIT_FOR_PLAYER_TO_LEAVE;
                startedWaiting = System.currentTimeMillis();
                score = 0;
                roundCount = 0;
                greeted = false;
                //do {

                //} while (!mu.speechFinished());
                activeUser = null;
            }
        }
        
        if (state == VisitorState.WAIT_FOR_PLAYER_TO_LEAVE) {
            activeUser = getActiveUser(users);
            if (activeUser==null) {
                state = VisitorState.NOTHINGNESS;
            }
        }
        
        if (state == VisitorState.GOODBYE_LEFT) {
            if (mu.speechFinished()) {
                mu.speak("Goodbye! Your final score was "+score);
                if (kinectRecording) {
                    kinectVideoRecorder.stop();
                }
                playState = PlayState.PLAY_START;
                state = VisitorState.NOTHINGNESS;
                score = 0;
                roundCount = 0;
                greeted = false;
                //do {

                //} while (!mu.speechFinished());
                activeUser = null;
            }
        }
        /*if (!anyoneInZone && mu.timeSinceLastSpeak() > 6000) {
         mu.speak("I'm ready to play. If you want to play, then enter the play zone and wave your arms.");
         }*/
    }

    boolean checkRequest() {
        boolean ret = false;
        String toSpeak = "";
        Emotion emotion = null;
        if (simonSays) {
            ret = mu.checkAction(currentAction);
            if (ret) {
                //toSpeak = "Yes, you got that right!";
                toSpeak = "Yes, well done, you got that right!";
                emotion = Emotion.Positive;
                ++score;
            } else {
                toSpeak = "Oh dear, I said Simon Says, so "+currentAction.getError() + ".";
                
                //toSpeak = "No you got that wrong " + currentAction.getError() + ".";
                emotion = Emotion.Negative;
            }

        } else {
            ret = !mu.checkAction(currentAction);

            if (ret) {
                toSpeak = "Yes, well done! I did not say Simon Says, and you kept still.";
                emotion = Emotion.Positive;
                ++score;
            } else {
                toSpeak = "Oh dear, I did not say Simon says, you should have kept still.";
                emotion = Emotion.Negative;
            }

        }

        toSpeak += " Your score is " + score;
        //mu.speak(toSpeak);
        
        mu.giveFeedback(toSpeak, emotion);
        return ret;

    }

    void makeRequest() {
        long now = System.currentTimeMillis();
        String toSpeak = "";
        if (simonSays) {
            toSpeak += "Simon says";
        }
        toSpeak += " " + currentAction.getCommand();

        //mu.speak(toSpeak);
        mu.makeRequest(toSpeak, currentAction);
        //endOfRequest = now + mu.speak(toSpeak);
        lastRequest = now;

    }

    long timeSinceRequest() {
        long now = System.currentTimeMillis();
        return now - lastRequest;

    }

    boolean anyoneInZone(List<UserData> users) {
        boolean anyoneInZone = false;
        for (UserData user : users) {
            if (mu.newPlayZone(user)) {
              
            //if (mu.inPlayZone(user)) {
                anyoneInZone = true;
            }
        }
        return anyoneInZone;
    }

    UserData getActiveUser(List<UserData> users) {
        UserData chosenUser = null;
        for (UserData user : users) {
            if (mu.newPlayZone(user)) {
              
            //if (mu.inPlayZone(user)) {
                chosenUser = user;
            }
        }
        return chosenUser;
    }

    public void stopRecording() {
        kinectVideoRecorder.stop();
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
