/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.shef.zeno.simon;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author samf
 */
public class PlayPatterns {
    List<PlayPattern> patterns;
    int count;
    private static final int NO_PATTERNS=6;
    PlayPatterns(int startCount) {
        
        try {
            count = startCount;
            String actionLine, simonLine;
            BufferedReader actionReader = new BufferedReader(new FileReader("config/actions-out.txt"));
            BufferedReader simonReader = new BufferedReader(new FileReader("config/rands-out.txt"));
            patterns = new ArrayList<PlayPattern>();
            int id =0;
            while ((actionLine=actionReader.readLine())!=null) {
                simonLine = simonReader.readLine();
                PlayPattern pattern = new PlayPattern(id++);
                String splits[] = actionLine.split("\t");
                for (String action : splits) {
                    if (action.equals("wave")) {
                        pattern.addAction(Action.WAVE);
                    }
                    else if (action.equals("jump")) {
                        pattern.addAction(Action.JUMP);
                    }
                    else if (action.equals("hands-up")) {
                        pattern.addAction(Action.HANDS_UP);
                    }
                }
                splits = simonLine.split("\t");
                for (String boolStr : splits) {
                    if (boolStr.equals("0")) {
                        pattern.addSimon(false);
                    }
                    else if (boolStr.equals("1")) {
                        pattern.addSimon(true);
                    }
                }
                patterns.add(pattern);
            }
            
        } catch (Exception ex) {
            Logger.getLogger(PlayPatterns.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    }
    
    PlayPattern getNextPattern() {
        PlayPattern pattern = patterns.get(count%NO_PATTERNS);
        count = count+1;
        return pattern;
    }
}
