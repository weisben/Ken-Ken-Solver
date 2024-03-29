import javax.swing.*;
import javax.swing.border.EmptyBorder;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Queue;

public class KenKenPlayer
{
    private int PUZZLE_SIZE;
    private int NUM_CELLS;

    int recursions;
    int[] vals;   

    ArrayList<Integer>[] globalDomains;
    ArrayList<Integer>[] neighbors;
    ArrayList<Arithmetic> regions = new ArrayList<Arithmetic>();
    Queue<Arc> globalQueue = new LinkedList<Arc>();


    /*
     * INITIALIZATION
     */
    private void setPuzzleSize(int dim){
        PUZZLE_SIZE = dim;
        NUM_CELLS = dim * dim;
        vals = new int[NUM_CELLS];
        globalDomains = new ArrayList[NUM_CELLS];
        neighbors = new ArrayList[NUM_CELLS];
    }

    /*
     * SOLVERS
     */
    private final void solver(String method){
        recursions = 0; 

        initGlobalDomains(); // initialize domains for each cell

        allDiff(); // initializes neighbors and globalQueue   
        
        boolean success = false;

        switch(method){
            case "FC":
                success = backtrackForwardChecking(0, globalDomains);
                break;
            case "AC3":
                success = backtrackAC3(0, globalDomains);
                break;
            case "MCV":
                success = backtrackMostConstrainedVariable(globalDomains);
                break;
            case "LCV":
                success = backtrackLeastConstrainingValue(globalDomains);
                break;
            default:
                System.out.println("Invalid method");
        }

        /*
         * sets final board values in vals
         */
        Finished(success, method);
    }

    /*
     * CONSTRAINT PROPOGATION
     */

    /*
     *  This method defines constraints between a set of variables.
     */
    private final void allDiff(){
        //initialize neighbors
        for (int i = 0; i < NUM_CELLS; i++){
            neighbors[i] = new ArrayList<Integer>();

            int row = i / PUZZLE_SIZE;
            int col = i % PUZZLE_SIZE;

            //populate rows
            for(int j = 0; j < PUZZLE_SIZE; j++){
                int neighbor_index = (row * PUZZLE_SIZE) + j;
                if (neighbor_index != i){
                    neighbors[i].add(neighbor_index);
                }
            }

            //populate columns
            for (int j = 0; j < PUZZLE_SIZE; j++){
                int neighbor_index = (j * PUZZLE_SIZE) + col;
                if (neighbor_index != i){
                    neighbors[i].add(neighbor_index);
                }
            }
            
            //use neighbors to create arcs and initialize global queue
            for (int k: neighbors[i]){
                Arc pair = new Arc(i, k);
                globalQueue.add(pair);
            }
        }

        for (Arithmetic box: regions){      
            if(box.operator != '#'){ // if there is more than one value in the box
                for(int i = 0; i < box.cells.size(); i++){
                    int cell = box.cells.get(i);
                    ArrayList<Integer> neighbors = new ArrayList<Integer>();
                    for(int j = 0; j < box.cells.size(); j++){
                        if(i!=j) neighbors.add(box.cells.get(j));
                    }
                    Arc group = new Arc(cell, neighbors, box.target, box.operator);
                    globalQueue.add(group);
                }
                
            } else { // if there is only one value in the box, assign it that value
                int cell = box.cells.get(0);
                globalDomains[cell].clear();
                globalDomains[cell].add(box.target);
            }
        }
    }


    /**
     * This method sets up the global domains for all cells
     */
    private void initGlobalDomains(){
        for (int i = 0; i < NUM_CELLS; i++){
            globalDomains[i] = new ArrayList<Integer>();
            for(int j = 1; j <= PUZZLE_SIZE; j++){
                globalDomains[i].add(j);
            }
        }
    }

    /*
     * Backtracking algorithms
     */
    
    /* Backtracking algorithm with AC3 */
    private final boolean backtrackAC3(int cell, ArrayList<Integer>[] Domains) {

        recursions++;
        if (cell >= NUM_CELLS){ // found a solution for the board
            return true;
        }

        // make a copy of domains so we don't modify global domains
        ArrayList<Integer>[] domain_copy = new ArrayList[NUM_CELLS];
        for (int i = 0; i < NUM_CELLS; i++) {
            domain_copy[i] = new ArrayList<>(Domains[i]);
        }
        
        // checks if previous cell assignment is consistent
        if (!AC3(domain_copy)) { // AC3 found an empty domain
            return false; // backtrack and find another value
        } 

        // copy of cell's domain to be iterated through
        ArrayList<Integer> domain_values = new ArrayList<Integer>();
        for(int val : domain_copy[cell]){
            domain_values.add(val);
        }

        // find a value for this cell
        for (int value : domain_values) {
            // assign a value to current cell
            domain_copy[cell].clear();
            domain_copy[cell].add(value);

            // check if value works by recursively calling backtrack on next cell
            boolean consistent = backtrackAC3(cell + 1, domain_copy);

            // if backtrack returns true, then all cells have worked, so assign the value
            if (consistent){
                vals[cell] = value; 
                return true;
            }
        }
        return false;
    }

