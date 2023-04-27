import java.io.*;
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
			search();
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
					TextField contents = new TextField("contents", text, Field.Store.YES);
					StoredField docId = new StoredField("id", id);
					doc.add(docId);
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
		System.out.println("Parsing completed in " + (end.getTime() - start.getTime()) + " ms \n");
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
			System.out.println("Indexing completed in " + (end.getTime() - start.getTime()) + " ms\nPress any button to continue");
			Scanner input = new Scanner(System.in);
			input.nextLine();
		}
		catch (IOException e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}

	public static void search(){
		String query = "";
		while (!query.equals("0")){
			query = "";
			while (query.equals("")) {
				System.out.print("\033[H\033[2J");
				System.out.flush(); //clean data from terminal

				Scanner input = new Scanner(System.in);
				System.out.println("Enter query (or press '0' to quit) : ");
				System.out.print(">> ");
				query = input.nextLine();
			}

			if(query.equals("0"))
				return;

			ScoreDoc[] results = findResults(query);
			try{
				System.out.println(results.length + " total matching documents");
				IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.

				for(int i=0; i<results.length; i++){
					Document hitDoc = indexReader.document(results[i].doc);
					System.out.println("\tScore "+results[i].score + "\tid=" + hitDoc.get("id")   +"\tcontent="+hitDoc.get("contents"));
				}

			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}


	public static ScoreDoc[] findResults(String searchQuery) {
		try{
			IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.
			IndexSearcher indexSearcher = new IndexSearcher(indexReader); //Creates a searcher searching the provided index, Implements search over a single IndexReader.
			indexSearcher.setSimilarity(new BM25Similarity());

			Analyzer analyzer = new EnglishAnalyzer();

			// create a query parser on the field "contents"
			QueryParser parser = new QueryParser("contents", analyzer);
			Query query = parser.parse(searchQuery);

			System.out.println("Searching for: " + query.toString());

			TopDocs results = indexSearcher.search(query, 10);
			return results.scoreDocs;
		}
		catch (Exception e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}

		return null;
	}
}
