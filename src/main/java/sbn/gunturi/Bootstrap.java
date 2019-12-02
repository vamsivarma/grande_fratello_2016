package sbn.gunturi;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.stilo.g.algo.ConnectedComponents;
import it.stilo.g.algo.HubnessAuthority;
import it.stilo.g.algo.SubGraph;
import it.stilo.g.structures.DoubleValues;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.util.NodesMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.lucene.document.Document;
import sbn.gunturi.algorithms.ComunityLPA;
import sbn.gunturi.entities.TweetTerm;
import sbn.gunturi.entities.ClusterGraph;
import sbn.gunturi.entities.Participant;
import sbn.gunturi.manager.TweetsIndexManager;
import sbn.gunturi.factory.ClusterGraphFactory;
import sbn.gunturi.manager.SupportersIndexManager;

/**
 * Bootstrap point for the application 
 * @author Vamsi Gunturi
 */
public class Bootstrap {
    
    private static Participant p = new Participant();

    // Initialize a map in which put all relevant componens for keys "yes" and "no". Each key has a list of 10 elements as value
    public static HashMap<String, ArrayList<ArrayList<String>>> relComps = new HashMap<String, ArrayList<ArrayList<String>>>();
    
    // Initialize a map in which put all relevant Cores for keys "yes" and "no". Each key has a list of 10 elements as value
    public static HashMap<String, ArrayList<ArrayList<String>>> relCores = new HashMap<String, ArrayList<ArrayList<String>>>();
    
    // Initialize a map in which put all relevant words for keys "yes" and "no".
    public static HashMap<String, ArrayList<String>> ccWords = new HashMap<String, ArrayList<String>>();
        
    public static HashMap<String, ArrayList<String>> coreWords = new HashMap<String, ArrayList<String>>();
    
    /**
     * Main method from which the whole analysis starts and is divided in to 
     * three parts: 
     * 1) Temporal Analysis 
     * 2) Identify mentions of candidates or YES/NO supporter 
     * 3) Spread of Influence
     * @param args
     */
    public static void main(String[] args) throws IOException {
        
        // This is the step 1
        temporal_analysis();
        
       
        // Step 2
        // For performing YES/NO supporter analysis
        yes_or_no_analysis();

        // Step 3    
        // For performing spread of influence
        spread_of_influence();
    }

    private static void temporal_analysis() throws IOException {
        
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
            
            if(termCounter <= 10) {
                
                //System.out.println("---------------------------------------");      
                //System.out.println("Term: " + pT.getWord());
                //System.out.println("Date: " + pT.getSaxRep());
                //System.out.println("Frequency: " + pT.getFrequency());
                //System.out.println("---------------------------------------");
            
            }
               
        }
        
