import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.print.Doc;

public class SearchEngineMain {

	static String indexLocation = ("index");

	public static void main (String[] args) {
		//run queries
		try {
			ArrayList<Document> docList = parseDocuments();
			createIndex(docList);

			Scanner input = new Scanner(System.in);
			String option = "";

			while(true){
				option = "";

				while(!option.equals("0") && !option.equals("1") && !option.equals("2")){
					System.out.println("1)Read queries from file / Export for trec_eval  \n2)Search manually\n0)Exit");
					option = input.nextLine();
				}

				if (option.equals("0"))
					break;
				else if (option.equals("1")){
					searchFromFile("collection/queries.txt" , 20);
					searchFromFile("collection/queries.txt" ,30);
					searchFromFile("collection/queries.txt" ,50);
					runTrecEval();
				}
				else{
					searchManually();
				}
			}
		} catch (IOException e) {
			System.out.println("Could not read the file");
		}
	}
	
	public static ArrayList<Document> parseDocuments() throws IOException {
		Date start = new Date();

		File file = new File("./collection/documents.txt");
		BufferedReader br  = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		}

		String st;
		String id = "";
		String text = "";
		ArrayList<Document> documentList = new ArrayList<>();
		while ((st = br.readLine()) != null){
			try {  
				Integer.parseInt(st);  
				id = st;
			} catch(NumberFormatException e){
				if(st.equals(" /// ")){
					Document doc = new Document();
					//maybe needs some more splitting
					String[] titleAndContent = text.split(":", 2);

					TextField title = new TextField("title", titleAndContent[0], Field.Store.YES); //TODO: there might be better to find a better way to split
					TextField contents = new TextField("contents", text, Field.Store.NO);
					StoredField docId = new StoredField("id", id);
					doc.add(docId);
					doc.add(title);
					doc.add(contents);
					documentList.add(doc);
					text = "";
				} 
				else{ 
					text += st;
				}
			}
		}

		br.close();
		Date end = new Date();
		System.out.println("Parsing completed in " + (end.getTime() - start.getTime()) + " ms");
		return documentList;
	}
	
	public static void createIndex(ArrayList<Document> docs) {
		Date start = new Date();

		try{
			Directory dir = FSDirectory.open(Paths.get(indexLocation));
			Analyzer analyzer = new EnglishAnalyzer();
			BM25Similarity similarity = new BM25Similarity();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setSimilarity(similarity);
			iwc.setOpenMode(OpenMode.CREATE);

			IndexWriter indexWriter = new IndexWriter(dir, iwc);

			for (Document doc : docs){
				//System.out.println("adding " + doc.getField("id"));
				indexWriter.addDocument(doc);
			}

			indexWriter.close();

			Date end = new Date();
			System.out.println("Indexing completed in " + (end.getTime() - start.getTime()) + " ms\n");
		}
		catch (IOException e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}

	public static void searchManually(){
		String searchQuery = "";
		while (true){
			searchQuery = "";
			while (searchQuery.equals("")) {
				System.out.print("\033[H\033[2J");
				System.out.flush(); //clean data from terminal

				Scanner input = new Scanner(System.in);
				System.out.println("Enter query (or press '0' to quit) : ");
				System.out.print(">> ");
				searchQuery = input.nextLine();
			}

			if(searchQuery.equals("0"))
				return;

			ScoreDoc[] results = findResults(searchQuery, 20);
			try{
				System.out.println(results.length + " total matching documents for " + searchQuery);
				IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.

				for(int i=0; i<results.length; i++){
					Document hitDoc = indexReader.document(results[i].doc);
					System.out.println("\tScore: "+results[i].score + "\tid: " + hitDoc.get("id")   +"\ttitle: "+hitDoc.get("title"));
				}

			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}


	public static void searchFromFile(String filepath, int maxResults){
		try{
			Date start = new Date();
			System.out.println("Fetching top " + maxResults + " results from file " + filepath);

			ArrayList<String> queries = getQueriesFromFile(filepath);
			String resultFile = "collection/top"+ maxResults +"queryResults.txt";
			File myObj = new File(resultFile);
			FileWriter myWriter = new FileWriter(resultFile);

			for (int i = 0; i < queries.size(); i++){
				ScoreDoc[] results = findResults(queries.get(i), maxResults);

				IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation)));
				try{
					for (int j = 0; j < results.length; j++){
						Document hitDoc = indexReader.document(results[j].doc);
						myWriter.write(i + 1  < 10 ? "Q0"+ (i + 1) : "Q"+ (i + 1)); //query code
						myWriter.write("\t0"); // iter
						myWriter.write("\t" + hitDoc.get("id") + "\t0"); //doc id , rank
						myWriter.write("\t" + results[j].score); // similarity
						myWriter.write("\tmyIRmethod"); // method name
						myWriter.write(System.lineSeparator()); //need to do this
					}
				} catch(Exception e){
					e.printStackTrace();
				}

			}

			myWriter.close();
			Date end = new Date();
			System.out.println("Process completed in " + (end.getTime() - start.getTime()) + " ms\n");

		}
		catch (Exception e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}
	private static ScoreDoc[] findResults(String searchQuery, int maxResults) {
		try{
			IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.
			IndexSearcher indexSearcher = new IndexSearcher(indexReader); //Creates a searcher searching the provided index, Implements search over a single IndexReader.
			indexSearcher.setSimilarity(new BM25Similarity());

			Analyzer analyzer = new EnglishAnalyzer();

			// create a query parser on the field "contents"
			QueryParser parser = new QueryParser("contents", analyzer);
			Query query = parser.parse(searchQuery);
			
			TopDocs results = indexSearcher.search(query, maxResults);
			return results.scoreDocs;
		}
		catch (Exception e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}

		return null;
	}

	private static ArrayList<String> getQueriesFromFile(String filepath){
		try{
			String[] lines = new Scanner(new File(filepath)).useDelimiter("\\Z").next().split("\n");
			ArrayList<String> queryList = new ArrayList<String>();

			for (int i = 0; i < lines.length; i++)
				if (
						i != 0 && //if not Q01
								!lines[i].contains("///") && //if not query number
								!lines[i - 1].contains("///") //if not seperator
				){
					queryList.add(lines[i]);
				}

			return queryList;
		}
		catch (IOException e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());

			return null;
		}
	}

	private static void runTrecEval(){

		try{
			Runtime.getRuntime().exec("cmd /c start \"\" collection\\run_trec_eval.bat");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
