import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.en.PorterStemFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
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
import org.deeplearning4j.models.embeddings.learning.impl.elements.CBOW;
import org.deeplearning4j.models.embeddings.learning.impl.elements.SkipGram;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;

public class SearchEngineMain {

	final static String indexLocation = ("index");

	private static Word2Vec vec;
	private static Analyzer synonymAnalyzer;
	/**
	 * main method of SearchEngineMain
	 * @param args
	 * @author Anastasios Angelidis / Panagiotis Lampropoulos
	 */
	public static void main (String[] args) {
		try {
			SentenceIterator iter = new BasicLineIterator("./collection/documents.txt");

			vec = new Word2Vec.Builder()
					.layerSize(500)
					.windowSize(10)
					.epochs(1)
					.iterate(iter)
					.elementsLearningAlgorithm(new CBOW<>())
					//.tokenizerFactory(new LuceneTokenizerFactory(new StandardAnalyzer()))
					.build();

			vec.fit();

			ArrayList<Document> docList = parseDocuments();
			createIndex(docList);
//			filterWordnetFile();

//			Scanner input = new Scanner(System.in);
//			String option = "";
//
//			while(true){
//				option = "";
//
//				while(!option.equals("0") && !option.equals("1") && !option.equals("2")){
//					System.out.println("1)Read queries from file / Run trec_eval  \n2)Search manually\n0)Exit");
//					option = input.nextLine();
//				}
//
//				if (option.equals("0"))
//					break;
//				else if (option.equals("1")){
					searchFromFile("collection/queries.txt" , 20);
					searchFromFile("collection/queries.txt" ,30);
					searchFromFile("collection/queries.txt" ,50);
					runTrecEval();
//				} else{
//					searchManually();
//				}
//			}
		} catch (IOException e) {
			System.out.println("Could not read the file");
		}
	}

