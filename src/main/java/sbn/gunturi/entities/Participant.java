/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbn.gunturi.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import sbn.gunturi.manager.TweetsIndexManager;

/**
 *
 * @author vamsigunturi
 */
public class Participant {
    
    // @TODO: Need to do this more efficiently and merge all these maps in to a single map for ease of access
    //Contestant to TweetIndexObject map
    public static Map<String, TweetsIndexManager> pMap = new HashMap<String, TweetsIndexManager>();
    
    //Contestant to TweetIndexObject map
    public static Map<String, String> pTMap = new HashMap<String, String>();
    
    //Contestant to Index path map, @TODO: Need to remove this, if not used 
    public static Map<String, String> pIMap = new HashMap<String, String>();
    
    // Contestents to expressionns map
    public static HashMap<String, ArrayList<String>> pEMap = new HashMap<String, ArrayList<String>>();
            
    //Contestant to Index path map 
    public static Map<String, ArrayList<TweetTerm>> pWMap = new HashMap<String, ArrayList<TweetTerm>>();
    
    // List for participant names
    public static ArrayList<String> pList = new ArrayList<String>();
    
    // List for participant twitter names
    public static ArrayList<String> pTList = new ArrayList<String>();
    
    
    public Participant() {
        
    }

}