    /* Backtracking algorithm with forward checking */
    private final boolean backtrackForwardChecking(int cell, ArrayList<Integer>[] Domains) {
        recursions++;
        if (cell >= NUM_CELLS){ // found a solution for the board
            return true;
        }

        // make a copy of domains so we don't modify global domains
        ArrayList<Integer>[] domain_copy = new ArrayList[NUM_CELLS];
        for (int i = 0; i < NUM_CELLS; i++) {
            domain_copy[i] = new ArrayList<>(Domains[i]);
        }

        // copy of cell's domain to be iterated through
        ArrayList<Integer> domain_values = new ArrayList<Integer>();
        for(int val : domain_copy[cell]){
            domain_values.add(val);
        }

        // find a value for this cell
        for (int value : domain_values) {
            // assign a value to current cell
            domain_copy[cell].clear();
            domain_copy[cell].add(value);

            // check if value works by recursively calling backtrack on next cell
            boolean consistent = forwardCheck(cell, domain_copy) && backtrackForwardChecking(cell + 1, domain_copy);

            // if backtrack returns true, then all cells have worked, so assign the value
            if (consistent){
                vals[cell] = value; 
                return true;
            }
        }
        return false;
    }

    // Checks if an assignment to a cell has any immediate inconsistencies
    private final boolean forwardCheck(int cell, ArrayList<Integer>[] Domains){
        ArrayList<Arc> arcs = getArcs(cell);
        for(Arc a: arcs){
            if(Revise(a, Domains)) return false;
        }
        return true;
    }


     /* Most Constrained Variable Heuristic */
    private final boolean backtrackMostConstrainedVariable(ArrayList<Integer>[] Domains) {
        recursions++;

        int cell = findMostConstrained(Domains); 

        // solution is found
        if (cell == -1){
            finalAssignment(Domains);
            return true;
        }

        ArrayList<Integer>[] domain_copy = new ArrayList[NUM_CELLS];
        for (int i = 0; i < NUM_CELLS; i++) {
            domain_copy[i] = new ArrayList<>(Domains[i]);
        }
        
        if (!AC3(domain_copy)) { // AC3 found an empty domain
            return false; // backtrack and find another value
        } 

        ArrayList<Integer> domain_values = new ArrayList<Integer>();
        for(int val : domain_copy[cell]){
            domain_values.add(val);
        }

        for (int value : domain_values) {
            // assign a value to current cell
            domain_copy[cell].clear();
            domain_copy[cell].add(value);

            boolean consistent = backtrackMostConstrainedVariable(domain_copy);

            // if backtrack returns true, then all cells have worked, so assign the value
            if (consistent){
                return true;
            }
        }
        return false;
    }

    /* finds and returns the cell with the most constrained domain */
    private final int findMostConstrained(ArrayList<Integer>[] Domains){

        int smallest_domain_size = PUZZLE_SIZE + 1 ; // domain size will never exceed board size
        int most_constrained_variable = -1; 

        for(int i = 0; i < Domains.length; i++){
            int domain_size = Domains[i].size();

            // if the domain hasn't been assigned a value and we find a more constrained domain
            if ( domain_size != 1 && domain_size < smallest_domain_size){
                smallest_domain_size = domain_size;
                most_constrained_variable = i;
            }
        }
        
        return most_constrained_variable;
    }


    /* Least Constraining Value Heuristic */
    private final boolean backtrackLeastConstrainingValue(ArrayList<Integer>[] Domains) {
        recursions++;

       int cell = findMostConstrained(Domains); 

        // solution is found
        if (cell == -1){
            finalAssignment(Domains);
            return true;
        }

        ArrayList<Integer>[] domain_copy = new ArrayList[NUM_CELLS];
        for (int i = 0; i < NUM_CELLS; i++) {
            domain_copy[i] = new ArrayList<>(Domains[i]);
        }
        
        if (!AC3(domain_copy)) { // AC3 found an empty domain
            return false; // backtrack and find another value
        } 

        ArrayList<Integer> domain_values = new ArrayList<Integer>();
        for(int val : domain_copy[cell]){
            domain_values.add(val);
        }

        // sorts the domain by least constraining value
        ArrayList<Integer> domain_LCV = sortDomainByLCV(cell, new ArrayList<>(domain_copy[cell]), domain_copy);

        for (int value : domain_LCV) {
            domain_copy[cell].clear();
            domain_copy[cell].add(value);

            boolean consistent = backtrackLeastConstrainingValue(domain_copy);

            if (consistent){
                vals[cell] = value; 
                return true;
            }
        }
        return false;
    }

