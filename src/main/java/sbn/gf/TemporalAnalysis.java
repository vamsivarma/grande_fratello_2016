/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbn.gf;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import org.apache.lucene.document.Document;
import sbn.gf.entities.ClusterGraph;

import sbn.gf.entities.Participant;

import sbn.gf.entities.TweetTerm;
import sbn.gf.factory.ClusterGraphFactory;
import sbn.gf.manager.TweetsIndexManager;

/**
 *
 * @author vamsigunturi
 */
public class TemporalAnalysis {
    
    private static Participant p = new Participant();
    
    // Initialize a map in which put all relevant componens for keys "yes" and "no". Each key has a list of 10 elements as value
    public static HashMap<String, ArrayList<ArrayList<String>>> relComps = new HashMap<String, ArrayList<ArrayList<String>>>();
    
    // Initialize a map in which put all relevant Cores for keys "yes" and "no". Each key has a list of 10 elements as value
    public static HashMap<String, ArrayList<ArrayList<String>>> relCores = new HashMap<String, ArrayList<ArrayList<String>>>();
    
    // Initialize a map in which put all relevant words for keys "yes" and "no".
    public static HashMap<String, ArrayList<String>> ccWords = new HashMap<String, ArrayList<String>>();
        
    public static HashMap<String, ArrayList<String>> coreWords = new HashMap<String, ArrayList<String>>();
    
    public static void temporal_analysis() throws IOException {
        
        //Generate the indices needed in firt task of part 0
        indexCreation();
        
        // Find frequently occuring words for each index that was created for each participant
        for (String i : p.pMap.keySet()) {
            
            relComps.put(i , new ArrayList<ArrayList<String>>());   
            relCores.put(i, new ArrayList<ArrayList<String>>()); 
            
            //System.out.println("key: " + i + " value: " + pMap.get(i));
            
            ArrayList<TweetTerm> pList = findFrequentWords(p.pMap.get(i), i);
            
            p.pWMap.put(i, pList);
            
            createClusterGraphs(pList, p.pMap.get(i), i);
            
        }
        
        writeTweetWords();
        
    }
    
