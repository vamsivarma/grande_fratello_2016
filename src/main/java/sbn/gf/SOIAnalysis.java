/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbn.gf;

import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.util.NodesMapper;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import sbn.gf.algorithms.ComunityLPA;
import sbn.gf.entities.Participant;
import sbn.gf.common.utils;

/**
 *
 * @author vamsigunturi
 */
public class SOIAnalysis {
    
    private static Participant p = new Participant();
    
    // compute label propagation in the entire graph using as prelabeled nodes
    // 1) Authorities
    // 2) Hubs
    public static void spread_of_influence() {
        
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
            
            System.out.println("--------------- Applying Label propagation algorithm -----------------");
            
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

                    if(utils.isInteger(s)) {
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
    
    
    
    
    private static void display_LPA_results(String rType, int[] lpaLabels) {
        
        List<Integer> lpaList = Arrays.asList(0,0,0,0,0);
        
        
        int ucCounter = 0;
        
        
            
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
        
    }
    
}
