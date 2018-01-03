package index;

import documentGeneration.GeneratedDocument;
import edu.stanford.nlp.ling.tokensregex.PhraseTable;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.base.Sys;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.*;

import org.apache.lucene.store.FSDirectory;
import org.openrdf.query.parser.QueryParser;


import java.io.IOException;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class OTFSearcher {

    private final int RESULTCOUNT = 1000;
    private final int SLOPFACTOR = 10000;
    private IndexSearcher searcher;
    private IndexReader reader;

    /**
     * Sets up the Searcher.
     * @throws IOException
     */
    public OTFSearcher(String pathToIndex) throws IOException {
        //open directory of the index
        FSDirectory indexDict = FSDirectory.open(Paths.get(pathToIndex));
        //create new indexsearcher for the index
        reader = DirectoryReader.open(indexDict);
        searcher = new IndexSearcher(reader);
    }

    public Set<String> urisToEntityName(String [] entityNames) throws IOException {
        Set<String> answers = new TreeSet<>();
        System.out.println(reader.numDocs());

        BooleanQuery.Builder bb = new BooleanQuery.Builder();
        for(String name : entityNames){
            PhraseQuery.Builder pb = new PhraseQuery.Builder();
            for(String part : name.split(" ")){
                pb.add(new Term("label", part.toLowerCase()));
            }
            bb.add(pb.build(), BooleanClause.Occur.SHOULD);
        }
        BooleanQuery bq = bb.build();
        System.out.println(bq.toString());
        TopDocs rs = searcher.search(bq, RESULTCOUNT);

        for(ScoreDoc doc : rs.scoreDocs){
            answers.add(reader.document(doc.doc).get("uri"));        }

        return answers;

    }
}
