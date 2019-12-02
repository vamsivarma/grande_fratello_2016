package sbn.gunturi.entities;

import it.stilo.g.algo.ConnectedComponents;
import it.stilo.g.algo.CoreDecomposition;
import it.stilo.g.structures.Core;
import it.stilo.g.structures.WeightedUndirectedGraph;
import it.stilo.g.util.NodesMapper;
import java.util.ArrayList;
import java.util.Set;

/**
 * This entity represents a specific kind of graph related to the clusters 
 * made and some quantities of interest for this sub network
 * @author Vamsi Gunturi
 */
public class ClusterGraph {
    
    // Graph
    private WeightedUndirectedGraph g;
    // Connected components
    private Set<Set<Integer>> comps;
    // Core
    private Core core;

    /**
     * Mapper that match strings with node ids
     */
    public NodesMapper<String> nodeMapper;

    /**
     * Initialize the graph and the node mapper and compute cc and core
     * @param g a weighted undirected graph
     * @param nodeMapper the mapper related to the graph
     */
    public ClusterGraph(WeightedUndirectedGraph g, NodesMapper<String> nodeMapper) {
        this.g = g;
        this.nodeMapper = nodeMapper;
        
        // Initialize the number of threads
        int worker = (int) (Runtime.getRuntime().availableProcessors());

        // Get the id of all nodes
        int[] all = new int[g.size];
        for (int i = 0; i < g.size; i++) {
            all[i] = i;
        }

        try {
            // Compute connecte components
            this.comps = ConnectedComponents.rootedConnectedComponents(g, all, worker);
            // Compute cores
            this.core = CoreDecomposition.getInnerMostCore(g, worker);
        } catch (InterruptedException ex) {
            this.comps = null;
            this.core = null;
            ex.printStackTrace();
        }
    }
    
    /**
     * Obtain the labels of a list of nodes
     * @param nodes list of nodes 
     * @return labels of the input nodes
     */
    public ArrayList<String> getWords(ArrayList<Integer> nodes){
        // Output list
        ArrayList<String> nodeNames = new ArrayList<String>();
        
        // For each node
        for(int node : nodes){
            // Get from the mapper te label of the node and save it
            nodeNames.add(nodeMapper.getNode(node));
        }
        // Return the nodes label
        return nodeNames;
    }

    /**
     *
     * @return the graph
     */
    public WeightedUndirectedGraph getG() {
        return g;
    }

    /**
     *
     * @return the connected components of the graph
     */
    public Set<Set<Integer>> getComps() {
        return comps;
    }

    /**
     *
     * @return the core of the graph
     */
    public Core getCore() {
        return core;
    }
}
