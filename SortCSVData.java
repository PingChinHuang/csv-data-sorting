import java.io.*;
import java.util.*;
import java.nio.charset.*;

public class SortCSVData {
	private static String inputFileName = null;
	private static String outputFileName = "./output.csv";
	private static List<String> dataTypeRow = new ArrayList<String>(); 
	private static List<Integer> newDataTypeOrder = new ArrayList<Integer>();
	private static char seperator = CSV.DEFAULT_SEP;
	private static CSV parser = null;
	private static int dataTypeIndex = -1;
	private static String sortingDataType = "";
	
	private static void setCSVParserSeperator() {
		if (seperator == CSV.DEFAULT_SEP)
			parser = new CSV();
		else
			parser = new CSV(seperator);
	}
	
	private static void sortList(List<String> aItems){
		Collections.sort(aItems, String.CASE_INSENSITIVE_ORDER);
	}
	
	private static void setSortingDataType(String row, String dataType) {
		int idx = 0;
		List<String> originalOrder = new ArrayList<String>();
		List<String> temp= parser.parse(row);
		
		originalOrder.addAll(temp);
		Iterator<String> it = originalOrder.iterator();
		while (it.hasNext()) {
			String next = it.next().toString();
			if (next.equalsIgnoreCase(dataType)) {
				dataTypeIndex = idx;
				break;
			} else
				idx++;
		}
		
		sortList(temp);
		dataTypeRow.addAll(temp);
		
		for (int i = 0; i < originalOrder.size(); i++) {
			for (int j = 0; j < dataTypeRow.size(); j++) {
				if (originalOrder.get(i) == dataTypeRow.get(j)) {
					newDataTypeOrder.add(j);
					break;
				}
			}
		}	
	}
	
	private static List<File> sortInBatch(File file, Charset charset, Comparator<String> cmp) throws IOException {
		List<File> files = new Vector<File>();
		BufferedReader fbr = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
		try{
			List<String> tmplist =  new Vector<String>();
			
			try {
				String line = fbr.readLine();
				
				if (line != null) {
					setSortingDataType(line, sortingDataType);
					if (dataTypeIndex < 0) {
						System.err.println("Specified data type was not in the CSV." +
								"\nCSV file " + inputFileName + " cannot be sorted.");
						return null;
					}
				}
				
				while(line != null) {
					tmplist = new Vector<String>();
					while ((Runtime.getRuntime().freeMemory() > 2097152)
							&& ((line = fbr.readLine()) != null)) { // as long as you have 2MB
						if (!line.isEmpty())
							tmplist.add(line);
					}
					files.add(sortAndSave(tmplist, charset, cmp));
					tmplist.clear();
				}
			} catch (EOFException oef) {
				if(tmplist.size()>0) {
					files.add(sortAndSave(tmplist, charset, cmp));
					tmplist.clear();
				}
			}
		} finally {
			fbr.close();
		}
		return files;
	}

	private static File sortAndSave(List<String> tmplist, Charset charset, Comparator<String> cmp) throws IOException  {
		Collections.sort(tmplist,cmp);
		File newtmpfile = File.createTempFile("sortInBatch", "flatfile");
		newtmpfile.deleteOnExit();
		BufferedWriter fbw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newtmpfile), charset));
		try {
			for(String r : tmplist) {
				fbw.write(r);
				fbw.newLine();
			}
		} finally {
			fbw.close();
		}
		return newtmpfile;
	}

	private static int mergeSortedFiles(List<File> files, File outputfile, Charset charset, Comparator<String> cmp) throws IOException {
		PriorityQueue<BinaryFileBuffer> pq = new PriorityQueue<BinaryFileBuffer>();
		for (File f : files) {
			BinaryFileBuffer bfb = new BinaryFileBuffer(f,charset,cmp);
			pq.add(bfb);
		}
		BufferedWriter fbw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputfile), charset));
		int rowcounter = 0;
		try {
			fbw.write(parser.convertToCSVFormat(dataTypeRow) + '\n');
			while(pq.size()>0) {
				BinaryFileBuffer bfb = pq.poll();
				String r = bfb.pop();
				
				List<String> originalList = parser.parse(r);
				List<String> newList = new ArrayList<String>();

				newList.addAll(originalList);
				for (int i = 0; i < originalList.size(); i++)
					newList.set(newDataTypeOrder.get(i), originalList.get(i));
				
				fbw.write(parser.convertToCSVFormat(newList));
				fbw.newLine();
				++rowcounter;
				if(bfb.empty()) {
					bfb.fbr.close();
					bfb.originalfile.delete();// we don't need you anymore
				} else {
					pq.add(bfb); // add it back
				}
			}
		} finally {
			fbw.close();
		}
		return rowcounter;
	}

	public static void main(String[] args) {
		int i = 0;
	    String arg;
	    boolean vflag = false;

	    while (i < args.length && args[i].startsWith("-")) {
	    	arg = args[i++];

	    	if (arg.equals("-verbose")) {
	    		System.out.println("verbose mode on");
			    vflag = true;
	    	} else if (arg.equals("-output")) {
	    		if (i < args.length)
	    			outputFileName = args[i++];
	    		else {
	    			System.err.println("-output requires a filename");
	    			return;
	    		}
	    		
	    		if (vflag)
	    			System.out.println("output file = " + outputFileName);
	    	} else if (arg.equals("-sorting")) {
	    		if (i < args.length) {
	    			if (args[i + 1].charAt(0) == '"') {
	    				int endIndex = args[i + 1].indexOf('"', 0);
	    				if (endIndex == -1)
	    					return;
	    				sortingDataType = args[i++].substring(0, endIndex);
	    			} else 	
	    				sortingDataType = args[i++];
	    		} else {
	    			System.err.println("-sorting requires a data type");
	    			return;
	    		}
	    		
	    		if (vflag)
	    			System.out.println("sorting data = " + sortingDataType);
	    	}
	    }
	    
	    if (i == args.length) {
	    	System.err.println("Usage: java SortCSVData [-verbose] " +
	    			"[-sorting [type or \"type\"] " +
	    			"[-output file] " +
	    			"filename.csv");
	    	return;
	    } else {
	    	String [] token = args[i].split("\\.");
	    	if (token[token.length - 1].compareToIgnoreCase("csv") == 0)
	    		inputFileName = args[i];
	    	else {
	    		System.err.println("The input file need to be a .csv file.");
	    		return;
	    	}
	    }
		
		try	{
            Charset charset = Charset.defaultCharset();
    		Comparator<String> comparator = new Comparator<String>() {
    			public int compare(String r1, String r2) {
					String r1Value = parser.parse(r1).get(dataTypeIndex).toString();
					String r2Value = parser.parse(r2).get(dataTypeIndex).toString();
					
					try {
						Double r1Double = Double.parseDouble(r1Value);
						Double r2Double = Double.parseDouble(r2Value);
						
						return r1Double.compareTo(r2Double);
					} catch (NumberFormatException e) {
						return r1Value.compareToIgnoreCase(r2Value);
					}
    			}
    		};
    		
			setCSVParserSeperator();
    		List<File> l = sortInBatch(new File(inputFileName), charset, comparator);
    		if (l == null) return;
    		mergeSortedFiles(l, new File(outputFileName), charset, comparator);
		} catch(IOException e) {
			System.out.println("Exception while sorting CSV file: " + e);			
		}
	}

}

