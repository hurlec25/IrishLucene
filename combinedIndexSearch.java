package Lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Scanner;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class combinedIndexSearch {
	
	public static String findWords;
	
	static void combIndex(){
		String indexPath = "/home/hurlec25/IrishLucene-master/TestPath";
		String docsPath = "/home/hurlec25/IrishLucene-master/TestData";
		boolean create = true;
		
		final Path docDir = Paths.get(docsPath);
		if (!Files.isReadable(docDir)) {
		 System.out.println("Document directory '" +docDir.toAbsolutePath()+ "' does not exist or is not readable, please check the path");
		 System.exit(1);
		}
		
		Date start = new Date();
		try {
		 System.out.println("Indexing to directory '" + indexPath + "'...");
		
		 Directory dir = FSDirectory.open(Paths.get(indexPath));
		 Analyzer analyzer = new StandardAnalyzer();
		 IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		
		 if (create) {
		   iwc.setOpenMode(OpenMode.CREATE);
		 } else {
		   iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		 }
		
		
		 IndexWriter writer = new IndexWriter(dir, iwc);
		 indexDocs(writer, docDir);

		
		 writer.close();
		
		 Date end = new Date();
		 System.out.println(end.getTime() - start.getTime() + " total milliseconds");
		
		} catch (IOException e) {
			 System.out.println(" caught a " + e.getClass() +
			  "\n with message: " + e.getMessage());
		}
	}
	
	static void indexDocs(final IndexWriter writer, Path path) throws IOException {
	    if (Files.isDirectory(path)) {
	      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	          try {
	            indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
	          } catch (IOException ignore) {
	            // don't index files that can't be read.
	          }
	          return FileVisitResult.CONTINUE;
	        }
	      });
	    } else {
	      indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
	    }
	}
	static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
		try (InputStream stream = Files.newInputStream(file)) {
	      // make a new, empty document
	      Document doc = new Document();  
	      
	      String result= findHighLight(file);
	      
	      Field pathField = new StringField("path", file.toString(), Field.Store.YES);
	      Field testField = new StringField("highlights", result,Field.Store.YES);
	      doc.add(pathField);
	      doc.add(testField);
	      
	      doc.add(new LongField("modified", lastModified, Field.Store.NO));
	      
	      doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
	      
	      if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
	        System.out.println("adding " + file);
	        writer.addDocument(doc);
	      } else {
	        System.out.println("updating " + file);
	        writer.updateDocument(new Term("path", file.toString()), doc);
      		}
	    }
	}
	
	static String findHighLight(Path file){
		String searchText="";
	  	String result="";
	  	try {
			Scanner sc=new Scanner(new File(file.toString()));
			searchText="";
			while(sc.hasNext()){
				searchText=searchText+" "+sc.next();
			}
			QueryParser parser = new QueryParser("f",new StandardAnalyzer());
			Query query = parser.parse(QueryParser.escape(findWords));
			TokenStream tokens = new StandardAnalyzer().tokenStream("f", new StringReader(searchText));
			QueryScorer scorer = new QueryScorer(query, "f");
			Highlighter highlighter = new Highlighter(scorer);
			highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer));
			result=highlighter.getBestFragments(tokens, searchText, 3, "...");
			sc.close();
		}
	  	catch(Exception e) {
	  		e.printStackTrace();
	  	}
	  	return result;
	}
	
	static void combSearch() throws IOException, ParseException{
		String index = "/home/hurlec25/IrishLucene-master/TestPath";
	    String field = "contents";
	    String queries = null;
	    int repeat = 0;
	    boolean raw = false;
	    String queryString = null;
	    int hitsPerPage = 50;
	    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
	    IndexSearcher searcher = new IndexSearcher(reader);
	    Analyzer analyzer = new StandardAnalyzer();
	
	    BufferedReader in = null;
	    if (queries != null) {
	    	in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
	    } else {
	    	in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
	    }
	    QueryParser parser = new QueryParser(field, analyzer);
	    while (true) {
    		if (queries == null && queryString == null) {                        // prompt the user
	    		System.out.println("Enter query: ");
	    	}
	
		    String line=findWords;
		
		    if (line == null || line.length() == -1) {
		    	break;
		    }
	
			line = line.trim();
			if (line.length() == 0) {
				break;
			}
		      
			Query query = parser.parse(line);
		    System.out.println("Searching for: " + query.toString(field));
		            
		    if (repeat > 0) {                           // repeat & time as benchmark
		    	Date start = new Date();
		    	for (int i = 0; i < repeat; i++) {
		    		searcher.search(query, 100);
		    	}
		    	Date end = new Date();
		    	System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
		    }
		    doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);
	
	    	if (queryString != null) {
	    		break;
		    }
	    }
	    reader.close();
	}
	
	public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query, 
              int hitsPerPage, boolean raw, boolean interactive) throws IOException {

		TopDocs results = searcher.search(query, 5 * hitsPerPage);
		ScoreDoc[] hits = results.scoreDocs;
		
		int numTotalHits = results.totalHits;
		System.out.println(numTotalHits + " total matching documents");
		
		int start = 0;
		int end = Math.min(numTotalHits, hitsPerPage);
		
		while (true) {
		if (end > hits.length) {
			System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
			System.out.println("Collect more (y/n) ?");
			String line = in.readLine();
			if (line.length() == 0 || line.charAt(0) == 'n') {
				break;
			}
		
			hits = searcher.search(query, numTotalHits).scoreDocs;
		}
		
		end = Math.min(hits.length, start + hitsPerPage);
		
		for (int i = start; i < end; i++) {
			if (raw) {                              // output raw format
				System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
				continue;
			}
		
			Document doc = searcher.doc(hits[i].doc);
			String path = doc.get("path");
			String testLetters = doc.get("highlights");
			if (path != null) {
				System.out.println((i+1) + ". " + path);
				System.out.println(testLetters);
				String title = doc.get("title");
				if (title != null) {
					System.out.println("   Title: " + doc.get("title"));
				}
			} 
			else {
				System.out.println((i+1) + ". " + "No path for this document");
			}
		}
		
		if (!interactive || end == 0) {
		break;
		}
		if (numTotalHits >= end) {
			boolean quit = false;
			while (true) {
				System.out.print("Press ");
				if (start - hitsPerPage >= 0) {
					System.out.print("(p)revious page, ");  
				}
				if (start + hitsPerPage < numTotalHits) {
					System.out.print("(n)ext page, ");
				}
				System.out.println("(q)uit or enter number to jump to a page.");
			
				String line = in.readLine();
				if (line.length() == 0 || line.charAt(0)=='q') {
					quit = true;
					break;
				}
				if (line.charAt(0) == 'p') {
					start = Math.max(0, start - hitsPerPage);
					break;
				} 
				else if (line.charAt(0) == 'n') {
					if (start + hitsPerPage < numTotalHits) {
						start+=hitsPerPage;
					}
					break;
				} 
				else {
					int page = Integer.parseInt(line);
					if ((page - 1) * hitsPerPage < numTotalHits) {
					start = (page - 1) * hitsPerPage;
					break;
					} 
					else {
						System.out.println("No such page");
					}
				}
			}
			if (quit) break;
			end = Math.min(numTotalHits, start + hitsPerPage);
			}
		}
	}
	public static void main(String[] args)throws Exception{
		BufferedReader search=new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
		System.out.println("Enter Search");
		findWords=search.readLine();
		combIndex();
		combSearch();
	}
}
	
