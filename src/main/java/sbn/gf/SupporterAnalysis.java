/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbn.gf;

import it.stilo.g.algo.HubnessAuthority;
import it.stilo.g.structures.DoubleValues;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.util.NodesMapper;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import sbn.gf.entities.Participant;
import sbn.gf.manager.SupportersIndexManager;
import sbn.gf.manager.TweetsIndexManager;
import sbn.gf.common.social_graph;

/**
 *
 * @author vamsigunturi
 */
public class SupporterAnalysis {
    
    private static Participant p = new Participant();
    
    public static void supporter_analysis() {

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
                social_graph.createCCSG(nodes);
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
            
            System.out.println("--------------- Computing HITS -----------------");
            
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
                int curIndex = authority.index;
                ArrayList<Document> aMapper = sim.searchForField("id", nodeMapper.getNode(curIndex), 10);
                if(!aMapper.isEmpty()) {
                    
                    Document supporter = aMapper.get(0);
                    
                    String name  = supporter.get("name");
                    
                    printWriter.print(authority.index + "; " + name + "; " + authority.value + "\n");
                }
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
                
                int curIndex = hub.index;
                ArrayList<Document> hMapper = sim.searchForField("id", nodeMapper.getNode(curIndex), 10);
                if(!hMapper.isEmpty()) {

                    Document supporter = hMapper.get(0);

                    String name  = supporter.get("name");

                    printWriter.print(hub.index + "; " + name + "; " + hub.value + "\n");
                }
                
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
    
}
