 package sortCompare;
import java.io.*;
import java.util.*;

/**
 * Sorts the data on-disk, by sorting the data in small chunks and then merging
 * the data into one larger chunk
 */
public class OnDiskSort{	
	// the maximum number of words to read on each iteration
	private int maxSize;
	
	// the current working directory
	private File workingDirectory;
	
	// the sorter
	private Sorter<String> sorter;
	

	// tempFile count in the linear merge iterations
	private int tempFileCount = 0;
	/**
	 * Creates a new sorter for sorting string data on disk.  The sorter operates by reading
	 * in maxSize worth of data elements (in this case, Strings) and then sorts them using
	 * the provided sorter.  It does this chunk by chunk for all of the data, at each stage
	 * writing the sorted data to temporary files in workingDirectory.  Finally, the sorted files
	 * are merged together (in pairs) until there is a single sorted file.  The final output of this
	 * sorting should be in outputFile
	 * 
	 * @param maxSize the maximum number of items to put in a chunk
	 * @param workingDirectory the directory where any temporary files created during sorting should be placed
	 * @param sorter the sorter to use to sort the chunks in memory
	 */
	public OnDiskSort(int maxSize, File workingDirectory, Sorter<String> sorter){
		
		// storing  constructor parameters		
		this.maxSize = maxSize;
		this.workingDirectory = workingDirectory;
		this.sorter = sorter;
		
		// create directory if it doesn't exist
		if( !workingDirectory.exists() ){
			workingDirectory.mkdir();
		}
	}
	
	/**
	 * Remove all files that that end with fileEnding in workingDirectory
	 * 
	 * If you name all of your temporary files with the same file ending, for example ".temp_sorted" 
	 * then it's easy to clean them up using this method
	 * 
	 * @param workingDirectory the directory to clear
	 * @param fileEnding clear only those files with fileEnding
	 */
	private void clearOutDirectory(File workingDirectory, String fileEnding){
		for( File file: workingDirectory.listFiles() ){
			if( file.getName().endsWith(fileEnding) ){
				file.delete();
			}
		}
	}
		
	/**
	 * Write the data in dataToWrite to outfile one String per line
	 * 
	 * @param outfile the output file
	 * @param dataToWrite the data to write out
	 */
	private void writeToDisk(File outfile, ArrayList<String> dataToWrite){
		try{
			PrintWriter out = new PrintWriter(new FileOutputStream(outfile));
			
			for( String s: dataToWrite ){
				out.println(s);
			}
			
			out.close();
		}catch(IOException e){
			throw new RuntimeException(e.toString());
		}
	}
	
	/**
	 * Copy data from fromFile to toFile
	 * 
	 * @param fromFile the file to be copied from
	 * @param toFile the destination file to be copied to
	 */
	private void copyFile(File fromFile, File toFile){
		try{
			BufferedReader in = new BufferedReader(new FileReader(fromFile));
			PrintWriter out = new PrintWriter(new FileOutputStream(toFile));
			
			String line = in.readLine();
			
			while( line != null ){
				out.println(line);
				line = in.readLine();
			}
			
			out.close();
			in.close();
		}catch(IOException e){
			throw new RuntimeException(e);
		}
	}
	
	/** 
	 * Sort the data in data using an on-disk version of sorting
	 * 
	 * @param dataReader an Iterator for the data to be sorted
	 * @param outputFile the destination for the final sorted data
	 */
	public void sort(Iterator<String> dataReader, File outputFile){
		// a temporary array to hold the temporary files
		ArrayList<File> tempFiles = new ArrayList<File>();
		
		// a desired chunk of the larger file
		ArrayList<String> chunk = new ArrayList<String>();

		// number of words read
		int	wordCount = 0;
		
		// loops through input to add chunks of desired sizes to 
		// array and to directory
		while (dataReader.hasNext()) {
			chunk.add(dataReader.next());
			wordCount++;
			
			if (wordCount % maxSize == 0) {
				sorter.sort(chunk);
				File tempFile = new File(workingDirectory, 
						String.format("%d.tempfile", wordCount/maxSize));
				
				// saving chunk to a file in working directory
				writeToDisk(tempFile, chunk);
				
				// adding file to array
				tempFiles.add(tempFile);
				
				// reseting chunk variable;
				chunk.removeAll(chunk);
			}
			
			// getting the last chunk in the array if it's smaller than maxSize
			if (!dataReader.hasNext() && !chunk.isEmpty()) {
				sorter.sort(chunk);
				File tempFile = new File(workingDirectory,
						"lastEntry.tempfile" );
				writeToDisk(tempFile, chunk);
				tempFiles.add(tempFile);
			}
		}
		
		// sorting the temporary files into one output file
		mergeFilesLinear(tempFiles, outputFile);
		
		// clearing out all temporary files
		clearOutDirectory(workingDirectory, "tempfile");
	}
	
