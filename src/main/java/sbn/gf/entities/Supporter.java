package sbn.gf.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This entity represents the supporter, someone who mentioned politicians, used structures 
 * discovered during the analysis and/or use yes and/or no well known expression 
 * @author Vamsi Gunturi
 */
public class Supporter {
    // User twitter id
    private String id;
    // User twitter screen name
    private String name;
    
    // To record politicians mentioned
    private ArrayList<Integer> pMentioned;
    
    // To record constructions used
    private ArrayList<Integer> cUsed;
    
    // To record expressions used
    private ArrayList<Integer> eUsed;
    
    
    // To differentiate between normal supporter and a participant
    private Integer pIndex;

    /**
     * Initialize user id and screen name
     * @param id user id
     * @param name screen name
     */
    public Supporter(String id, String name) {
        this.id = id;
        this.name = name;
        
        // Initial value of the other attributes
        this.pMentioned = initList();
        this.cUsed = initList();
        this.eUsed = initList();
        this.pIndex = -1;
        
    }
    
    public static ArrayList<Integer> initList() {
        
        //Initialize the counters of 5 participants with 0's
        List<Integer> temp = Arrays.asList( 0,0,0,0,0);
         
        //Create a 
        ArrayList<Integer> initList = new ArrayList<Integer>();
         
        //Copy all items from list 1 to list 2
        initList.addAll(temp);
        
        return initList;
    
    }

    /**
     *
     * @return user id
     */
    public String getId() {
        return id;
    }
    
    /**
     *
     * @return the screen name
     */
    public String getName() {
        return name;
    }
    
    public ArrayList<Integer> getPMentioned() {
        return pMentioned;
    }
    
    public ArrayList<Integer> getCUsed() {
        return cUsed;
    }
   
    public ArrayList<Integer> getEUsed() {
        return eUsed;
    }
    
    public Integer getPIndex() {
        return pIndex;
    }
    
    public void setPMentioned(ArrayList<Integer> pMentioned) {
        this.pMentioned = pMentioned;
    }

   
    public void setCUsed(ArrayList<Integer> cUsed) {
        this.cUsed = cUsed;
    }

   
    public void setEUsed(ArrayList<Integer> eUsed) {
        this.eUsed = eUsed;
    }
    
    public void setPIndex(Integer pIndex) {
        this.pIndex = pIndex;
    }
    
}