	/**
	 * breaks documents.txt into a list of Lucene Documents
	 * @return ArrayList of documents
	 * @throws IOException
	 * @author Anastasios Angelidis
	 */
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
				} else{
					text += st;
				}
			}
		}

		br.close();
		Date end = new Date();
		System.out.println("Parsing completed in " + (end.getTime() - start.getTime()) + " ms");
		return documentList;
	}

	/**
	 * creates an index on the indexLocation directory
	 * @param docs (a list of documents)
	 * @author Panagiotis Lampropoulos
	 */
	public static void createIndex(ArrayList<Document> docs) {
		Date start = new Date();

		try{
			Directory dir = FSDirectory.open(Paths.get(indexLocation));
			BM25Similarity similarity = new BM25Similarity();
			IndexWriterConfig iwc = new IndexWriterConfig(new WhitespaceAnalyzer());
			iwc.setSimilarity(similarity);
			iwc.setOpenMode(OpenMode.CREATE);

			IndexWriter indexWriter = new IndexWriter(dir, iwc);

			for (Document doc : docs){
				indexWriter.addDocument(doc);
			}

			indexWriter.close();

			Date end = new Date();
			System.out.println("Indexing completed in " + (end.getTime() - start.getTime()) + " ms");
		} catch (IOException e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}

	/**
	 * manual searching mode, mostly developed for debugging
	 * shows titles of top 20 matching documents
	 * @author Panagiotis Lampropoulos
	 */
	public static void searchManually(){
		String searchQuery = "";
		while (true){
			searchQuery = "";
			while (searchQuery.equals("")) {
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

			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}


	/**
	 * @param filepath the filepath which the queries are located in
	 * @param maxResults the maximum number of results we want to find
	 * creates a text file which can be used by trec_eval
	 * @author Anastasios Angelidis / Panagiotis Lampropoulos
	 */
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
					System.out.println("Query " + (i + 1) + ": results: " + results.length + "(max:" + maxResults + ")");
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

		} catch (Exception e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}
	}


	/**
	 * @param searchQuery the text of the query made by the user
	 * @param maxResults the maximum number of results to be shown
	 * @return a ScoreDoc array of the top results
	 * @author Panagiotis Lampropoulos
	 */
	private static ScoreDoc[] findResults(String searchQuery, int maxResults) {
		try{
			IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation))); //IndexReader is an abstract class, providing an interface for accessing an index.
			IndexSearcher indexSearcher = new IndexSearcher(indexReader); //Creates a searcher searching the provided index, Implements search over a single IndexReader.
			indexSearcher.setSimilarity(new BM25Similarity());

			// create a query parser on the field "contents"
			synonymAnalyzer = new Analyzer() {
				@Override
				protected TokenStreamComponents createComponents(String fieldName) {
					Tokenizer tokenizer = new WhitespaceTokenizer();
					double minAcc = 0.9;
					TokenFilter synFilter = new W2VSynonymFilter(tokenizer, vec, minAcc);
					return new TokenStreamComponents(tokenizer, synFilter);
				}
			};

			QueryParser parser = new QueryParser("contents", synonymAnalyzer);
			Query query = parser.parse(searchQuery);

			TopDocs results = indexSearcher.search(query, maxResults);
			return results.scoreDocs;
		} catch (Exception e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());
		}

		return null;
	}

	/**
	 * @param filepath the filepath we want to get the queries from
	 * @return a ArrayList of strings (the queries)
	 * @author Panagiotis Lampropoulos
	 */
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
		} catch (IOException e){
			System.out.println(" caught a " + e.getClass() +
					"\n with message: " + e.getMessage());

			return null;
		}
	}

	/**
	 * method that executes the run_trec_eval.bat batch file
	 * this batch file runs trec_eval, comparing collection\qrels.txt with the files generated by searchFromFile()
	 * @author Panagiotis Lampropoulos
	 */
	private static void runTrecEval(){

		try{
			Runtime.getRuntime().exec("cmd /c start \"\" collection\\run_trec_eval.bat");
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Filters Wordnet file and keeps only hyponym nouns
	 * @author Anastasios Angelidis
	 */
	private static void filterWordnetFile() throws IOException {
		Date start = new Date();

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream inputStream = classloader.getResourceAsStream("wn_s.pl");
		InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		BufferedReader br  = new BufferedReader(streamReader);

		String filteredFile = "src/main/resources/my_wn_s.pl";
		File myObj = new File(filteredFile);
		FileWriter myWriter = new FileWriter(filteredFile);

		String ln;
		while ((ln = br.readLine()) != null){
			String[] noun = ln.split(",");
			if(noun[3].equals("n") && Integer.parseInt(noun[5].substring(0, noun[5].length()-2)) < 1){
				myWriter.write(ln);
				myWriter.write(System.lineSeparator());
			}
		}

		myWriter.close();

		Date end = new Date();
		System.out.println("Filtering Wordnet completed in " + (end.getTime() - start.getTime()) + " ms\n");
	}

	/**
	 * Custom analyzer using wordent
	 * @return the new analyzer
	 * @throws IOException
	 * @author Anastasios Angelidis
	 */
	private static CustomAnalyzer customAnalyzerForQueryExpansion() throws IOException {
		Map<String, String> sffargs = new HashMap<>();
		sffargs.put("synonyms", "my_wn_s.pl");
		sffargs.put("format", "wordnet");

		CustomAnalyzer.Builder builder = CustomAnalyzer.builder()
				.withTokenizer(StandardTokenizerFactory.class)
				.addTokenFilter(StandardFilterFactory.class)
				.addTokenFilter(EnglishPossessiveFilterFactory.class)
				.addTokenFilter(LowerCaseFilterFactory.class)
				.addTokenFilter(StopFilterFactory.class)
				.addTokenFilter(PorterStemFilterFactory.class)
				.addTokenFilter(SynonymGraphFilterFactory.class, sffargs);
		CustomAnalyzer analyzer = builder.build();
		return analyzer;
	}
}
