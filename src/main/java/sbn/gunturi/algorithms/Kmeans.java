package sbn.gunturi.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import sbn.gunturi.entities.TweetTerm;

/**
 * This class implements all the features and the phases of the K-means algorithm
 * @author Vamsi Gunturi
 */
public class Kmeans {

    /**
     * Compute the k-means for a given group of tweet terms.
     * @param wordsInfo terms to cluster
     * @param k number of clusters
     * @param maxIt max number of iteration
     * @return Return an Array of memberships.
     */
    public static int[] computeKmeans(ArrayList<TweetTerm> wordsInfo, int k, int maxIt) {

        // The number of cluster must be equal or greater to 2
        if (k < 2) {
            throw new IllegalArgumentException("Invalid number of Clusters: " + k);
        }
        
        // The max number of iterations must be a positive number
        if (maxIt <= 0) {
            throw new IllegalArgumentException("Invalid number of iterations: " + maxIt);
        }

        int n = wordsInfo.size(); // number of points
        int m = wordsInfo.get(0).getTimeSeries().length; // number of dimensions

        // Modify de structure of our tweets terms
        double[][] data = getData(wordsInfo);
        // Obtain initial centroids
        double[][] centroids = initializeCentroidspp(data, k, m);
        
        // Initialize the array of membersihps
        int[] memberships = new int[n];

        int i = 0;
        // Flag to check if centroids are changed
        boolean areCentroidsChanged = Boolean.TRUE;
        // Until centroids change and the number of iterations has not reached the max iter yet
        while (areCentroidsChanged && i < maxIt) {
            
            // Assign centers to points
            memberships = assignMembership(data, centroids);
            // Calculate new centroids
            double[][] newCentroids = updateCentroids(data, memberships, k, m);
            
            // Compute the magnitude in order to estimate if the centers has been moved
            areCentroidsChanged = computeMagnitudeUpdate(newCentroids, centroids);
            
            // Override old centroids
            centroids = newCentroids.clone();
            
            // Increment iteration counter
            i++;

        }
        // Exit Statements:
        // In case the procedure stopped for max iter reached
        if (i >= maxIt) {
            System.out.println("MaxIter reached!");
        // In case centroids didn't changed
        } else {
            System.out.println("Eps reached. Num iter = " + i);
        }

        return memberships;

    }
    
    // This method rearrange data given in input to computeKmeans
    private static double[][] getData(ArrayList<TweetTerm> wordsInfo) {
        
        // save the number of words in the list
        int n = wordsInfo.size();
        // save the size of the time series related to the tweetTerms
        int m = wordsInfo.get(0).getTimeSeries().length;
        // New data stracture that will contain togheter all the time series
        double[][] data = new double[n][m];
        
        // For each term in in the list
        int i = 0;
        for (TweetTerm elem : wordsInfo) {
            // Get the binary representation of the time series and put it in the matrix
            // @TODO: Need to double check if its better to use actual SAX 
            data[i] = elem.getBinaryRep();
            i++;

        }
        // Return the matrix
        return data;
    }

    // This method compute a routine in order to find the initial centroids
    private static double[][] initializeCentroidspp(double[][] data, int k, int m) {
        
        // Create a matrix in which the centroids time series are going to be stored
        double[][] centroids = new double[k][m];
        
        // Array of weights used to balance probabilities for each data point based on its possibility to come
        double[] weights = new double[data.length];
        Arrays.fill(weights, 1);
        
        // Generate a stram of pseudo random numbers
        Random rand = new Random(1);
        

        // Iterate for a number of time equal to the number of centroids needed
        for (int i = 0; i < k; i++) {

            // Generate a random number
            double random = rand.nextDouble();
            // Get the i-th centroid index in the matrix of time series
            int newIndex = selectRandomWeightedIndex(weights, random);
            // Add the centroid to the set of centroids
            centroids[i] = data[newIndex];
            
            // For each data point
            for (int idx = 0; idx < data.length; idx++) {
                // Set an initial min dinstance
                double minDist = 1000000;
                
                // For each centroid
                for (int c = 0; c < i + 1; c++) {
                    
                    // Compute the distance between the point and the centroid
                    double centrDist = computeDistance(data[idx], centroids[c]);
                    // If this distance is better than the previous taken
                    if (centrDist < minDist) {
                        // Save it
                        minDist = centrDist;
                    }
                    
                    // Rebalance the weight of the data point
                    weights[idx] = Math.pow(minDist, 2);

                }

            }

        }
        
        // Return the initial centroids
        return centroids;

    }

