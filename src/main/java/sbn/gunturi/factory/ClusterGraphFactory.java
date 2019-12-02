package sbn.gunturi.factory;

import it.stilo.g.structures.WeightedUndirectedGraph;
import it.stilo.g.util.NodesMapper;
import static java.lang.Float.max;
import java.util.ArrayList;
import sbn.gunturi.algorithms.Kmeans;
import sbn.gunturi.entities.TweetTerm;
import sbn.gunturi.entities.ClusterGraph;
import sbn.gunturi.manager.TweetsIndexManager;

/**
 * This Factory generate graphs starting from the clusterization of a list of terms
 * @author Vamsi Gunturi
 */
public class ClusterGraphFactory {
    // Number of clusters
    private int nCluster;
    // Number of iteration for the clusterization algorithm
    private int nIter;

    /**
     * Initialize the factory attributes
     * @param nCluster
     * @param nIter
     */
    public ClusterGraphFactory(int nCluster, int nIter) {
        this.nCluster = nCluster;
        this.nIter = nIter;
    }

    /**
     * Compute the K-means on a list of terms and generate the ClusterGraph of each cluster obtained
     * @param relWords Terms to clusterize
     * @param tim A TweetIndexManager used to comunicate with the index
     * @return A list of ClusterGraphs
     */
   public ArrayList<ClusterGraph> generate(ArrayList<TweetTerm> relWords, TweetsIndexManager tim) {
        // Resulting List
        ArrayList<ClusterGraph> cgs = new ArrayList<ClusterGraph>();
        
        // compute the K-means and get the membership of each term
        int[] membership = Kmeans.computeKmeans(relWords, nCluster, nIter);

        // For each cluster
        for (int i = 0; i < nCluster; i++) {
            System.out.println("+Cluster N°" + (i + 1) + ":");
            // Count the number of elements of the cluster
            int k = 0;
            for (int j = 0; j < membership.length; j++) {
                if (membership[j] == i) {
                    k++;
                }
            }
            System.out.println("+-+ Nmuber of elements: " + k + "\n");
        }

        //Fore each cluster 
        for (int k = 0; k < nCluster; k++) {
            // Create a list of the tweetTerms that will contain the elements of the cluster
            ArrayList<TweetTerm> clusterWords = new ArrayList<TweetTerm>();
            
            // For each term labeled with a cluster
            for (int idx = 0; idx < membership.length; idx++) {
                // If the label is the same of the current cluster
                if (membership[idx] == k) {
                    // Add the term to the cluster terms
                    clusterWords.add(relWords.get(idx));
                }
            }
            
            // Create a new weightd undirected graph with number of nodes equal to the cluster terms
            WeightedUndirectedGraph g = new WeightedUndirectedGraph(clusterWords.size() + 1);
            // Create a node mapper related to the graph
            NodesMapper<String> nodeMapper = new NodesMapper<String>();

            // For each element of the cluster
            for (int i = 1; i < clusterWords.size(); i++) {
                // Get the word of the current tweetTerm 
                String u = clusterWords.get(i).getWord();
                // Add it to the mapper
                nodeMapper.getId(u);
                // Get also its type and frequency
                String uType = clusterWords.get(i).getType();
                double uFreq = clusterWords.get(i).getFrequency();

                // For each other term
                for (int j = i + 1; j < clusterWords.size(); j++) {
                    // Get its word, type and frequency
                    String v = clusterWords.get(j).getWord();
                    String vType = clusterWords.get(j).getType();
                    double vFreq = clusterWords.get(j).getFrequency();
                    // Compute the intersection and get the length
                    double intersection = tim.searchTwoTermsInFields(u, uType, v, vType).length;
                    // Obtain these two ratio
                    double div1 = intersection / uFreq;
                    double div2 = intersection / vFreq;
                    // Get the max of them
                    float maxRelFreq = max((float) div1, (float) div2);

                    // If this quantity is higher than a threshold add the edge between the nodes
                    // @TODO: Need to tweak this more to obtain better results
                    if (maxRelFreq > 0.0001) {
                        g.add(i, j, 1);
                    }
                }
            }
            // add to the list a new ClusterGraph with the just created graph and node mapper
            cgs.add(new ClusterGraph(g, nodeMapper));
        }

        return cgs;
    }
}