	/**
	 * Merges all the Files in sortedFiles into one sorted file, whose destination is outputFile.
	 * 
	 * @pre All of the files in sortedFiles contained data that is sorted
	 * @param sortedFiles a list of files containing sorted data
	 * @param outputFile the destination file for the final sorted data
	 */
	protected void mergeFiles(ArrayList<File> sortedFiles, File outputFile){
		// the first file index
		int firstFileIndex = 0;
		
		// temporary files to hold sorting outputs
		File tempFile = new File("sorting_run//tempFile.tempfile");
		File tempOutput = new File("sorting_run//tempOutput.tempfile");
		
		try {
			PrintWriter tempWrite = new PrintWriter(tempFile);
			PrintWriter tempOut = new PrintWriter(tempOutput);
			
			tempWrite.close();
			tempOut.close();
			
			// iterating through file list until files are exhausted
			while (firstFileIndex < sortedFiles.size() ) {
				// the first and second files to sort
				File firstFile = sortedFiles.get(firstFileIndex);
				File secondFile;
				
				// 
				BufferedReader tempFileReader = new 
						BufferedReader(new FileReader(tempFile));
				String line = tempFileReader.readLine();
				
				// making the second file the output of a 
				// previous merge if there have been any
				if(line != null) {
					tempFileReader.close();
					secondFile = tempFile;
				} else {
					secondFile = sortedFiles.get(firstFileIndex + 1);
					tempFileReader.close();
				}
				
				// making the output of the last merge the desired 
				// output if all files get exhausted
				if (firstFileIndex == sortedFiles.size() - 1) {
					merge(firstFile, secondFile, outputFile);					
				}
				else {
					merge(firstFile, secondFile, tempOutput);
				}
				
				// increasing the first file index
				if (firstFileIndex == 0) {
					firstFileIndex += 2;
				}
				else if(firstFileIndex != 0) {
					firstFileIndex++;
				}
				
				// copying merge outputs to be used as subsequent inputs
				copyFile(tempOutput, tempFile);
				
				// clearing out output file;
				clearFileContents(tempOutput);
			}
		}
		catch(IOException e) {
			// Printing out out IO exceptions if one occurs
			throw new RuntimeException(e.toString() + "in MergeFiles");
		}
		catch(Exception e) {
			// Printing out other kinds of exceptions
			throw new RuntimeException(e.toString() + "in MergeFiles");	
		} 
	}
	
	/**
	 * A method to delete all content from a file
	 * 
	 * @pre the file has some content/ or not
	 * @post the file contents are cleared 
	 *  
	 * @param file the file to be reset
	 */
	public void clearFileContents(File file) {
		try {
			// deleting contents from file;
			PrintWriter deleter = new PrintWriter(file);
			deleter.print("");
			deleter.close();
		} 
		catch(IOException e) {
			// Printing out out IO exceptions if one occurs
			throw new RuntimeException(e.toString() + "in clearContents");
		}
		catch(Exception e) {
			// Printing out other kinds of exceptions
			throw new RuntimeException(e.toString() + "in clearContents");	
		} 
	}
	
