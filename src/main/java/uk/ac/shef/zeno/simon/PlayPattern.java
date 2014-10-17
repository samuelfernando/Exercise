/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.shef.zeno.simon;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author samf
 */
public class PlayPattern {
    List<Action> actions;
    List<Boolean> simonSeq;
    int actionCount, simonCount;
    int id;
    static final int NO_ROUNDS = 10;
    PlayPattern(int id) {
        this.id = id;
        actionCount = 0;
        simonCount=0;
        actions = new ArrayList<Action>();
        simonSeq = new ArrayList<Boolean>();
    }
    void addAction(Action action) {
        actions.add(action);
    }

    void addSimon(boolean b) {
        simonSeq.add(b);
    }
    Action getNextAction() {
        Action ret = actions.get(actionCount%NO_ROUNDS);
        actionCount++;
        return ret;
        
    }
    boolean getNextSimon() {
        boolean ret = simonSeq.get(simonCount%NO_ROUNDS);
        simonCount++;
        return ret;
    }
    @Override
    public String toString() {
        return "playPattern"+id;
    }
    
}
