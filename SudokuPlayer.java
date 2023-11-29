/*
 * Nusrat Atiya and Ben Weisenbeck
 * All group members were present and contributing during all work on this project.
 * We have neither given nor received unauthorized aid on this project.
 */



import javax.swing.*;


import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Queue;
import java.text.DecimalFormat;

public class SudokuPlayer implements Runnable, ActionListener {

    // final values must be assigned in vals[][]
    int[][] vals = new int[9][9];
    Board board = null;



    /// --- AC-3 Constraint Satisfication --- ///
   
    
    ArrayList<Integer>[] globalDomains = new ArrayList[81];
    ArrayList<Integer>[] neighbors = new ArrayList[81];
    Queue<Arc> globalQueue = new LinkedList<Arc>();
        

	/*
	 * This method sets up the data structures and the initial global constraints
	 * (by calling allDiff()) and makes the initial call to backtrack().
 	 */

    private final void AC3Init(){
        board.Clear();
		recursions = 0; 

        allDiff(); // initializes neighbors and globalQueue

        initGlobalDomains(); // initialize domains for each cell

         // Initial call to backtrack() on cell 0 (top left)
        boolean success = backtrack(0,globalDomains);

        // Prints evaluation of run

        /*
         * sets final board values in vals
         */
        Finished(success);

    }

    /**
     * This method sets up the global domains for all cells
     */
    private void initGlobalDomains(){
        for (int i = 0; i < 81; i++){
            globalDomains[i] = new ArrayList<Integer>();

            // calc row and column and get value
            int row = i / 9;
            int col = i % 9;
            int val = vals[row][col];
            
            // if cell is unassigned, give it a domain of 1-9
            if (val == 0){
                for (int j = 1; j < 10; j++){
                    globalDomains[i].add(j);
                }
            } else { //otherwise the domain should be just the value it is assigned
                globalDomains[i].add(val);
            }
        }
    }

    /*
     *  This method defines constraints between a set of variables.
     */
    private final void allDiff(){
                
        //initialize neighbors
        for (int i = 0; i < 81; i++){
            neighbors[i] = new ArrayList<Integer>();

            int row = i / 9;
            int col = i % 9;

            //populate rows
            for(int j = 0; j < 9; j++){
                int neighbor_index = (row * 9) + j;
                if (neighbor_index != i){
                    neighbors[i].add(neighbor_index);
                }
            }

            //populate columns
            for (int j = 0; j < 9; j++){
                int neighbor_index = (j * 9) + col;
                if (neighbor_index != i){
                    neighbors[i].add(neighbor_index);
                }
            }
            
            //populate box
            int boxRow = row / 3;
            int boxCol = col / 3;
            
            for (int j = boxRow * 3; j < boxRow * 3 + 3; j++){
                for (int k = boxCol * 3; k < boxCol * 3 + 3; k++){
                    int neighbor_index = j * 9 + k;
                    if(neighbor_index != i && !neighbors[i].contains(neighbor_index)){
                        neighbors[i].add(neighbor_index);
                    }
                }
            }
            
            //use neighbors to create arcs and initialize global queue
            for (int k: neighbors[i]){
                Arc pair = new Arc(i, k);
                globalQueue.add(pair);
            }
        }
    }


    /*
     * This is the backtracking algorithm. 
     * returns true if a solution is found
     */
    private final boolean backtrack(int cell, ArrayList<Integer>[] Domains) {
    	recursions +=1;

        if (cell > 80){ // found a solution for the board
            return true;
        }

        int row = cell / 9;
        int col = cell % 9;

        // make a copy of domains so we don't modify global domains
        ArrayList<Integer>[] domain_copy = new ArrayList[81];
        for (int i = 0; i < 81; i++) {
            domain_copy[i] = new ArrayList<>(Domains[i]);
        }

        if (vals[row][col] != 0){ // check if cell is assigned an initial value
            return backtrack(cell + 1, domain_copy);
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
            boolean consistent = backtrack(cell + 1, domain_copy);

            // if backtrack returns true, then all cells have worked, so assign the value
            if (consistent){
                vals[row][col] = value; 
                return true;
            }
        }
        return false;
    }