	/**
	 * Linearly merges all the Files in sortedFiles into one sorted file, whose destination is outputFile.
	 * 
	 * @pre All of the files in sortedFiles contained data that is sorted
	 * @param sortedFiles a list of files containing sorted data
	 * @param outputFile the destination file for the final sorted data
	 */
	protected void mergeFilesLinear(ArrayList<File> sortedFiles, File outputFile){
		// the first and second file indices
		int firstFileIndex = 0;
		int secondFileIndex = 1;
		
		// an array to hold the temporary merged outputs
		ArrayList<File> tempFiles = new ArrayList<File>();
		
		try {	
			while (firstFileIndex < sortedFiles.size()) {
				// the files to be merged
				File firstFile = sortedFiles.get(firstFileIndex);
				File secondFile = null;
				
				// a temporary output file
				File tempOutput = new 
						File(String.format("sorting_run//%d.temp", tempFileCount));
				
				if (secondFileIndex < sortedFiles.size()) {
					secondFile = sortedFiles.get(secondFileIndex);
				} 				
				
				// merging the files into one file and adding them to array
				if (secondFileIndex < sortedFiles.size()) {
					merge(firstFile, secondFile, tempOutput);
					tempFiles.add(tempOutput);
					
				}
				// adding the last file to the array
				if(firstFileIndex < sortedFiles.size() &&
						secondFileIndex >= sortedFiles.size()) {
					tempFiles.add(firstFile);
				}
				
				// increasing the file indices
				firstFileIndex+=2;
				secondFileIndex+=2;
				
				tempFileCount++;

			}
		
			// recursively sorting files until there's 
			// only one file left
			 if (tempFiles.size() > 1) {
				 mergeFilesLinear(tempFiles, outputFile);
			}
			
			// copying output into desired file
			if (tempFiles.size() == 1) {
				copyFile(tempFiles.get(0), outputFile);
				
				// clearing temporary files form directory
				clearOutDirectory(workingDirectory, "temp");
			}
			
		} catch(Exception e) {
			// Printing out other kinds of exceptions
			throw new RuntimeException(e.toString() + "in MergeFilesLinear");	
		}  
	}
	
	/**
	 * Given two files containing sorted strings, one string per line, merge them into
	 * one sorted file
	 * 
	 * @param file1 file containing sorted strings, one per line
	 * @param file2 file containing sorted strings, one per line
	 * @param outFile destination file for the results of merging the two files
	 */
	protected void merge(File file1, File file2, File outFile){
		try {
			BufferedReader bReader1 = new BufferedReader(new FileReader(file1)); 
			BufferedReader bReader2 = new BufferedReader(new FileReader(file2));
			PrintWriter outputWriter = new PrintWriter(new PrintWriter(outFile));
			
			// a line from each file
			String line1 = bReader1.readLine();
			String line2 = bReader2.readLine();
			
			// comparing lines to sort them into an output file
			while (line1 != null && line2 != null) {
				if (line1.compareTo(line2) <= 0) {
					outputWriter.println(line1);
					line1 = bReader1.readLine();
				}
				else {
					outputWriter.println(line2);
					line2 = bReader2.readLine();
				}
			}
			
			// adding lines to output if first file's not empty
			while (line1 != null) {
				outputWriter.println(line1);
				line1 = bReader1.readLine();
				
			}
			// adding lines to output if second file's not empty
			while (line2 != null) {
				outputWriter.println(line2);
				line2 = bReader2.readLine();
			}
			
			// closing all files
			bReader1.close();
			bReader2.close();
			outputWriter.close();
		}
		// Printing out IO exceptions if one occurs
		catch(IOException e) {
			throw new RuntimeException(e.toString() + "in Merge");
		}
		// Printing out other kinds of exceptions
		catch(Exception e) {
			throw new RuntimeException(e.toString() + "in Merge");
		}
	}
	
	/**
	 * Create a sorter that does a mergesort in memory
	 * Create a diskSorter to do external merges
	 * Use subdirectory "sorting_run" of your project as the working directory
	 * Create a word scanner to read King's "I have a dream" speech.
	 * Sort all the words of the speech and put them in file data.sorted
	 * @param args -- not used!
	 */
	public static void main(String[] args){
		MergeSort<String> sorter = new MergeSort<String>();
		OnDiskSort diskSorter = new OnDiskSort(10, new File("sorting_run"), 
				sorter);
		
		WordScanner scanner = new WordScanner(new File("sorting_run//Ihaveadream.txt"));
		
		System.out.println("running");		
		diskSorter.sort(scanner, new File("sorting_run//data.sorted"));
		System.out.println("done");	
	}
}