    /* sorts the domain copy in order of least constraining values */
    private ArrayList<Integer> sortDomainByLCV(int cell, ArrayList<Integer> domainValues, ArrayList<Integer>[] Domains) {
        domainValues.sort((val1, val2) -> {
            int lcv1 = findLeastConstrainingValue(cell, val1, Domains);
            int lcv2 = findLeastConstrainingValue(cell, val2, Domains);
            return Integer.compare(lcv1, lcv2);
        });
        return domainValues;
    }

    /* finds the least constraining value by calculating the number of times it appears in a neighbor's cell*/
    private int findLeastConstrainingValue(int cell, int value, ArrayList<Integer>[] Domains) {
        int frequency = 0;
        for (int neighbor : neighbors[cell]) {
            if (Domains[neighbor].contains(value)) {
                frequency++;
            }
        }
        return frequency;
    }




    /*
     * AC3 algorithm and utilities
     */

    /**
     * AC3 algorithm
     * Given domains of all variables, use the global constraints to look for inconsistencies
     * @param Domains: Array of domains of all cells
     * @return
     */
    private final boolean AC3(ArrayList<Integer>[] Domains) {
        // copy queue, initially all the arcs in csp
        Queue<Arc> Q = new LinkedList<Arc>();
        for(Arc a : globalQueue){
            Q.add(a);
        }
        
        // iterate through queue until there are no more arcs to revise over
        while(!Q.isEmpty()){
            Arc current_arc = Q.poll();

            boolean revised = Revise(current_arc, Domains);
                
            if(!revised) continue; // if the domain wasn't revised move to the next arc
            if (Domains[current_arc.cell].isEmpty()){ // an inconsistency was found
                return false;
            }
            
            // If revised, add adjacent arcs back to queue
            if(current_arc.constraintType == "diff") {
                for(int n: neighbors[current_arc.cell]){
                    if(current_arc.neighbors.get(0)!=n){
                        Arc new_arc = new Arc(n, current_arc.cell);
                        if(!Q.contains(new_arc)){
                            Q.add(new_arc);
                        }
                    }
                }
            }
        }
        
		return true;
    }


    // General method for revising arcs
    private final boolean Revise(Arc t, ArrayList<Integer>[] Domains){
        if(t.constraintType == "diff"){
            return ReviseDiff(t, Domains);
        } else {
            return ReviseMath(t, Domains);
        }
    }

    // Method for revising diff arcs
    private final boolean ReviseDiff(Arc t, ArrayList<Integer>[] Domains){
        // extract endpoints of arc
        int Xi = t.cell;
        int Xj = t.neighbors.get(0);

        // Domain of Xi needs to be revised only if the domain of Xj is a
        // singular value which is contained in the domain of Xi
        if(Domains[Xj].size() == 1 && Domains[Xi].contains(Domains[Xj].get(0))){
            Domains[Xi].remove(Domains[Xj].get(0));
            return true; 
        } else {
            return false;
        }       
 	}

    // General method for revising math arcs
    private final boolean ReviseMath(Arc t, ArrayList<Integer>[] Domains){
        switch(t.operator){
            case '+':
                return ReviseAdd(t, Domains);
            case '-':
                return ReviseSub(t, Domains);
            case '*':
                return ReviseMul(t, Domains);
            case '/':
                return ReviseDiv(t, Domains);
            default:
                System.out.println("Invalid operator");
                return false;
        }
    }

    // Method for revising addition arcs
    private final boolean ReviseAdd(Arc t, ArrayList<Integer>[] Domains){
        boolean revised = false;

        ArrayList<Integer> dom = Domains[t.cell];

        // Calculate maximum and minimum sums of neighbor domains
        int min_sum = 0;
        int max_sum = 0;
        for(int i: t.neighbors){
            min_sum += min(Domains[i]);
            max_sum += max(Domains[i]);
        }

        //Remove a value if it does not fit in the bounds defined by the max and min
        for(int i=0; i<dom.size(); i++){
            if(dom.get(i) + min_sum > t.target || dom.get(i) + max_sum < t.target){
                dom.remove(i);
                revised = true;
            }
        }

        return revised;
    }

    // Method for revising subtraction arcs
    private final boolean ReviseSub(Arc t, ArrayList<Integer>[] Domains){
        boolean revised = false;

        ArrayList<Integer> dom = Domains[t.cell];
        int neighbor = t.neighbors.get(0);
        ArrayList<Integer> neighborDom = Domains[neighbor];

        // For each value in cell's domain check if there is a value
        // in neighbor's domain that can be subtracted to get target
        for(int i=0; i<dom.size(); i++){
            int val1 = dom.get(i);
            boolean works = false;
            for(int j=0; j<neighborDom.size(); j++){
                int val2 = neighborDom.get(j);
                if(Math.abs(val1 - val2) == t.target){
                    works = true;
                }
            }
            if(!works){
                dom.remove(i);
                revised = true;
            }
        }

        return revised;
    }

