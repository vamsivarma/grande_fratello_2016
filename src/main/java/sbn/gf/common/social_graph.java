/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbn.gf.common;

import it.stilo.g.algo.ConnectedComponents;
import it.stilo.g.algo.SubGraph;
import it.stilo.g.structures.WeightedDirectedGraph;
import it.stilo.g.util.NodesMapper;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author vamsigunturi
 */
public class social_graph {
    
    // Save the Connected component subGraph of an initial graph based on our supporters
    public static void createCCSG(ArrayList<String> nodes) throws FileNotFoundException, IOException, InterruptedException {

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
    
}
