import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class SearchEngineMain {

	static String indexLocation = ("index");
	public static void main (String[] args) {
		//run queries
		try {
			ArrayList<Document> docList = parseDocuments();
			createIndex(docList);
		} catch (IOException e) {
			System.out.println("Could not read the file");
		}
	}
	
	public static ArrayList<Document> parseDocuments() throws IOException {
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
					TextField contents = new TextField("contents", text, Field.Store.NO);
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
		System.out.println("Parsing Done\n");
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
				System.out.println("adding " + doc.getField("id"));
				indexWriter.addDocument(doc);
			}

			System.out.println(indexWriter);
			indexWriter.close();

			Date end = new Date();
			System.out.println(end.getTime() - start.getTime() + " total milliseconds");

		}
		catch (IOException e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}

	public static void search(String searchQuery) {
	}
}