    // Method for revising multiplication arcs
    private final boolean ReviseMul(Arc t, ArrayList<Integer>[] Domains){
        boolean revised = false;

        ArrayList<Integer> dom = Domains[t.cell];

        // Calculate maximum and minimum products of neighbor domains
        int min_prod = 1;
        int max_prod = 1;
        for(int i: t.neighbors){
            min_prod *= min(Domains[i]);
            max_prod *= max(Domains[i]);
        }

        //Remove a value if it does not fit in the bounds defined by the max and min
        for(int i=0; i<dom.size(); i++){
            int val = dom.get(i);
            if(t.target % val != 0 || val * min_prod > t.target || val * max_prod < t.target){
                dom.remove(i);
                revised = true;
            }
        }

        return revised;
    }

    // Method for revising division arcs
    private final boolean ReviseDiv(Arc t, ArrayList<Integer>[] Domains){
        boolean revised = false;

        ArrayList<Integer> dom = Domains[t.cell];
        int neighbor = t.neighbors.get(0);
        ArrayList<Integer> neighborDom = Domains[neighbor];

        // For each value in cell's domain check if there is a value
        // in neighbor's domain that can be divided to get target
        for(int i=0; i<dom.size(); i++){
            int val1 = dom.get(i);
            boolean works = false;
            for(int j=0; j<neighborDom.size(); j++){
                int val2 = neighborDom.get(j);
                if(val1 / val2 == t.target || val2 / val1 == t.target){
                    works = true;
                }
            }
            if(!works){
                dom.remove(i);
                revised = true;
            }
        }

        return revised;
    }

    // Helper method for finding the minimum value of an ArrayList of integers
    private int min(ArrayList<Integer> L){
        // int min_index = -1;
        int min_value = PUZZLE_SIZE;
        for(int i=0; i<L.size(); i++){
            if(L.get(i) < min_value){
                min_value = L.get(i);
                // min_index = i;
            }
        }  
        return min_value; 
    }

    // Helper method for finding the maximum value of an ArrayList of integers
    private int max(ArrayList<Integer> L){
        // int max_index = -1;
        int max_value = 0;
        for(int i=0; i<L.size(); i++){
            if(L.get(i) > max_value){
                max_value = L.get(i);
                // max_index = i;
            }
        }  
        return max_value; 
    }

    //Helper function for finding all arcs that originate from a specified cell
    private ArrayList<Arc> getArcs(int cell){
        ArrayList<Arc> arcs = new ArrayList<Arc>();

        //add row/column neighbors
        for(int neighbor: neighbors[cell]){
            arcs.add(new Arc(cell, neighbor));
        }

        //add cage arc
        for(Arithmetic cage: regions){
            if(cage.operator == '#') continue;
            if(cage.cells.contains(cell)){
                ArrayList<Integer> neighbors = new ArrayList<Integer>();
                for(int j: cage.cells){
                    if(j != cell) neighbors.add(j);
                }
                arcs.add(new Arc(cell, neighbors, cage.target, cage.operator));
            }
        }

        return arcs;
    }


    /**
     * Called when all variables have a domain of 1
     * Assigns all variables to their singular available value
     */
    private void finalAssignment(ArrayList<Integer>[] Domains){
        for(int cell = 0; cell < PUZZLE_SIZE*PUZZLE_SIZE; cell++){
            vals[cell] = Domains[cell].get(0); 
        }
    }

    /*
     * Prints out summary of run
     */
    private void Finished(boolean success, String method){
    	
    	if(success){
            System.out.println("----------\nSolved board:");
            printBoard();
        } else {
            System.out.println("Failed");
        }
        System.out.println("Method: "+method+"\nRecursions: "+recursions+"\n----------");
    }

    /*
     * Prints out values in board
     */
    private void printBoard(){
        for(int i=0; i<PUZZLE_SIZE; i++){
            for(int j=0; j<PUZZLE_SIZE; j++){
                int cellNum = i * PUZZLE_SIZE + j;
                System.out.print(vals[cellNum]);
            }
            System.out.println();
        }
    }

    /**
     * CLASSES
     */

    /**
     * Class for defining arithmetic cages that make up a puzzle
     * Used for encoding puzzles
     */
    class Arithmetic {
        int target;
        char operator;
        ArrayList<Integer> cells;
        
        public Arithmetic(int target, char operator, ArrayList<Integer> cells){
            this.target = target;
            this.operator = operator;
            this.cells = cells;
        }

        public Arithmetic(int target, ArrayList<Integer> cells){
            this.target = target;
            this.cells = cells;
            this.operator = '#';
        }
    }

    /**
     * Class for defining arcs for AC3
     * Two types of arcs - two constructors
     */
    class Arc implements Comparable<Object>{
        int cell;
        ArrayList<Integer> neighbors;
        int target;
        char operator;
        String constraintType;

