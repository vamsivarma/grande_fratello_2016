package sbn.gf.builder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.search.ScoreDoc;
import sbn.gf.entities.Participant;
import twitter4j.TwitterException;
import sbn.gf.entities.Supporter;
import sbn.gf.manager.SupportersIndexManager;
import sbn.gf.manager.TweetsIndexManager;

/**
 * Builder that creates index of supporters
 *
 * @author Vamsi Gunturi
 */
public class SupportersIndexBuilder extends IndexBuilder {

    // Document Structure
    private Document supporter;
    private StringField name;
    private StringField id;
    
    // To decide on a support based on a score function
    private LongField vote;
    
    // To record votes
    private ArrayList<Integer> vList;
    
    private static Participant p;

    /**
     * Initialize Builder params
     *
     * @param indexPath where the index will be stored
     * @param sourcePath where the data to create the index are stored
     */
    public SupportersIndexBuilder(String indexPath, String sourcePath) {
        
        
        super();
        
        p = new Participant();

        this.indexPath = indexPath;
        this.sourcePath = sourcePath;

        // Initialize the document
        this.supporter = new Document();
        // Initialize its fields
        this.name = new StringField("name", "", Field.Store.YES);
        this.id = new StringField("id", "", Field.Store.YES);
        this.vote = new LongField("vote", 0L, Field.Store.YES);
        

        // Add the fields to the document
        this.supporter.add(this.name);
        this.supporter.add(this.id);
        this.supporter.add(this.vote);
        
        this.vList = initList();
        
    }

    @Override
    public void build() throws IOException, TwitterException {
        // Set builder params
        setBuilderParams(indexPath);
        // Get all the supporters
        HashMap<String, Supporter> supporters = collectIndexElements();
        
        ArrayList<Integer> vList = new ArrayList<Integer>();
        
        long sCounter = 0;

        // For each supporter id
        for (String key : supporters.keySet()) {
            
            sCounter++;
            
            // Get the supporter
            Supporter s = supporters.get(key);
            // Fill the fields with the supporter info
            this.name.setStringValue(s.getName());
            this.id.setStringValue(s.getId());
            
            
            if(s.getPIndex() != -1) {
                this.vote.setLongValue(s.getPIndex());
            } else {
                
                int voteIndex = computeSupporterScore(s);
                
                this.updateVotes(voteIndex);
                
                this.vote.setLongValue(voteIndex);
            }

            // Write document
            this.writer.addDocument(this.supporter);
        }
        
        System.out.println("Total supporters: " + sCounter);
        
        System.out.println("Supporter votes: " + this.getVList());
        
        // Make a commit
        this.writer.commit();
        // Close the writer

        this.writer.close();
    }

