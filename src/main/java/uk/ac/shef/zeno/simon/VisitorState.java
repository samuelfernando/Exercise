/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.shef.zeno.simon;

/**
 *
 * @author samf
 */
public enum VisitorState {
  NOTHINGNESS, BODIES, TRACKING, INZONE_START_GAME, PLAYING_GAME, END_GAME, PENDING_GOODBYE, GOODBYE_LIMIT, GOODBYE_LEFT, WAIT_FOR_PLAYER_TO_LEAVE;
}