        //Constructor for diff arcs
        public Arc(int cell_i, int cell_j){
            this.cell = cell_i;
            this.neighbors = new ArrayList<Integer>(Arrays.asList(cell_j));
            constraintType = "diff";
        }

        //Constructor for math arcs
        public Arc(int cell, ArrayList<Integer> neighbors, int target, char operator){
            this.target = target;
            this.operator = operator;
            this.cell = cell;
            this.neighbors = neighbors;
            constraintType = "math";
        }

        public int compareTo(Object o){
            return this.toString().compareTo(o.toString());
        }

        public String toString(){
            return "(" + cell + "->" + neighbors + ", "+ operator + target + ")";
        }

        public boolean equals(Object o){
            return this.compareTo(o) == 0;
        }
    }

    public final void createPuzzle(String difficulty){    
        
        // CREATE BOARD
        // Manual encodings of expert boards taken from KenKen website: www.kenkenpuzzle.com/
        switch(difficulty){
            case "3x3":
                setPuzzleSize(3);
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(0,3))));
                regions.add(new Arithmetic(6, '*', new ArrayList<Integer>(Arrays.asList(1,2))));
                regions.add(new Arithmetic(3, '/', new ArrayList<Integer>(Arrays.asList(4,7))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(5,8))));
                regions.add(new Arithmetic(2, new ArrayList<Integer>(Arrays.asList(6))));
                break;
            case "4x4":
                setPuzzleSize(4);
                regions.add(new Arithmetic(24, '*', new ArrayList<Integer>(Arrays.asList(0,4,8))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(1,2,3))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(5,9))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(6,10))));
                regions.add(new Arithmetic(3, new ArrayList<Integer>(Arrays.asList(7))));
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(12,13))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(11,14,15))));
                break;
            case "5x5":
                setPuzzleSize(5);
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(0,1))));
                regions.add(new Arithmetic(12, '*', new ArrayList<Integer>(Arrays.asList(2,7,8))));
                regions.add(new Arithmetic(9, '+', new ArrayList<Integer>(Arrays.asList(3,4))));
                regions.add(new Arithmetic(12, '+', new ArrayList<Integer>(Arrays.asList(5,6,10))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(9,14))));
                regions.add(new Arithmetic(4, '-', new ArrayList<Integer>(Arrays.asList(11,16))));
                regions.add(new Arithmetic(1, new ArrayList<Integer>(Arrays.asList(12))));
                regions.add(new Arithmetic(9, '+', new ArrayList<Integer>(Arrays.asList(13,17,18))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(15,20))));
                regions.add(new Arithmetic(8, '+', new ArrayList<Integer>(Arrays.asList(19,23,24))));
                regions.add(new Arithmetic(20, '*', new ArrayList<Integer>(Arrays.asList(21,22))));
                break;
             case "6x6":
                setPuzzleSize(6);  
                regions.add(new Arithmetic(48, '*', new ArrayList<Integer>(Arrays.asList(0,1,6,7))));
                regions.add(new Arithmetic(8, '+', new ArrayList<Integer>(Arrays.asList(2,8))));
                regions.add(new Arithmetic(5,  new ArrayList<Integer>(Arrays.asList(3))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(4,5))));
                regions.add(new Arithmetic(5, '-', new ArrayList<Integer>(Arrays.asList(9,15))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(10,11))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(12,13))));
                regions.add(new Arithmetic(60, '*', new ArrayList<Integer>(Arrays.asList(14,19,20))));
                regions.add(new Arithmetic(30, '*', new ArrayList<Integer>(Arrays.asList(16,17))));
                regions.add(new Arithmetic(13, '+', new ArrayList<Integer>(Arrays.asList(18,24,30))));
                regions.add(new Arithmetic(11, '+', new ArrayList<Integer>(Arrays.asList(21,22,27))));
                regions.add(new Arithmetic(5, '-', new ArrayList<Integer>(Arrays.asList(23,29))));
                regions.add(new Arithmetic(3, '/', new ArrayList<Integer>(Arrays.asList(25,31))));
                regions.add(new Arithmetic(3, '-', new ArrayList<Integer>(Arrays.asList(26,32))));
                regions.add(new Arithmetic(2,  new ArrayList<Integer>(Arrays.asList(28))));
                regions.add(new Arithmetic(30, '*', new ArrayList<Integer>(Arrays.asList(33,34,35))));
                break;     
            case "7x7":
                setPuzzleSize(7);
                regions.add(new Arithmetic(392, '*', new ArrayList<Integer>(Arrays.asList(0,1,2,8))));
                regions.add(new Arithmetic(15, '+', new ArrayList<Integer>(Arrays.asList(3,4,11,18))));
                regions.add(new Arithmetic(6, '+', new ArrayList<Integer>(Arrays.asList(5,6))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(7,14))));
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(9,10))));
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(12,13))));
                regions.add(new Arithmetic(3, '/', new ArrayList<Integer>(Arrays.asList(15,22))));
                regions.add(new Arithmetic(17, '+', new ArrayList<Integer>(Arrays.asList(16,17,23,24))));
                regions.add(new Arithmetic(18, '+', new ArrayList<Integer>(Arrays.asList(19,20,25,26))));
                regions.add(new Arithmetic(8, '+', new ArrayList<Integer>(Arrays.asList(21,28,29))));
                regions.add(new Arithmetic(3, '-', new ArrayList<Integer>(Arrays.asList(27,34))));
                regions.add(new Arithmetic(3, new ArrayList<Integer>(Arrays.asList(30))));
                regions.add(new Arithmetic(13, '+', new ArrayList<Integer>(Arrays.asList(31,32))));
                regions.add(new Arithmetic(13, '+', new ArrayList<Integer>(Arrays.asList(33,39,40))));
                regions.add(new Arithmetic(3, '-', new ArrayList<Integer>(Arrays.asList(35,42))));
                regions.add(new Arithmetic(5,  new ArrayList<Integer>(Arrays.asList(36))));
                regions.add(new Arithmetic(54, '*', new ArrayList<Integer>(Arrays.asList(37,38,43,44))));
                regions.add(new Arithmetic(15, '+', new ArrayList<Integer>(Arrays.asList(41,47,48))));
                regions.add(new Arithmetic(7, '+', new ArrayList<Integer>(Arrays.asList(45,46))));
                break;
            case "8x8":   
                setPuzzleSize(8);    
                regions.add(new Arithmetic(15, '+', new ArrayList<Integer>(Arrays.asList(0,8))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(1,9))));
                regions.add(new Arithmetic(6,  new ArrayList<Integer>(Arrays.asList(2))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(3,11))));
                regions.add(new Arithmetic(3, '+', new ArrayList<Integer>(Arrays.asList(4,5))));
                regions.add(new Arithmetic(3, '-', new ArrayList<Integer>(Arrays.asList(6,7))));
                regions.add(new Arithmetic(7, '-', new ArrayList<Integer>(Arrays.asList(10,18))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(12,20))));
                regions.add(new Arithmetic(105, '*', new ArrayList<Integer>(Arrays.asList(13,14,15))));
                regions.add(new Arithmetic(4, '-', new ArrayList<Integer>(Arrays.asList(16,17))));
                regions.add(new Arithmetic(20, '*', new ArrayList<Integer>(Arrays.asList(19,26,27))));
                regions.add(new Arithmetic(17, '+', new ArrayList<Integer>(Arrays.asList(21,29,37))));
                regions.add(new Arithmetic(42, '*', new ArrayList<Integer>(Arrays.asList(22,23,30))));
                regions.add(new Arithmetic(3, '-', new ArrayList<Integer>(Arrays.asList(24,25))));
                regions.add(new Arithmetic(40, '*', new ArrayList<Integer>(Arrays.asList(28,35,36))));
                regions.add(new Arithmetic(48, '*', new ArrayList<Integer>(Arrays.asList(31,39,47))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(32,40))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(33,34))));
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(38,46))));
                regions.add(new Arithmetic(19, '+', new ArrayList<Integer>(Arrays.asList(41,49,57))));
                regions.add(new Arithmetic(2, '-', new ArrayList<Integer>(Arrays.asList(42,43))));
                regions.add(new Arithmetic(7, '-', new ArrayList<Integer>(Arrays.asList(44,45))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(48,56)))); 
                regions.add(new Arithmetic(168, '*', new ArrayList<Integer>(Arrays.asList(50,51,58))));
                regions.add(new Arithmetic(5, '-', new ArrayList<Integer>(Arrays.asList(52,53))));
                regions.add(new Arithmetic(120, '*', new ArrayList<Integer>(Arrays.asList(54,55,62,63))));
                regions.add(new Arithmetic(13, '+', new ArrayList<Integer>(Arrays.asList(59,60,61))));

                break;
            case "9x9":
                setPuzzleSize(9);
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(0,1))));
                regions.add(new Arithmetic(4, '-', new ArrayList<Integer>(Arrays.asList(2,3))));
                regions.add(new Arithmetic(336, '*', new ArrayList<Integer>(Arrays.asList(4,5,14))));
                regions.add(new Arithmetic(19, '+', new ArrayList<Integer>(Arrays.asList(6,15,16,17))));
                regions.add(new Arithmetic(3, '/', new ArrayList<Integer>(Arrays.asList(7,8))));
                regions.add(new Arithmetic(14, '+', new ArrayList<Integer>(Arrays.asList(9,18,27))));
                regions.add(new Arithmetic(16, '+', new ArrayList<Integer>(Arrays.asList(10,19))));
                regions.add(new Arithmetic(720, '*', new ArrayList<Integer>(Arrays.asList(11,20,28,29))));
                regions.add(new Arithmetic(2, '/', new ArrayList<Integer>(Arrays.asList(12,13))));
                regions.add(new Arithmetic(10, '+', new ArrayList<Integer>(Arrays.asList(21,22,23))));
                regions.add(new Arithmetic(80, '*', new ArrayList<Integer>(Arrays.asList(24,25,34))));
                regions.add(new Arithmetic(8, '-', new ArrayList<Integer>(Arrays.asList(26,35))));
                regions.add(new Arithmetic(7, '-', new ArrayList<Integer>(Arrays.asList(30,31))));
                regions.add(new Arithmetic(1920, '*', new ArrayList<Integer>(Arrays.asList(32,33,42,50,51))));
                regions.add(new Arithmetic(9, new ArrayList<Integer>(Arrays.asList(36))));
                regions.add(new Arithmetic(5, '+', new ArrayList<Integer>(Arrays.asList(37,38))));
                regions.add(new Arithmetic(17, '+', new ArrayList<Integer>(Arrays.asList(39,40,41))));
                regions.add(new Arithmetic(9, '+', new ArrayList<Integer>(Arrays.asList(43,44))));
                regions.add(new Arithmetic(14, '+', new ArrayList<Integer>(Arrays.asList(45,54,63))));
                regions.add(new Arithmetic(8, '-', new ArrayList<Integer>(Arrays.asList(46,47))));
                regions.add(new Arithmetic(8, '+', new ArrayList<Integer>(Arrays.asList(48,49))));
                regions.add(new Arithmetic(108, '*', new ArrayList<Integer>(Arrays.asList(52,60,61))));
                regions.add(new Arithmetic(3, '-', new ArrayList<Integer>(Arrays.asList(53,62))));
                regions.add(new Arithmetic(6, '-', new ArrayList<Integer>(Arrays.asList(55,64))));
                regions.add(new Arithmetic(42, '*', new ArrayList<Integer>(Arrays.asList(56,65))));
                regions.add(new Arithmetic(20, '+', new ArrayList<Integer>(Arrays.asList(57,58,59))));
                regions.add(new Arithmetic(8, '-', new ArrayList<Integer>(Arrays.asList(66,67))));
                regions.add(new Arithmetic(216, '*', new ArrayList<Integer>(Arrays.asList(68,76,77))));
                regions.add(new Arithmetic(12, '+', new ArrayList<Integer>(Arrays.asList(69,70,71))));
                regions.add(new Arithmetic(1, '-', new ArrayList<Integer>(Arrays.asList(72,73))));
                regions.add(new Arithmetic(4, '/', new ArrayList<Integer>(Arrays.asList(74,75))));
                regions.add(new Arithmetic(12, '+', new ArrayList<Integer>(Arrays.asList(78,79,80))));
                break;
            default:
                System.out.println("Invalid difficulty selected.");
        }
        
        // INIT CELLS
        for(int cell = 0; cell < NUM_CELLS; cell++){
            vals[cell] = 0;
        }
    }
    

    public void run(String difficulty){
        createPuzzle(difficulty);
        
        GUI gui = new GUI();
        gui.initVals();
    }

    public static void main(String[] args) {

        Scanner scan = new Scanner(System.in);
        System.out.println("board size? \t3x3 - 4x4 - 5x5 - 6x6 - 7x7 - 8x8 - 9x9 ");

        char choice = scan.nextLine().charAt(0);

        KenKenPlayer app = new KenKenPlayer();

        switch(choice){
            case '3':
                app.run("3x3");
                break;
            case '4':
                app.run("4x4");
                break;
            case '5':
                app.run("5x5");
                break;
            case '6':
                app.run("6x6");
                break;
            case '7':
                app.run("7x7");
                break;
            case '8':
                app.run("8x8");
                break;
            case '9':
                app.run("9x9");
                break;
            default:
                System.out.println("Invalid difficulty");
        }

        scan.close();
    }


    /**
     * GUI
     */

    class GUI {
        JFrame mainFrame;
        CellPanel[][] cellPanels;
        JPanel gamePanel, buttonPanel;
        JLabel recursionLabel;
        JButton forwardCheckingButton, ac3Button, mostConstrainedButton, leastConstrainingButton, clearButton;
        // Assuming a maximum puzzle size for color array initialization
        Color[] regionColors = new Color[PUZZLE_SIZE * PUZZLE_SIZE]; 

        public GUI() {
            mainFrame = new JFrame("KenKen Player");
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setLayout(new BorderLayout());

            gamePanel = new JPanel();
            gamePanel.setLayout(new GridLayout(PUZZLE_SIZE, PUZZLE_SIZE)); // Grid layout for the puzzle

            // Initialize the cell panels
            cellPanels = new CellPanel[PUZZLE_SIZE][PUZZLE_SIZE];
            for (int r = 0; r < PUZZLE_SIZE; r++) {
                for (int c = 0; c < PUZZLE_SIZE; c++) {
                    cellPanels[r][c] = new CellPanel();
                    gamePanel.add(cellPanels[r][c]);
                }
            }

            mainFrame.add(gamePanel, BorderLayout.CENTER);
            mainFrame.pack();
            mainFrame.setVisible(true);

        // Button panel
        buttonPanel = new JPanel();
        forwardCheckingButton = new JButton("Forward Checking");
        ac3Button = new JButton("AC3");
        mostConstrainedButton = new JButton("Most Constrained Variable");
        leastConstrainingButton = new JButton("Least Constraining Value");
        clearButton = new JButton("Clear Board");
        recursionLabel = new JLabel("Recursions: ");
        
        // action listeners for buttons
        forwardCheckingButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                solver("FC");
                recursionLabel.setText("Recursions: " + recursions);
                updateBoard();
            }
        });

        ac3Button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                solver("AC3");
                recursionLabel.setText("Recursions: " + recursions);
                updateBoard();
            }
        });
        
        mostConstrainedButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                solver("MCV");
                recursionLabel.setText("Recursions: " + recursions);
                updateBoard();
            }
        });

        leastConstrainingButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                solver("LCV");
                recursionLabel.setText("Recursions: " + recursions);
                updateBoard();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearBoard();
            }
        });

        buttonPanel.add(forwardCheckingButton);
        buttonPanel.add(ac3Button);
        buttonPanel.add(mostConstrainedButton);
        buttonPanel.add(leastConstrainingButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(recursionLabel);

        mainFrame.add(gamePanel, BorderLayout.CENTER);
        mainFrame.add(buttonPanel, BorderLayout.SOUTH);
        mainFrame.pack();
        mainFrame.setVisible(true);

        }

        //  update the board with the final solution values
        private void updateBoard() {
            for (int i = 0; i < PUZZLE_SIZE; i++) {
                for (int j = 0; j < PUZZLE_SIZE; j++) {
                    int cellNum = i * PUZZLE_SIZE + j;
                    cellPanels[i][j].setValue(Integer.toString(vals[cellNum]));
                }
            }
        }

        private void clearBoard() {
            for (int i = 0; i < PUZZLE_SIZE; i++) {
                for (int j = 0; j < PUZZLE_SIZE; j++) {
                    cellPanels[i][j].valueField.setText(""); // Clear the text field
                    cellPanels[i][j].valueField.setEditable(true); // Make the field editable again
                }
            }
            recursionLabel.setText("Recursions: 0"); // Reset the recursion label
        }

        public void initVals() {
            // Assign colors to regions
            assignRegionColors();

            // Set the borders and constraints for each cell panel
            for (int i = 0; i < regions.size(); i++) {
                Arithmetic arithmetic = regions.get(i);
                for (int cellIndex : arithmetic.cells) {
                    int row = cellIndex / PUZZLE_SIZE;
                    int col = cellIndex % PUZZLE_SIZE;
                    CellPanel cellPanel = cellPanels[row][col];
                    cellPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    cellPanel.setRegionColor(regionColors[i]);
                    if (cellIndex == arithmetic.cells.get(0)) { // Top-left cell of the region
                        cellPanel.setConstraint(arithmetic.target + String.valueOf(arithmetic.operator));
                    }
                }
            }
        }

        private void assignRegionColors() {
            // Set a different color for each region
            for (int i = 0; i < regions.size(); i++) {
                // Cycle through a predefined array of colors
                regionColors[i] = new Color(
                    (int)(Math.random() * 256),
                    (int)(Math.random() * 256),
                    (int)(Math.random() * 256),
                    (int)(Math.random() * 256)
                );
            }
        }
        
    }

    class CellPanel extends JPanel {
        JLabel constraintLabel;
        JTextField valueField;

        public CellPanel() {
            super(new BorderLayout());
            setBorder(BorderFactory.createLineBorder(Color.BLACK)); // Set the border for the cell

            constraintLabel = new JLabel();
            valueField = new JTextField();
            valueField.setHorizontalAlignment(JTextField.CENTER);

            constraintLabel.setOpaque(true);
            valueField.setOpaque(true);

            
            // Adjust the font size for the constraint label
            Font currentFont = constraintLabel.getFont();
            constraintLabel.setFont(new Font(currentFont.getName(), Font.BOLD, 17)); 
            constraintLabel.setBorder(new EmptyBorder(0, 2, 0, 0)); // Add some padding to the label

            add(constraintLabel, BorderLayout.NORTH);
            add(valueField, BorderLayout.CENTER);

            setOpaque(false);

        }

        public void setConstraint(String text) {
            constraintLabel.setText(text);
        }

        public void setRegionColor(Color color) {
            setBackground(color);
            constraintLabel.setBackground(color); 
            valueField.setBackground(color);
        }

        public void setValue(String value) {
            valueField.setText(value);
        }
    }
}