class CSV {  
	public static final char DEFAULT_SEP = ',';

	public CSV() {
		this(DEFAULT_SEP);
	}

	public CSV(char sep) {
		fieldSep = sep;
	}

	protected List<String> list = new ArrayList<String>();

	protected char fieldSep;

	public List<String> parse(String line) {
		StringBuffer sb = new StringBuffer();
		list.clear();      // recycle to initial state
		int i = 0;

		if (line.length() == 0) {
			list.add(line);
			return list;
		}

		do {
			sb.setLength(0);
			if (i < line.length() && line.charAt(i) == '"')
				i = advQuoted(line, sb, ++i);  // skip quote
			else
				i = advPlain(line, sb, i);
			list.add(sb.toString());
			i++;
		} while (i < line.length());

		return list;
	}
	
	public String convertToCSVFormat(List<String> l) {
		String csvStr = "";
		Iterator<String> it = l.iterator();
		
	    while (it.hasNext()) {
	    	String next = it.next();
	    	
	    	try {
	    		Double.parseDouble(next);
	    		csvStr += next;
	    	} catch (NumberFormatException e) {
	    		csvStr += '\"' + next + '\"';
	    	}
	    	
    		if (it.hasNext())
    			csvStr += ',';
	    }
		
		return csvStr;
	}

	protected int advQuoted(String s, StringBuffer sb, int i) {
		String token = null;
		int j = 0;
		int sepIdx = s.indexOf(fieldSep, i);
		
		if (sepIdx == -1) {
			token = s.substring(i);
			sepIdx = s.length();
		} else
			token = s.substring(i, sepIdx);
		
		for (j = 0; j < token.length(); j++)
			if (token.charAt(j) != '"')
				sb.append(token.charAt(j));

		return sepIdx;
	}

	protected int advPlain(String s, StringBuffer sb, int i) {
		int j;

		j = s.indexOf(fieldSep, i); // look for separator
		if (j == -1) {              // none found
			sb.append(s.substring(i));
			return s.length();
		} else {
			sb.append(s.substring(i, j));
			return j;
		}
	}
}

class BinaryFileBuffer implements Comparable<BinaryFileBuffer>{
	public static int BUFFERSIZE = 512;
	public BufferedReader fbr;
	private List<String> buf = new Vector<String>();
	int currentpointer = 0;
	Comparator<String> mCMP;
	public File originalfile;
	
	public BinaryFileBuffer(File f, Charset charset, Comparator<String> cmp) throws IOException {
		originalfile = f;
		mCMP = cmp;
		fbr = new BufferedReader(new InputStreamReader(new FileInputStream(f), charset));
		reload();
	}
	
	public boolean empty() {
		return buf.size()==0;
	}
	
	private void reload() throws IOException {
		  buf.clear();
		  try {
		  	  String line;
	 		  while((buf.size()<BUFFERSIZE) && ((line = fbr.readLine()) != null))
				buf.add(line);
			} catch(EOFException oef) {
			}		
	}
	
	public String peek() {
		if(empty()) return null;
		return buf.get(currentpointer);
	}
	
	public String pop() throws IOException {
	  String answer = peek();
	  ++currentpointer;
	  if(currentpointer == buf.size()) {
		  reload();
		  currentpointer = 0;
	  }
	  return answer;
	}
	
	public int compareTo(BinaryFileBuffer b) {
		return mCMP.compare(peek(), b.peek());
	}
}