        return pList;
    }
    
    private static void createClusterGraphs(ArrayList<TweetTerm> pList, TweetsIndexManager pObj, String pName) {
        
        // List of all the words of yes and no
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

        // Yes graphs generated by the factory
        ArrayList<ClusterGraph> pGraphs = cgf.generate(pList, pObj);
        
        
        // For each yes cluster...
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

    private static void yes_or_no_analysis() {

        try {

            
            ArrayList<String> noExp = new ArrayList<String>();
            
            for (String i : p.pTMap.keySet()) {
                
                // Create lists of yes and no expressionas
                ArrayList<String> pExp = new ArrayList<String>();
                pExp.add("#" + p.pTMap.get(i));
                
                p.pEMap.put(i, pExp);   
            }
            
            // Need to add expressions of individual participants separately
            
            // Add hashtags for Alessia Macari
            p.pEMap.get("Alessia Macari").addAll(Arrays.asList("#cioco", "#macari", "#alessia", 
                                                                "#frosinoneculone", "#laciociaria", "#donne", "#malessia"));
            
            
            // Add hashtags for Valeria Marini
            p.pEMap.get("Valeria Marini").addAll(Arrays.asList("#valeria", "#marini", "#donne", "#mvaleria"));
            
            // Add hashtags for Andrea Damante
            p.pEMap.get("Andrea Damante").addAll(Arrays.asList("#andrea", "#damante", "#damellis", "#giuliadelellis", "#andreaiannone", "#uomo", "#dandrea"));
            
            // Add hashtags for Elenoire Casalegno
            p.pEMap.get("Elenoire Casalegno").addAll(Arrays.asList("#elenoire", "#casalegno", "#donne", "#celenoire"));
            
            // Add hashtags for Pamela Prati
            p.pEMap.get("Pamela Prati").addAll(Arrays.asList("#pamela", "#prati", "#donne", "#pratiful"));
            
            
            
            // Initialize a SupportersIndexManager
            SupportersIndexManager sim = new SupportersIndexManager("index/SupportersIndex");
            // If the index doesn't exist yet
            Path dir = Paths.get("index/SupportersIndex");
            if (!Files.exists(dir)) {
                // Create it
                // @TODO: Identify difference in results from CC and Cores
                
                sim.create("output/relComps.json");
                //sim.create("output/relCores.json");
                //sim.create("output/ccWords.json");
                //sim.create("output/ccCombWords.json");
                
                //sim.create("output/coreWords.json");
                //sim.create("output/coreCombWords.json");
                
                
            } else {
                System.out.println(dir.toString() + ": Index already created!");
            }
            
            
            
            // Get all supporters ids and save them in a list
            ArrayList<String> nodes = sim.getFieldValuesList(sim.getAllDocs(), "id");
            TweetsIndexManager tim = new TweetsIndexManager("index/AllTweetsIndex");
            
            System.out.println("Total supporters: " + nodes.size());
            
            HashSet noDupSet = new HashSet();
            
            for(String node: nodes) {
                noDupSet.add(node);            
            }
            
            System.out.println("Number of unique supporters: " + noDupSet.size());
            
            System.out.println("Total supporters: " + nodes.size());
            
            // Set number of workers
            int worker = (int) (Runtime.getRuntime().availableProcessors());

            // Connected Component SubGraph
            WeightedDirectedGraph ccsg;
            
            // A mapper for node label and id
            NodesMapper<String> nodeMapper = new NodesMapper<String>();

            dir = Paths.get("output/ccsg.txt");
            // If the list of the edges of ccsg doesn't exist
            if (!Files.exists(dir)) {
                // Create it
                createCCSG(nodes);
            } 

            // Build the graph
            FileReader fr = new FileReader("output/ccsg.txt");
            BufferedReader br = new BufferedReader(fr);

            String line;
            

            ccsg = new WeightedDirectedGraph(55000 + 1);
            nodeMapper = new NodesMapper<String>();
            // Creating the graph
            while ((line = br.readLine()) != null) {
                String[] splittedLine = line.split(" ");
                ccsg.add(nodeMapper.getId(splittedLine[1]), nodeMapper.getId(splittedLine[0]), Integer.parseInt(splittedLine[2]));
            }

            br.close();
            fr.close();
            
            ArrayList<DoubleValues> dummyList = new ArrayList<DoubleValues>();
            
            // Group supporters based on vote
            HashMap<String, HashMap<String, String>> supMap = group_supporters("Supporters", ccsg, nodeMapper, 
                                                                                sim, ccsg.size, dummyList, false);
            
            // Save supporters group
            save_supporters_group(supMap, "supporters", nodeMapper);
            
            // Compute HITS
            ArrayList<ArrayList<DoubleValues>> hitsResult = HubnessAuthority.compute(ccsg, 0.00001, worker);
            // Get authorities
            ArrayList<DoubleValues> authorities = hitsResult.get(0);
            
            System.out.println("No of authorities: " + authorities.size());
            
            FileWriter fileWriter;
            PrintWriter printWriter;
            
            // Save authorities
            fileWriter = new FileWriter("output/authorities.txt");
            printWriter = new PrintWriter(fileWriter);

            for (DoubleValues authority : authorities) {
                printWriter.print(authority.index + "; " + authority.value + "\n");
            }
            printWriter.close();
            fileWriter.close();
            
            int iLimit = (9000 < authorities.size() ? 9000 : authorities.size());
            
             // Group authorities based on vote
            HashMap<String, HashMap<String, String>> authMap = group_supporters("Authorities", ccsg, nodeMapper, 
                                                                                sim, iLimit, authorities, true);
            
            // Save authorities group
            save_supporters_group(authMap, "authorities", nodeMapper);
            
            
            // Finding Hubs
            
            ArrayList<DoubleValues> hubs = hitsResult.get(1);
            iLimit = hubs.size();
            
            // Finding Hubs
            HashMap<String, HashMap<String, String>> hubMap = group_supporters("Hubs", ccsg, nodeMapper, 
                                                                                sim, iLimit, hubs, true);
            
            // Save hubs group
            save_supporters_group(hubMap, "hubs", nodeMapper);
            
            // Save all the hubs
            fileWriter = new FileWriter("output/hubs.txt");
            printWriter = new PrintWriter(fileWriter);

            for (DoubleValues hub : hubs) {
                printWriter.print(hub.index + "; " + hub.value + "\n");
            }
            printWriter.close();

            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static HashMap<String, HashMap<String, String>> group_supporters(String sType, 
                                                        WeightedDirectedGraph ccsg, 
                                                        NodesMapper<String> nodeMapper,
                                                        SupportersIndexManager sim,
                                                        int iLimit,
                                                        ArrayList<DoubleValues> hList,
                                                        Boolean limitFlag) {
    
        HashMap<String, HashMap<String, String>> gMap = new HashMap<String, HashMap<String, String>>();
            
        //Initialize Supporters map
        // For each supporter id
        for (String key : p.pMap.keySet()) {
            gMap.put(key, (new HashMap<String, String>()));
        }

        int nfCounter = 0;

        for(int i = 1; i < iLimit; i++){
            //System.out.println("UserId: " + nodeMapper.getNode(i));
            if(nodeMapper.getNode(i) != null) {
                
                int curIndex = i;
                
                if(limitFlag) {
                    curIndex = hList.get(i).index;
                }

                ArrayList<String> row = new ArrayList<>();

                ArrayList<Document> gMapper = sim.searchForField("id", nodeMapper.getNode(curIndex), 10);

                if(!gMapper.isEmpty()) {
                    Document supporter = gMapper.get(0);

                    //System.out.println(supporter.get("vote"));

                    // Need to go better exception handling here...
                    int curVote = Integer.parseInt(supporter.get("vote"));
                    String curKey = p.pList.get(curVote);
                    
                    HashMap<String, String> curMap = gMap.get(curKey);
                    
                    // For authorities and hub we need maximum 1000 per group or participant
                    if(limitFlag && curMap.size() > 1000) {    
                        continue; 
                    }
                    
                    gMap.get(curKey).put(supporter.get("id"), supporter.get("name"));


                } else {
                    nfCounter++;
                }
            } else {
                nfCounter++;
            }
        }

        System.out.println( sType + " not found: " + nfCounter);
        
        return gMap;
    
    }
    
    
    private static void save_supporters_group(HashMap<String, HashMap<String, String>> gMap, 
                                String gType, NodesMapper<String> nodeMapper) throws IOException {
    
        FileWriter fileWriter;
        PrintWriter printWriter;


        // Save the supporters list to external files
        for (String key : gMap.keySet()) {

            int sCounter = 0;
            HashMap<String, String> curG = gMap.get(key);

            fileWriter = new FileWriter("output/" + key +  "_" + gType + ".txt");
            printWriter = new PrintWriter(fileWriter);


            for (String gKey : curG.keySet()) {

                

                printWriter.print(gKey + "; "
                        + curG.get(gKey) + "; "
                        + nodeMapper.getId(gKey) + "\n");
            }
            printWriter.close();

            

            System.out.println( key + " has " + curG.size() + " " +  gType);
        }
    }

    // Save the Connected component subGraph of an initial graph based on our supporters
    private static void createCCSG(ArrayList<String> nodes) throws FileNotFoundException, IOException, InterruptedException {

        // Create a weighted directed graph that will be saved
        WeightedDirectedGraph ccsg;
        // Set number of workers
        int worker = (int) (Runtime.getRuntime().availableProcessors());

        // Relative path to the original graph
        String sourcePath = "input/Official_SBN-ITA-2016-Net.gz";

        // Zip file reader
        FileInputStream fstream = new FileInputStream(sourcePath);
        GZIPInputStream gzstream = new GZIPInputStream(fstream);
        InputStreamReader isr = new InputStreamReader(gzstream, "UTF-8");
        BufferedReader br = new BufferedReader(isr);

        String line;
        // Inizialize a mapper
        NodesMapper<String> nodeMapper = new NodesMapper<String>();

        // To get the number of nodes of the graph
//            HashSet<Integer> nodeIds = new HashSet<Integer>();
//
//            while ((line = br.readLine()) != null) {
//                String[] splittedLine = line.split("\t");
//                nodeIds.add(nodeMapper.getId(splittedLine[0]));
//                nodeIds.add(nodeMapper.getId(splittedLine[1]));
//            }
//            
//            System.out.println(nodeIds.size()); //450193
        //Initial graph
        WeightedDirectedGraph g = new WeightedDirectedGraph(450193 + 1);
        // Populate the graph
        while ((line = br.readLine()) != null) {
            String[] splittedLine = line.split("\t");
            g.add(nodeMapper.getId(splittedLine[0]), nodeMapper.getId(splittedLine[1]), Integer.parseInt(splittedLine[2]));
        }

        br.close();
        isr.close();
        gzstream.close();
        fstream.close();

        // Get all the nodes ids
        int[] ids = new int[nodes.size()];

        int i = 0;
        // For each supporter id
        for (String node : nodes) {
            // If the node id corresponding to its id is in the graph
            if (nodeMapper.getId(node) < 450193) {
                // Add it
                ids[i] = nodeMapper.getId(node);
                i++;
            }
        }
        // Resize the array of supporters id (to remove null pointers in the array).
        ids = Arrays.copyOf(ids, i);

        // Extract the sub graph of the supporters
        WeightedDirectedGraph sg = SubGraph.extract(g, ids, worker);

        // get the connected components
        Set<Set<Integer>> comps = ConnectedComponents.rootedConnectedComponents(sg, ids, worker);

        System.out.println("CONNECTED COMPONENTS DONE");

        // get the one higher on
        int max = 0;
        Set<Integer> maxElem = new HashSet<Integer>();

        for (Set<Integer> comp : comps) {
            if (comp.size() > max) {
                max = comp.size();
                maxElem = comp;
            }
        }

        // get the array version of max cc nodes
        Integer[] maxElemArray = maxElem.toArray(new Integer[maxElem.size()]);

        // Parse the ids in int
        int[] ccids = new int[maxElemArray.length];
        for (i = 0; i < maxElemArray.length; i++) {
            ccids[i] = maxElemArray[i].intValue();
        }

        // Exstract the connected component subgraph
        ccsg = SubGraph.extract(sg, ccids, worker);

        // Save its edges
        FileWriter fileWriter = new FileWriter("output/ccsg.txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);

        for (i = 0; i < ccsg.out.length; i++) {
            if (ccsg.out[i] != null) {
                for (int j = 0; j < ccsg.out[i].length; j++) {
                    printWriter.print(nodeMapper.getNode(i) + " " + nodeMapper.getNode(ccsg.out[i][j]) + " 1\n");
                }
            }
        }
        printWriter.close();
    }

    // compute label propagation in the entire graph using as prelabeled nodes
    // 1) Authorities
    // 2) Hubs
    private static void spread_of_influence() {
        
        // Path of the entire graph
        String sourcePath = "input/Official_SBN-ITA-2016-Net.gz";
        FileInputStream fstream;
        try {
            // Zip file reader
            fstream = new FileInputStream(sourcePath);
            GZIPInputStream gzstream = new GZIPInputStream(fstream);
            InputStreamReader isr = new InputStreamReader(gzstream, "UTF-8");
            BufferedReader br = new BufferedReader(isr);

            String line;
            NodesMapper<String> nodeMapper = new NodesMapper<String>();
            WeightedDirectedGraph g = new WeightedDirectedGraph(450193 + 1);

            // import the entire graph
            while ((line = br.readLine()) != null) {
                String[] splittedLine = line.split("\t");
                g.add(nodeMapper.getId(splittedLine[1]), nodeMapper.getId(splittedLine[0]), Integer.parseInt(splittedLine[2]));
            }

            // Get the number of workers
            int worker = (int) (Runtime.getRuntime().availableProcessors());
            
            System.out.println("Initializing labels for supporters:");
            
            // obtain the initial label by the files of yes and no Supporters
            int[] initLabels = getInitLabel("supporters", g);
            
            // System.out.println(initLabels);
            
            // Compute LPA for Supporters
            int[] labelsSupporters = ComunityLPA.compute(g, .99d, worker, initLabels);
            
            display_LPA_results("Supporters", labelsSupporters);
            
            System.out.println("Initializing labels for hubs:");
            
            // obtain the initial label by the files of yes and no hubs
            initLabels = getInitLabel("hubs", g);
            // Compute LPA for hubs
            int[] labelsHubs = ComunityLPA.compute(g, .99d, worker, initLabels);
            
            // Display label results of Hubs
            display_LPA_results("Hubs", labelsHubs);
            
            System.out.println("Initializing labels for authorities:");
       
            // obtain the initial label by the files of yes and no hubs
            initLabels = getInitLabel("authorities", g);
            
            // Compute LPA for authorities
            int[] labelsAuthorities = ComunityLPA.compute(g, .99d, worker, initLabels);
            
            // Display label results of Hubs
            display_LPA_results("Authorities", labelsAuthorities);
            

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static int[] getInitLabel(String pathLabel, WeightedDirectedGraph g) {
        
        int[] initLabel = new int[g.size];
        
        FileReader fr;
        
        try {
            
            for (String key : p.pMap.keySet()) {
                
                String file_path = "output/" + key + "_" + pathLabel + ".txt";
                
                //System.out.println("File path is: " + file_path);
                
                fr = new FileReader(file_path);
                BufferedReader br = new BufferedReader(fr);

                String line;

                while ((line = br.readLine()) != null) {
                    //System.out.println(line);

                    String s = line.split("; ")[2];

                    if(isInteger(s)) {
                        int id = Integer.parseInt(s);

                        if(initLabel.length >= id && id >=0) {
                            initLabel[id] = p.pList.indexOf(key) + 1; // since the arraylist index starts from 0
                        }
                    }
                }
                br.close();
            }
            
            //System.out.println("Init labels length: " + initLabel.length);
            
            return initLabel;
           
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Bootstrap.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }
    
    
    private static void display_LPA_results(String rType, int[] lpaLabels) {
        
        List<Integer> lpaList = Arrays.asList(0,0,0,0,0);
        
        
        int ucCounter = 0;
        
        //try {
            
            // Count the nodes for each label
            for (int curLabel : lpaLabels) {

                if(curLabel <= 0) {
                    // If the node label is either 0 or -1 that means it is un classified
                    ucCounter++;
                } else {
                    // If it is greater than or equal to 1 then it is a categorized label
                    // Since arraylist index starts from 
                    int curIndex = curLabel - 1;
                    int curC = lpaList.get(curIndex);
                    lpaList.set(curIndex, curC + 1);
                }

            }
        
            System.out.println("----------" +  rType +"----------");
            
            //System.out.println("P List size: " + p.pList.size());

            for(int i=0; i < p.pList.size(); i++) {
                System.out.println(p.pList.get(i) + " has " + lpaList.get(i) + " " +  rType.toLowerCase());
            }

            System.out.println("UNCLASSIFIED " + rType.toLowerCase() + ": " +  ucCounter);
        
        
        //} catch(Exception e) {
        //    System.out.println(e);
        //}
        
        
    }
    
    public static ArrayList<Integer> initList() {
        
        //Initialize the counters of 5 participants with 0's
        List<Integer> temp = Arrays.asList(0,0,0,0,0);
         
        //Create a 
        ArrayList<Integer> initList = new ArrayList<Integer>();
         
        //Copy all items from list 1 to list 2
        initList.addAll(temp);
        
        return initList;
    
    }
}