    /*
     * returns false if an inconsistency is found and true otherwise
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
            int Xi = current_arc.Xi;
            int Xj = current_arc.Xj;

            boolean revised = Revise(current_arc, Domains);
            
            if(!revised) continue; // if the domain wasn't revised move to the next arc
            if (Domains[Xi].isEmpty()){ // an inconsistency was found
                return false;
            }
            // add other neighbors to queue
            for (int k: neighbors[Xi]){
                if (k != Xj){
                    Arc neighbor = new Arc(k, Xi);
                    boolean inQ = false;
                    // check if the arc is already in queue
                    for(Arc a : Q){ 
                        if(a.compareTo(neighbor) == 0) {
                            inQ = true;
                            break;
                        }
                    }
                    if (!inQ){ //add the arc if it was not already in the queue
                        Q.add(neighbor);
                    }
                }
            }
        }
        
		return true;
    }
    
    

    /*
     * returns true if we revised the domain of Xi
     */
     private final boolean Revise(Arc t, ArrayList<Integer>[] Domains){
        // extract endpoints of arc
    	int Xi = t.Xi;
        int Xj = t.Xj;

        // Domain of Xi needs to be revised only if the domain of Xj is a
        // singular value which is contained in the domain of Xi
        if(Domains[Xj].size() == 1 && Domains[Xi].contains(Domains[Xj].get(0))){
            Domains[Xi].remove(Domains[Xj].get(0));
            return true; 
        } else {
            return false;
        }
 	}

   /*
    * custom solver: solves the sudoku board using the most constrained variable
    */
    private final void customSolver(){
    	   
        //set 'success' to true if a successful board    
        //is found and false otherwise.
        board.Clear();
        recursions = 0;
        System.out.println("Running custom algorithm");
        initGlobalDomains();
        allDiff();

        boolean success = backtrack_custom(globalDomains);
        
        Finished(success);
    	       
    }

    /*
     * custom backtrack uses the most constrained to backtrack on
     */
    private final boolean backtrack_custom(ArrayList<Integer>[] Domains) {

    	recursions +=1;

        // assigns the most constrained cell
        int cell = find_most_constrained(Domains); 

        // solution is found
        if (cell == -1){
            final_assignment(Domains);
            return true;
        }

        ArrayList<Integer>[] domain_copy = new ArrayList[81];
        for (int i = 0; i < 81; i++) {
            domain_copy[i] = new ArrayList<>(Domains[i]);
        }
        
        if (!custom_AC3(domain_copy)) { // AC3 found an empty domain
            return false; // backtrack and find another value
        } 

        // create duplicate domain values for iterating through while we mutate the real list
        ArrayList<Integer> domain_values = new ArrayList<Integer>();
        for(int val : domain_copy[cell]){
            domain_values.add(val);
        }

        // find a value for this cell
        for (int value : domain_values) {
            // assign a value to current cell
            domain_copy[cell].clear();
            domain_copy[cell].add(value);

            // runs backtrack on the most constrained cell
            boolean consistent = backtrack_custom(domain_copy);

            if (consistent){
                return true;
            }
        }
        return false;
    }

    /*
     * returns false if an inconsistency is found and true otherwise
     */
    private final boolean custom_AC3(ArrayList<Integer>[] Domains) {

        // copy queue, initially all the arcs in csp
        Queue<Arc> Q = new LinkedList<Arc>();
        for(Arc a : globalQueue){
            Q.add(a);
        }

        while(!Q.isEmpty()){
            Arc current_arc = Q.poll();
            int Xi = current_arc.Xi;

            Revise(current_arc, Domains);
            
            // does not check neighbors, this gives a runtime boost

            if (Domains[Xi].isEmpty()){ // an inconsistency was found
                return false;
            }
        }
        
		return true;
    }

    /**
     * Called when all variables have a domain of 1
     * Assigns all variables to their singular available value
     */
    private void final_assignment(ArrayList<Integer>[] Domains){
        for(int i = 0; i < 81; i++){
            int row = i / 9;
            int col = i % 9;
            vals[row][col] = Domains[i].get(0); 
        }
    }
    