    private static void indexCreation() throws IOException {
        
        // Initialize a TweetsIndexManager for the index of all tweets
        TweetsIndexManager tim = new TweetsIndexManager("index/AllTweetsIndex");

        // If the index of all tweets doesn't exist
        Path dir = Paths.get("index/AllTweetsIndex");
        
        if (!Files.exists(dir)) {
            // Create it
            tim.create("input/stream");
            //tim.create("input/basic");
        } else {
            // Advise the index already exist
            System.out.println(dir.toString() + ": Index already created!");
        }
        
        int totalTweetSize = tim.getIndexSizes();
        
        System.out.println("TOTAL TWEETS INDEXED: " + totalTweetSize);

        // Initialize a politician object to segregate Yes and No politicians
        //Politician p = new Politician();
        
        
        // We need tweet index managers for following contestants
        // Alessia Macari, Valeria Marini, Elenoire Casalegno, Andrea Damante, Pamela Prati
    
        //Create all the indexes for below contestents
        
        // Alessia Macari - AlessiaMacari1 - 1385 - am
        //TweetsIndexManager amTim = new TweetsIndexManager("index/AMI"); 
        TweetsIndexManager amTim = createParticipantIndex("index/AMI", "Alessia Macari", "AlessiaMacari1");
        p.pMap.put("Alessia Macari", amTim);
        p.pIMap.put("Alessia Macari", "index/AMI");
        p.pTMap.put("Alessia Macari", "AlessiaMacari1");      
        p.pList.add("Alessia Macari");
        p.pTList.add("AlessiaMacari1");
        
        
        // Valeria Marini - ValeriaMariniVM - 1200 - vm
        //TweetsIndexManager vmTim = new TweetsIndexManager("index/ValeriaMariniIndex"); 
        TweetsIndexManager vmTim = createParticipantIndex("index/VMI", "Valeria Marini", "ValeriaMariniVM");
        p.pMap.put("Valeria Marini", vmTim);
        p.pIMap.put("Valeria Marini", "index/VMI");
        p.pTMap.put("Valeria Marini", "ValeriaMariniVM");
        p.pList.add("Valeria Marini");
        p.pTList.add("ValeriaMariniVM");
        
        // Elenoire Casalegno - elenoirec - 1094 - ec
        //TweetsIndexManager ecTim = new TweetsIndexManager("index/ElenoireCasalegnoIndex");
        TweetsIndexManager ecTim = createParticipantIndex("index/ECI", "Elenoire Casalegno", "elenoirec");
        p.pMap.put("Elenoire Casalegno", ecTim);
        p.pIMap.put("Elenoire Casalegno", "index/ECI");
        p.pTMap.put("Elenoire Casalegno", "elenoirec");
        p.pList.add("Elenoire Casalegno");
        p.pTList.add("elenoirec");
        
        // Andrea Damante - AndreaDamante - 262 - ad
        //TweetsIndexManager adTim = new TweetsIndexManager("index/AndreaDamanteIndex");
        TweetsIndexManager adTim = createParticipantIndex("index/ADI", "Andrea Damante", "AndreaDamante");
        p.pMap.put("Andrea Damante", adTim);
        p.pIMap.put("Andrea Damante", "index/ADI");
        p.pTMap.put("Andrea Damante", "AndreaDamante");
        p.pList.add("Andrea Damante");
        p.pTList.add("AndreaDamante");
        
         // Pamela Prati - PamelaPrati - 119 - pp
        //TweetsIndexManager ppTim = new TweetsIndexManager("index/PamelaPratiIndex");
        TweetsIndexManager ppTim = createParticipantIndex("index/PPI", "Pamela Prati", "PamelaPrati");
        p.pMap.put("Pamela Prati", ppTim);
        p.pIMap.put("Pamela Prati", "index/PPI");
        p.pTMap.put("Pamela Prati", "PamelaPrati");
        p.pList.add("Pamela Prati");
        p.pTList.add("PamelaPrati");
        
    }
    
    public static ArrayList<TweetTerm> findFrequentWords(TweetsIndexManager pObj, String pName) {  
        
        // Define a time interval for SAX procedure (12h)
        long timeInterval = 43200000L;
        
        // @TODO: Need to double check the validity of this regular expression
        
        // Define the regex to be match that shows a pattern of collective attention
        String regex = "a+b+a*b*a*";
        
        // Relevant field in which search relevant words
        String[] fieldNames = {"tweetText", "hashtags"};
        
        // Get Yes and No relevant words
        ArrayList<TweetTerm> pList = pObj.getRelFieldTerms(fieldNames, regex, timeInterval);
        
        HashMap<String, Integer> pW = new HashMap<String, Integer>();
        
        //System.out.println("Word list length: " + pList.size());
        
        
        int termCounter = 0;
        
        //System.out.println("Top 10 words tweeted by " + pName);
        
        for (TweetTerm pT: pList) {
            
            pW.put(pT.getWord(), (int) pT.getFrequency());
            
            termCounter++;
            
            //if(termCounter <= 10) {
                
                //System.out.println("---------------------------------------");      
                //System.out.println("Term: " + pT.getWord());
                //System.out.println("Date: " + pT.getSaxRep());
                //System.out.println("Frequency: " + pT.getFrequency());
                //System.out.println("---------------------------------------");
            
            //}
               
        }
        
        return pList;
    }
    
