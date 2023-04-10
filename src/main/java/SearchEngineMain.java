import java.io.*;
import java.util.ArrayList;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;

public class SearchEngineMain {
	public static void main (String[] args) {
		//run queries
		try {
			System.out.println(parseDocuments());
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
	
	public static void createIndex() {
		
	}
	
	public static void search() {
		
	}
}