    // finds and returns the cell with the most constrained domain 
    private final int find_most_constrained(ArrayList<Integer>[] Domains){

        int smallest_domain_size = 10; // domain size will never exceed 9
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


    /// ---------- HELPER FUNCTIONS --------- ///
    /// ----   DO NOT EDIT REST OF FILE   --- ///
    /// ---------- HELPER FUNCTIONS --------- ///
    /// ----   DO NOT EDIT REST OF FILE   --- ///
    public final boolean valid(int x, int y, int val){
        
        if (vals[x][y] == val)
            return true;
        if (rowContains(x,val))
            return false;
        if (colContains(y,val))
            return false;
        if (blockContains(x,y,val))
            return false;
        return true;
    }

    public final boolean blockContains(int x, int y, int val){
        int block_x = x / 3;
        int block_y = y / 3;
        for(int r = (block_x)*3; r < (block_x+1)*3; r++){
            for(int c = (block_y)*3; c < (block_y+1)*3; c++){
                if (vals[r][c] == val)
                    return true;
            }
        }
        return false;
    }

    public final boolean colContains(int c, int val){
        for (int r = 0; r < 9; r++){
            if (vals[r][c] == val)
                return true;
        }
        return false;
    }

    public final boolean rowContains(int r, int val) {
        for (int c = 0; c < 9; c++)
        {
            if(vals[r][c] == val)
                return true;
        }
        return false;
    }

    private void CheckSolution() {
        // If played by hand, need to grab vals
        board.updateVals(vals);

        /*for(int i=0; i<9; i++){
	        for(int j=0; j<9; j++)
	        	System.out.print(vals[i][j]+" ");
	        System.out.println();
        }*/
        
        for (int v = 1; v <= 9; v++){
            // Every row is valid
            for (int r = 0; r < 9; r++)
            {
                if (!rowContains(r,v))
                {
                    board.showMessage("Value "+v+" missing from row: " + (r+1));// + " val: " + v);
                    return;
                }
            }
            // Every column is valid
            for (int c = 0; c < 9; c++)
            {
                if (!colContains(c,v))
                {
                    board.showMessage("Value "+v+" missing from column: " + (c+1));// + " val: " + v);
                    return;
                }
            }
            // Every block is valid
            for (int r = 0; r < 3; r++){
                for (int c = 0; c < 3; c++){
                    if(!blockContains(r, c, v))
                    {
                        return;
                    }
                }
            }
        }
        board.showMessage("Success!");
    }

    

    /// ---- GUI + APP Code --- ////
    /// ----   DO NOT EDIT  --- ////
    enum algorithm {
        AC3, Custom
    }
    class Arc implements Comparable<Object>{
        int Xi, Xj;
        public Arc(int cell_i, int cell_j){
            if (cell_i == cell_j){
                try {
                    throw new Exception(cell_i+ "=" + cell_j);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            Xi = cell_i;      Xj = cell_j;
        }

        public int compareTo(Object o){
            return this.toString().compareTo(o.toString());
        }

        public String toString(){
            return "(" + Xi + "," + Xj + ")";
        }
    }

    enum difficulty {
        easy, medium, hard, random
    }

    public void actionPerformed(ActionEvent e){
        String label = ((JButton)e.getSource()).getText();
        if (label.equals("AC-3"))
        	AC3Init();
        else if (label.equals("Clear"))
            board.Clear();
        else if (label.equals("Check"))
            CheckSolution();
            //added
        else if(label.equals("Custom"))
            customSolver();
    }

    public void run() {
        board = new Board(gui,this);
        
        long start=0, end=0;
       
        while(!initialize());
        if (gui)
            board.initVals(vals);
        else {
            board.writeVals();
            System.out.println("Algorithm: " + alg);
            switch(alg) {
                default:
                case AC3:
                	start = System.currentTimeMillis();
                	AC3Init();
                    end = System.currentTimeMillis();
                    break;
                case Custom: //added
                	start = System.currentTimeMillis();
                	customSolver();
                	end = System.currentTimeMillis();
                    break;
            }
            
            CheckSolution();
            
            if(!gui)
            	System.out.println("time to run: "+(end-start));
        }
    }

    public final boolean initialize(){
        switch(level) {
            case easy:
                vals[0] = new int[] {0,0,0,1,3,0,0,0,0};
                vals[1] = new int[] {7,0,0,0,4,2,0,8,3};
                vals[2] = new int[] {8,0,0,0,0,0,0,4,0};
                vals[3] = new int[] {0,6,0,0,8,4,0,3,9};
                vals[4] = new int[] {0,0,0,0,0,0,0,0,0};
                vals[5] = new int[] {9,8,0,3,6,0,0,5,0};
                vals[6] = new int[] {0,1,0,0,0,0,0,0,4};
                vals[7] = new int[] {3,4,0,5,2,0,0,0,8};
                vals[8] = new int[] {0,0,0,0,7,3,0,0,0};
                break;
            case medium:
                vals[0] = new int[] {0,4,0,0,9,8,0,0,5};
                vals[1] = new int[] {0,0,0,4,0,0,6,0,8};
                vals[2] = new int[] {0,5,0,0,0,0,0,0,0};
                vals[3] = new int[] {7,0,1,0,0,9,0,2,0};
                vals[4] = new int[] {0,0,0,0,8,0,0,0,0};
                vals[5] = new int[] {0,9,0,6,0,0,3,0,1};
                vals[6] = new int[] {0,0,0,0,0,0,0,7,0};
                vals[7] = new int[] {6,0,2,0,0,7,0,0,0};
                vals[8] = new int[] {3,0,0,8,4,0,0,6,0};
                break;
            case hard:
            	vals[0] = new int[] {1,2,0,4,0,0,3,0,0};
            	vals[1] = new int[] {3,0,0,0,1,0,0,5,0};  
            	vals[2] = new int[] {0,0,6,0,0,0,1,0,0};  
            	vals[3] = new int[] {7,0,0,0,9,0,0,0,0};    
            	vals[4] = new int[] {0,4,0,6,0,3,0,0,0};    
            	vals[5] = new int[] {0,0,3,0,0,2,0,0,0};    
            	vals[6] = new int[] {5,0,0,0,8,0,7,0,0};    
            	vals[7] = new int[] {0,0,7,0,0,0,0,0,5};    
            	vals[8] = new int[] {0,0,0,0,0,0,0,9,8};  
                break;
            case random:
            default:
                ArrayList<Integer> preset = new ArrayList<Integer>();
                while (preset.size() < numCells)
                {
                    int r = rand.nextInt(81);
                    if (!preset.contains(r))
                    {
                        preset.add(r);
                        int x = r / 9;
                        int y = r % 9;
                        if (!assignRandomValue(x, y))
                            return false;
                    }
                }
                break;
        }
        return true;
    }

    public final boolean assignRandomValue(int x, int y){
        ArrayList<Integer> pval = new ArrayList<Integer>(Arrays.asList(1,2,3,4,5,6,7,8,9));

        while(!pval.isEmpty()){
            int ind = rand.nextInt(pval.size());
            int i = pval.get(ind);
            if (valid(x,y,i)) {
                vals[x][y] = i;
                return true;
            } else
                pval.remove(ind);
        }
        System.err.println("No valid moves exist.  Recreating board.");
        for (int r = 0; r < 9; r++){
            for(int c=0;c<9;c++){
                vals[r][c] = 0;
            }    }
        return false;
    }

    private void Finished(boolean success){
    	
    	if(success) {
            board.writeVals();
            //board.showMessage("Solved in " + myformat.format(ops) + " ops \t(" + myformat.format(recursions) + " recursive ops)");
            board.showMessage("Solved in " + myformat.format(recursions) + " recursive ops");

    	} else {
            //board.showMessage("No valid configuration found in " + myformat.format(ops) + " ops \t(" + myformat.format(recursions) + " recursive ops)");
        	board.showMessage("No valid configuration found");
        }
         recursions = 0;
       
    }
 
    public static void main(String[] args) {

        Scanner scan = new Scanner(System.in);

        System.out.println("Gui? y or n ");
        char g=scan.nextLine().charAt(0);

        if (g=='n')
            gui = false;
        else
            gui = true;
        
        if(gui) {
        	System.out.println("difficulty? \teasy (e), medium (m), hard (h), random (r)");

	        char c = '*';

	        while (c != 'e' && c != 'm' && c != 'n' && c != 'h' && c != 'r') {
	        	c = scan.nextLine().charAt(0);
	            if(c=='e')
	                level = difficulty.valueOf("easy");
	            else if(c=='m')
	                level = difficulty.valueOf("medium");
	            else if(c=='h')
	                level = difficulty.valueOf("hard");
	            else if(c=='r')
	                level = difficulty.valueOf("random");
	            else{
	                System.out.println("difficulty? \teasy (e), medium (m), hard (h), random(r)");
	            }
	        }
	        
	        SudokuPlayer app = new SudokuPlayer();
	        app.run();
	        
        }
        else { //no gui
        	
        	boolean again = true;
        
        	int numiters = 0;
        	long starttime, endtime, totaltime=0;
        
        	while(again) {
        
        		numiters++;
        		System.out.println("difficulty? \teasy (e), medium (m), hard (h), random (r)");

        		char c = '*';

		        while (c != 'e' && c != 'm' && c != 'n' && c != 'h' && c != 'r') {
		        	c = scan.nextLine().charAt(0);
		            if(c=='e')
		                level = difficulty.valueOf("easy");
		            else if(c=='m')
		                level = difficulty.valueOf("medium");
		            else if(c=='h')
		                level = difficulty.valueOf("hard");
		            else if(c=='r')
		                level = difficulty.valueOf("random");
		            else{
		                System.out.println("difficulty? \teasy (e), medium (m), hard (h), random(r)");
		            }
	            
		        }

	            System.out.println("Algorithm? AC3 (1) or Custom (2)");
	            if(scan.nextInt()==1)
	                alg = algorithm.valueOf("AC3");
	            else
	                alg = algorithm.valueOf("Custom");
	        
	
		        SudokuPlayer app = new SudokuPlayer();
		       
		        starttime = System.currentTimeMillis();
		        
		        app.run();
		        
		        endtime = System.currentTimeMillis();
		        
		        totaltime += (endtime-starttime);
	        
	       
	        	System.out.println("quit(0), run again(1)");
	        	if (scan.nextInt()==1)
	        		again=true;
	        	else
	        		again=false;
	        
	        	scan.nextLine();
	        
        	}
        
        	System.out.println("average time over "+numiters+" iterations: "+(totaltime/numiters));
        }
    
        
        
        scan.close();
    }



    class Board {
        GUI G = null;
        boolean gui = true;

        public Board(boolean X, SudokuPlayer s) {
            gui = X;
            if (gui)
                G = new GUI(s);
        }

        public void initVals(int[][] vals){
            G.initVals(vals);
        }

        public void writeVals(){
            if (gui)
                G.writeVals();
            else {
                for (int r = 0; r < 9; r++) {
                    if (r % 3 == 0)
                        System.out.println(" ----------------------------");
                    for (int c = 0; c < 9; c++) {
                        if (c % 3 == 0)
                            System.out.print (" | ");
                        if (vals[r][c] != 0) {
                            System.out.print(vals[r][c] + " ");
                        } else {
                            System.out.print("_ ");
                        }
                    }
                    System.out.println(" | ");
                }
                System.out.println(" ----------------------------");
            }
        }

        public void Clear(){
            if(gui)
                G.clear();
        }

        public void showMessage(String msg) {
            if (gui)
                G.showMessage(msg);
            System.out.println(msg);
        }

        public void updateVals(int[][] vals){
            if (gui)
                G.updateVals(vals);
        }

    }

    class GUI {
        // ---- Graphics ---- //
        int size = 40;
        JFrame mainFrame = null;
        JTextField[][] cells;
        JPanel[][] blocks;

        public void initVals(int[][] vals){
            // Mark in gray as fixed
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (vals[r][c] != 0) {
                        cells[r][c].setText(vals[r][c] + "");
                        cells[r][c].setEditable(false);
                        cells[r][c].setBackground(Color.lightGray);
                    }
                }
            }
        }

        public void showMessage(String msg){
            JOptionPane.showMessageDialog(null,
                    msg,"Message",JOptionPane.INFORMATION_MESSAGE);
        }

        public void updateVals(int[][] vals) {

           // System.out.println("calling update");
            for (int r = 0; r < 9; r++) {
                for (int c=0; c < 9; c++) {
                    try {
                        vals[r][c] = Integer.parseInt(cells[r][c].getText());
                    } catch (java.lang.NumberFormatException e) {
                        System.out.println("Invalid Board: row col: "+(r+1)+" "+(c+1));
                        showMessage("Invalid Board: row col: "+(r+1)+" "+(c+1));
                        return;
                    }
                }
            }
        }

        public void clear() {
            for (int r = 0; r < 9; r++){
                for (int c = 0; c < 9; c++){
                    if (cells[r][c].isEditable())
                    {
                        cells[r][c].setText("");
                        vals[r][c] = 0;
                    } else {
                        cells[r][c].setText("" + vals[r][c]);
                    }
                }
            }
        }

        public void writeVals(){
            for (int r=0;r<9;r++){
                for(int c=0; c<9; c++){
                    cells[r][c].setText(vals[r][c] + "");
                }   }
        }

        public GUI(SudokuPlayer s){

            mainFrame = new javax.swing.JFrame();
            mainFrame.setLayout(new BorderLayout());
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel gamePanel = new javax.swing.JPanel();
            gamePanel.setBackground(Color.black);
            mainFrame.add(gamePanel, BorderLayout.NORTH);
            gamePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            gamePanel.setLayout(new GridLayout(3,3,3,3));

            blocks = new JPanel[3][3];
            for (int i = 0; i < 3; i++){
                for(int j =2 ;j>=0 ;j--){
                    blocks[i][j] = new JPanel();
                    blocks[i][j].setLayout(new GridLayout(3,3));
                    gamePanel.add(blocks[i][j]);
                }
            }

            cells = new JTextField[9][9];
            for (int cell = 0; cell < 81; cell++){
                int i = cell / 9;
                int j = cell % 9;
                cells[i][j] = new JTextField();
                cells[i][j].setBorder(BorderFactory.createLineBorder(Color.BLACK));
                cells[i][j].setHorizontalAlignment(JTextField.CENTER);
                cells[i][j].setSize(new java.awt.Dimension(size, size));
                cells[i][j].setPreferredSize(new java.awt.Dimension(size, size));
                cells[i][j].setMinimumSize(new java.awt.Dimension(size, size));
                blocks[i/3][j/3].add(cells[i][j]);
            }

            JPanel buttonPanel = new JPanel(new FlowLayout());
            mainFrame.add(buttonPanel, BorderLayout.SOUTH);
            //JButton DFS_Button = new JButton("DFS");
            //DFS_Button.addActionListener(s);
            JButton AC3_Button = new JButton("AC-3");
            AC3_Button.addActionListener(s);
            JButton Clear_Button = new JButton("Clear");
            Clear_Button.addActionListener(s);
            JButton Check_Button = new JButton("Check");
            Check_Button.addActionListener(s);
            //buttonPanel.add(DFS_Button);
            JButton Custom_Button = new JButton("Custom");
            Custom_Button.addActionListener(s);
            //added
            buttonPanel.add(AC3_Button);
            buttonPanel.add(Custom_Button);
            buttonPanel.add(Clear_Button);
            buttonPanel.add(Check_Button);






            mainFrame.pack();
            mainFrame.setVisible(true);

        }
    }

    Random rand = new Random();

    // ----- Helper ---- //
    static algorithm alg = algorithm.AC3;
    static difficulty level = difficulty.easy;
    static boolean gui = true;
    static int numCells = 15;
    static DecimalFormat myformat = new DecimalFormat("###,###");
    
    //For printing
	static int recursions;
}