    public static void writeTweetWords() {
    
         // Save maps obtained in json
        ObjectMapper mapper = new ObjectMapper();
        try {
            
            // Words from connected components
            mapper.writeValue(new File("output/ccWords.json"), ccWords);
            
            // Words from k-cores
            mapper.writeValue(new File("output/coreWords.json"), coreWords);
            
            mapper.writeValue(new File("output/relComps.json"), relComps);
            mapper.writeValue(new File("output/relCores.json"), relCores);
            
            for (String i : p.pWMap.keySet()) {
                mapper.writeValue(new File("output/" + i + "Words.json"), p.pWMap.get(i));   
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    
    
    }
    
    private static TweetsIndexManager createParticipantIndex(String pPath, String pName, String ptName) throws IOException {
        
        TweetsIndexManager pObj = new TweetsIndexManager(pPath); 
        
        ArrayList<String> pNameAL = new ArrayList<String>();
        
        // If the index of all yes tweets doesn't exist
        Path dir = Paths.get(pPath);
        if (!Files.exists(dir)) {
            // Create it collecting all the yes ploticians screen name
            pNameAL.add(ptName);
            
            pNameAL = pObj.create("index/AllTweetsIndex", "", pNameAL);
            
            ArrayList<Document> pObjDocs = pObj.getAllDocs();
            System.out.println( pName + " tweets count: " + pObjDocs.size()); 
            
            saveTweetTimestamps(pObjDocs, pName);
            
        } else {
            // Advise the index already exist
            System.out.println(dir.toString() + ": Index already created!");
        }
         
        return pObj;
        
    }
    
    private static void saveTweetTimestamps(ArrayList<Document> pDocs, String pName) {
        
        try (PrintWriter writer = new PrintWriter(new File("tweet_timestamp/" + pName + "_tt.csv"))) {
             StringBuilder sb = new StringBuilder();
             
             for (Document doc: pDocs){

                    //System.out.println("---------------------------------------");
                    //System.out.println("Hashtags: " + doc.get("hashtags"));
                    //System.out.println("Mentioned: " + doc.get("mentioned"));
                    //System.out.println("Mentioned: " + doc.get("followers"));
                    
                     sb.append(doc.get("date"));
                    
                     sb.append('\n');

                    //System.out.println("---------------------------------------"); 

             }
             
             writer.write(sb.toString());
             
             //System.out.println( pName + " tweet timestamps saved!" );
     

        } catch (FileNotFoundException e) {
              System.out.println(e.getMessage());
        }
    
    
    }
    
    private static void createClusterGraphs(ArrayList<TweetTerm> pList, TweetsIndexManager pObj, String pName) {
        
        // List of all the words
        ArrayList<String> ccWordsList = new ArrayList<String>();
        
        ArrayList<String> coreWordsList = new ArrayList<String>();
        
        
         // @TODO: Need to assign these 2 dynamically from Elbow method

        // N° of cluster in witch divide the words found
        int nCluster = 2;
        // max number of iteration for k-means
        int nIter = 1000;

        // Initialize a ClusterGraphFactory in order to create graphs generated by k-means
        // Just created, these graph compute their cc and cores storing them in attributes.
        ClusterGraphFactory cgf = new ClusterGraphFactory(nCluster, nIter);

        // Participant graphs generated by the factory
        ArrayList<ClusterGraph> pGraphs = cgf.generate(pList, pObj);
        
        
        // For each participant word cluster...
        for (ClusterGraph cg : pGraphs) {

            // ...Get the core elements and save them in coreList
            int[] core = cg.getCore().seq;
            ArrayList<Integer> coreList = new ArrayList<Integer>();
            for (int k = 0; k < core.length; k++) {
                coreList.add(core[k]);
            }
            
            for (String word : cg.getWords(coreList)) {
               coreWordsList.add(word);
            }

            // Get all the labels of the words in the core and save them in relCores
            relCores.get(pName).add(cg.getWords(coreList));  

            // Get cluster comps
            Set<Set<Integer>> comps = cg.getComps();
            // For each comp
            for (Set<Integer> comp : comps) {
                // Get all the elements of the comp
                ArrayList<Integer> compElems = new ArrayList<Integer>();
                for (int elem : comp) {
                    compElems.add(elem);
                }

                // Get all the labels of the words in the comp elements and save them in relComps
                relComps.get(pName).add(cg.getWords(compElems));
                
                // Add all the words found in the list of the yes words
                for (String word : cg.getWords(compElems)) {
                    ccWordsList.add(word);
                }

            }
        }
        
        // Add Words to the words map
        ccWords.put(pName, ccWordsList);      
        coreWords.put(pName, coreWordsList);
        
    }
        
       
    
}