    // Get an index at random based on the weights that each index have
    private static int selectRandomWeightedIndex(double[] weights, double random) {


        // Compute the total weight of all items together
        double totalWeight = 0.0d;
        for (double i : weights) {

            totalWeight += i;
        }
        
        // Now choose a random item
        int randomIndex = -1;
        // Multiply the random value in input with the totalWeight in order to get a portion of the weights vector
        random *= totalWeight;
        
        // For each data point
        for (int i = 0; i < weights.length; ++i) {
            // Remove its weight
            random -= weights[i];
            // If random become negative get i as index for the next centroid
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        // Return the random index
        return randomIndex;

    }

    // Assign to each point the "nearest" center
    private static int[] assignMembership(double[][] data, double[][] centroids) {
        
        // Membership vector
        int[] memberships = new int[data.length];

        // For each data point
        for (int i = 0; i < data.length; i++) {
            // Set an initial min dinstance
            double minDist = 1000000;
            // set an initial value for the best centroid
            int bestCentroid = 0;

            // For each centroid
            for (int centroid = 0; centroid < centroids.length; centroid++) {
                // Compute the distance between the point and the centroid
                double centrDist = computeDistance(data[i], centroids[centroid]);
                // If this distance is better than the previous taken
                if (centrDist < minDist) {
                    // Save the dist value and the nearest centroid
                    minDist = centrDist;
                    bestCentroid = centroid;
                }

            }
            // Assign the best centroid to the point
            memberships[i] = bestCentroid;
        }
        // Return the full membership vector
        return memberships;

    }
    
    // Compute the distance between two vectors using Euclidian Distance
    private static double computeDistance(double[] vec1, double[] vec2) {
        
        // Vectors length
        int n = vec1.length;
        
        // Initial dist value
        double dist = 0;
        
        // For each vectors component
        for (int i = 0; i < n; i++) {
            // Add to dist the square of the different between them
            dist += Math.pow(vec1[i] - vec2[i], 2);
        }
        
        // Compute the square root
        dist = Math.sqrt(dist);
        
        // Return dist
        return dist;

    }

    // Update centroids values
    private static double[][] updateCentroids(double[][] data, int[] membership, int k, int m) {
        
        // New centroids matrix
        double[][] centroids = new double[k][m];

        // For each centroid
        for (int c = 0; c < k; c++) {
            
            // Create a new centroid vector
            double[] centroid = new double[m];
            
            // Initialize its numerosity to 0 (number of nodes of its cluster)
            int numerosity = 0;
            
            // For each data point
            for (int i = 0; i < data.length; i++) {
                
                // If it is a member of the current center cluster
                if (membership[i] == c) {
                    // Increment numerosity and sum the vectors components
                    numerosity++;
                    centroid = addVectors(centroid, data[i]);

                }

            }
            // Compute the average on the resulting vector in order to obtain the new centroid
            centroid = averageVec(centroid, numerosity);
            // Add the centroid to the Array of new centroids
            centroids[c] = centroid;
        }
        // Return the new centroids
        return centroids;

    }
    
    // Compute the sum of two vectors component by component
    private static double[] addVectors(double[] vec1, double[] vec2) {
        // Get the vector length
        int n = vec1.length;
        // Resulting vector is initilialized
        double[] result = new double[n];

        // For each component
        for (int i = 0; i < n; i++) {
            // Sum the values of the two vectors component and save the result
            result[i] = vec1[i] + vec2[i];

        }
        
        // Return the resulting vector
        return result;
    }

    // Compute the average of a vectors component by component
    private static double[] averageVec(double[] vec, int divisor) {
        // For each vector component
        for (int i = 0; i < vec.length; i++) {
            // Divide it by the divisor
            vec[i] /= (double) divisor;
        }
        
        // Return the resulting vector
        return vec;
    }

    // Check if the centroids found during the current iteration are different from the previous
    private static boolean computeMagnitudeUpdate(double[][] newCentroids, double[][] oldCentroids) {
        // Initial value of magnitude
        double magnitude = 0;
        
        // For each centroid in both matrices
        for (int i = 0; i < newCentroids.length; i++) {
            
            // Compute the distance between the centroids and sum the result to magnitude
            magnitude += computeDistance(newCentroids[i], oldCentroids[i]);
            
            // If the magnitude become positive
            if (magnitude > 0) {
                // Centroids have changed
                return Boolean.TRUE;
            }
        }
        
        // Otherwise
        return Boolean.FALSE;
    }
}