    @Override
    public ArrayList<String> build(String fieldName, ArrayList<String> fieldValues) throws IOException {
        // Method not implemented
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    // Get all the supporters from other indices
    private HashMap<String, Supporter> collectIndexElements() throws IOException {
        
        // initialize a TweetIndexManager for the index of all the tweets
        TweetsIndexManager tim = new TweetsIndexManager("index/AllTweetsIndex");
        // initialize a TweetIndexManager for the index of all the politicians
        //Politician p = new Politician();
        // Create an hashmap of supporters
        HashMap<String, Supporter> supporters = new HashMap<String, Supporter>();

        try {
            // Set TweetIndexManager params
            tim.setReader("index/AllTweetsIndex");
            
            //Contestant to Index path map, @TODO: Need to remove this, if not used 
            Map<String, Integer> pMMap = new HashMap<String, Integer>();
            
            
            // @TODO: Need to properly optimize the code in this loop
            for (String i : p.pTMap.keySet()) {
                
                int curIndex = p.pList.indexOf(i);
                
                ArrayList<String> pT = new ArrayList<String>();
                pT.add(p.pTMap.get(i));
                
                
                ////////System.out.println("Adding Participants:");

                // Get the userId of the yes screenName just obtained
                 ScoreDoc[] results = tim.searchTermsInAField(pT, "screenName");

                 // And create a supporter for each politician
                 for (ScoreDoc doc : results) {
                     
                     Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                     supporter.setPIndex(curIndex);
                     supporters.put(supporter.getId(), supporter);
                     
                 }
                 
                 ////////System.out.println("Adding for mentioned:");
            
           
                // Search the tweets were is mentioned at leat one yes pol
                results = tim.searchTermsInAField(pT, "mentioned");

                // For each result
                for (ScoreDoc doc : results) {

                    // Get the user id of the tweet
                    String userId = tim.ir.document(doc.doc).get("userId");
                    
                    // If the supporter is already present in the map of supporters
                    if (supporters.containsKey(userId)) {
                        
                        // Get the user id of the tweet
                        String curName = tim.ir.document(doc.doc).get("name");
                        
                        Supporter supporter = supporters.get(userId);
          
                        updateMentioned(supporter, curIndex);
                        
                    } else {
                        
                        // Otherwise create a new supporter 
                        Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                        
                        updateMentioned(supporter, curIndex);
                        supporter.setPIndex(-1);
                        
                        // And add it to the map
                        supporters.put(supporter.getId(), supporter);
                    }
                }
                
                /////// Adding suporters based on terms used
           
                // If the structure selected is the relevant word
                 if ( sourcePath == "output/coreWords.json" || 
                         sourcePath == "output/coreCombWords.json" || 
                         sourcePath == "output/ccWords.json" || 
                         sourcePath == "output/ccCombWords.json" ) {
                     
                     // Get the rel words from the json and put it into a map
                     ObjectMapper mapper = new ObjectMapper();

                     HashMap<String, ArrayList<String>> representativeWordsMap;
                     representativeWordsMap = mapper.readValue(new File(sourcePath),
                             new TypeReference<HashMap<String, ArrayList<String>>>() {
                     });

                     // Add them Yes words
                     ArrayList<String> pTerms = representativeWordsMap.get(i);
                     
                     System.out.println("Adding for terms:");
                     // Divide words by hashtags
                     ArrayList<String> pWords = new ArrayList<String>();
                     ArrayList<String> pTags = new ArrayList<String>();
                     for (String term : pTerms) {
                         if (term.startsWith("#")) {
                             pTags.add(term);
                         } else {
                             pWords.add(term);
                         }
                     }
                     // Get all the tweets were there is a rel word into
                     results = tim.searchTermsInAField(pWords, "tweetText");
                     results = (ScoreDoc[]) ArrayUtils.addAll(results, tim.searchTermsInAField(pTags, "hashtags"));
                     // For each result increment yes construction count or create a new supporter
                     for (ScoreDoc doc : results) {
                         String userId = tim.ir.document(doc.doc).get("userId");
                         if (supporters.containsKey(userId)) {
                             Supporter supporter = supporters.get(userId);
                             
                             updateConstructions(supporter, curIndex);
                            
                         } else {
                             Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                             
                             updateConstructions(supporter, curIndex);
                             supporter.setPIndex(-1);
                             
                             supporters.put(supporter.getId(), supporter);
                         }
                     }
                     
                 } else {
                     
                     // Otherwise, If core or connected components are used as construction
                     ObjectMapper mapper = new ObjectMapper();
                     
                     // Get the rel constructions from the json and put it into a map
                     HashMap<String, ArrayList<ArrayList<String>>> representativeWordsMap;
                     representativeWordsMap = mapper.readValue(new File(sourcePath),
                             new TypeReference<HashMap<String, ArrayList<ArrayList<String>>>>() {
                     });

                     // And do the same procedure as before
                     ArrayList<ArrayList<String>> pCores = representativeWordsMap.get(i);
                     System.out.println("Adding for Cores or CC:");
                     results = tim.searchORANDCondInAField(pCores);
                     for (ScoreDoc doc : results) {
                         String userId = tim.ir.document(doc.doc).get("userId");
                         
                         if (supporters.containsKey(userId)) {
                             
                             Supporter supporter = supporters.get(userId);
                             
                             updateConstructions(supporter, curIndex);
                             
                         } else {
                             Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                             
                             updateConstructions(supporter, curIndex);
                             supporter.setPIndex(-1);
                             
                             supporters.put(supporter.getId(), supporter);
                         }
                     }
                 }
                 
                 
                 /////// Adding for expressions
          
                System.out.println("Adding for Expressions:");
                 // At the end add Rel Expression with the same procedure
                 results = tim.searchTermsInAField(p.pEMap.get(i), "hashtags");
                 for (ScoreDoc doc : results) {
                     String userId = tim.ir.document(doc.doc).get("userId");
                     
                     if (supporters.containsKey(userId)) {
                         
                         Supporter supporter = supporters.get(userId);
                         
                         updateExpressions(supporter, curIndex);
                     
                     } else {
                     
                         Supporter supporter = new Supporter(tim.ir.document(doc.doc).get("userId"), tim.ir.document(doc.doc).get("name"));
                         
                         updateExpressions(supporter, curIndex);
                         supporter.setPIndex(-1);
                         
                         supporters.put(supporter.getId(), supporter);
                     
                     }
                 }
                
            }
        
            
            // Return supporters
            return supporters;

        } catch (IOException ex) {
            Logger.getLogger(SupportersIndexManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    
    public ArrayList<Integer> getVList() {
        return vList;
    }
    
    public void setVList(ArrayList<Integer> vList) {
        this.vList = vList;
    }
    
    // Update Expression counters of corresponding participants
    private void updateVotes(int curIndex) {
        
        ArrayList<Integer> curV = getVList();
        int curC = curV.get(curIndex);
        curV.set(curIndex, curC + 1);

        setVList(curV);
    }
    
    public static ArrayList<Integer> initList() {
        
        //Initialize the counters of 5 participants with 0's
        List<Integer> temp = Arrays.asList( 0,0,0,0,0,0);
         
        //Create a 
        ArrayList<Integer> initList = new ArrayList<Integer>();
         
        //Copy all items from list 1 to list 2
        initList.addAll(temp);
        
        return initList;
    
    }
    
    // Update mentioned counters of corresponding participants
    private static void updateMentioned(Supporter s, int curIndex) {
        
        ArrayList<Integer> curPM = s.getPMentioned();
        int curC = curPM.get(curIndex);
        curPM.set(curIndex, curC + 1);

        s.setPMentioned(curPM);
    
    }
    
    // Update Construction counters of corresponding participants
    private static void updateConstructions(Supporter s, int curIndex) {
        
        ArrayList<Integer> curC = s.getCUsed();
        int curCounter = curC.get(curIndex);
        curC.set(curIndex, curCounter + 1);

        s.setCUsed(curC);
    
    }
    
    // Update Expression counters of corresponding participants
    private static void updateExpressions(Supporter s, int curIndex) {
        
        ArrayList<Integer> curE = s.getEUsed();
        int curC = curE.get(curIndex);
        curE.set(curIndex, curC + 1);

        s.setEUsed(curE);
    
    }
    
    // Compute the vote of a supporter on the basis of who he mentioned and which expression and construction used
    private static int computeSupporterScore(Supporter s) {
        
        
        ArrayList<Float> scoreList = new ArrayList<Float>();
        
        ArrayList<Integer> mList = s.getPMentioned();
        ArrayList<Integer> cUsed = s.getCUsed();
        ArrayList<Integer> eUsed = s.getEUsed();
        
        //System.out.println("-------------------------");
        
        //System.out.println("Mentioned List: " + mList);
        
        //System.out.println("Constructions List: " + cUsed);
        
        //System.out.println("Expressions List: " + eUsed);
        
        //System.out.println("-------------------------");
        
        for(int i=0; i < p.pList.size(); i++) {
            
            float curScore = (float) ( mList.get(i) + 1 * cUsed.get(i) + 3 * eUsed.get(i)) ;
            scoreList.add(curScore);
        }
        
        float maxVal = Collections.max(scoreList); 
        int voteIndex = p.pList.size();
        
        //System.out.println("Maximum score: " + maxVal);
        
        if(maxVal != 0) {
            voteIndex = scoreList.indexOf(maxVal);
        } 
        
        return voteIndex;
        
    }
}
